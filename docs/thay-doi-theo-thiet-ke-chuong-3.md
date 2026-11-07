# Thay đổi cần áp dụng vào project theo thiết kế Chương 3

**Cập nhật:** 07/11/2026 · **Nguồn:** bộ slide Chương 3 (BFD, IFD, DFD, ERD, biểu đồ lớp, CSDL) và SRS đã sửa.

Chương 3 là kết quả phân tích thiết kế: nó quy định hệ thống **phải** như thế nào. Mã nguồn hiện tại
được viết trước bản thiết kế này nên lệch nhiều chỗ. Tài liệu này liệt kê từng chỗ lệch và việc cần làm
để đưa project về đúng thiết kế. Khi thiết kế và mã nguồn khác nhau, **thiết kế là chuẩn**.

> Tài liệu này hướng dẫn sửa **mã Node/TypeScript hiện tại**. Nếu hiện thực lại bằng Java Spring Boot,
> đọc [`DAC-TA-TRIEN-KHAI.md`](DAC-TA-TRIEN-KHAI.md) — đặc tả chính thức, đầy đủ lược đồ, API và luồng.

**Mức thiết kế: client–server.** Thiết kế chỉ mô tả hai tầng: client (trình duyệt của sinh viên, giáo
vụ, quản trị viên) gửi request, server trả response; CSDL và mọi xử lý nằm ở server.

---

## 0. Tóm tắt thiết kế mới

| | Hiện tại (mã nguồn) | Thiết kế Chương 3 |
|---|---|---|
| Hỏi đáp | Sinh viên gõ câu hỏi tự do; RAG (pgvector + Gemini) sinh câu trả lời, trả qua SSE | Sinh viên **chọn nút** chủ đề → nút câu hỏi; server trả **câu trả lời soạn sẵn** kèm tài liệu nguồn và trang, qua REST/JSON |
| AI | Gemini nhúng vector và sinh câu trả lời | **Không dùng AI** |
| Nội dung trả lời | Sinh từ tài liệu đã nhúng | Giáo vụ soạn ở **UC22 Quản lý chủ đề**, **UC23 Quản lý câu hỏi – trả lời** |
| Tài liệu | Tải lên → worker cắt đoạn, nhúng vector, trạng thái PENDING/READY/FAILED | Chỉ lưu tệp làm **nguồn trích dẫn**; không xử lý, không trạng thái |
| Đơn vị, vai trò | Bảng `departments` + `department_members` (M:N, vai theo đơn vị) | Thuộc tính `department` và `role` **trên `users`** |
| CSDL | 15 bảng (kể cả chunks, embeddings, jobs, citations, eval) | **6 bảng**: `users`, `topics`, `questions`, `documents`, `conversations`, `messages` |

Use case giữ nguyên: UC01–UC08, UC10–UC12, UC14–UC16, UC20.
**Bỏ:** UC09 Theo dõi xử lý, UC13 Chạy lại khi lỗi, UC18 Tra cứu qua API, UC19 Đánh giá truy hồi,
UC21 Cấu hình dịch vụ AI. **Thêm:** UC22 Quản lý chủ đề, UC23 Quản lý câu hỏi – trả lời (Giáo vụ).

---

## 1. CSDL — `server/prisma/`

### Lược đồ đích (6 bảng, khớp ERD)

| Bảng | Cột chính | Ràng buộc |
|---|---|---|
| `users` | `id`, `email`, `password_hash`, `full_name`, `role` (`USER`/`CONTENT_ADMIN`/`SYSTEM_ADMIN`), `department` (text, ví dụ `CNTT`), `is_active` | `email` duy nhất |
| `topics` | `id`, `name`, `sort_order` | `name` duy nhất |
| `questions` | `id`, `topic_id`, `content` (chữ trên nút), `answer` (câu trả lời soạn sẵn), `document_id`, `page`, `is_active` | `topic_id` → `topics` **RESTRICT**; `document_id` → `documents` **RESTRICT**, bắt buộc (BR06) |
| `documents` | `id`, `title`, `file_url`, `file_hash`, `mime_type`, `page_count`, `uploaded_by` | `file_hash` duy nhất; `uploaded_by` → `users` |
| `conversations` | `id`, `user_id`, `title`, `created_at` | `user_id` → `users` CASCADE |
| `messages` | `id`, `conversation_id`, `question_id`, `sent_at` | `conversation_id` → `conversations` **CASCADE**; `question_id` → `questions` RESTRICT |

- **Gỡ câu hỏi = đặt `is_active = false`**, không xóa dòng, để lịch sử hội thoại vẫn hiển thị được
  (luồng UC05, ngoại lệ "câu hỏi đã bị gỡ").
