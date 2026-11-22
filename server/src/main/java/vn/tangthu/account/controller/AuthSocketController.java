package vn.tangthu.account.controller;

import java.util.Map;
import org.springframework.stereotype.Component;
import vn.tangthu.account.dto.ChangePasswordRequest;
import vn.tangthu.account.dto.LoginRequest;
import vn.tangthu.account.model.Role;
import vn.tangthu.account.service.AuthService;
import vn.tangthu.common.socket.SocketController;

/**
 * M1 · Tài khoản — UC01, UC03, UC04. UC02 (đăng xuất) chỉ là client bỏ token và đóng kết nối.
 *
 * <pre>
 * auth.login           {login, password}               → {token, user}       công khai
 * auth.me              —                               → {user}              mọi vai
 * auth.changePassword  {currentPassword, newPassword}  → {}                  mọi vai
 * </pre>
 */
@Component
public class AuthSocketController implements SocketController {
    private final AuthService auth;

    public AuthSocketController(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public void register(Routes routes) {
        routes.open("auth.login", r -> {
            LoginRequest req = r.body(LoginRequest.class);
            return auth.login(req.login(), req.password());
        });
        routes.secured("auth.me", Role.USER, r -> Map.of("user", auth.me(r.me().id())));
        routes.secured("auth.changePassword", Role.USER, r -> {
            ChangePasswordRequest req = r.body(ChangePasswordRequest.class);
            auth.changePassword(r.me().id(), req.currentPassword(), req.newPassword());
            return Map.of();
        });
    }
}
