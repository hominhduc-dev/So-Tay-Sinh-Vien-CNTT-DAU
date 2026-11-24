# Sổ Tay Sinh Viên CNTT

Hệ thống tra cứu quy định học vụ cho sinh viên **ngành Công nghệ Thông tin — Trường Đại học Kiến trúc
Đà Nẵng**. Đồ án học phần **Lập trình mạng máy tính**, xây dựng theo mô hình **client–server tách biệt,
giao tiếp qua TCP/IP bằng socket**: client là ứng dụng desktop Java Swing, server là máy chủ Java
`ServerSocket`.

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

Quy tắc chính: mỗi yêu cầu đọc lại vai từ CSDL nên khóa tài khoản hay đổi vai có hiệu lực ngay; quản trị
viên không tự khóa hay tự hạ vai; mỗi câu trả lời bắt buộc có tài liệu nguồn.

| Soạn nội dung (giáo vụ) | Quản trị tài khoản |
|---|---|
| ![Nội dung](docs/images/noi-dung.png) | ![Quản trị](docs/images/quan-tri.png) |
| **Đăng nhập (nhập IP máy chủ)** | **Trạng thái hệ thống, độ trễ TCP** |
| ![Đăng nhập](docs/images/dang-nhap.png) | ![Hệ thống](docs/images/he-thong.png) |

## Kiến trúc

```
 client/ (Java Swing)            TCP/IP, cổng 9090              server/ (Java)                 PostgreSQL 16
 ┌──────────────────┐   java.net.Socket ──▶ ServerSocket   ┌──────────────────────┐  JDBC   ┌──────────┐
 │ máy sinh viên    │ ── [4 byte độ dài][JSON yêu cầu] ──▶ │ luồng nhận kết nối   │ ──────▶ │ 6 bảng   │
 │ máy giáo vụ      │ ◀─ [4 byte độ dài][JSON phản hồi] ── │ + mỗi client 1 luồng │         └──────────┘
 │ … (mọi máy LAN)  │       một kết nối giữ suốt phiên     │ + thư mục lưu tệp    │
 └──────────────────┘                                      └──────────────────────┘
```

