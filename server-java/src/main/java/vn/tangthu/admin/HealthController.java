package vn.tangthu.admin;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC20 — Kiểm tra trạng thái hệ thống. Lỗi CSDL không làm endpoint lỗi theo. */
@RestController
public class HealthController {
    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Health(String status, String database, long uptimeSeconds, Instant time) {}

    @GetMapping("/api/health")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Health health() {
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
