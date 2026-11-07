package vn.tangthu.account;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tangthu.common.ApiException;
import vn.tangthu.common.AppProperties;
import vn.tangthu.common.JwtService;

/** M1 · Tài khoản: UC01, UC03, UC04. UC02 (đăng xuất) chỉ xóa token ở client. */
@Service
public class AuthService {
    private static final String BAD_LOGIN = "Thông tin đăng nhập không đúng.";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String department;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, AppProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.department = props.department();
    }

    public record LoginResult(String token, UserDto user) {}

    /** UC01. Mọi lý do thất bại trả cùng một thông báo để không lộ tài khoản nào tồn tại. */
    @Transactional(readOnly = true)
    public LoginResult login(String login, String password) {
        User u = users.findByLogin(login.trim())
                .filter(x -> x.canUseSystem(department))
                .filter(x -> x.login(password, encoder))
                .orElseThrow(() -> ApiException.unauthorized(BAD_LOGIN));
        return new LoginResult(jwt.issue(u.getId()), UserDto.of(u));
    }

    /** UC03 */
    @Transactional(readOnly = true)
    public UserDto me(UUID userId) {
        return users.findById(userId).map(UserDto::of)
                .orElseThrow(() -> ApiException.unauthorized("Cần đăng nhập."));
    }

    /** UC04 */
    @Transactional
    public void changePassword(UUID userId, String current, String next) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Cần đăng nhập."));
        if (!encoder.matches(current, u.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_PASSWORD", "Mật khẩu hiện tại không đúng.");
        }
        u.changePassword(encoder.encode(next));
    }
}
