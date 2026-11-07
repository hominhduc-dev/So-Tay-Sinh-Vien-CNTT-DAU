# Thay đổi cần áp dụng vào project theo thiết kế Chương 3

**Ngày lập:** 31/10/2026 · **Nguồn:** bộ slide Chương 3 (BFD, IFD, DFD, ERD, biểu đồ lớp, CSDL).

Chương 3 là kết quả phân tích thiết kế: nó quy định hệ thống **phải** như thế nào. Mã nguồn hiện tại
được viết trước bản thiết kế này nên có chỗ lệch. Tài liệu này liệt kê từng chỗ lệch và việc cần làm
để đưa project về đúng thiết kế. Khi thiết kế và mã nguồn khác nhau, **thiết kế là chuẩn**.

**Mức thiết kế: client–server.** Chương 3 chỉ mô tả hai tầng: client (trình duyệt của sinh viên,
giáo vụ, quản trị viên) gửi request và server trả response; CSDL và mọi xử lý nằm ở server. Phần
xử lý bên trong server (worker nạp tài liệu, đoạn văn và vector, hàng đợi `ingest_jobs`, pgvector)
không xuất hiện trong thiết kế. Vì vậy CSDL trong thiết kế có **7 bảng** ứng với 7 thực thể ERD;
3 bảng `chunks`, `chunk_embeddings`, `ingest_jobs` vẫn giữ trong mã nguồn (tổng **10 bảng** ở các
mục dưới) và **không** tính là chỗ lệch cần sửa.

---

## 1. Vai trò lưu trên người dùng, bỏ bảng nối `department_members`

### Thiết kế (ERD, biểu đồ lớp, CSDL)

- Mỗi người dùng **thuộc đúng một đơn vị** (Đơn vị 1 — N Người dùng).
- Mỗi người dùng có **đúng một vai trò**: `USER | CONTENT_ADMIN | SYSTEM_ADMIN`, lưu ngay trên `users`.
- Phân quyền (UC16) là **đổi vai trò của một tài khoản**: `User.assignRole()`.
- Bỏ `department_members` (−1 bảng); cộng thêm bảng cấu hình AI ở mục 2 (+1 bảng), CSDL còn **10 bảng** sau khi bỏ thêm 5 bảng đánh giá ở mục 5, 5 enum (`member_role` giữ nguyên giá trị, có thể đổi tên thành `role`).

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
| `eval/run-eval.ts` | Bỏ cùng bộ đánh giá (mục 5); nếu giữ làm công cụ nội bộ thì tạo người dùng giả lập với `role`. |
| Test (`*.test.ts`) | Cập nhật fixture tạo user; thêm test: đổi vai có hiệu lực ở request kế tiếp, chặn tự hạ vai. |

**Frontend — `web/src/features/`**

| Tệp | Thay đổi |
|---|---|
| `auth/useAuth.ts` | Phiên chỉ còn `{ user }`; bỏ kiểu `Membership`, hàm `useMemberships()`. |
| `admin/api.ts` | Bỏ `UserMembership`; `doiVai()` gọi `PATCH /api/users/:id` với `{ role }`. |
| `admin/UserTable.tsx` | Hiển thị và đổi một vai cho mỗi tài khoản. |
| `documents/DocumentManager.tsx` | Lấy quyền từ `user.role` thay vì `useMemberships()`. |

**Tài liệu — `docs/`**

- `erd.md`: quan hệ users — departments 1:N, bỏ `department_members`, thêm `ai_configs` (mục 2), bỏ 5 bảng đánh giá (mục 5), tổng 10 bảng.
- `api-contract.md`: sửa mục đăng nhập / `me` (bỏ `memberships`), mục quản trị (thêm `PATCH /users/:id`, bỏ `/departments/:id/members`).
- `phan-quyen.md`, `use-cases/README.md` (UC16): phân quyền = đổi vai của tài khoản.

---

## 2. Dịch vụ AI do quản trị viên cấu hình (UC21)

### Thiết kế (BFD, IFD, DFD, ERD, biểu đồ lớp, CSDL)

