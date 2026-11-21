package vn.tangthu.admin.controller;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.tangthu.account.model.Role;
import vn.tangthu.admin.dto.Health;
import vn.tangthu.admin.dto.UpdateUserRequest;
import vn.tangthu.admin.service.AdminService;
import vn.tangthu.common.dto.PageResponse;
import vn.tangthu.common.exception.ApiException;
import vn.tangthu.common.socket.SocketController;

/**
 * M4 · Quản trị — UC14–UC16, UC20 (chỉ quản trị viên).
 *
 * <pre>
 * user.list      {q?, page?, size?}     → {items, page, size, total}
 * user.update    {id, active?, role?}   → user        (BR04: không tự khóa / tự đổi vai)
 * system.health  —                      → {status, database, uptimeSeconds, time}
 * </pre>
 */
@Component
public class AdminSocketController implements SocketController {
    private final AdminService admin;
    private final JdbcTemplate jdbc;

    public AdminSocketController(AdminService admin, JdbcTemplate jdbc) {
        this.admin = admin;
        this.jdbc = jdbc;
    }

    @Override
    public void register(Routes routes) {
        routes.secured("user.list", Role.SYSTEM_ADMIN, r -> {
            String q = r.text("q");
            if (q != null && q.length() > 200) throw ApiException.validation("q tối đa 200 ký tự.");
            return admin.search(q, PageResponse.request(r.integer("page", 0), r.integer("size", 20), Sort.by("code")));
        });
        routes.secured("user.update", Role.SYSTEM_ADMIN, r -> {
            UpdateUserRequest req = r.body(UpdateUserRequest.class);
            return admin.update(r.me().id(), r.uuid("id"), req.active(), req.role());
        });
        routes.secured("system.health", Role.SYSTEM_ADMIN, r -> health());
    }

    /** UC20 — lỗi CSDL không làm yêu cầu lỗi theo, chỉ báo database: DOWN. */
    private Health health() {
        boolean dbUp;
        try {
            Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
            dbUp = one != null && one == 1;
        } catch (RuntimeException e) {
            dbUp = false;
        }
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        return new Health(dbUp ? "UP" : "DEGRADED", dbUp ? "UP" : "DOWN", uptime, Instant.now());
    }
}
