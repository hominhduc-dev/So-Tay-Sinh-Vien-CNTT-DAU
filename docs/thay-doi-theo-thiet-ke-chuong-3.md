# Thay đổi cần áp dụng vào project theo thiết kế Chương 3

**Ngày lập:** 31/10/2026 · **Nguồn:** bộ slide Chương 3 (BFD, IFD, DFD, ERD, biểu đồ lớp, CSDL).

Chương 3 là kết quả phân tích thiết kế: nó quy định hệ thống **phải** như thế nào. Mã nguồn hiện tại
được viết trước bản thiết kế này nên có chỗ lệch. Tài liệu này liệt kê từng chỗ lệch và việc cần làm
để đưa project về đúng thiết kế. Khi thiết kế và mã nguồn khác nhau, **thiết kế là chuẩn**.

---

## 1. Vai trò lưu trên người dùng, bỏ bảng nối `department_members`

### Thiết kế (ERD, biểu đồ lớp, CSDL)

- Mỗi người dùng **thuộc đúng một đơn vị** (Đơn vị 1 — N Người dùng).
- Mỗi người dùng có **đúng một vai trò**: `USER | CONTENT_ADMIN | SYSTEM_ADMIN`, lưu ngay trên `users`.
- Phân quyền (UC16) là **đổi vai trò của một tài khoản**: `User.assignRole()`.
- CSDL còn **14 bảng** (bỏ `department_members`), vẫn 5 enum (`member_role` giữ nguyên giá trị, có thể đổi tên thành `role`).

### Hiện trạng

- Quan hệ User — Department là **M:N** qua bảng `department_members (user_id, department_id, role)`.
- Vai hiệu dụng được suy ra bằng `effectiveRole()` từ danh sách tư cách (`server/src/lib/roles.ts`).
- Phân quyền qua `POST /api/departments/:id/members` và `DELETE /api/departments/:id/members/:userId`.

### Việc cần làm

**CSDL — `server/prisma/`**

1. `schema.prisma`, model `User`: thêm
   - `role MemberRole @default(USER)`
   - `departmentId String @map("department_id") @db.Uuid` + quan hệ tới `Department` (`onDelete: Restrict`).
2. Xóa model `DepartmentMember`; bỏ `memberships` ở `User` và `members` ở `Department`, thêm `users User[]` ở `Department`.
3. Migration mới, theo thứ tự:
   1. Thêm cột `role`, `department_id` (cho phép NULL tạm thời).
   2. Chuyển dữ liệu: mỗi user lấy vai cao nhất trong các tư cách thuộc khoa CNTT (đúng quy tắc của `effectiveRole`), `department_id` = id khoa CNTT.
   3. User **không có** tư cách CNTT: quyết định trước khi chạy — khóa (`is_active = false`) hay xóa. Ghi rõ vào migration.
   4. Đặt `department_id` NOT NULL, tạo chỉ mục `users(department_id)`.
   5. Xóa bảng `department_members`.
   - Sao lưu DB trước khi chạy trên dữ liệu thật; **không** dùng seed để nâng cấp.
4. `prisma/check.ts`: bỏ `department_members` khỏi danh sách bảng, thêm kiểm tra cột `users.role`, `users.department_id`.
5. `prisma/seed.ts`, `scripts/bootstrap-admin.ts`: tạo user với `role`, `departmentId` thay vì `departmentMember.upsert`.

**Backend — `server/src/`**

| Tệp | Thay đổi |
|---|---|
| `middleware/auth.middleware.ts` | Đọc `user.role`, `user.department` thay vì `memberships`. BR01: chặn khi `isActive = false` hoặc đơn vị không phải CNTT. BR02 giữ nguyên: mỗi request đọc lại `role` từ CSDL. |
| `lib/roles.ts` | Bỏ `effectiveRole()`; các hàm `canManageContent`, `assertContentAdmin`, `assertSystemAdmin` giữ nguyên. |
| `types/express.d.ts` | `AuthenticatedUser`: một `role`, một `department`; bỏ danh sách `departments`. |
| `modules/auth/auth.service.ts`, `auth.controller.ts` | Bỏ `MembershipDto`, `toMembershipDtos`. `login` và `GET /auth/me` trả `{ token?, user }`, trong `user` có `role` và `department`. |
| `modules/admin/admin.service.ts`, `admin.schema.ts`, `admin.route.ts` | Thay `ganThanhVien` / `goThanhVien` bằng `assignRole(userId, role)`. API mới: `PATCH /api/users/:id` nhận `{ role?, isActive? }`. Bỏ `POST/DELETE /api/departments/:id/members`. Giữ BR04: không tự khóa, không tự hạ vai. |
| `eval/run-eval.ts` | Tạo người dùng giả lập với `role`, không còn `departments[]`. |
| Test (`*.test.ts`) | Cập nhật fixture tạo user; thêm test: đổi vai có hiệu lực ở request kế tiếp, chặn tự hạ vai. |

