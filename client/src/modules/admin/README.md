# M4 · Quản trị — client

**Người phụ trách:** _Họ tên — @github_

**Trang:** `/admin/users`, `/admin/health`, `/admin/permissions` · **Use case:** UC14 Tra cứu người dùng, UC15 Khóa / mở tài khoản, UC16 Phân quyền, UC20 Kiểm tra trạng thái hệ thống

| Thư mục / tệp | Vai trò | Nội dung |
|---|---|---|
| `views/` | View — giao diện | AdminDashboard, HealthPanel, PermissionMatrix, UserTable |
| `api.ts`, `permissions.ts` | Gọi REST API của module cùng tên ở server | |

Phía server: [`server/src/main/java/vn/tangthu/admin/`](../../../../server/src/main/java/vn/tangthu/admin/).
