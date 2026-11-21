# Đặc tả triển khai — Sổ Tay Sinh Viên CNTT (Spring Boot)

**Phiên bản:** 2.0 · 21/11/2026 — client desktop Java Swing, giao tiếp qua TCP socket
**Nguồn thiết kế:** slide Chương 3 (BFD, IFD, DFD, ERD, biểu đồ lớp, CSDL) và
`docs/slides/SRS-tac-nhan-va-use-case.pptx`.

Đây là **tài liệu chính thức** để hiện thực server bằng Java Spring Boot. Thiết kế RAG cũ (Gemini,
pgvector, SSE) và mã Node.js đã gỡ khỏi repo; khi có mâu thuẫn với bất kỳ tài liệu nào khác,
**làm theo tài liệu này**.

---

## 1. Tổng quan

### 1.1. Bài toán

Sinh viên ngành CNTT tra cứu quy định học vụ (đăng ký học phần, điều kiện đồ án, chuẩn đầu ra, bảo
lưu, cảnh báo học vụ, xét tốt nghiệp) qua một **chatbot dạng nút chọn**:

1. Sinh viên mở khung chat, thấy các **nút chủ đề**.
2. Bấm một chủ đề → thấy các **nút câu hỏi** của chủ đề đó.
3. Bấm một câu hỏi → server trả **câu trả lời do giáo vụ soạn sẵn**, kèm **tài liệu nguồn và số trang**.

Hệ thống **không dùng AI**, không nhận câu hỏi gõ tự do. Ngoài phạm vi: điểm, học phí, lịch học cá
nhân, đăng ký tín chỉ, nhiều khoa, SSO, OCR.

### 1.2. Kiến trúc client–server

```
 Client Java Swing      ──TCP socket :9090──▶   Server Java (ServerSocket)  ──JDBC──▶  PostgreSQL 16
 (máy bất kỳ trong LAN)  khung [4 byte độ dài]   Spring Boot không web,                  6 bảng
 Sinh viên / Giáo vụ /   [JSON UTF-8], JWT       mỗi client một luồng                    + thư mục lưu tệp
 Quản trị viên          ◀──────────────────────
```

- Client là **ứng dụng desktop Java Swing**, mở một `Socket` tới `ServerSocket` của server; không qua
  trình duyệt, không HTTP.
- Mỗi lượt bấm nút là **một khung yêu cầu / một khung phản hồi** trên cùng một kết nối TCP lâu dài.
- Server phục vụ nhiều client cùng lúc: luồng nhận kết nối + pool luồng xử lý (thread-per-connection).
- Client không truy cập CSDL trực tiếp; giao thức chi tiết ở mục 6.

### 1.3. Công nghệ

| Thành phần | Lựa chọn |
|---|---|
| Ngôn ngữ | Java 21 |
| Server | Spring Boot 3.3.x **không web** (`web-application-type: none`): Data JPA, Validation, Jackson; `java.net.ServerSocket` tự viết |
| Client | Java 21 Swing + FlatLaf, Jackson; `java.net.Socket` |
| Giao thức | TCP, khung độ dài 4 byte + JSON UTF-8 (mục 6) |
| CSDL | PostgreSQL 16, migration bằng Flyway |
| Xác thực | JWT (thư viện `io.jsonwebtoken:jjwt` 0.12.x) gửi trong trường `token`, mật khẩu BCrypt (`spring-security-crypto`) |
| Đọc số trang PDF | Apache PDFBox 3.x |
| Build | Maven |
| Chạy | Docker Compose: `postgres:16` + server; client chạy `java -jar` trên máy người dùng |

---

## 2. Tác nhân và phân quyền

| Tác nhân | `Role` | Quyền |
|---|---|---|
| Sinh viên CNTT | `USER` | Đăng nhập, đổi mật khẩu; chọn câu hỏi, xem nguồn, xem lại hội thoại của mình; tra cứu tài liệu |
| Giáo vụ khoa CNTT | `CONTENT_ADMIN` | Mọi quyền của Sinh viên + quản lý tài liệu, chủ đề, câu hỏi – câu trả lời |
| Quản trị viên | `SYSTEM_ADMIN` | Mọi quyền của Giáo vụ + xem, khóa / mở, phân quyền tài khoản; kiểm tra trạng thái hệ thống |

Quan hệ kế thừa: `SYSTEM_ADMIN > CONTENT_ADMIN > USER` (dùng `RoleHierarchy` của Spring Security).
Mỗi tài khoản có **đúng một** `role` và **một** `department` (đơn vị, ví dụ `CNTT`), lưu trên bảng `users`.

### Ma trận quyền

| Chức năng | Sinh viên | Giáo vụ | Quản trị viên |
|---|---|---|---|
| Đăng nhập / đăng xuất, đổi mật khẩu | Có | Có | Có |
| Chọn câu hỏi, xem hội thoại của mình | Có | Có | Có |
| Đọc tài liệu, mở tệp gốc | Có | Có | Có |
| Quản lý tài liệu, chủ đề, câu hỏi – trả lời | — | Có | Có |
| Xem, khóa / mở, phân quyền tài khoản | — | — | Có |
| Kiểm tra trạng thái hệ thống | — | — | Có |
| Tự khóa hoặc tự hạ vai của chính mình | — | — | — |

---

## 3. Use case