**Frontend — `web/src/features/`**

| Tệp | Thay đổi |
|---|---|
| `auth/useAuth.ts` | Phiên chỉ còn `{ user }`; bỏ kiểu `Membership`, hàm `useMemberships()`. |
| `admin/api.ts` | Bỏ `UserMembership`; `doiVai()` gọi `PATCH /api/users/:id` với `{ role }`. |
| `admin/UserTable.tsx` | Hiển thị và đổi một vai cho mỗi tài khoản. |
| `documents/DocumentManager.tsx` | Lấy quyền từ `user.role` thay vì `useMemberships()`. |

**Tài liệu — `docs/`**

- `erd.md`: quan hệ users — departments 1:N, bỏ `department_members`, tổng 14 bảng.
- `api-contract.md`: sửa mục đăng nhập / `me` (bỏ `memberships`), mục quản trị (thêm `PATCH /users/:id`, bỏ `/departments/:id/members`).
- `phan-quyen.md`, `use-cases/README.md` (UC16): phân quyền = đổi vai của tài khoản.

---

## 2. Tên phương thức theo biểu đồ lớp

Biểu đồ lớp đặt tên phương thức thống nhất bằng tiếng Anh. Mã hiện tại trộn tên tiếng Việt và tiếng Anh.
Đổi tên để code đọc khớp thiết kế (không đổi hành vi):

| Lớp | Thiết kế | Hiện tại |
|---|---|---|
| User | `login()` | `auth.service.login` |
| User | `changePassword()` | `auth.service.changePassword` |
| User | `assignRole()` | `admin.service.ganThanhVien` + `goThanhVien` (gộp, xem mục 1) |
| Conversation | `list()` | `chat.service.danhSachHoiThoai` |
| Conversation | `getDetail()` | `chat.service.chiTietHoiThoai` |
| Message | `ask()` | `chat.service.hoi` |
| Document | `upload()` | `documents.service.create` |
| Document | `update()`, `remove()`, `retry()` | giữ nguyên |
| IngestJob | `process()` | `worker/ingest.worker.chayMotLuot` |
| Chunk | `split()` | `rag/chunk.catDoan` |
| Chunk | `search()` | `retrieval.service.search` |

Mục này ưu tiên thấp hơn mục 1: chỉ đổi tên, có thể làm sau cùng.

---

## 3. Năm module theo BFD

BFD chia hệ thống thành 5 module. Cấu trúc thư mục `server/src/modules/` nên khớp với cách chia này:

| Module (BFD) | Use case | Thư mục hiện tại | Việc cần làm |
|---|---|---|---|
| M1 · Tài khoản | UC01–UC04 | `modules/auth` | Giữ nguyên |
| M2 · Hỏi đáp | UC05, UC06, UC08 | `modules/chat` | Giữ nguyên |
| M3 · Tài liệu | UC07, UC09–UC13 | `modules/documents`, `worker/` | Giữ nguyên; worker là phần xử lý nền của M3 |
| M4 · Quản trị | UC14–UC16 | `modules/admin` | Áp dụng mục 1 (phân quyền = đổi `users.role`) |
| M5 · Vận hành | UC18–UC20 | `modules/retrieval`, `eval/`, route `/health` | Gom về một module `operations` (hoặc ghi rõ ánh xạ trong `docs/cau-truc-thu-muc.md`) |

So với phần SRS (4 phân hệ PH1–PH4), phân hệ PH1 được tách thành M1 Tài khoản và M4 Quản trị.

---

## 4. Thứ tự đề xuất

1. Mục 1 — CSDL: schema + migration + check, chạy trên DB thử nghiệm riêng.
2. Mục 1 — backend, rồi frontend; chạy toàn bộ test.
3. Cập nhật tài liệu trong `docs/`.
4. Mục 2 — đổi tên phương thức.
5. Chạy lại kịch bản demo 5 bước ở slide 3.5 trên DB thử nghiệm trước khi áp dụng cho DB thật.
