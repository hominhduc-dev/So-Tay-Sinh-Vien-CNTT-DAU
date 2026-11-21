package vn.tangthu.common.socket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import vn.tangthu.account.model.Role;
import vn.tangthu.account.model.User;
import vn.tangthu.account.repository.UserRepository;
import vn.tangthu.common.config.AppProperties;
import vn.tangthu.common.exception.ApiException;
import vn.tangthu.common.security.AuthUser;
import vn.tangthu.common.security.JwtService;

/**
 * Bộ điều phối: nhận yêu cầu JSON đã tách khung, tìm action, xác thực, kiểm tra quyền, gọi
 * controller và đóng gói kết quả.
 *
 * <pre>
 * yêu cầu  {"id": 7, "action": "chat.ask", "token": "eyJ…", "data": {"questionId": "…"}}
 * phản hồi {"id": 7, "ok": true,  "data": {…}}
 *          {"id": 7, "ok": false, "error": {"code": "FORBIDDEN", "message": "Không đủ quyền."}}
 * </pre>
 *
 * BR01, BR02: với action cần đăng nhập, token chỉ mang userId; tài khoản được đọc lại từ CSDL ở
 * mỗi yêu cầu, nên khóa tài khoản hay đổi vai có hiệu lực ngay ở yêu cầu kế tiếp.
 */
@Component
public class SocketDispatcher {
    private static final Logger log = LoggerFactory.getLogger(SocketDispatcher.class);
    private static final Map<Role, Integer> LEVEL = Map.of(Role.USER, 0, Role.CONTENT_ADMIN, 1, Role.SYSTEM_ADMIN, 2);

    private record Route(Role minRole, SocketController.Handler handler) {}

    private final Map<String, Route> routes = new HashMap<>();
    private final ObjectMapper json;
    private final Validator validator;
    private final JwtService jwt;
    private final UserRepository users;
    private final String department;

    public SocketDispatcher(List<SocketController> controllers, ObjectMapper json, Validator validator,
                            JwtService jwt, UserRepository users, AppProperties props) {
        this.json = json;
        this.validator = validator;
        this.jwt = jwt;
        this.users = users;
        this.department = props.department();
        SocketController.Routes registry = new SocketController.Routes() {
            public void open(String action, SocketController.Handler h) { add(action, null, h); }
            public void secured(String action, Role role, SocketController.Handler h) { add(action, role, h); }
        };
        registry.open("system.ping", r -> Map.of("pong", true));
        controllers.forEach(c -> c.register(registry));
        log.info("Giao thức socket: {} action", routes.size());
    }

    private void add(String action, Role minRole, SocketController.Handler handler) {
        if (routes.putIfAbsent(action, new Route(minRole, handler)) != null) {
            throw new IllegalStateException("Action trùng: " + action);
        }
    }

    /** Xử lý một khung yêu cầu, luôn trả về một khung phản hồi (không ném lỗi ra ngoài). */
    public byte[] handle(byte[] frame) {
        JsonNode id = null;
        try {
            JsonNode req;
            try {
                req = json.readTree(frame);
            } catch (Exception e) {
                throw new ApiException("BAD_FRAME", "Thông điệp không phải JSON hợp lệ.");
            }
            if (req == null || !req.isObject()) throw new ApiException("BAD_FRAME", "Thông điệp phải là đối tượng JSON.");
            id = req.get("id");
            String action = req.path("action").asText("");
            Route route = routes.get(action);
            if (route == null) throw new ApiException("UNKNOWN_ACTION", "Không có action '" + action + "'.");
            AuthUser me = route.minRole() == null ? null : authorize(req.path("token").asText(null), route.minRole());
            Object result = route.handler().handle(new SocketRequest(action, req.get("data"), me, json, validator));
            ObjectNode res = json.createObjectNode();
            res.set("id", id);
            res.put("ok", true);
            res.set("data", json.valueToTree(result));
            return json.writeValueAsBytes(res);
        } catch (ApiException e) {
            return error(id, e.getCode(), e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.warn("Vi phạm ràng buộc dữ liệu: {}", e.getMostSpecificCause().getMessage());
            return error(id, "CONFLICT", "Dữ liệu xung đột với bản ghi đã có.");
        } catch (Exception e) {
            log.error("Lỗi xử lý yêu cầu socket", e);
            return error(id, "INTERNAL_ERROR", "Lỗi máy chủ.");
        }
    }

    private AuthUser authorize(String token, Role minRole) {
        Optional<User> user = Optional.ofNullable(token).flatMap(jwt::parseUserId).flatMap(users::findById)
                .filter(u -> u.canUseSystem(department));
        User u = user.orElseThrow(() -> ApiException.unauthorized("Cần đăng nhập."));
        if (LEVEL.get(u.getRole()) < LEVEL.get(minRole)) throw ApiException.forbidden("Không đủ quyền.");
        return new AuthUser(u.getId(), u.getRole());
    }

    public byte[] error(JsonNode id, String code, String message) {
        ObjectNode res = json.createObjectNode();
        res.set("id", id);
        res.put("ok", false);
        res.putObject("error").put("code", code).put("message", message);
        try {
            return json.writeValueAsBytes(res);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