- `page` phải ≤ `documents.page_count` (kiểm tra ở service, luồng UC23 bước 6).
- Giáo vụ không gỡ được tài liệu còn câu hỏi dẫn tới (UC12), không xóa được chủ đề còn câu hỏi (UC22):
  do khóa ngoại RESTRICT, service bắt lỗi và trả 409.

### Việc cần làm

1. `schema.prisma`:
   - Xóa model `Department`, `DepartmentMember`, `Chunk`, `ChunkEmbedding`, `IngestJob`,
     `MessageCitation`, `EvalSet`, `EvalQuestion`, `EvalGoldChunk`, `EvalRun`, `EvalResult`.
   - Xóa enum `DeptType`, `DocStatus`, `JobStatus`, `MessageRole`; đổi `MemberRole` thành `Role`.
   - `User`: thêm `role Role @default(USER)`, `department String`.
   - `Document`: bỏ `status`, `departmentId`, các quan hệ tới chunk/job; thêm `pageCount`.
   - Thêm model `Topic`, `Question`; `Message` bỏ `role`, `content`, thêm `questionId`.
2. Migration mới (sao lưu DB trước; chạy trên DB thử nghiệm riêng):
   1. Thêm `users.role`, `users.department`; chép vai cao nhất trong khoa CNTT từ `department_members`
      sang `users.role`, `department = 'CNTT'`. User không có tư cách CNTT: khóa (`is_active = false`).
   2. Tạo `topics`, `questions`.
   3. Xóa dữ liệu hỏi đáp cũ (`message_citations`, `messages`, `conversations`): tin nhắn sinh từ RAG
      không ánh xạ được sang câu hỏi có sẵn. Ghi rõ điều này trong migration.
   4. Xóa các bảng ở bước 1 và extension `vector`.
3. `prisma/check.ts`: danh sách bảng còn 6; kiểm tra cột `users.role`, `users.department`.
4. `prisma/seed.ts`, `scripts/bootstrap-admin.ts`: tạo user với `role`, `department`; seed vài chủ
   đề và câu hỏi mẫu (đăng ký học phần, đồ án, chuẩn đầu ra, bảo lưu, cảnh báo học vụ, tốt nghiệp).
5. `docker-compose.yml`: đổi image `pgvector/pgvector:pg16` thành `postgres:16`.

---

## 2. Bỏ AI / RAG — `server/`

| Xóa | Ghi chú |
|---|---|
| `src/rag/` (`chunk.ts`, `embed.ts`, `generate.ts`, `prompt.ts`, `citation-guard.ts` và test) | Không còn sinh câu trả lời |
| `src/worker/ingest.worker.ts` và dịch vụ worker trong `docker-compose.yml` | Tài liệu không còn xử lý nền |
| `src/modules/retrieval/` | Bỏ cả `POST /api/search` (UC18) |
| `src/eval/` và script `"eval"` trong `server/package.json` | UC19 |
| `GEMINI_*` trong `src/config/env.ts`, `.env.example` | Không còn khóa AI |
| `documents.parser.ts` | Chỉ giữ phần đếm số trang (để kiểm tra `page`) |

Gỡ các thư viện chỉ dùng cho phần trên (SDK Gemini, thư viện trích văn bản) khỏi `server/package.json`.

---

## 3. API — `server/src/modules/`

| Module (BFD) | Use case | API đích | So với hiện tại |
|---|---|---|---|
| M1 · Tài khoản (`auth`) | UC01–UC04 | `POST /auth/login`, `GET /auth/me`, `PUT /auth/password` | `login`, `me` trả `user.role`, `user.department`; bỏ `memberships` |
| M2 · Hỏi đáp (`chat`) | UC05, UC06, UC08 | `GET /topics`, `GET /topics/:id/questions`, `POST /chat` `{ questionId, conversationId? }` → `{ conversationId, question, answer, source: { documentId, title, page } }`, `GET /conversations`, `GET /conversations/:id` | `POST /chat` nhận `questionId` thay vì câu hỏi tự do; trả JSON, **bỏ SSE** |
| M3 · Nội dung (`documents`, `content`) | UC07, UC10–UC12, UC22, UC23 | `GET/POST/PATCH/DELETE /documents`, `GET /documents/:id/file`; `POST/PATCH/DELETE /topics`, `POST/PATCH/DELETE /questions` (CONTENT_ADMIN) | Bỏ `/documents/:id/chunks`, `/status`, `/retry`; thêm module quản lý chủ đề và câu hỏi |
| M4 · Quản trị (`admin`, health) | UC14–UC16, UC20 | `GET /users`, `PATCH /users/:id` `{ role?, isActive? }`, `GET /health` | Bỏ `/departments*`, `/departments/:id/members`; `/health` trả trạng thái server, CSDL, uptime |

