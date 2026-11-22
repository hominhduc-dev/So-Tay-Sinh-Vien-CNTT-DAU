# M1 · Tài khoản — server

**Người phụ trách:** _Họ tên — @github_

**Use case:** UC01 Đăng nhập, UC02 Đăng xuất, UC03 Xem thông tin cá nhân, UC04 Đổi mật khẩu

**Action socket:** `auth.login`, `auth.me`, `auth.changePassword`

| Thư mục | Tầng | Lớp |
|---|---|---|
| `model/` | Model — entity JPA | Role, User |
| `repository/` | Model — truy vấn CSDL | UserRepository |
| `service/` | Nghiệp vụ | AuthService |
| `controller/` | Controller — đăng ký action socket | AuthSocketController |
| `dto/` | Dữ liệu `data` gửi / nhận (JSON) | ChangePasswordRequest, LoginRequest, UserDto |

Giao diện (View) của module: [`client/src/main/java/vn/tangthu/client/account/`](../../../../../../../client/src/main/java/vn/tangthu/client/account/).
