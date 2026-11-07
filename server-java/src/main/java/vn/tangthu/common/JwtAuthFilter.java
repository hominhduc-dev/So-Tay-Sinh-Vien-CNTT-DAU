package vn.tangthu.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.tangthu.account.User;
import vn.tangthu.account.UserRepository;

/**
 * Đọc header {@code Authorization: Bearer <jwt>}, nạp lại tài khoản từ CSDL ở MỖI request (BR02)
 * và chỉ xác thực khi tài khoản đang hoạt động, thuộc khoa CNTT (BR01).
 * Token sai thì bỏ qua; endpoint cần đăng nhập sẽ trả 401.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;
    private final RoleHierarchy roleHierarchy;
    private final String department;

    public JwtAuthFilter(JwtService jwt, UserRepository users, RoleHierarchy roleHierarchy, AppProperties props) {
        this.jwt = jwt;
        this.users = users;
        this.roleHierarchy = roleHierarchy;
        this.department = props.department();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            jwt.parseUserId(header.substring(7))
                    .flatMap(users::findById)
                    .filter(u -> u.canUseSystem(department))
                    .ifPresent(this::authenticate);
        }
        chain.doFilter(req, res);
    }

    private void authenticate(User u) {
        var authorities = roleHierarchy.getReachableGrantedAuthorities(
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
        var auth = new UsernamePasswordAuthenticationToken(new AuthUser(u.getId(), u.getRole()), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