| Module (BFD) | Mã | Use case | Tác nhân chính |
|---|---|---|---|
| M1 · Tài khoản | UC01 | Đăng nhập | Mọi vai |
| | UC02 | Đăng xuất (client xóa token, không có API) | Mọi vai |
| | UC03 | Xem thông tin cá nhân | Mọi vai |
| | UC04 | Đổi mật khẩu | Mọi vai |
| M2 · Hỏi đáp | UC05 | Chọn câu hỏi, nhận trả lời | Sinh viên |
| | UC06 | Xem lại hội thoại | Sinh viên |
| | UC08 | Xem nguồn trích dẫn (mở tệp gốc tại trang) | Sinh viên |
| M3 · Nội dung | UC07 | Tra cứu kho tài liệu | Sinh viên |
| | UC10 | Tải lên tài liệu | Giáo vụ |
| | UC11 | Sửa thông tin tài liệu | Giáo vụ |
| | UC12 | Gỡ tài liệu | Giáo vụ |
| | UC22 | Quản lý chủ đề | Giáo vụ |
| | UC23 | Quản lý câu hỏi – câu trả lời | Giáo vụ |
| M4 · Quản trị | UC14 | Tra cứu người dùng | Quản trị viên |
| | UC15 | Khóa / mở tài khoản | Quản trị viên |
| | UC16 | Phân quyền (đổi vai) | Quản trị viên |
| | UC20 | Kiểm tra trạng thái hệ thống | Quản trị viên |

Mã đã bỏ, không hiện thực: UC09, UC13, UC18, UC19, UC21.

---

## 4. Quy tắc nghiệp vụ

| Mã | Quy tắc | Hiện thực |
|---|---|---|
| BR01 | Tài khoản phải đang hoạt động và thuộc khoa CNTT mới được đăng nhập | Kiểm tra `is_active` và `department = 'CNTT'` khi đăng nhập **và** ở mỗi yêu cầu socket |
| BR02 | Mỗi yêu cầu kiểm tra lại quyền từ CSDL; đổi vai hoặc khóa có hiệu lực ngay yêu cầu sau | JWT chỉ chứa `userId`; `SocketDispatcher` đọc lại `users` ở mỗi yêu cầu |
| BR03 | Sinh viên chỉ đọc; giáo vụ và quản trị viên mới ghi nội dung; chỉ quản trị viên đổi tài khoản | Mỗi action khai báo vai tối thiểu (`routes.secured(action, role, …)`); thiếu vai → `FORBIDDEN` |
| BR04 | Quản trị viên không tự khóa hay tự hạ vai của chính mình | `AdminService` từ chối khi `targetId == currentUserId` → `SELF_MODIFICATION` |
| BR05 | Hội thoại thuộc riêng người tạo; mở nguồn và tệp gốc phải đăng nhập | Truy vấn hội thoại luôn kèm `user_id`; không phải chủ → `CONVERSATION_NOT_FOUND` |
| BR06 | Chatbot chỉ trả lời các câu hỏi có sẵn; mỗi câu trả lời soạn sẵn phải gắn tài liệu nguồn | `chat.ask` chỉ nhận `questionId`; `UC23` bắt buộc `documentId` và `page` |

---

## 5. Cơ sở dữ liệu

### 5.1. ERD (6 thực thể)

```
User 1 ──owns──── 0..* Conversation 1 ──contains── 0..* Message
  1                                                     0..*
  │ uploads                                               │ refers to
0..*                                                      1
Document 0..1 ──cited by── 0..* Question 0..* ──belongs to── 1 Topic
```

### 5.2. DDL — `src/main/resources/db/migration/V1__init.sql`

```sql
CREATE TABLE users (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code          varchar(50)  NOT NULL UNIQUE,          -- mã sinh viên / cán bộ
    email         varchar(255) NOT NULL UNIQUE,
    password_hash varchar(100) NOT NULL,
    full_name     varchar(200) NOT NULL,
    role          varchar(20)  NOT NULL DEFAULT 'USER'
                  CHECK (role IN ('USER', 'CONTENT_ADMIN', 'SYSTEM_ADMIN')),
    department    varchar(50)  NOT NULL,
    is_active     boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE documents (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    title       varchar(500) NOT NULL,
    file_url    varchar(500) NOT NULL,                   -- đường dẫn tương đối trong thư mục lưu tệp
    file_hash   varchar(64)  NOT NULL UNIQUE,            -- SHA-256, chặn tải trùng
    mime_type   varchar(100) NOT NULL,
    page_count  integer,                                 -- NULL với DOCX
    uploaded_by uuid         NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE topics (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name       varchar(100) NOT NULL UNIQUE,
    sort_order integer      NOT NULL DEFAULT 0
);

CREATE TABLE questions (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id    uuid          NOT NULL REFERENCES topics(id)    ON DELETE RESTRICT,
    content     varchar(200)  NOT NULL,                  -- chữ hiện trên nút
    answer      varchar(5000) NOT NULL,                  -- câu trả lời soạn sẵn
    document_id uuid          NOT NULL REFERENCES documents(id) ON DELETE RESTRICT,
    page        integer       NOT NULL CHECK (page >= 1),
    is_active   boolean       NOT NULL DEFAULT true,     -- gỡ câu hỏi = false
    sort_order  integer       NOT NULL DEFAULT 0
);
CREATE INDEX idx_questions_topic ON questions(topic_id) WHERE is_active;

CREATE TABLE conversations (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title      varchar(200) NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_conversations_user ON conversations(user_id, created_at DESC);

CREATE TABLE messages (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id uuid        NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    question_id     uuid        NOT NULL REFERENCES questions(id)     ON DELETE RESTRICT,
    sent_at         timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_conversation ON messages(conversation_id, sent_at);
```

Ghi chú thiết kế:

- **Câu hỏi không bao giờ bị xóa dòng**, chỉ đặt `is_active = false`, vì `messages` trỏ tới nó.
  Câu hỏi ẩn không hiện thành nút nhưng lịch sử hội thoại vẫn đọc được.
