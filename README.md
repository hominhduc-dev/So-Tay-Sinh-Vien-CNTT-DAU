# Sổ Tay Sinh Viên CNTT

Hệ thống tra cứu quy định học vụ cho sinh viên **ngành Công nghệ Thông tin — Trường Đại học Kiến trúc
Đà Nẵng**. Đồ án học phần **Lập trình mạng máy tính**, xây dựng theo mô hình **client–server**.

Sinh viên dùng một **chatbot dạng nút chọn**: chọn chủ đề → chọn câu hỏi → nhận câu trả lời do giáo vụ
khoa soạn sẵn, kèm **tài liệu nguồn và số trang** để tự kiểm chứng. Hệ thống không dùng AI và không nhận
câu hỏi gõ tự do — câu trả lời luôn đúng nội dung giáo vụ đã duyệt.

![Khung chat](docs/images/chat.png)

## Chức năng

| Vai | Làm được gì |
|---|---|
| **Sinh viên** (`USER`) | Chọn chủ đề và câu hỏi, nhận câu trả lời kèm nguồn; mở tệp gốc đúng trang; xem lại hội thoại; tra cứu kho tài liệu |
| **Giáo vụ khoa** (`CONTENT_ADMIN`) | Mọi quyền của sinh viên + tải lên / đổi tên / gỡ tài liệu; tạo chủ đề; soạn câu hỏi – câu trả lời và gắn nguồn |
| **Quản trị viên** (`SYSTEM_ADMIN`) | Mọi quyền của giáo vụ + tra cứu, khóa / mở, phân quyền tài khoản; kiểm tra trạng thái hệ thống |

Quy tắc chính: mỗi request đọc lại vai từ CSDL nên khóa tài khoản hay đổi vai có hiệu lực ngay; quản trị
viên không tự khóa hay tự hạ vai; mỗi câu trả lời bắt buộc có tài liệu nguồn.

| Soạn nội dung (giáo vụ) | Quản trị tài khoản |
|---|---|
| ![Nội dung](docs/images/noi-dung.png) | ![Quản trị](docs/images/quan-tri.png) |

## Kiến trúc

```
 Trình duyệt ──HTTP──▶  client (Next.js, :3000)  ──/api/* ──▶  server (Spring Boot, :8080)  ──JDBC──▶  PostgreSQL 16
 (máy bất kỳ trong LAN)    giao diện                         REST API JSON, JWT                         6 bảng
                                                             + thư mục lưu tệp PDF / DOCX
```

- **Client:** trình duyệt gọi `/api/...` trên chính máy web; Next.js chuyển tiếp sang server, nên máy nào
  trong LAN mở `http://<IP máy chủ>:3000` cũng dùng được, không vướng CORS.
- **Server:** REST API, mỗi thao tác là một cặp request / response JSON. Xác thực bằng JWT (header
  `Authorization: Bearer`), mật khẩu băm BCrypt.
- **CSDL:** 6 bảng — `users`, `topics`, `questions`, `documents`, `conversations`, `messages` — quản lý
  bằng Flyway.

| Thành phần | Công nghệ |
|---|---|
| Server | Java 21, Spring Boot 3.3 (Web, Data JPA, Security, Validation), Flyway, jjwt, PDFBox |
| Client | Next.js 16, React 19, TypeScript, Tailwind CSS 4 |
| CSDL | PostgreSQL 16 |
| Triển khai | Docker Compose (db + api + web) |

## Chạy nhanh bằng Docker

Cần Docker và Docker Compose.

```bash
git clone https://github.com/hominhduc-dev/knowledge-base-rag.git
cd knowledge-base-rag
cp .env.example .env          # sửa DB_PASSWORD và JWT_SECRET (≥ 32 ký tự)
docker compose up --build
```

Mở **http://localhost:3000**. API ở `http://localhost:8080/api`.

### Tài khoản demo

Mật khẩu chung: `Matkhau@123`

| Đăng nhập | Vai |
|---|---|
| `SV001` | Sinh viên |
| `GV001` | Giáo vụ khoa |
| `QTV001` | Quản trị viên |

Dữ liệu demo tự khởi tạo khi chạy ứng dụng với cấu hình mặc định. Đăng nhập `SV001` → **Hỏi đáp**
để thử 12 câu hỏi mẫu thuộc 6 chủ đề. Mỗi câu trả lời dẫn đến một trang trong PDF **[DEMO] Sổ tay học vụ mẫu**.

> Khi triển khai thật: đặt `FLYWAY_LOCATIONS=classpath:db/migration` trong `.env` để **không** tạo tài khoản
> demo, và đổi `JWT_SECRET`, `DB_PASSWORD`.

## Chạy khi phát triển (không dùng Docker)

Cần Java 21, Maven 3.9, Node.js 22, PostgreSQL 16.

```bash
# 1. CSDL
createuser -P tangthu                 # mật khẩu: tangthu
createdb -O tangthu tangthu

# 2. Server — http://localhost:8080
cd server
export JWT_SECRET="chuoi-ngau-nhien-dai-it-nhat-32-ky-tu"
mvn spring-boot:run

# 3. Client — http://localhost:3000 (terminal khác)
cd client
npm install
npm run dev
```

Chi tiết biến môi trường: [`server/README.md`](server/README.md), [`client/README.md`](client/README.md).

## Kiểm thử

