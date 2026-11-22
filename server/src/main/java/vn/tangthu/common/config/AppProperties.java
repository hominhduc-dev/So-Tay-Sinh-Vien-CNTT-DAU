package vn.tangthu.common.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Cấu hình ứng dụng, khối {@code app:} trong application.yml. */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        @NotBlank String storageDir,
        @NotBlank String department,
        Socket socket) {

    public record Jwt(
            @NotBlank @Size(min = 32, message = "JWT_SECRET phải dài ít nhất 32 ký tự") String secret,
            Duration ttl) {}

    /**
     * Máy chủ TCP socket.
     *
     * @param port        cổng lắng nghe; 0 = hệ điều hành tự chọn (dùng khi kiểm thử)
     * @param threads     số luồng phục vụ đồng thời (mỗi kết nối chiếm một luồng)
     * @param maxFrameMb  kích thước tối đa một khung thông điệp
     * @param idleTimeout đóng kết nối không gửi gì trong khoảng này
     */
    public record Socket(@Min(0) @Max(65535) int port, @Min(1) int threads, @Min(1) int maxFrameMb,
                         Duration idleTimeout) {}
}
