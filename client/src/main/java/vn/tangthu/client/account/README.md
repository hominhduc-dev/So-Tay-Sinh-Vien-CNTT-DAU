# M1 · Tài khoản — client

**Người phụ trách:** _Họ tên — @github_

**Use case:** UC01 Đăng nhập, UC02 Đăng xuất, UC03 Đổi mật khẩu, UC04 Xem thông tin cá nhân

**Action gửi tới server:** `auth.login`, `auth.me`, `auth.changePassword`

| Thư mục | Tầng | Lớp |
|---|---|---|
| `model/` | Model — record nhận từ JSON | Role, User, LoginResult, Session |
| `controller/` | Controller — gửi action qua `SocketClient` | AuthController |
| `view/` | View — màn hình Swing | LoginPanel, ChangePasswordDialog |

Phía server: [`server/src/main/java/vn/tangthu/account/`](../../../../../../../../server/src/main/java/vn/tangthu/account/).