- Lịch sử hội thoại hiển thị **nội dung hiện tại** của câu hỏi. Nếu giáo vụ sửa câu trả lời, hội
  thoại cũ thấy bản mới — chấp nhận được vì quy định mới là quy định đúng.
- `questions.document_id` bắt buộc theo BR06. RESTRICT làm UC12 thất bại khi tài liệu còn câu hỏi
  dẫn tới (kể cả câu hỏi đã ẩn).
- `topics` RESTRICT: không xóa được chủ đề còn câu hỏi.

### 5.3. Dữ liệu mẫu — `db/demo/V2__demo_data.sql`

Tạo một tài khoản mỗi vai (`department = 'CNTT'`, mật khẩu BCrypt) và 6 chủ đề: Đăng ký học phần,
Đồ án tốt nghiệp, Chuẩn đầu ra, Bảo lưu – nghỉ học, Cảnh báo học vụ, Xét tốt nghiệp. Câu hỏi mẫu cần
tài liệu nguồn nên nhập qua giao diện giáo vụ (UC10 → UC23) sau khi chạy.

---

## 6. Giao thức TCP socket

Client và server trao đổi trực tiếp qua **một kết nối TCP** (`java.net.Socket` ↔ `java.net.ServerSocket`),
cổng mặc định **9090**. Không dùng HTTP, REST, WebSocket.

### 6.1. Quy ước chung

**Đóng khung (framing).** TCP là luồng byte, không có ranh giới thông điệp, nên mỗi thông điệp được đóng
khung:

```
┌──────────────────────────┬─────────────────────────────────────┐
│ 4 byte: độ dài N         │ N byte: JSON mã hóa UTF-8            │
│ số nguyên big-endian     │                                     │
└──────────────────────────┴─────────────────────────────────────┘
```

Hai phía dùng cùng lớp `FrameCodec` (`DataOutputStream.writeInt` + `write`, `DataInputStream.readInt` +
`readFully`). Khung dài quá **32 MB** → server trả lỗi `FRAME_TOO_LARGE` rồi đóng kết nối.

**Yêu cầu** (client → server):

```json
{ "id": 7, "action": "chat.ask", "token": "eyJ…", "data": { "questionId": "…", "conversationId": null } }
```

**Phản hồi** (server → client), cùng `id` với yêu cầu:

```json
{ "id": 7, "ok": true,  "data": { … } }
{ "id": 7, "ok": false, "error": { "code": "QUESTION_NOT_FOUND", "message": "Câu hỏi không còn." } }
```

- **Một kết nối lâu dài** cho cả phiên: đăng nhập, rồi gửi tiếp nhiều yêu cầu trên cùng socket. Mỗi yêu cầu
  nhận đúng một phản hồi, theo thứ tự. Đăng xuất (UC02) = client bỏ token và đóng socket.
- **Xác thực:** trường `token` (JWT nhận từ `auth.login`) gửi kèm mọi action trừ `auth.login`, `system.ping`.
  Mỗi yêu cầu server đọc lại người dùng từ CSDL (BR01, BR02) nên khóa tài khoản / đổi vai có hiệu lực ngay.
- **Phân trang:** `data` có `page` (≥ 0), `size` (1–100); kết quả dạng
  `{ "items": [...], "page": 0, "size": 20, "total": 57 }`.
- **Tệp nhị phân** đi trong JSON dưới dạng base64 (`contentBase64`), tệp ≤ 20 MB.
- **Đa luồng:** luồng `socket-acceptor` nhận kết nối; mỗi client được một luồng trong pool cố định
  (`SOCKET_THREADS`, mặc định 64). Kết nối im lặng quá `SOCKET_IDLE_TIMEOUT` (mặc định 30 phút) bị đóng.

**Mã lỗi**

| Nhóm | Mã |
|---|---|
| Giao thức | `BAD_FRAME` (không phải JSON), `UNKNOWN_ACTION`, `FRAME_TOO_LARGE` |
| Xác thực, quyền | `UNAUTHORIZED` (thiếu / sai token, tài khoản khóa, không thuộc CNTT), `FORBIDDEN` (thiếu vai), `WRONG_PASSWORD`, `SELF_MODIFICATION` |
| Dữ liệu | `VALIDATION_ERROR`, `QUESTION_NOT_FOUND`, `CONVERSATION_NOT_FOUND`, `TOPIC_NOT_FOUND`, `DOCUMENT_NOT_FOUND`, `USER_NOT_FOUND` |
| Xung đột | `DUPLICATE_DOCUMENT`, `DOCUMENT_IN_USE`, `TOPIC_NOT_EMPTY`, `TOPIC_NAME_TAKEN`, `CONFLICT` |
| Tệp | `UNSUPPORTED_FILE` (không phải PDF / DOCX), `FILE_TOO_LARGE` (> 20 MB) |
| Khác | `INTERNAL_ERROR` |

**Ví dụ một phiên** (mỗi dòng là một khung):

```
client ─▶ {"id":1,"action":"auth.login","data":{"login":"SV001","password":"Matkhau@123"}}
       ◀─ {"id":1,"ok":true,"data":{"token":"eyJ…","user":{"code":"SV001","role":"USER",…}}}
client ─▶ {"id":2,"action":"topic.list","token":"eyJ…"}
       ◀─ {"id":2,"ok":true,"data":[{"id":"…","name":"Đăng ký học phần","sortOrder":1,"questionCount":2},…]}
client ─▶ {"id":3,"action":"chat.ask","token":"eyJ…","data":{"questionId":"…"}}
       ◀─ {"id":3,"ok":true,"data":{"conversationId":"…","message":{…}}}
client ─▶ {"id":4,"action":"document.upload","token":"eyJ…","data":{…}}
       ◀─ {"id":4,"ok":false,"error":{"code":"FORBIDDEN","message":"Không đủ quyền."}}
```

