package vn.tangthu.common.socket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import vn.tangthu.common.exception.ApiException;
import vn.tangthu.common.security.AuthUser;

/**
 * Một yêu cầu đã tách khung: {@code {"id", "action", "token", "data"}}, kèm người gọi đã xác thực.
 * Controller socket đọc tham số qua các hàm dưới đây.
 */
public final class SocketRequest {
    private final String action;
    private final JsonNode data;
    private final AuthUser me;
    private final ObjectMapper json;
    private final Validator validator;

    SocketRequest(String action, JsonNode data, AuthUser me, ObjectMapper json, Validator validator) {
        this.action = action;
        this.data = data == null ? NullNode.getInstance() : data;
        this.me = me;
        this.json = json;
        this.validator = validator;
    }

    public String action() { return action; }

    /** Người gọi; chỉ null với action công khai (đăng nhập, ping). */
    public AuthUser me() { return me; }

    /** Chuyển {@code data} thành record DTO và kiểm tra ràng buộc {@code @NotBlank}, {@code @Size}… */
    public <T> T body(Class<T> type) {
        T value;
        try {
            value = json.treeToValue(data.isNull() ? json.createObjectNode() : data, type);
        } catch (Exception e) {
            throw ApiException.validation("Dữ liệu không hợp lệ.");
        }
        Set<ConstraintViolation<T>> errors = validator.validate(value);
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted().collect(Collectors.joining("; ")));
        }
        return value;
    }

    public String text(String field) {
        JsonNode n = data.get(field);
        return n == null || n.isNull() ? null : n.asText();
    }

    public UUID uuid(String field) {
        String s = text(field);
        if (s == null) throw ApiException.validation("Thiếu " + field + ".");
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw ApiException.validation(field + " không hợp lệ.");
        }
    }

    public int integer(String field, int defaultValue) {
        JsonNode n = data.get(field);
        if (n == null || n.isNull()) return defaultValue;
        if (!n.canConvertToInt()) throw ApiException.validation(field + " phải là số nguyên.");
        return n.asInt();
    }
}
