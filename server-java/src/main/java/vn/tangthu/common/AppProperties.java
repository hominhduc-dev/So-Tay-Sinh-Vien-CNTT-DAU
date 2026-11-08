package vn.tangthu.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Cấu hình ứng dụng, khối {@code app:} trong application.yml. */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        @NotBlank String storageDir,
        @NotBlank String department,
        List<String> corsOrigins) {

    public record Jwt(
            @NotBlank @Size(min = 32, message = "JWT_SECRET phải dài ít nhất 32 ký tự") String secret,
            Duration ttl) {}
}
