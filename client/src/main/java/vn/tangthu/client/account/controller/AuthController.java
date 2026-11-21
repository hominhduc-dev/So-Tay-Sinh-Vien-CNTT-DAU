package vn.tangthu.client.account.controller;

import vn.tangthu.client.account.model.LoginResult;
import vn.tangthu.client.account.model.User;
import vn.tangthu.client.common.net.SocketClient;

/** M1 · Tài khoản — UC01 đăng nhập, UC02 đăng xuất, UC03 đổi mật khẩu, UC04 xem thông tin. */
public class AuthController {
    private final SocketClient client;

    public AuthController(SocketClient client) {
        this.client = client;
    }

    public LoginResult login(String login, String password) {
        LoginResult r = client.call("auth.login", SocketClient.params("login", login, "password", password),
                LoginResult.class);
        client.setToken(r.token());
        return r;
    }

    public User me() {
        return SocketClient.convert(client.call("auth.me", null).get("user"), User.class);
    }

    public void changePassword(String currentPassword, String newPassword) {
        client.call("auth.changePassword",
                SocketClient.params("currentPassword", currentPassword, "newPassword", newPassword));
    }

    public void logout() {
        client.close();
    }
}