### 6.2. M1 · Tài khoản

| Action | Vai tối thiểu | `data` gửi | `data` trả về |
|---|---|---|---|
| `system.ping` | — (công khai) | — | `{ "pong": true }` — đo độ trễ khứ hồi |
| `auth.login` (UC01) | — (công khai) | `{ "login", "password" }` — login = mã hoặc email | `{ "token", "user" }` |
| `auth.me` (UC03) | USER | — | `{ "user": { "id", "code", "email", "fullName", "role", "department", "active" } }` |
| `auth.changePassword` (UC04) | USER | `{ "currentPassword", "newPassword" }` (8–72 ký tự) | `{}`; sai mật khẩu cũ → `WRONG_PASSWORD` |

Đăng nhập sai, tài khoản khóa hay `department ≠ CNTT` đều trả `UNAUTHORIZED` cùng một thông báo, không
tiết lộ lý do. Token hết hạn sau 8 giờ.

### 6.3. M2 · Hỏi đáp

| Action | Vai | `data` gửi | `data` trả về |
|---|---|---|---|
| `chat.ask` (UC05) | USER | `{ "questionId", "conversationId"? }` — bỏ `conversationId` = hội thoại mới | `{ "conversationId", "message" }` |
| `conversation.list` (UC06) | USER | `{ "page", "size" }` | trang `{ "id", "title", "createdAt", "messageCount" }`, mới nhất trước |
| `conversation.get` (UC06) | USER | `{ "id" }` | `{ "id", "title", "createdAt", "messages": [ message ] }` |

`message` = `{ "id", "sentAt", "question": { "id", "content" }, "answer", "source": { "documentId", "title", "page" } }`.
Câu hỏi đã ẩn → `QUESTION_NOT_FOUND`; hội thoại không thuộc người gọi → `CONVERSATION_NOT_FOUND` (BR05).
Hội thoại mới lấy `title` = nội dung câu hỏi đầu tiên. Hội thoại cũ vẫn hiện câu hỏi đã ẩn.

### 6.4. M3 · Nội dung

| Action | Vai | `data` gửi | `data` trả về |
|---|---|---|---|
| `topic.list` (UC07) | USER | — | `[ { "id", "name", "sortOrder", "questionCount" } ]` — chỉ đếm câu hỏi đang hiện |
| `topic.questions` (UC07) | USER | `{ "topicId" }` | `[ { "id", "content" } ]` — nút câu hỏi đang hiện |
| `topic.create` (UC22) | CONTENT_ADMIN | `{ "name", "sortOrder"? }` | chủ đề |
| `topic.update` (UC22) | CONTENT_ADMIN | `{ "id", "name"?, "sortOrder"? }` | chủ đề |
| `topic.delete` (UC22) | CONTENT_ADMIN | `{ "id" }` | `{}`; còn câu hỏi → `TOPIC_NOT_EMPTY` |
| `question.listForEditor` (UC23) | CONTENT_ADMIN | `{ "topicId" }` | mọi câu hỏi, kể cả đã ẩn, kèm `answer`, `documentId`, `page`, `active` |
| `question.create` (UC23) | CONTENT_ADMIN | `{ "topicId", "content" (≤ 200), "answer" (≤ 5000), "documentId", "page", "sortOrder"?, "active"? }` | câu hỏi |
| `question.update` (UC23) | CONTENT_ADMIN | `{ "id", …các trường như trên }` | câu hỏi |
| `question.hide` (UC23) | CONTENT_ADMIN | `{ "id" }` | `{}` — đặt `is_active = false` |
| `document.list` (UC07) | USER | `{ "q"?, "page", "size" }` — tìm theo tiêu đề | trang `{ "id", "title", "mimeType", "pageCount", "uploadedBy", "createdAt" }` |
| `document.get` | USER | `{ "id" }` | một tài liệu |
| `document.download` (UC08) | USER | `{ "id" }` | `{ "fileName", "mimeType", "size", "contentBase64" }` |
| `document.upload` (UC10) | CONTENT_ADMIN | `{ "title" (3–500), "fileName", "contentBase64" }` | tài liệu; trùng SHA-256 → `DUPLICATE_DOCUMENT` |
| `document.rename` (UC11) | CONTENT_ADMIN | `{ "id", "title" }` | tài liệu |
| `document.delete` (UC12) | CONTENT_ADMIN | `{ "id" }` | `{}`; còn câu hỏi dẫn tới → `DOCUMENT_IN_USE` |

Kiểm tra câu hỏi: `page ≥ 1` và `page ≤ page_count` khi tài liệu có `page_count`; chủ đề và tài liệu phải
tồn tại (`VALIDATION_ERROR` nếu sai). Loại tệp nhận diện bằng nội dung (`%PDF`, hoặc ZIP + đuôi `.docx`).

### 6.5. M4 · Quản trị

| Action | Vai | `data` gửi | `data` trả về |
|---|---|---|---|
| `user.list` (UC14) | SYSTEM_ADMIN | `{ "q"?, "page", "size" }` — tìm theo mã, email, họ tên | trang người dùng |
| `user.update` (UC15, UC16) | SYSTEM_ADMIN | `{ "id", "active"? , "role"? }` | người dùng sau khi sửa; sửa chính mình → `SELF_MODIFICATION` (BR04) |
| `system.health` (UC20) | SYSTEM_ADMIN | — | `{ "status", "database", "uptimeSeconds", "time" }` |

`database` = kết quả `SELECT 1`; lỗi CSDL → `"DOWN"`, `status` = `"DEGRADED"`.

---