Middleware và phân quyền:

- `middleware/auth.middleware.ts`: đọc `user.role`, `user.department`. BR01: chặn khi `isActive = false`
  hoặc `department` khác `CNTT`. BR02: mỗi request đọc lại `role` từ CSDL.
- `lib/roles.ts`: bỏ `effectiveRole()`; giữ `assertContentAdmin`, `assertSystemAdmin`.
- `types/express.d.ts`: `AuthenticatedUser` có một `role`, một `department`.
- `admin.service.ts`: thay `ganThanhVien` / `goThanhVien` bằng `assignRole(userId, role)`; giữ BR04
  (không tự khóa, không tự hạ vai).

---

## 4. Frontend — `web/src/features/`

| Tệp | Thay đổi |
|---|---|
| `chat/ChatBox.tsx`, `MessageList.tsx`, `MessageBubble.tsx` | Thay ô nhập bằng **nút chủ đề → nút câu hỏi**; mỗi lượt hiển thị câu hỏi đã chọn, câu trả lời và nguồn |
| `chat/useSseStream.ts` | Xóa; gọi `POST /api/chat` dạng JSON |
| `chat/CitationChip.tsx`, `CitationDrawer.tsx` | Hiển thị một nguồn (tên tài liệu + trang), mở tệp gốc tại trang đó (UC08) |
| `documents/StatusBadge.tsx` | Xóa (không còn trạng thái xử lý) |
| `documents/DocumentManager.tsx`, `DocumentTable.tsx`, `api.ts` | Bỏ cột trạng thái, nút chạy lại; quyền lấy từ `user.role` |
| Mới: `content/` | Trang cho giáo vụ: quản lý chủ đề (UC22), soạn câu hỏi – câu trả lời, chọn tài liệu nguồn và trang (UC23) |
| `auth/useAuth.ts` | Phiên chỉ còn `{ user }` có `role`, `department`; bỏ `Membership`, `useMemberships()` |
| `admin/api.ts`, `admin/UserTable.tsx` | Mỗi tài khoản một vai; đổi vai qua `PATCH /api/users/:id` |

---

## 5. Tên lớp và phương thức theo biểu đồ lớp

Đặt tên trong service/model khớp biểu đồ lớp (cú pháp `+tên(in tham số: kiểu): kiểu trả về`):

| Lớp | Phương thức thiết kế | Vị trí đề xuất |
|---|---|---|
| User | `login(in pwd: string): boolean`, `assignRole(in role: Role): void` | `auth.service`, `admin.service` |
| Conversation | `ask(in q: Question): Message`, `getMessages(): Message[]` | `chat.service` (thay `hoi`, `chiTietHoiThoai`) |
| Message | `getQuestion(): Question` | `chat.service` |
| Question | `getAnswer(): string`, `getSource(): Document` | `content.service` |
| Topic | `getQuestions(): Question[]`, `addQuestion(in q: Question): void` | `content.service` |
| Document | `upload(in file: File): void`, `remove(): void` | `documents.service` (thay `create`, `remove`) |

---

## 6. Tài liệu trong `docs/`

- `erd.md`: vẽ lại theo 6 bảng ở mục 1.
- `api-contract.md`: theo bảng API ở mục 3; xóa mục SSE, `/search`, `/documents/:id/chunks|status|retry`,
  `/departments*`.
- `use-cases/README.md`: đánh dấu UC09, UC13, UC18, UC19, UC21 đã bỏ; thêm UC22, UC23; UC05 thành
  "Chọn câu hỏi, nhận trả lời".
- `phan-quyen.md`: phân quyền = đổi `users.role`; Giáo vụ thêm quyền quản lý chủ đề, câu hỏi.
- `README.md`: bỏ hướng dẫn cấu hình Gemini, pgvector.

---

## 7. Thứ tự đề xuất

1. CSDL (mục 1) trên DB thử nghiệm riêng: schema, migration, check, seed chủ đề và câu hỏi mẫu.
2. Xóa phần AI/RAG (mục 2) để backend biên dịch được với schema mới.
3. API (mục 3), chạy `pnpm typecheck` và test; viết test cho: chọn câu hỏi trả đúng `answer` và nguồn,
   gỡ tài liệu còn câu hỏi bị chặn, đổi vai có hiệu lực ở request kế tiếp.
4. Frontend (mục 4).
5. Tài liệu (mục 6) và đổi tên phương thức (mục 5).
6. Kiểm thử theo ba vai: sinh viên chọn câu hỏi và mở nguồn; giáo vụ tải tài liệu, tạo chủ đề, soạn câu
   hỏi; quản trị viên khóa tài khoản, đổi vai, xem `/health`.
