# Đặc tả triển khai — Sổ Tay Sinh Viên CNTT (Spring Boot)

**Phiên bản:** 1.0 · 07/11/2026
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
 Trình duyệt (client)  ──HTTP request (JSON, JWT)──▶  Spring Boot server  ──JDBC──▶  PostgreSQL 16
 Sinh viên / Giáo vụ / ◀──HTTP response (JSON/tệp)──  REST API /api/*                 6 bảng
 Quản trị viên                                         + thư mục lưu tệp tài liệu
```

- Client chỉ gọi REST API; không truy cập CSDL trực tiếp.
- Mỗi lượt bấm nút là **một request / một response**. Không SSE, không WebSocket.
- Client có thể là bất kỳ web app nào (React, Next.js, Thymeleaf…); tài liệu này chỉ quy định server.

### 1.3. Công nghệ

| Thành phần | Lựa chọn |
|---|---|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot 3.3.x: Web, Data JPA, Security, Validation, Actuator (tùy chọn) |
| CSDL | PostgreSQL 16, migration bằng Flyway |
| Xác thực | JWT (thư viện `io.jsonwebtoken:jjwt` 0.12.x), mật khẩu BCrypt |
| Đọc số trang PDF | Apache PDFBox 3.x |
| Build | Maven |
| Chạy | Docker Compose: `postgres:16` + ứng dụng |

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
| BR01 | Tài khoản phải đang hoạt động và thuộc khoa CNTT mới được đăng nhập | Kiểm tra `is_active` và `department = 'CNTT'` khi đăng nhập **và** ở mỗi request |
| BR02 | Mỗi request kiểm tra lại quyền từ CSDL; đổi vai hoặc khóa có hiệu lực ngay request sau | JWT chỉ chứa `userId`; filter đọc lại `users` ở mỗi request |
| BR03 | Sinh viên chỉ đọc; giáo vụ và quản trị viên mới ghi nội dung; chỉ quản trị viên đổi tài khoản | `@PreAuthorize` theo vai trên từng endpoint |
| BR04 | Quản trị viên không tự khóa hay tự hạ vai của chính mình | `AdminService` từ chối khi `targetId == currentUserId` → 400 `SELF_MODIFICATION` |
| BR05 | Hội thoại thuộc riêng người tạo; mở nguồn và tệp gốc phải đăng nhập | Truy vấn hội thoại luôn kèm `user_id`; không phải chủ → 404 |
| BR06 | Chatbot chỉ trả lời các câu hỏi có sẵn; mỗi câu trả lời soạn sẵn phải gắn tài liệu nguồn | `POST /api/chat` chỉ nhận `questionId`; `UC23` bắt buộc `documentId` và `page` |

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
    file_hash   char(64)     NOT NULL UNIQUE,            -- SHA-256, chặn tải trùng
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

### 5.3. Dữ liệu mẫu — `V2__seed.sql`

Tạo một tài khoản mỗi vai (`department = 'CNTT'`, mật khẩu BCrypt) và 6 chủ đề: Đăng ký học phần,
Đồ án tốt nghiệp, Chuẩn đầu ra, Bảo lưu – nghỉ học, Cảnh báo học vụ, Xét tốt nghiệp. Câu hỏi mẫu cần
tài liệu nguồn nên nhập qua giao diện giáo vụ (UC10 → UC23) sau khi chạy.

---

## 6. API REST

### 6.1. Quy ước chung

- Gốc: `/api`. Dữ liệu JSON UTF-8, trừ tải lên (`multipart/form-data`) và tải tệp (nhị phân).
- Xác thực: header `Authorization: Bearer <jwt>` cho mọi endpoint trừ `POST /api/auth/login`.
- Phân trang: `?page=0&size=20` (size ≤ 100); response dạng
  `{ "items": [...], "page": 0, "size": 20, "total": 57 }`.
- Lỗi: luôn có dạng

```json
{ "error": { "code": "QUESTION_NOT_FOUND", "message": "Câu hỏi không còn." } }
```

| HTTP | Khi nào |
|---|---|
| 400 | Dữ liệu không hợp lệ (`VALIDATION_ERROR`), tự sửa chính mình (`SELF_MODIFICATION`) |
| 401 | Thiếu / sai token, tài khoản bị khóa hoặc không thuộc CNTT (`UNAUTHORIZED`) |
| 403 | Đủ đăng nhập nhưng thiếu vai (`FORBIDDEN`) |
| 404 | Không tồn tại hoặc không thuộc người gọi (`*_NOT_FOUND`) |
| 409 | Xung đột dữ liệu (`DUPLICATE_DOCUMENT`, `DOCUMENT_IN_USE`, `TOPIC_NOT_EMPTY`) |
| 413 / 415 | Tệp quá lớn / sai định dạng |

### 6.2. M1 · Tài khoản

**UC01 — `POST /api/auth/login`** (công khai)

```json
// request
{ "login": "2051220001", "password": "••••••••" }       // login = mã hoặc email, ≤ 255
// 200
{ "token": "eyJ...", "user": { "id": "…", "code": "2051220001", "email": "…",
  "fullName": "Nguyễn Văn A", "role": "USER", "department": "CNTT" } }
```
Lỗi: 401 nếu sai thông tin, `is_active = false`, hoặc `department ≠ CNTT` (cùng một thông báo, không
tiết lộ lý do). Token hết hạn sau 8 giờ.

**UC03 — `GET /api/auth/me`** → 200 `{ "user": {…} }` như trên.

**UC04 — `PUT /api/auth/password`**
```json
{ "currentPassword": "…", "newPassword": "…" }           // newPassword 8–72 ký tự
```
→ 204. Lỗi 400 nếu mật khẩu hiện tại sai.

### 6.3. M2 · Hỏi đáp

**`GET /api/topics`** (mọi vai) → 200
```json
[ { "id": "…", "name": "Đăng ký học phần", "questionCount": 7 } ]
```
Sắp theo `sort_order`; chỉ đếm câu hỏi `is_active`.

**`GET /api/topics/{id}/questions`** (mọi vai) → 200
```json
[ { "id": "…", "content": "Mỗi học kỳ đăng ký tối đa bao nhiêu tín chỉ?" } ]
```
Chỉ trả câu hỏi `is_active`, sắp theo `sort_order`. Chủ đề không tồn tại → 404 `TOPIC_NOT_FOUND`.
Danh sách rỗng → client hiện "Chủ đề chưa có câu hỏi".

**UC05 — `POST /api/chat`** (mọi vai)
```json
// request
{ "questionId": "…", "conversationId": null }            // null = bắt đầu hội thoại mới
// 201
{
  "conversationId": "…",
  "message": {
    "id": "…", "sentAt": "2026-11-07T08:15:00Z",
    "question": { "id": "…", "content": "Mỗi học kỳ đăng ký tối đa bao nhiêu tín chỉ?" },
    "answer": "Theo Điều 9 Quy chế đào tạo, sinh viên đăng ký tối đa 25 tín chỉ…",
    "source": { "documentId": "…", "title": "Quy chế đào tạo 2024", "page": 12 }
  }
}
```
Lỗi: 404 `QUESTION_NOT_FOUND` nếu câu hỏi không tồn tại hoặc đã ẩn; 404 `CONVERSATION_NOT_FOUND`
nếu hội thoại không thuộc người gọi. Hội thoại mới lấy `title` = nội dung câu hỏi đầu tiên.

**UC06 — `GET /api/conversations`** (của chính mình, phân trang, mới nhất trước)
```json
{ "items": [ { "id": "…", "title": "…", "createdAt": "…", "messageCount": 3 } ], … }
```

**UC06 — `GET /api/conversations/{id}`** → 200 `{ "id", "title", "createdAt", "messages": [ <message như UC05> ] }`.
Không phải chủ → 404.

**UC08 — `GET /api/documents/{id}/file`** (mọi vai) → tệp gốc, `Content-Type` theo `mime_type`,
`Content-Disposition: inline`. Client mở PDF tại trang bằng `…/file#page=12`.

### 6.4. M3 · Nội dung

**UC07 — `GET /api/documents?q=&page=&size=`** (mọi vai) — tìm theo tiêu đề (`ILIKE`, `q` ≤ 200)
```json
{ "items": [ { "id": "…", "title": "…", "mimeType": "application/pdf", "pageCount": 48,
  "uploadedBy": "Trần Thị B", "createdAt": "…" } ], … }
```
**`GET /api/documents/{id}`** → một mục như trên.

**UC10 — `POST /api/documents`** (CONTENT_ADMIN), `multipart/form-data`: `file` (PDF/DOCX,
≤ 20 MB), `title` (3–500 ký tự) → 201 tài liệu vừa tạo.
Lỗi: 409 `DUPLICATE_DOCUMENT` (trùng SHA-256), 415 sai định dạng, 413 quá lớn.

**UC11 — `PATCH /api/documents/{id}`** (CONTENT_ADMIN) `{ "title": "…" }` → 200.

**UC12 — `DELETE /api/documents/{id}`** (CONTENT_ADMIN) → 204, xóa cả tệp.
Lỗi: 409 `DOCUMENT_IN_USE` nếu còn câu hỏi dẫn tới.

**UC22 — chủ đề** (CONTENT_ADMIN)

| Method | Đường dẫn | Body | Kết quả |
|---|---|---|---|
| POST | `/api/topics` | `{ "name": "…", "sortOrder": 1 }` | 201 |
| PATCH | `/api/topics/{id}` | `{ "name"?, "sortOrder"? }` | 200 |
| DELETE | `/api/topics/{id}` | — | 204; 409 `TOPIC_NOT_EMPTY` nếu còn câu hỏi |

**UC23 — câu hỏi – câu trả lời** (CONTENT_ADMIN)

| Method | Đường dẫn | Body | Kết quả |
|---|---|---|---|
| GET | `/api/admin/questions?topicId=` | — | Mọi câu hỏi của chủ đề, kể cả đã ẩn, kèm `answer`, `documentId`, `page`, `active` |
| POST | `/api/questions` | `{ "topicId", "content" (≤200), "answer" (≤5000), "documentId", "page", "sortOrder"? }` | 201 |
| PATCH | `/api/questions/{id}` | các trường như POST, thêm `"active"` | 200 |
| DELETE | `/api/questions/{id}` | — | 204, đặt `is_active = false` |

Kiểm tra: `page ≥ 1` và `page ≤ page_count` khi tài liệu có `page_count`; chủ đề và tài liệu phải
tồn tại (400 `VALIDATION_ERROR` nếu sai).

### 6.5. M4 · Quản trị

**UC14 — `GET /api/users?q=&page=&size=`** (SYSTEM_ADMIN) — tìm theo mã, email, họ tên
```json
{ "items": [ { "id", "code", "email", "fullName", "role", "department", "active" } ], … }
```

**UC15, UC16 — `PATCH /api/users/{id}`** (SYSTEM_ADMIN)
```json
{ "active": false }            // UC15 khóa / mở
{ "role": "CONTENT_ADMIN" }    // UC16 phân quyền
```
→ 200 người dùng sau khi sửa. Lỗi 400 `SELF_MODIFICATION` nếu `id` là chính người gọi (BR04).
Có hiệu lực ngay request kế tiếp của người bị sửa (BR02).

**UC20 — `GET /api/health`** (SYSTEM_ADMIN)
```json
{ "status": "UP", "database": "UP", "uptimeSeconds": 86400, "time": "2026-11-07T08:15:00Z" }
```
`database` = kết quả `SELECT 1`; lỗi CSDL → `"DOWN"`, `status` = `"DEGRADED"`, vẫn trả 200.

---

## 7. Luồng xử lý chính

**UC05 — Chọn câu hỏi, nhận trả lời**

1. Client gọi `GET /api/topics`, hiện các nút chủ đề.
2. Server kiểm tra token, tài khoản, vai (2a: sai → 401).
3. Sinh viên chọn chủ đề; client gọi `GET /api/topics/{id}/questions` (3a: rỗng → "chưa có câu hỏi").
4. Sinh viên bấm một nút; client gửi `POST /api/chat { questionId, conversationId }`.
5. Server đọc câu hỏi `is_active`, câu trả lời và tài liệu nguồn (5a: không còn → 404).
6. Server lưu hội thoại (nếu mới) và một `message` trong cùng transaction.
7. Server trả câu trả lời kèm nguồn; client hiện thành một lượt hỏi – đáp.
8. Sinh viên mở nguồn (UC08) bằng `GET /api/documents/{id}/file#page=N`.

**UC10 → UC22 → UC23 — Nhập nội dung (Giáo vụ)**

1. Tải lên PDF / DOCX, nhập tiêu đề (UC10).
2. Server kiểm quyền, tính SHA-256 (trùng → 409), lưu tệp vào `storage/yyyy/MM/<uuid>.<ext>`.
3. Server lưu bản ghi `documents`, đọc `page_count` bằng PDFBox nếu là PDF.
4. Chọn hoặc tạo chủ đề (UC22).
5. Soạn câu hỏi và câu trả lời (UC23).
6. Gắn tài liệu nguồn, số trang; server kiểm tra (lỗi → 400, giáo vụ sửa và gửi lại).
7. Lưu thành công → câu hỏi hiện thành nút ngay ở lần sinh viên mở chủ đề kế tiếp.

**UC14 → UC15 / UC16 — Quản trị tài khoản**

1. Quản trị viên mở danh sách tài khoản (UC14).
2. Chọn khóa / mở (UC15) hoặc đổi vai (UC16).
3. Server kiểm tra thao tác lên chính mình? Có → 400 `SELF_MODIFICATION`.
4. Cập nhật `users.is_active` hoặc `users.role` trong transaction.
5. Request kế tiếp của tài khoản đó đọc lại quyền từ CSDL (BR02).

---

## 8. Hiện thực bằng Spring Boot

### 8.1. Cấu trúc dự án

```
server-java/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/vn/tangthu/
    │   ├── TangThuApplication.java
    │   ├── common/            # SecurityConfig, JwtService, JwtAuthFilter, AuthUser,
    │   │                      # ApiException, GlobalExceptionHandler, PageResponse, FileStorage
    │   ├── account/           # M1: User, Role, UserRepository, AuthController, AuthService
    │   ├── chat/              # M2: Conversation, Message, repositories, ChatController, ChatService
    │   ├── content/           # M3: Topic, Question, Document, repositories,
    │   │                      #     TopicController, QuestionController, DocumentController, services
    │   └── admin/             # M4: AdminController (users), HealthController, AdminService
    └── resources/
        ├── application.yml
        └── db/migration/      # V1__init.sql, V2__seed.sql
```

Mỗi package ứng với một module của BFD. Controller chỉ nhận/trả DTO (`record`); entity không lộ ra JSON.

### 8.2. `pom.xml` — phụ thuộc chính

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.3.5</version>
</parent>
<properties><java.version>21</java.version></properties>
<dependencies>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
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
  <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
</dependencies>
```

### 8.3. `application.yml`

```yaml
server:
  port: 8080
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/tangthu}
    username: ${DB_USER:tangthu}
    password: ${DB_PASSWORD:tangthu}
  jpa:
    hibernate.ddl-auto: validate      # schema do Flyway quản lý
    open-in-view: false
  servlet.multipart:
    max-file-size: 20MB
    max-request-size: 21MB
app:
  jwt:
    secret: ${JWT_SECRET}             # ≥ 32 byte, bắt buộc
    ttl: PT8H
  storage-dir: ${STORAGE_DIR:./storage}
  department: CNTT
  cors-origins: ${CORS_ORIGINS:http://localhost:3000}
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

### 8.5. Bảo mật — JWT và BR01, BR02

```java
public record AuthUser(UUID id, Role role) {}

@Component @RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;
    @Value("${app.department}") private String department;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (h != null && h.startsWith("Bearer ")) {
            jwt.parseUserId(h.substring(7))                       // Optional<UUID>, rỗng nếu token sai/hết hạn
               .flatMap(users::findById)                          // BR02: đọc lại từ CSDL mỗi request
               .filter(u -> u.isActive() && department.equals(u.getDepartment()))   // BR01
               .ifPresent(u -> SecurityContextHolder.getContext().setAuthentication(
                   new UsernamePasswordAuthenticationToken(new AuthUser(u.getId(), u.getRole()), null,
                       List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole())))));
        }
        chain.doFilter(req, res);
    }
}

@Configuration @EnableMethodSecurity @RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtFilter;

    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(c -> c.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((rq, rs, ex) -> ApiErrors.write(rs, 401, "UNAUTHORIZED", "Cần đăng nhập."))
                .accessDeniedHandler((rq, rs, ex) -> ApiErrors.write(rs, 403, "FORBIDDEN", "Không đủ quyền.")))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
    @Bean static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_SYSTEM_ADMIN > ROLE_CONTENT_ADMIN\nROLE_CONTENT_ADMIN > ROLE_USER");
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
```

Phân quyền từng endpoint bằng `@PreAuthorize("hasRole('CONTENT_ADMIN')")` (giáo vụ và quản trị viên
nhờ `RoleHierarchy`) hoặc `hasRole('SYSTEM_ADMIN')`.

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

1. Kiểm tra `contentType` ∈ {`application/pdf`,
   `application/vnd.openxmlformats-officedocument.wordprocessingml.document`} (415 nếu không).
2. Tính SHA-256 trên luồng tệp; `existsByFileHash` → 409 `DUPLICATE_DOCUMENT`.
3. Ghi tệp, đọc `pageCount` bằng `Loader.loadPDF(...)` nếu là PDF, lưu bản ghi.
   Lỗi khi lưu bản ghi → xóa tệp vừa ghi.

`DocumentService.remove`: `questionRepository.existsByDocumentId(id)` → 409 `DOCUMENT_IN_USE`; nếu
không, xóa bản ghi rồi xóa tệp.

### 8.8. Xử lý lỗi

`ApiException(HttpStatus status, String code, String message)` ném từ service.
`@RestControllerAdvice GlobalExceptionHandler` chuyển thành JSON lỗi ở mục 6.1, gồm cả
`MethodArgumentNotValidException` → 400 `VALIDATION_ERROR`,
`MaxUploadSizeExceededException` → 413, `DataIntegrityViolationException` → 409.

### 8.9. Chạy bằng Docker Compose

```yaml
services:
  db:
    image: postgres:16
    environment: { POSTGRES_DB: tangthu, POSTGRES_USER: tangthu, POSTGRES_PASSWORD: ${DB_PASSWORD} }
    volumes: [ "pgdata:/var/lib/postgresql/data" ]
  api:
    build: ./server-java
    environment:
      DATABASE_URL: jdbc:postgresql://db:5432/tangthu
      DB_USER: tangthu
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      STORAGE_DIR: /data/storage
    volumes: [ "storage:/data/storage" ]
    ports: [ "8080:8080" ]
    depends_on: [ db ]
volumes: { pgdata: {}, storage: {} }
```

`Dockerfile` hai giai đoạn: `maven:3.9-eclipse-temurin-21` để build, `eclipse-temurin:21-jre` để chạy.

---

## 9. Kiểm thử và nghiệm thu

Kiểm thử tích hợp (`@SpringBootTest` + `MockMvc`, CSDL PostgreSQL thử nghiệm) tối thiểu:

| # | Kịch bản | Kết quả mong đợi |
|---|---|---|
| 1 | Đăng nhập đúng / sai mật khẩu / tài khoản khóa / `department ≠ CNTT` | 200 / 401 / 401 / 401 |
| 2 | Khóa tài khoản rồi dùng lại token cũ | 401 ở request kế tiếp (BR02) |
| 3 | Đổi vai USER → CONTENT_ADMIN rồi gọi `POST /api/topics` bằng token cũ | 201 (vai mới có hiệu lực ngay) |
| 4 | Quản trị viên tự khóa / tự hạ vai | 400 `SELF_MODIFICATION` |
| 5 | Sinh viên gọi `POST /api/documents` | 403 |
| 6 | `POST /api/chat` với câu hỏi đang hiện | 201, `answer` và `source` khớp câu hỏi |
| 7 | `POST /api/chat` với câu hỏi đã ẩn | 404 `QUESTION_NOT_FOUND` |
| 8 | Xem hội thoại của người khác | 404 |
| 9 | Tải lên cùng một tệp hai lần | Lần 2: 409 `DUPLICATE_DOCUMENT` |
| 10 | Gỡ tài liệu còn câu hỏi dẫn tới | 409 `DOCUMENT_IN_USE` |
| 11 | Xóa chủ đề còn câu hỏi | 409 `TOPIC_NOT_EMPTY` |
| 12 | Tạo câu hỏi với `page` > `page_count` | 400 `VALIDATION_ERROR` |
| 13 | Ẩn câu hỏi rồi xem lại hội thoại cũ có câu hỏi đó | Vẫn hiển thị câu hỏi và câu trả lời |
| 14 | `GET /api/health` khi CSDL dừng | 200, `database: "DOWN"` |

Nghiệm thu thủ công theo ba vai: sinh viên chọn chủ đề → câu hỏi → mở nguồn đúng trang; giáo vụ tải
tài liệu, tạo chủ đề, soạn câu hỏi rồi thấy nút mới phía sinh viên; quản trị viên khóa tài khoản,
đổi vai, xem trạng thái hệ thống.

---

## 10. Đối chiếu thiết kế ↔ mã nguồn

| Thiết kế | Mã nguồn Spring Boot |
|---|---|
| Module M1–M4 (BFD) | Package `account`, `chat`, `content`, `admin` |
| 3 tác nhân (DFD, use case) | `Role` + `RoleHierarchy` |
| 6 thực thể (ERD) | 6 bảng trong `V1__init.sql` |
| 6 lớp (biểu đồ lớp) | 6 `@Entity`: `User`, `Topic`, `Question`, `Document`, `Conversation`, `Message` |
| Hợp thành Conversation ◆ Message | `cascade = ALL, orphanRemoval = true` + `ON DELETE CASCADE` |
| `User.login`, `User.assignRole` | Phương thức trên entity `User` |
| `Conversation.ask(q): Message` | `Conversation.ask`, gọi từ `ChatService.ask` |
| `Question.getAnswer`, `getSource` | Getter trên entity `Question` |
| `Topic.getQuestions`, `addQuestion` | Phương thức trên entity `Topic` |
| `Document.upload`, `remove` | `DocumentService.upload`, `DocumentService.remove` (cần lưu / xóa tệp) |
| BR01–BR06 | Mục 4 |