## 7. Luồng xử lý chính

**UC05 — Chọn câu hỏi, nhận trả lời**

1. Client gửi `topic.list`, hiện danh sách chủ đề.
2. Server kiểm tra token, tài khoản, vai (2a: sai → `UNAUTHORIZED`).
3. Sinh viên chọn chủ đề; client gửi `topic.questions` (3a: rỗng → "chưa có câu hỏi").
4. Sinh viên bấm một nút; client gửi `chat.ask { questionId, conversationId }`.
5. Server đọc câu hỏi `is_active`, câu trả lời và tài liệu nguồn (5a: không còn → `QUESTION_NOT_FOUND`).
6. Server lưu hội thoại (nếu mới) và một `message` trong cùng transaction.
7. Server trả câu trả lời kèm nguồn; client hiện thành một lượt hỏi – đáp.
8. Sinh viên bấm nguồn (UC08): client gửi `document.download`, ghi tệp tạm rồi mở bằng ứng dụng mặc định, báo số trang.

**UC10 → UC22 → UC23 — Nhập nội dung (Giáo vụ)**

1. Tải lên PDF / DOCX, nhập tiêu đề (UC10).
2. Client gửi `document.upload` (tệp base64); server kiểm quyền, tính SHA-256 (trùng → `DUPLICATE_DOCUMENT`), lưu tệp vào `storage/yyyy/MM/<uuid>.<ext>`.
3. Server lưu bản ghi `documents`, đọc `page_count` bằng PDFBox nếu là PDF.
4. Chọn hoặc tạo chủ đề (UC22).
5. Soạn câu hỏi và câu trả lời (UC23).
6. Gắn tài liệu nguồn, số trang; server kiểm tra (lỗi → `VALIDATION_ERROR`, giáo vụ sửa và gửi lại).
7. Lưu thành công → câu hỏi hiện thành nút ngay ở lần sinh viên mở chủ đề kế tiếp.

**UC14 → UC15 / UC16 — Quản trị tài khoản**

1. Quản trị viên mở danh sách tài khoản (UC14).
2. Chọn khóa / mở (UC15) hoặc đổi vai (UC16).
3. Server kiểm tra thao tác lên chính mình? Có → `SELF_MODIFICATION`.
4. Cập nhật `users.is_active` hoặc `users.role` trong transaction.
5. Yêu cầu kế tiếp của tài khoản đó đọc lại quyền từ CSDL (BR02).

---

## 8. Hiện thực bằng Spring Boot

### 8.1. Cấu trúc dự án

```
server/                                # Java Spring Boot (không web) — Model + Controller
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/vn/tangthu/
    │   ├── TangThuApplication.java
    │   ├── common/                    # dùng chung, không thuộc module nào
    │   │   ├── socket/                # SocketServer, ClientConnection, FrameCodec, SocketDispatcher,
    │   │   │                          # SocketController, SocketRequest — tầng mạng TCP
    │   │   ├── config/                # AppProperties, CryptoConfig
    │   │   ├── security/              # JwtService, AuthUser
    │   │   ├── exception/             # ApiException
    │   │   ├── dto/                   # PageResponse
    │   │   └── util/                  # FileStorage
    │   ├── account/                   # M1 · Tài khoản — UC01–UC04
    │   │   ├── model/                 # User, Role
    │   │   ├── repository/            # UserRepository
    │   │   ├── service/               # AuthService
    │   │   ├── controller/            # AuthSocketController   (auth.*)
    │   │   └── dto/                   # LoginRequest, ChangePasswordRequest, UserDto
    │   ├── chat/                      # M2 · Hỏi đáp — UC05, UC06
    │   │   ├── model/                 # Conversation, Message
    │   │   ├── repository/            # ConversationRepository, MessageRepository
    │   │   ├── service/               # ChatService
    │   │   ├── controller/            # ChatSocketController   (chat.*, conversation.*)
    │   │   └── dto/                   # AskRequest, ChatDtos
    │   ├── content/                   # M3 · Nội dung — UC07, UC08, UC10–UC12, UC22, UC23
    │   │   ├── model/                 # Topic, Question, Document
    │   │   ├── repository/            # TopicRepository, QuestionRepository, DocumentRepository
    │   │   ├── service/               # TopicService, QuestionService, DocumentService
    │   │   ├── controller/            # TopicSocketController, QuestionSocketController, DocumentSocketController
    │   │   └── dto/                   # ContentDtos, CreateTopic, UpdateTopic, QuestionRequest, RenameRequest
    │   └── admin/                     # M4 · Quản trị — UC14–UC16, UC20 (dùng model User của M1)
    │       ├── service/               # AdminService
    │       ├── controller/            # AdminSocketController  (user.*, system.health)
    │       └── dto/                   # UpdateUserRequest, Health
    └── resources/
        ├── application.yml
        └── db/
            ├── migration/             # V1__init.sql — lược đồ
            └── demo/                  # dữ liệu demo — tắt bằng FLYWAY_LOCATIONS khi triển khai thật

client/                                # Java Swing desktop — View (+ Controller phía client)
├── pom.xml                            # đóng gói tangthu-client.jar chạy được
└── src/main/java/vn/tangthu/client/
    ├── App.java                       # main: đọc IP / cổng máy chủ
    ├── common/
    │   ├── net/                       # SocketClient, FrameCodec, ApiError, PageResult — tầng mạng TCP
    │   └── view/                      # MainFrame (tab theo vai), Async (SwingWorker), FileOpener, Ui
    ├── account/  {model, controller, view}   # M1 · LoginPanel, ProfileDialog, ChangePasswordDialog
    ├── chat/     {model, controller, view}   # M2 · ChatPanel
    ├── content/  {model, controller, view}   # M3 · DocumentsPanel, ContentPanel
    └── admin/    {model, controller, view}   # M4 · UsersPanel, HealthPanel
```

