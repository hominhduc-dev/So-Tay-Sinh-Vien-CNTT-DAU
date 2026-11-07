package vn.tangthu.common;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Cấp và đọc JWT. Token chỉ chứa userId; vai luôn đọc lại từ CSDL (BR02). */
@Service
public class JwtService {
    private final SecretKey key;
    private final AppProperties.Jwt cfg;

    public JwtService(AppProperties props) {
        this.cfg = props.jwt();
        this.key = Keys.hmacShaKeyFor(cfg.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issue(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(cfg.ttl())))
                .signWith(key)
                .compact();
    }

    /** Rỗng nếu token sai chữ ký, hết hạn hoặc hỏng. */
    public Optional<UUID> parseUserId(String token) {
        try {
            String sub = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().getSubject();
            return Optional.of(UUID.fromString(sub));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
