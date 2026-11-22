# M4 · Quản trị — server

**Người phụ trách:** _Họ tên — @github_

**Use case:** UC14 Tra cứu người dùng, UC15 Khóa / mở tài khoản, UC16 Phân quyền, UC20 Kiểm tra trạng thái hệ thống

**Action socket:** `user.list`, `user.update`, `system.health`

| Thư mục | Tầng | Lớp |
|---|---|---|
| `service/` | Nghiệp vụ | AdminService |
| `controller/` | Controller — đăng ký action socket | AdminSocketController |
| `dto/` | Dữ liệu `data` gửi / nhận (JSON) | Health, UpdateUserRequest |

Module này không có `model/` riêng: dùng `User`, `Role` của M1 (`account/model/`).

Giao diện (View) của module: [`client/src/main/java/vn/tangthu/client/admin/`](../../../../../../../client/src/main/java/vn/tangthu/client/admin/).