Chia **theo module trước, theo tầng MVC sau**: mỗi thành viên phụ trách trọn một module ở cả `server/`
lẫn `client/`. Phía server: Model (`model/` + `repository/`), Controller (`controller/` — đăng ký các
action socket), nghiệp vụ (`service/`), dữ liệu trao đổi (`dto/`). Phía client: `model/` (record nhận từ
JSON), `controller/` (gửi action qua `SocketClient`), `view/` (màn hình Swing). Controller chỉ nhận/trả
DTO (`record`); entity không lộ ra JSON.

### 8.2. `pom.xml` — phụ thuộc chính

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.3.5</version>
</parent>
<properties><java.version>21</java.version></properties>
<dependencies>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-json</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
  <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-crypto</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
  <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
  <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
  <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
  <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>0.12.6</version></dependency>
  <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>0.12.6</version><scope>runtime</scope></dependency>
  <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>0.12.6</version><scope>runtime</scope></dependency>
  <dependency><groupId>org.apache.pdfbox</groupId><artifactId>pdfbox</artifactId><version>3.0.3</version></dependency>
  <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
</dependencies>
```

### 8.3. `application.yml`

```yaml
spring:
  main:
    web-application-type: none        # không Tomcat; client nối qua TCP socket
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/tangthu}
    username: ${DB_USER:tangthu}
    password: ${DB_PASSWORD:tangthu}
  jpa:
    hibernate.ddl-auto: validate      # schema do Flyway quản lý
    open-in-view: false
app:
  jwt:
    secret: ${JWT_SECRET}             # ≥ 32 byte, bắt buộc
    ttl: PT8H
  storage-dir: ${STORAGE_DIR:./storage}
  department: CNTT
  socket:
    port: ${SOCKET_PORT:9090}
    threads: ${SOCKET_THREADS:64}     # số client phục vụ đồng thời
    max-frame-mb: 32                  # đủ cho tệp 20 MB mã hóa base64
    idle-timeout: ${SOCKET_IDLE_TIMEOUT:PT30M}
```

### 8.4. Entity theo biểu đồ lớp

Tên lớp, thuộc tính và phương thức khớp biểu đồ lớp Chương 3. Phương thức nghiệp vụ đặt trong entity
khi chỉ dùng dữ liệu của entity; việc cần hạ tầng (lưu tệp, mã hóa mật khẩu) đặt ở service.

```java
public enum Role { USER, CONTENT_ADMIN, SYSTEM_ADMIN }

@Entity @Table(name = "users") @Getter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private String code;
    private String email;
    @Column(name = "password_hash") private String passwordHash;
    @Column(name = "full_name") private String fullName;
    @Enumerated(EnumType.STRING) private Role role;
    private String department;
    @Column(name = "is_active") private boolean active;

    /** +login(in pwd: string): boolean */
    public boolean login(String pwd, PasswordEncoder encoder) {
        return active && encoder.matches(pwd, passwordHash);
    }
    /** +assignRole(in role: Role): void */
    public void assignRole(Role role) { this.role = role; }
    public void setActive(boolean active) { this.active = active; }
    public void changePassword(String hash) { this.passwordHash = hash; }
}

@Entity @Table(name = "topics") @Getter @Setter @NoArgsConstructor
public class Topic {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private String name;
    @Column(name = "sort_order") private int sortOrder;
    @OneToMany(mappedBy = "topic") @OrderBy("sortOrder")
    private List<Question> questions = new ArrayList<>();

    /** +getQuestions(): Question[] — chỉ câu hỏi đang hiện */
    public List<Question> getQuestions() {
        return questions.stream().filter(Question::isActive).toList();
    }
    /** +addQuestion(in q: Question): void */
    public void addQuestion(Question q) { q.setTopic(this); questions.add(q); }
}

@Entity @Table(name = "questions") @Getter @Setter @NoArgsConstructor
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "topic_id") private Topic topic;
    private String content;
    private String answer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "document_id") private Document document;
    private int page;
    @Column(name = "is_active") private boolean active = true;
    @Column(name = "sort_order") private int sortOrder;

    /** +getAnswer(): string — kế thừa từ @Getter */
    /** +getSource(): Document */
    public Document getSource() { return document; }
}

@Entity @Table(name = "documents") @Getter @Setter @NoArgsConstructor
public class Document {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private String title;
    @Column(name = "file_url") private String fileUrl;
    @Column(name = "file_hash") private String fileHash;
    @Column(name = "mime_type") private String mimeType;
    @Column(name = "page_count") private Integer pageCount;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "uploaded_by") private User uploadedBy;
    @Column(name = "created_at", insertable = false, updatable = false) private Instant createdAt;
    // +upload(in file: File): void → DocumentService.upload(...)   (cần lưu tệp)
    // +remove(): void              → DocumentService.remove(...)   (cần xóa tệp)
}

@Entity @Table(name = "conversations") @Getter @NoArgsConstructor
public class Conversation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    private String title;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
    /** Hợp thành ◆: xóa hội thoại thì xóa tin nhắn */
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sentAt")
    private List<Message> messages = new ArrayList<>();

    public static Conversation start(User user, Question first) {
        Conversation c = new Conversation(); c.user = user; c.title = first.getContent(); return c;
    }
    /** +ask(in q: Question): Message */
    public Message ask(Question q) {
        Message m = new Message(this, q); messages.add(m); return m;
    }
    /** +getMessages(): Message[] — kế thừa từ @Getter */
}