- Hệ thống chỉ có **3 tác nhân**: Sinh viên CNTT, Giáo vụ khoa, Quản trị viên. Gemini API **không** còn là tác nhân ngoài; nó là thành phần bên trong hệ thống.
- Quản trị viên cấu hình dịch vụ AI qua chức năng mới **UC21 Cấu hình dịch vụ AI** (module M4 Quản trị).
- ERD có thêm tập thực thể **Cấu hình AI** (Người dùng 1 — n Cấu hình AI, tham gia toàn phần); biểu đồ lớp có thêm lớp `AIConfiguration` (`model`, `apiKey`, `update()`) và phương thức `User.configureAI()`.

### Hiện trạng

- Khóa và model đọc từ biến môi trường: `GEMINI_API_KEY`, `GEMINI_EMBEDDING_MODEL`, `GEMINI_GENERATION_MODEL`, `GEMINI_USE_TASK_TYPE` (`.env.example`, `server/src/config/env.ts`). Đổi cấu hình phải sửa `.env` và khởi động lại dịch vụ.

### Việc cần làm

1. **CSDL:** bảng mới `ai_configs` — `id`, `provider`, `embedding_model`, `generation_model`, `api_key_encrypted`, `use_task_type`, `is_active`, `updated_by` (FK `users`), `updated_at`. Chỉ một dòng `is_active = true` tại một thời điểm.
   - Khóa API **mã hóa** trước khi lưu (khóa mã hóa lấy từ biến môi trường, không lưu trong DB); API không bao giờ trả lại khóa đầy đủ, chỉ trả 4 ký tự cuối.
   - Lần chạy đầu: migration/seed tạo một dòng từ các biến `GEMINI_*` hiện có để không gián đoạn.
2. **Backend:** `GET /api/admin/ai-config`, `PUT /api/admin/ai-config` (chỉ `SYSTEM_ADMIN`). `rag/embed.ts`, `rag/generate.ts` và worker đọc cấu hình đang hoạt động từ DB (có cache, làm mới khi cập nhật) thay vì `env`.
   - Lưu ý: đổi **model embedding** làm vector cũ không dùng chung được. Thiết kế đã tách `chunk_embeddings` theo cột `model`; khi đổi model phải chạy lại nhúng vector cho tài liệu.
3. **Frontend:** trang `/admin/ai` cho quản trị viên: chọn model, nhập khóa mới, xem trạng thái kết nối.
4. **Tài liệu:** `use-cases/README.md` thêm UC21; `api-contract.md` thêm hai API trên; `.env.example` ghi rõ `GEMINI_*` chỉ dùng cho lần khởi tạo đầu.

---

## 3. Tên phương thức theo biểu đồ lớp

Biểu đồ lớp đặt tên phương thức thống nhất bằng tiếng Anh, theo cú pháp UML
`+tên(in tham số: kiểu): kiểu trả về`. Mã hiện tại trộn tên tiếng Việt và tiếng Anh.
Đổi tên để code đọc khớp thiết kế (không đổi hành vi):

| Lớp | Thiết kế | Hiện tại |
|---|---|---|
| Department | `getDocuments(): Document[]`, `getMembers(): User[]` | truy vấn trong `admin.service`, `documents.service` |
| User | `login(in pwd: string): boolean` | `auth.service.login` |
| User | `assignRole(in role: Role): void` | `admin.service.ganThanhVien` + `goThanhVien` (gộp, xem mục 1) |
| AIConfiguration | `update(in model: string): void`, `getModel(): string` | chưa có (xem mục 2) |
| Conversation | `ask(in question: string): Message` | `chat.service.hoi` |
| Conversation | `getMessages(): Message[]` | `chat.service.chiTietHoiThoai` |
| Message | `getContent(): string` | đọc trực tiếp cột `content` |
| Message | `getCitations(): MessageCitation[]` | đọc kèm trong `chat.service.chiTietHoiThoai` |
| MessageCitation | `getSource(): Document` | `documents.service.get` (kèm trang, câu trích lưu trên trích dẫn) |
| Document | `upload(in file: File): void` | `documents.service.create` |
| Document | `retry(): void` | `documents.service.retry` |