```bash
# Server: 15 kiểm thử tích hợp trên PostgreSQL thật (cần database tangthu_test)
createdb -O tangthu tangthu_test
cd server && mvn test

# Client: kiểm tra kiểu, lint, build production
cd client && npm run typecheck && npm run lint && npm run build
```

Các kịch bản kiểm thử (đăng nhập, khóa tài khoản có hiệu lực ngay, đổi vai, tự sửa chính mình, chọn câu
hỏi, câu hỏi đã ẩn, hội thoại của người khác, tải trùng tệp, gỡ tài liệu đang dùng…) liệt kê ở
`docs/DAC-TA-TRIEN-KHAI.md` mục 9.

## Cấu trúc thư mục — mô hình MVC, chia theo module

```
.
├── server/                          Server Spring Boot — Model + Controller
│   └── src/main/java/vn/tangthu/
│       ├── common/                  dùng chung: config, security (JWT), exception, util
│       ├── account/                 M1 · Tài khoản
│       ├── chat/                    M2 · Hỏi đáp
│       ├── content/                 M3 · Nội dung
│       └── admin/                   M4 · Quản trị
│           (mỗi module: model/ repository/ service/ controller/ dto/)
├── client/                          Web client Next.js — View
│   └── src/
│       ├── app/                     định tuyến các trang (/login, /chat, /documents, /content, /admin/…)
│       ├── modules/
│       │   ├── account/             M1 · đăng nhập, phiên (useAuth, LoginForm, AuthGuard)
│       │   ├── chat/                M2 · khung chat (ChatBox, AnswerCard)
│       │   ├── content/             M3 · tài liệu, chủ đề, câu hỏi (DocumentManager, ContentManager…)
│       │   └── admin/               M4 · tài khoản, trạng thái hệ thống (UserTable, HealthPanel…)
│       │       (mỗi module: views/ = giao diện, api = gọi REST tới module cùng tên ở server)
│       ├── components/              khung trang dùng chung (Header, MainNav, Sidebar), nút, ô nhập
│       └── lib/                     api-client (gọi /api, xử lý lỗi, mở tệp nguồn)
├── docs/                            SRS, đặc tả triển khai, báo cáo, sơ đồ, slide, ảnh chụp
├── docker-compose.yml               db + server + client
└── .env.example
```

| Tầng | Ở đâu | Vai trò |
|---|---|---|
| **Model** | `server/.../<module>/model/`, `repository/` | Entity JPA ánh xạ bảng PostgreSQL; truy vấn dữ liệu |
| **Controller** | `server/.../<module>/controller/` (+ `service/`) | Nhận request HTTP, kiểm tra quyền, xử lý nghiệp vụ, trả JSON |
| **View** | `client/src/modules/<module>/views/` | Giao diện trên trình duyệt; gọi API qua `client/src/modules/<module>/api` |

## Phân công module

Mỗi thành viên phụ trách trọn **một module ở cả server và client**.

| Module | Use case | Server | Client | Người phụ trách |
|---|---|---|---|---|
| M1 · Tài khoản | UC01–UC04 | `server/src/main/java/vn/tangthu/account/` | `client/src/modules/account/` | _Họ tên — @github_ |
| M2 · Hỏi đáp | UC05, UC06, UC08 | `server/src/main/java/vn/tangthu/chat/` | `client/src/modules/chat/` | _Họ tên — @github_ |
| M3 · Nội dung | UC07, UC10–UC12, UC22, UC23 | `server/src/main/java/vn/tangthu/content/` | `client/src/modules/content/` | _Họ tên — @github_ |
| M4 · Quản trị | UC14–UC16, UC20 | `server/src/main/java/vn/tangthu/admin/` | `client/src/modules/admin/` | _Họ tên — @github_ |
| Dùng chung | — | `server/.../common/`, `db/` | `client/src/components/`, `lib/`, `app/` | _Nhóm trưởng_ |

## Tài liệu

| Tệp | Nội dung |
|---|---|
| [`docs/reports/bao-cao-so-tay-sinh-vien-cntt.docx`](docs/reports/bao-cao-so-tay-sinh-vien-cntt.docx) | Báo cáo phân tích và thiết kế hệ thống |
| [`docs/diagrams/so-do-he-thong.drawio`](docs/diagrams/so-do-he-thong.drawio) | Bộ sơ đồ hệ thống tổng hợp trong một file nhiều trang |
| [`docs/SRS.md`](docs/SRS.md) | Đặc tả yêu cầu phần mềm: phạm vi, tác nhân, 17 use case, yêu cầu dữ liệu, giao diện, phi chức năng, quy tắc nghiệp vụ, ma trận truy vết |
| [`docs/DAC-TA-TRIEN-KHAI.md`](docs/DAC-TA-TRIEN-KHAI.md) | Đặc tả triển khai: lược đồ CSDL, API REST, luồng xử lý, hướng dẫn Spring Boot, kịch bản kiểm thử |
| [`docs/slides/SRS-tac-nhan-va-use-case.pptx`](docs/slides/SRS-tac-nhan-va-use-case.pptx) | Slide SRS: tác nhân, use case, ma trận quyền, input / output, luồng |

## Ghi chú

Phiên bản trước của dự án (Node.js / Express, hỏi đáp tự do bằng RAG với Gemini) đã được thay thế; mã cũ
vẫn xem lại được trong lịch sử git.