@Entity @Table(name = "messages") @Getter @NoArgsConstructor
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "conversation_id") private Conversation conversation;
    /** +getQuestion(): Question */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "question_id") private Question question;
    @Column(name = "sent_at") private Instant sentAt = Instant.now();

    Message(Conversation c, Question q) { this.conversation = c; this.question = q; }
}
```

### 8.5. Máy chủ socket, xác thực — JWT và BR01, BR02

```java
@Component   // SmartLifecycle: mở cổng khi Spring khởi động xong, đóng khi tắt
public class SocketServer implements SmartLifecycle {
    public void start() {
        server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress(cfg.port()), 100);
        workers = Executors.newFixedThreadPool(cfg.threads());          // mỗi client một luồng
        acceptor = new Thread(() -> {
            while (running) {
                Socket s = server.accept();                               // chờ client kết nối
                s.setTcpNoDelay(true);
                s.setSoTimeout((int) cfg.idleTimeout().toMillis());
                workers.execute(new ClientConnection(s, dispatcher, maxFrameBytes));
            }
        }, "socket-acceptor");
        acceptor.start();
    }
}

class ClientConnection implements Runnable {                              // vòng đời một kết nối
    public void run() {
        byte[] frame;
        while ((frame = FrameCodec.read(in, maxFrameBytes)) != null) {     // null = client đóng socket
            FrameCodec.write(out, dispatcher.handle(frame));
        }
    }
}
```

`SocketDispatcher` gom action của mọi `SocketController` (mỗi module một lớp), rồi với mỗi khung:

1. Đọc JSON (`BAD_FRAME` nếu hỏng), tìm action (`UNKNOWN_ACTION`).
2. Action cần đăng nhập: `jwt.parseUserId(token)` → `users.findById` — **đọc lại từ CSDL mỗi yêu cầu**
   (BR02) → `u.canUseSystem("CNTT")` (BR01: còn hoạt động, đúng khoa). Sai → `UNAUTHORIZED`.
3. So cấp vai `USER < CONTENT_ADMIN < SYSTEM_ADMIN` với vai tối thiểu của action → `FORBIDDEN`.
4. Gọi service, bọc kết quả `{ id, ok: true, data }`; `ApiException` → `{ id, ok: false, error }`.

```java
public class ChatSocketController implements SocketController {
    public void register(Routes routes) {
        routes.secured("chat.ask", Role.USER, r -> {
            AskRequest req = r.body(AskRequest.class);                    // Jackson + Bean Validation
            return chat.ask(r.me().id(), req.questionId(), req.conversationId());
        });
    }
}
```

### 8.6. Ví dụ service — UC05

```java
@Service @RequiredArgsConstructor
public class ChatService {
    private final QuestionRepository questions;      // Optional<Question> findByIdAndActiveTrue(UUID id)
    private final ConversationRepository conversations; // Optional<Conversation> findByIdAndUserId(UUID id, UUID userId)
    private final MessageRepository messages;
    private final UserRepository users;

    @Transactional
    public ChatResponse ask(AuthUser me, AskRequest req) {
        Question q = questions.findByIdAndActiveTrue(req.questionId())
            .orElseThrow(() -> ApiException.notFound("QUESTION_NOT_FOUND", "Câu hỏi không còn."));
        Conversation c = (req.conversationId() == null)
            ? conversations.save(Conversation.start(users.getReferenceById(me.id()), q))
            : conversations.findByIdAndUserId(req.conversationId(), me.id())                 // BR05
                .orElseThrow(() -> ApiException.notFound("CONVERSATION_NOT_FOUND", "Không tìm thấy hội thoại."));
        Message m = messages.save(c.ask(q));
        return ChatResponse.of(c.getId(), m);         // answer = q.getAnswer(), source = q.getSource()
    }
}

public record AskRequest(@NotNull UUID questionId, UUID conversationId) {}
```

### 8.7. Lưu tệp — UC10, UC12

`FileStorage` ghi vào `app.storage-dir`, tên tệp `yyyy/MM/<uuid>.<ext>`, không dùng tên gốc của người
dùng. `DocumentService.upload`:

1. Giải mã base64; > 20 MB → `FILE_TOO_LARGE`. Nhận diện loại theo nội dung: `%PDF` hoặc ZIP + đuôi
   `.docx` (`UNSUPPORTED_FILE` nếu không).
2. Tính SHA-256; `existsByFileHash` → `DUPLICATE_DOCUMENT`.
3. Ghi tệp, đọc `pageCount` bằng `Loader.loadPDF(...)` nếu là PDF, lưu bản ghi.
   Lỗi khi lưu bản ghi → xóa tệp vừa ghi.

`DocumentService.remove`: `questionRepository.existsByDocumentId(id)` → `DOCUMENT_IN_USE`; nếu
không, xóa bản ghi rồi xóa tệp.

### 8.8. Xử lý lỗi

`ApiException(String code, String message)` ném từ service. `SocketDispatcher` bắt và trả khung
`{ "ok": false, "error": { code, message } }`, gồm cả lỗi Bean Validation → `VALIDATION_ERROR`,
`DataIntegrityViolationException` → `CONFLICT`, lỗi bất ngờ → `INTERNAL_ERROR` (ghi log, không lộ chi tiết).
Kết nối **không bị đóng** khi gặp lỗi nghiệp vụ; chỉ đóng khi khung quá lớn hoặc client ngắt.

Phía client, `SocketClient.call` ném `ApiError(code, message)`; lỗi mạng thành `NETWORK_ERROR` và lần
gọi sau tự kết nối lại. `UNAUTHORIZED` đưa người dùng về màn đăng nhập.

### 8.9. Chạy bằng Docker Compose

```yaml
services:
  db:
    image: postgres:16
    environment: { POSTGRES_DB: tangthu, POSTGRES_USER: tangthu, POSTGRES_PASSWORD: ${DB_PASSWORD} }
    volumes: [ "pgdata:/var/lib/postgresql/data" ]
  server:
    build: ./server
    environment:
      DATABASE_URL: jdbc:postgresql://db:5432/tangthu
      DB_USER: tangthu
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      STORAGE_DIR: /data/storage
    volumes: [ "storage:/data/storage" ]
    ports: [ "9090:9090" ]          # cổng TCP socket
    depends_on: [ db ]
