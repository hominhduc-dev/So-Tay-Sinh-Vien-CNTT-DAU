# M4 · Quản trị — client

**Người phụ trách:** _Họ tên — @github_

**Use case:** UC14 Tra cứu người dùng, UC15 Khóa / mở, UC16 Phân quyền, UC20 Trạng thái hệ thống

**Action gửi tới server:** `user.list`, `user.update`, `system.health`, `system.ping`

| Thư mục | Tầng | Lớp |
|---|---|---|
| `model/` | Model — record nhận từ JSON | Health (dùng User, Role của M1) |
| `controller/` | Controller — gửi action qua `SocketClient` | AdminController |
| `view/` | View — màn hình Swing | UsersPanel, HealthPanel |

Phía server: [`server/src/main/java/vn/tangthu/admin/`](../../../../../../../../server/src/main/java/vn/tangthu/admin/).
