# M4 · Quản trị — server

**Người phụ trách:** _Họ tên — @github_

**Use case:** UC14 Tra cứu người dùng, UC15 Khóa / mở tài khoản, UC16 Phân quyền, UC20 Kiểm tra trạng thái hệ thống

**API:** `GET /api/users`, `PATCH /api/users/{id}`, `GET /api/health`

| Thư mục | Tầng | Lớp |
|---|---|---|
| `service/` | Nghiệp vụ | AdminService |
| `controller/` | Controller — REST API | AdminController, HealthController |
| `dto/` | Dữ liệu request / response JSON | Health, UpdateUserRequest |

Module này không có `model/` riêng: dùng `User`, `Role` của M1 (`account/model/`).

Giao diện (View) của module: [`client/src/modules/admin/`](../../../../../../../client/src/modules/admin/).