Các thao tác còn lại (đổi mật khẩu, sửa/gỡ tài liệu, truy hồi) giữ tên hiện tại; biểu đồ lớp chỉ ghi phương thức tiêu biểu. Đoạn văn và vector (`chunks`, `chunk_embeddings`) cùng hàng đợi xử lý nền (`ingest_jobs`, `worker/ingest.worker.ts`) là thành phần kỹ thuật, không có trong ERD và biểu đồ lớp; giữ nguyên trong mã nguồn và CSDL.

Mục này ưu tiên thấp hơn mục 1 và 2: chỉ đổi tên, có thể làm sau cùng.

---

## 4. Bốn module theo BFD

BFD chia hệ thống thành 4 module. Cấu trúc thư mục `server/src/modules/` nên khớp với cách chia này:

| Module (BFD) | Use case | Thư mục hiện tại | Việc cần làm |
|---|---|---|---|
| M1 · Tài khoản | UC01–UC04 | `modules/auth` | Giữ nguyên |
| M2 · Hỏi đáp | UC05, UC06, UC08 | `modules/chat` | Giữ nguyên; truy hồi tài liệu (`modules/retrieval`) là phần xử lý bên trong của M2, không có API riêng |
| M3 · Tài liệu | UC07, UC09–UC13 | `modules/documents`, `worker/` | Giữ nguyên; worker là phần xử lý nền của M3 |
| M4 · Quản trị | UC14–UC16, UC20, UC21 | `modules/admin`, route `/health` | Áp dụng mục 1 (phân quyền = đổi `users.role`) và mục 2 (cấu hình AI) |

So với phần SRS (4 phân hệ PH1–PH4), phân hệ PH1 được tách thành M1 Tài khoản và M4 Quản trị; phân hệ PH4 Vận hành kỹ thuật bị bỏ, UC20 chuyển vào M4 (xem mục 5).

---

## 5. Bỏ UC18 (tra cứu lai qua API) và UC19 (đánh giá truy hồi)

### Thiết kế

- BFD không còn UC18, UC19 và module Vận hành. CSDL còn **10 bảng**: bỏ 5 bảng của bộ đánh giá truy hồi.
- Truy hồi tài liệu vẫn chạy bên trong hỏi đáp (UC05), chỉ không mở ra thành API riêng.

### Việc cần làm

1. **UC18:** bỏ `routes.use(retrievalRouter)` trong `server/src/routes.ts` (endpoint `POST /api/search`), bỏ `retrieval.controller.ts`, `retrieval.route.ts`, `retrieval.schema.ts`. **Giữ** `retrieval.service.ts`, `retrieval.sql.ts` vì `chat.service.ts` và `rag/prompt.ts` dùng chúng. Xóa mục `/search` trong `docs/api-contract.md`.
2. **UC19:** bỏ thư mục `server/src/eval/` và script `"eval"` trong `server/package.json`; bỏ 5 model `EvalSet`, `EvalQuestion`, `EvalGoldChunk`, `EvalRun`, `EvalResult` trong `schema.prisma` bằng một migration xóa bảng; bỏ tên các bảng này khỏi `prisma/check.ts`.
   - Nếu nhóm vẫn muốn đo chất lượng truy hồi khi phát triển, có thể giữ bộ đánh giá như công cụ nội bộ ngoài thiết kế; khi đó ghi rõ trong `docs/` rằng nó không thuộc hệ thống bàn giao.
3. **Tài liệu:** `use-cases/README.md` đánh dấu UC18, UC19 là đã bỏ; `erd.md` tổng số bảng 10.

---

## 6. Thứ tự đề xuất

1. Mục 1, 2 và 5 — CSDL: schema + migration + check (bỏ `department_members`, thêm `ai_configs`, bỏ 5 bảng đánh giá), chạy trên DB thử nghiệm riêng.
2. Mục 1, 2 và 5 — backend, rồi frontend; chạy toàn bộ test.
3. Cập nhật tài liệu trong `docs/`.
4. Mục 3 — đổi tên phương thức.
5. Kiểm thử lại trên DB thử nghiệm theo ba vai (đăng nhập, hỏi đáp, tải tài liệu, khóa tài khoản, đổi cấu hình AI) trước khi áp dụng cho DB thật.