- **Giao thức:** mỗi thông điệp là một *khung* — 4 byte độ dài (big-endian) rồi JSON UTF-8.
  Yêu cầu `{ "id", "action", "token", "data" }` → phản hồi `{ "id", "ok", "data" | "error" }`.
  25 action, ví dụ `auth.login`, `chat.ask`, `document.upload`. Đặc tả đầy đủ:
  [`docs/DAC-TA-TRIEN-KHAI.md` mục 6](docs/DAC-TA-TRIEN-KHAI.md#6-giao-thức-tcp-socket).
- **Server:** không dùng HTTP. `SocketServer` mở `ServerSocket`, mỗi client được một luồng trong pool;
  `SocketDispatcher` kiểm token JWT, đọc lại người dùng từ CSDL, kiểm vai rồi chuyển action tới controller
  của module.
- **Client:** `SocketClient` giữ một `Socket` lâu dài, tự nối lại khi mất kết nối; mọi lời gọi mạng chạy
  bằng `SwingWorker` nên giao diện không bị treo. Tệp tải lên / mở nguồn đi trong JSON dạng base64.
- **CSDL:** 6 bảng — `users`, `topics`, `questions`, `documents`, `conversations`, `messages` — quản lý
  bằng Flyway.

| Thành phần | Công nghệ |
|---|---|
| Server | Java 21, Spring Boot 3.3 (không web: Data JPA, Validation), `java.net.ServerSocket`, Flyway, jjwt, BCrypt, PDFBox |
| Client | Java 21 Swing, FlatLaf, Jackson, `java.net.Socket` |
| CSDL | PostgreSQL 16 |
| Triển khai | Docker Compose (db + server); client là một tệp `.jar` chạy trên từng máy |

## Chuẩn bị máy

| Cần | Vì sao | Kiểm tra |
|---|---|---|
| **Docker Desktop** | chạy PostgreSQL 16, và chạy luôn server nếu bạn chọn cách 1 | `docker --version` |
| **JDK 21** | build và chạy cả server lẫn client | `java -version` |
| **Maven 3.9** | chỉ cần khi build ngoài IDE — IntelliJ đã có bản bundled | `mvn -v` |

Trên Windows, cài JDK bằng một lệnh:

```powershell
winget install --exact --id EclipseAdoptium.Temurin.21.JDK
```

> Phải đúng **JDK 21**. Hai `pom.xml` đều khai `release 21` và ảnh Docker dùng `eclipse-temurin:21-jre`;
> chạy Spring Boot 3.3 trên JDK mới hơn (25, 27) rất dễ gãy ở Hibernate / Byte Buddy.

## Thiết lập lần đầu

```bash
git clone https://github.com/hominhduc-dev/So-Tay-Sinh-Vien-CNTT-DAU.git
cd So-Tay-Sinh-Vien-CNTT-DAU
cp .env.example .env
```

Sinh hai chuỗi ngẫu nhiên rồi dán vào `.env`:

```bash
openssl rand -base64 24    # DB_PASSWORD
openssl rand -base64 48    # JWT_SECRET — bắt buộc ≥ 32 ký tự
```

| Biến trong `.env` | Ý nghĩa |
|---|---|
| `DB_PASSWORD` | mật khẩu Postgres trong container |
| `JWT_SECRET` | khoá ký JWT; đổi khoá là mọi token đã phát hết hiệu lực |
| `SOCKET_PORT` | cổng TCP mở ra LAN cho client, mặc định `9090` |
| `FLYWAY_LOCATIONS` | giữ `,classpath:db/demo` để có sẵn tài khoản và câu hỏi demo |
| `DB_HOST_PORT` | *(tuỳ chọn)* cổng Postgres phía máy chủ, mặc định `5432`; đặt `5433` nếu máy đã cài sẵn PostgreSQL |

> `DB_PASSWORD` chỉ có tác dụng ở **lần khởi tạo CSDL đầu tiên**. Đổi nó khi volume `pgdata` đã tồn tại
> thì Postgres vẫn giữ mật khẩu cũ và server báo lỗi xác thực — xem bảng cuối trang.

## Chạy

Ba cách, chọn một. Cách 1 để demo, cách 2 để lập trình, cách 3 khi không muốn mở IDE.

### Cách 1 — tất cả bằng Docker Compose

```bash
docker compose up --build        # db + server, TCP socket ở cổng 9090

# terminal khác — client desktop
cd client && mvn package
java -jar target/tangthu-client.jar                     # nối localhost:9090
java -jar target/tangthu-client.jar 192.168.1.10 9090   # nối máy chủ khác trong LAN
```

Client **luôn** chạy trên máy người dùng, không nằm trong Docker — nó là ứng dụng desktop.

### Cách 2 — IntelliJ IDEA (có debug)

1. Mở thư mục repo. IntelliJ hỏi nạp Maven project → **Load**; phải thấy đủ **hai** module
   `tangthu-server` và `tangthu-client` trong cây Project.
2. `File → Project Structure → Project → SDK` chọn **JDK 21**.
3. Chỉ chạy CSDL bằng Docker, nhường cổng 9090 cho IDE:

   ```bash
   docker compose up -d db
   ```

4. Tạo hai cấu hình chạy (`Run → Edit Configurations → + → Application`):

   | Trường | Server | Client |
   |---|---|---|
   | Main class | `vn.tangthu.TangThuApplication` | `vn.tangthu.client.App` |
   | Module (`-cp`) | `tangthu-server` | `tangthu-client` |
   | Working directory | `<repo>/server` | `<repo>/client` |
   | VM options | `-Duser.timezone=UTC` | — |
   | Environment variables | `DB_PASSWORD=<trong .env>;JWT_SECRET=<trong .env>` | — |

5. Chạy **Server** trước, đợi dòng `Máy chủ TCP socket lắng nghe ở cổng 9090`, rồi chạy **Client**.

> Hai điều hay sai: chọn nhầm module ở ô `-cp` (main class sẽ bị gạch đỏ và Run không khởi động được),
> và thiếu `-Duser.timezone=UTC` (server chết ngay ở bước Flyway). Thư mục `.run/` không được commit
> vì cấu hình chạy chứa mật khẩu của từng máy.

### Cách 3 — dòng lệnh, không cần IDE

```bash
docker compose up -d db                      # chỉ CSDL

# nạp DB_PASSWORD và JWT_SECRET từ .env vào môi trường rồi chạy server
set -a; . ./.env; set +a
cd server && mvn package
java -Duser.timezone=UTC -jar target/tangthu-server-2.0.0.jar

# terminal khác
cd client && mvn package && java -jar target/tangthu-client.jar
```

Server đọc cấu hình qua biến môi trường `DATABASE_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`,
`STORAGE_DIR`; mặc định trong `application.yml` trỏ `localhost:5432`, user/mật khẩu `tangthu`.
Nếu đã đổi `DB_HOST_PORT`, đặt thêm `DATABASE_URL=jdbc:postgresql://localhost:<cổng>/tangthu`.
Chi tiết: [`server/README.md`](server/README.md), [`client/README.md`](client/README.md).

### Tài khoản demo

Mật khẩu chung: `Matkhau@123`

| Đăng nhập | Vai |
|---|---|
| `SV001` | Sinh viên |
| `GV001` | Giáo vụ khoa |
| `QTV001` | Quản trị viên |

Dữ liệu demo tự khởi tạo khi chạy server với cấu hình mặc định. Đăng nhập `SV001` → tab **Hỏi đáp**
để thử 12 câu hỏi mẫu thuộc 6 chủ đề. Mỗi câu trả lời dẫn đến một trang trong PDF **[DEMO] Sổ tay học vụ mẫu**.

> Khi triển khai thật: đặt `FLYWAY_LOCATIONS=classpath:db/migration` trong `.env` để **không** tạo tài khoản
> demo, và đổi `JWT_SECRET`, `DB_PASSWORD`.

## Khi gặp lỗi

| Thông báo | Nguyên nhân | Cách chữa |
|---|---|---|
| `FATAL: invalid value for parameter "TimeZone": "Asia/Saigon"` | JVM trên Windows gửi timezone mặc định làm tham số kết nối, tzdata của ảnh `postgres:16` không có alias đó. Chạy trong Docker không gặp vì container dùng UTC | thêm `-Duser.timezone=UTC` vào VM options. Đặt `hibernate.jdbc.time_zone` **không** chữa được, lỗi xảy ra trước khi Hibernate khởi động |
| `FATAL: password authentication failed for user "tangthu"` | đổi `DB_PASSWORD` sau khi volume `pgdata` đã được khởi tạo; biến đó chỉ dùng ở lần `initdb` đầu tiên | `docker compose exec db psql -U tangthu -d tangthu -c "ALTER USER tangthu PASSWORD 'mật-khẩu-mới';"` — hoặc `docker compose down -v` nếu chấp nhận xoá sạch dữ liệu |
| `Address already in use: bind` (cổng 9090) | đã có một server chạy, thường là container `server` hoặc tiến trình trong IDE | `docker compose stop server`, hoặc dừng tiến trình đang giữ cổng |
| Docker không khởi động được dịch vụ `db`, cổng 5432 bận | máy đã cài sẵn PostgreSQL | đặt `DB_HOST_PORT=5433` trong `.env` |
| IntelliJ: main class bị gạch đỏ, bấm Run không có gì xảy ra | ô `-cp` của run config trỏ sai module (IntelliJ tự chọn module khác khi tên module trong config chưa tồn tại) | mở `Edit Configurations`, chọn lại đúng `tangthu-client` / `tangthu-server` |
| IntelliJ chỉ thấy một module, hoặc module trỏ vào thư mục không tồn tại | cache mô hình dự án cũ còn sót sau khi đổi cấu trúc thư mục | đóng IDE, xoá `%LOCALAPPDATA%\JetBrains\<phiên bản>\projects\<tên-dự-án>.<hash>\`, mở lại |
| IntelliJ không build được, log có `BuildManager ... NullPointerException` và `Unable to establish loopback connection` | trên vài máy Windows, phần mềm bảo vệ chặn file socket AF_UNIX trong `%APPDATA%`, khiến `Selector.open()` của JDK 21+ gãy | `Help → Edit Custom VM Options`, thêm `-Djdk.net.unixdomain.tmpdir=C:\Users\<tên>\.jbsockets` rồi khởi động lại IDE |

## Kiểm thử

```bash
# Server: 19 kiểm thử tích hợp qua socket thật, trên PostgreSQL thật (cần database tangthu_test)
createdb -O tangthu tangthu_test
cd server && mvn test

# Client: SocketClient với server giả
cd client && mvn test
```

Các kịch bản (nhiều yêu cầu trên một kết nối, khung hỏng, khung quá lớn, 10 client đồng thời, đăng nhập,
khóa tài khoản có hiệu lực ngay, đổi vai, tự sửa chính mình, câu hỏi đã ẩn, hội thoại của người khác, tải
trùng tệp, gỡ tài liệu đang dùng…) liệt kê ở `docs/DAC-TA-TRIEN-KHAI.md` mục 9. Quan sát gói tin bằng
Wireshark với bộ lọc `tcp.port == 9090`.

## Cấu trúc thư mục — mô hình MVC, chia theo module

```
.
├── server/                               Server Java — Model + Controller
│   └── src/main/java/vn/tangthu/
│       ├── common/                       dùng chung
│       │   ├── socket/                   ServerSocket, đọc/ghi khung, điều phối action — tầng mạng
│       │   └── config/ security/ exception/ dto/ util/
│       ├── account/                      M1 · Tài khoản
│       ├── chat/                         M2 · Hỏi đáp
│       ├── content/                      M3 · Nội dung
│       └── admin/                        M4 · Quản trị
│           (mỗi module: model/ repository/ service/ controller/ dto/)
├── client/                               Client Java Swing — View (+ Model, Controller phía client)
│   └── src/main/java/vn/tangthu/client/
│       ├── App.java                      điểm khởi động
│       ├── common/
│       │   ├── net/                      SocketClient, FrameCodec — tầng mạng
│       │   └── view/                     MainFrame (tab theo vai), Async, FileOpener
│       ├── account/                      M1 · LoginPanel, ProfileDialog, ChangePasswordDialog
│       ├── chat/                         M2 · ChatPanel
│       ├── content/                      M3 · DocumentsPanel, ContentPanel
│       └── admin/                        M4 · UsersPanel, HealthPanel
│           (mỗi module: model/ controller/ view/)
├── docs/                                 SRS, đặc tả triển khai + giao thức, báo cáo, sơ đồ, slide, ảnh
├── docker-compose.yml                    db + server
└── .env.example
```

| Tầng | Ở đâu | Vai trò |
|---|---|---|
| **Model** | `server/.../<module>/model/`, `repository/`; `client/.../<module>/model/` | Entity JPA ánh xạ bảng PostgreSQL; record dữ liệu phía client |
| **Controller** | `server/.../<module>/controller/` (+ `service/`); `client/.../<module>/controller/` | Server: nhận action, kiểm quyền, xử lý nghiệp vụ. Client: gửi action qua socket |
| **View** | `client/.../<module>/view/` | Màn hình Swing |

## Phân công module

Mỗi thành viên phụ trách trọn **một module ở cả server và client**. Quy trình clone → tạo nhánh → Pull Request: [`CONTRIBUTING.md`](CONTRIBUTING.md).

| Module | Use case | Phạm vi | Server | Client | Người phụ trách |
|---|---|---|---|---|---|
| M1 · Tài khoản | UC01–UC04 | Đăng nhập, đăng xuất, xem thông tin cá nhân, đổi mật khẩu | `server/src/main/java/vn/tangthu/account/` | `client/src/main/java/vn/tangthu/client/account/` | Trương Xuân Anh |
| M2 · Hỏi đáp | UC05, UC06, UC08 | Chọn câu hỏi và nhận trả lời, xem lại hội thoại, mở nguồn trích dẫn | `server/src/main/java/vn/tangthu/chat/` | `client/src/main/java/vn/tangthu/client/chat/` | Hồ Minh Đức |
| M3 · Nội dung | UC07, UC10–UC12, UC22, UC23 | Tra cứu, tải lên, sửa thông tin và gỡ tài liệu. Quản lý chủ đề, câu hỏi và câu trả lời | `server/src/main/java/vn/tangthu/content/` | `client/src/main/java/vn/tangthu/client/content/` | Ngô Xuân Thuỵ |
| M4 · Quản trị | UC14–UC16, UC20 | Tra cứu người dùng, khóa hoặc mở tài khoản, phân quyền, kiểm tra trạng thái hệ thống | `server/src/main/java/vn/tangthu/admin/` | `client/src/main/java/vn/tangthu/client/admin/` | Đặng Hoàng Vũ |
| Dùng chung | — | Hạ tầng socket, cơ sở dữ liệu và các thành phần dùng chung của client | `server/.../common/` (socket/), `db/` | `client/.../common/` (net/, view/), `App.java` | Hồ Minh Đức |

### Vai trò trong báo cáo

- **Hồ Minh Đức** phụ trách điều phối nhóm, tích hợp và theo dõi tiến độ.
- **Trương Xuân Anh** phụ trách phân tích nghiệp vụ và đặc tả yêu cầu.
- **Ngô Xuân Thuỵ** phụ trách thiết kế hệ thống và cơ sở dữ liệu.
- **Đặng Hoàng Vũ** phụ trách phân rã chức năng, mô tả IPO, ma trận CRUD và luồng dữ liệu.

## Tài liệu

| Tệp | Nội dung |
|---|---|
| [`docs/reports/bao-cao-so-tay-sinh-vien-cntt.docx`](docs/reports/bao-cao-so-tay-sinh-vien-cntt.docx) | Báo cáo phân tích và thiết kế hệ thống |
| [`docs/diagrams/so-do-he-thong.drawio`](docs/diagrams/so-do-he-thong.drawio) | 56 trang sơ đồ UML và phân tích: use case (tổng quan + 4 module), ngữ cảnh, kiến trúc TCP socket, cấu trúc MVC, **thành phần, triển khai, gói**, ERD, tuần tự (kết nối TCP + 17 UC), **hoạt động 17 UC**, sơ đồ lớp, **5 sơ đồ trạng thái**, BFD |
| [`docs/DAC-TA-USE-CASE.md`](docs/DAC-TA-USE-CASE.md) | Đặc tả đầy đủ 17 use case: tác nhân, tiền / hậu điều kiện, luồng chính, thay thế, ngoại lệ, action socket, sơ đồ đi kèm |
| [`docs/SRS.md`](docs/SRS.md) | Đặc tả yêu cầu phần mềm: phạm vi, tác nhân, 17 use case, yêu cầu dữ liệu, giao diện, phi chức năng, quy tắc nghiệp vụ, ma trận truy vết |
| [`docs/DAC-TA-TRIEN-KHAI.md`](docs/DAC-TA-TRIEN-KHAI.md) | Đặc tả triển khai: lược đồ CSDL, **giao thức TCP socket** (mục 6), luồng xử lý, hiện thực server / client, kịch bản kiểm thử |
| [`docs/slides/SRS-tac-nhan-va-use-case.pptx`](docs/slides/SRS-tac-nhan-va-use-case.pptx) | Slide SRS: tác nhân, use case, ma trận quyền, input / output, luồng |
| [`docs/thuat-ngu.md`](docs/thuat-ngu.md) | Bảng thuật ngữ Việt – Anh và quy tắc dùng từ, áp dụng cho báo cáo, tài liệu và bình luận mã nguồn |

## Ghi chú

Các phiên bản trước (Node.js / Express với RAG Gemini; rồi REST API + web Next.js) đã được thay bằng kiến
trúc socket hiện tại; mã cũ vẫn xem lại được trong lịch sử git.
