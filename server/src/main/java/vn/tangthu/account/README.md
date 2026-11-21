# M1 · Tài khoản — server

**Người phụ trách:** _Họ tên — @github_

**Use case:** UC01 Đăng nhập, UC02 Đăng xuất, UC03 Xem thông tin cá nhân, UC04 Đổi mật khẩu

**API:** `POST /api/auth/login`, `GET /api/auth/me`, `PUT /api/auth/password`

| Thư mục | Tầng | Lớp |
|---|---|---|
| `model/` | Model — entity JPA | Role, User |
| `repository/` | Model — truy vấn CSDL | UserRepository |
| `service/` | Nghiệp vụ | AuthService |
| `controller/` | Controller — REST API | AuthController |
| `dto/` | Dữ liệu request / response JSON | ChangePasswordRequest, LoginRequest, UserDto |

Giao diện (View) của module: [`client/src/modules/account/`](../../../../../../../client/src/modules/account/).