volumes: { pgdata: {}, storage: {} }
```

`Dockerfile` hai giai đoạn: `maven:3.9-eclipse-temurin-21` để build, `eclipse-temurin:21-jre` để chạy.
Client không chạy trong Docker: mỗi máy chạy `java -jar tangthu-client.jar <IP máy chủ> 9090`.

---

## 9. Kiểm thử và nghiệm thu

Kiểm thử tích hợp `SocketIntegrationTest` (`@SpringBootTest`, server mở cổng ngẫu nhiên, client TCP thật,
CSDL PostgreSQL thử nghiệm):

| # | Kịch bản | Kết quả mong đợi |
|---|---|---|
| P1 | Nhiều yêu cầu trên một kết nối | Phản hồi đúng thứ tự, cùng `id` |
| P2 | Khung không phải JSON / action lạ | `BAD_FRAME` / `UNKNOWN_ACTION`, kết nối vẫn mở |
| P3 | Khung khai báo dài hơn 32 MB | `FRAME_TOO_LARGE`, server đóng kết nối |
| P4 | 10 client đồng thời × 20 yêu cầu | Tất cả thành công |
| 1 | Đăng nhập đúng / sai mật khẩu / tài khoản khóa / `department ≠ CNTT` | `ok` / `UNAUTHORIZED` ×3 |
| 2 | Khóa tài khoản rồi dùng lại token cũ | `UNAUTHORIZED` ở yêu cầu kế tiếp (BR02) |
| 3 | Đổi vai USER → CONTENT_ADMIN rồi gửi `topic.create` bằng token cũ | `ok` (vai mới có hiệu lực ngay) |
| 4 | Quản trị viên tự khóa / tự hạ vai | `SELF_MODIFICATION` |
| 5 | Sinh viên gửi `document.upload` | `FORBIDDEN` |
| 6 | `chat.ask` với câu hỏi đang hiện | `ok`, `answer` và `source` khớp câu hỏi |
| 7 | `chat.ask` với câu hỏi đã ẩn | `QUESTION_NOT_FOUND` |
| 8 | Xem hội thoại của người khác | `CONVERSATION_NOT_FOUND` |
| 9 | Tải lên cùng một tệp hai lần | Lần 2: `DUPLICATE_DOCUMENT` |
| 10 | Gỡ tài liệu còn câu hỏi dẫn tới | `DOCUMENT_IN_USE` |
| 11 | Xóa chủ đề còn câu hỏi | `TOPIC_NOT_EMPTY` |
| 12 | Tạo câu hỏi với `page` > `page_count` | `VALIDATION_ERROR` |
| 13 | Ẩn câu hỏi rồi xem lại hội thoại cũ có câu hỏi đó | Vẫn hiển thị câu hỏi và câu trả lời |
| 14 | `system.health` | `status`, `database` = `UP` |

Phía client: `SocketClientTest` chạy với một server giả trên cổng ngẫu nhiên (gửi / nhận khung, tiếng Việt
có dấu, lỗi `FORBIDDEN`, không có server → `NETWORK_ERROR`).

Nghiệm thu thủ công theo ba vai: sinh viên chọn chủ đề → câu hỏi → mở nguồn đúng trang; giáo vụ tải
tài liệu, tạo chủ đề, soạn câu hỏi rồi thấy nút mới phía sinh viên; quản trị viên khóa tài khoản,
đổi vai, xem trạng thái hệ thống.

---

## 10. Đối chiếu thiết kế ↔ mã nguồn

| Thiết kế | Mã nguồn Spring Boot |
|---|---|
| Module M1–M4 (BFD) | Mỗi module một package, trong đó chia tầng MVC: `account` (M1), `chat` (M2), `content` (M3), `admin` (M4); phía client là `client/src/main/java/vn/tangthu/client/<module>/` |
| Mô hình MVC | Server: `model/` (Model), `controller/` (Controller — action socket), `dto/` → JSON. Client: `model/`, `controller/`, `view/` (Swing) |
| Giao tiếp client–server (TCP/IP) | `common/socket/` (server) ↔ `common/net/` (client), mục 6 |
| 3 tác nhân (DFD, use case) | `Role` + cấp vai trong `SocketDispatcher`; tab hiện theo vai ở `MainFrame` |
| 6 thực thể (ERD) | 6 bảng trong `V1__init.sql` |
| 6 lớp (biểu đồ lớp) | 6 `@Entity`: `User`, `Topic`, `Question`, `Document`, `Conversation`, `Message` |
| Hợp thành Conversation ◆ Message | `cascade = ALL, orphanRemoval = true` + `ON DELETE CASCADE` |
| `User.login`, `User.assignRole` | Phương thức trên entity `User` |
| `Conversation.ask(q): Message` | `Conversation.ask`, gọi từ `ChatService.ask` |
| `Question.getAnswer`, `getSource` | Getter trên entity `Question` |
| `Topic.getQuestions`, `addQuestion` | Phương thức trên entity `Topic` |
| `Document.upload`, `remove` | `DocumentService.upload`, `DocumentService.remove` (cần lưu / xóa tệp) |
| BR01–BR06 | Mục 4 |
