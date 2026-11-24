# Bảng thuật ngữ — Sổ Tay Sinh Viên CNTT

Dùng thống nhất trong **báo cáo, tài liệu và bình luận mã nguồn**. Tên biến, tên hàm, tên bảng trong
code vẫn giữ tiếng Anh.

> Thuật ngữ **nghiệp vụ** (chủ đề, câu hỏi, nguồn, hội thoại, ẩn câu hỏi) nằm ở
> [`SRS.md` mục 1.3](SRS.md#13-định-nghĩa-và-viết-tắt). Tệp này lo phần **kỹ thuật**: mạng, kiến trúc,
> cơ sở dữ liệu, bảo mật, kiểm thử, hạ tầng.

## Quy tắc dùng từ

**Lần đầu xuất hiện trong báo cáo:** viết tiếng Việt kèm tiếng Anh trong ngoặc — *"đóng khung thông
điệp (framing)"*. Từ lần thứ hai trở đi chỉ dùng tiếng Việt.

**Không dịch:** tên riêng công nghệ (PostgreSQL, Spring Boot, Flyway, Docker, Swing, Maven), tên định
danh trong code (`SocketDispatcher`, `FrameCodec`, `is_active`, `chat.ask`), và các từ đã thành chuẩn
quốc tế không có từ Việt gọn hơn (socket, token, JWT, base64).

**Tránh dịch nửa vời.** Trộn hai ngôn ngữ trong một cụm — *"đọc frame từ luồng byte"* — là lỗi hay gặp
nhất. Hoặc dịch cả cụm, hoặc giữ nguyên cả cụm.

---

## 1. Mạng và giao thức

### 1.1. Bốn từ lõi

Bốn từ này xuất hiện dày nhất trong báo cáo — chương kiến trúc, chương giao thức và phần kiểm thử đều
dùng tới. Dùng không nhất quán thì cả báo cáo đọc lệch, nên chốt ở đây trước.

<!-- TODO(human): điền cột "Dùng trong báo cáo" và "Ghi chú" cho bốn từ dưới đây -->

| Tiếng Anh | Dùng trong báo cáo | Ghi chú |
|---|---|---|
| frame | | |
| framing | | |
| dispatcher | | |
| action | | |

### 1.2. Các từ còn lại

| Tiếng Anh | Dùng trong báo cáo | Ghi chú |
|---|---|---|
| socket | **socket** | Giữ nguyên. Diễn giải lần đầu: "điểm cuối của một kết nối TCP" |
| byte stream | **luồng byte** | "TCP là luồng byte, không có ranh giới thông điệp" — câu nền của mục giao thức |
| three-way handshake | **bắt tay ba bước** | Quan sát được trên Wireshark: SYN, SYN-ACK, ACK |
| listen / accept | **lắng nghe** / **chấp nhận kết nối** | `ServerSocket.accept()` chặn cho tới khi có client |
| port | **cổng** | Cổng 9090 |
| big-endian | **byte cao trước** | Thứ tự 4 byte độ dài; `DataOutputStream.writeInt` ghi sẵn kiểu này |
| payload | **phần nội dung** | Phần JSON nằm sau 4 byte độ dài |
| request / response | **yêu cầu** / **phản hồi** | Đừng gọi là "request HTTP" — hệ thống không dùng HTTP |
| persistent connection | **kết nối giữ lâu dài** | Một socket dùng cho cả phiên, không mở lại mỗi lượt hỏi |
| timeout | **thời gian chờ tối đa** | |
| idle timeout | **thời gian chờ khi im lặng** | `SOCKET_IDLE_TIMEOUT`, mặc định 30 phút |
| thread pool | **bể luồng** | Có thể giữ "thread pool"; nhất quán là được |
| thread per connection | **mỗi kết nối một luồng** | Mô hình phục vụ của `SocketServer` |
| concurrent clients | **client đồng thời** | Kịch bản P4: 10 client cùng lúc |
| graceful shutdown | **tắt gọn** | Đóng `ServerSocket`, chờ luồng đang phục vụ kết thúc |
| disconnect | **ngắt kết nối** | |
| packet | **gói tin** | Dùng khi nói về Wireshark; đừng gọi khung thông điệp là "gói tin" |
| latency | **độ trễ** | Tab Hệ thống đo độ trễ một lượt gọi |
| throughput | **thông lượng** | Số yêu cầu phục vụ được trong một giây |
| LAN | **mạng cục bộ** | Giữ viết tắt LAN sau lần đầu |
| TLS | **TLS** | Giữ nguyên. Hiện chưa dùng — thuộc phần hướng phát triển |

## 2. Kiến trúc và mã nguồn

| Tiếng Anh | Dùng trong báo cáo | Ghi chú |
|---|---|---|
| MVC (Model–View–Controller) | **MVC** | Diễn giải lần đầu: "mô hình – khung nhìn – điều khiển" |
| module | **module** | Bốn module M1–M4. Có thể dùng "phân hệ" |
| layer | **tầng** | Tầng mạng, tầng nghiệp vụ, tầng truy cập dữ liệu |
| controller | **lớp điều khiển** | Server: nhận action, kiểm quyền. Client: gửi action |
| service | **lớp nghiệp vụ** | |
| repository | **lớp truy cập dữ liệu** | |
| entity | **thực thể** | Lớp JPA ánh xạ một bảng |
| DTO (Data Transfer Object) | **đối tượng truyền dữ liệu** | Giữ viết tắt DTO sau lần đầu |
| record (Java) | **`record`** | Giữ nguyên tên cú pháp Java, đừng dịch thành "bản ghi" |
| dependency injection | **tiêm phụ thuộc** | Spring dựng sẵn và truyền vào hàm khởi tạo |
| bean | **bean** | Giữ nguyên |
| EDT (Event Dispatch Thread) | **luồng sự kiện giao diện** | Mọi thay đổi giao diện Swing phải chạy trên luồng này |
| callback | **hàm gọi lại** | |
| refactor | **tái cấu trúc** | Đổi cấu trúc mã, không đổi hành vi |

## 3. Cơ sở dữ liệu

| Tiếng Anh | Dùng trong báo cáo | Ghi chú |
|---|---|---|
| schema | **lược đồ** | "lược đồ cơ sở dữ liệu" |
| ERD (Entity–Relationship Diagram) | **sơ đồ thực thể – quan hệ** | |
| attribute / column | **thuộc tính** / **cột** | Thuộc tính khi nói mô hình, cột khi nói bảng thật |
| primary key | **khóa chính** | |
| foreign key | **khóa ngoại** | |
| constraint | **ràng buộc** | `CHECK (page >= 1)` là một ràng buộc |
| index | **chỉ mục** | Không dùng "chỉ số" — đó là *metric* |
| query | **truy vấn** | |
| migration | **di trú lược đồ** | Có thể giữ "migration"; nhất quán là được |
| seed data | **dữ liệu mồi** | Thư mục `db/demo` |
| ORM (Object–Relational Mapping) | **ánh xạ đối tượng – quan hệ** | Giữ viết tắt ORM sau lần đầu |
| transaction | **giao dịch** | `@Transactional` |
| lazy loading | **nạp trễ** | `FetchType.LAZY`: chỉ truy vấn khi thật sự đọc tới |
| cascade / restrict | **xóa lan** / **chặn xóa** | `ON DELETE CASCADE` và `ON DELETE RESTRICT` trong `V1__init.sql` |
| soft delete | **xóa mềm** | Ẩn câu hỏi (`is_active = false`) là một dạng xóa mềm |
| normalization | **chuẩn hóa** | |

## 4. Xác thực và phân quyền

| Tiếng Anh | Dùng trong báo cáo | Ghi chú |
|---|---|---|
| authentication | **xác thực** | Trả lời "anh là ai" |
| authorization | **phân quyền** | Trả lời "anh được làm gì". **Hai từ này khác nhau, đừng dùng lẫn** |
| RBAC (Role-Based Access Control) | **phân quyền theo vai trò** | |
| role | **vai** hoặc **vai trò** | Ba vai: `USER`, `CONTENT_ADMIN`, `SYSTEM_ADMIN` |
| permission | **quyền** | |
| permission matrix | **ma trận phân quyền** | SRS mục 7 |
| principal | **người gọi đã xác thực** | `AuthUser` trong mã nguồn |
| JWT (JSON Web Token) | **JWT** | Giữ nguyên. Diễn giải: "chuỗi thông hành đã ký" |
| claim | **thông tin trong token** | Token của đồ án chỉ mang một claim: `userId` |
| TTL (time to live) | **thời hạn hiệu lực** | Mặc định 8 giờ |
| hash | **băm** | "mật khẩu được băm bằng BCrypt" |
| salt | **muối** | BCrypt tự sinh và nhúng vào chuỗi băm |
| brute force | **dò mật khẩu bằng cách thử hàng loạt** | |
| privilege escalation | **leo thang quyền** | BR04 chặn quản trị viên tự đổi vai của chính mình |

## 5. Kiểm thử

| Tiếng Anh | Dùng trong báo cáo | Ghi chú |
|---|---|---|
| unit test | **kiểm thử đơn vị** | "đơn vị" ở đây là *đơn vị mã*. Lần đầu nên viết rõ "kiểm thử đơn vị (unit test)" |
| integration test | **kiểm thử tích hợp** | 19 ca của đồ án đều thuộc loại này: qua socket thật, CSDL thật |
| test case / scenario | **ca kiểm thử** / **kịch bản** | |
| test fixture | **dữ liệu dựng sẵn cho kiểm thử** | Bốn tài khoản dựng trong `@BeforeEach` |
| mock / fake | **giả lập** | `SocketClientTest` dùng một server giả trên cổng ngẫu nhiên |
| assertion | **mệnh đề kiểm tra** | |
| coverage | **độ phủ** | |
| regression | **lỗi tái xuất** | Lỗi đã sửa rồi quay lại |
| flaky test | **kiểm thử không ổn định** | Lúc đạt lúc không, thường do thời gian chờ hoặc thứ tự luồng |

## 6. Hạ tầng và triển khai

| Tiếng Anh | Dùng trong báo cáo | Ghi chú |
|---|---|---|
| deploy / deployment | **triển khai** | |
| container | **container** | Giữ nguyên |
| image | **ảnh** | "ảnh Docker `postgres:16`" |
| volume | **ổ lưu trữ** | `pgdata`, `storage` — dữ liệu còn lại sau khi container bị xóa |
| healthcheck | **kiểm tra sức khỏe** | `pg_isready` trong `docker-compose.yml` |
| environment variable | **biến môi trường** | `DB_PASSWORD`, `JWT_SECRET` |
| build | **dựng** | "dựng bằng Maven" |
| artifact | **sản phẩm dựng** | Tệp `.jar` sau khi dựng |
| fat jar / shaded jar | **jar gộp phụ thuộc** | `tangthu-client.jar` chạy được bằng `java -jar` |
| CI/CD | **tích hợp và triển khai liên tục** | Giữ viết tắt sau lần đầu |
| rollback | **quay về bản trước** | |

---

## 7. Danh mục từ viết tắt cho báo cáo

Dán vào mục **DANH MỤC TỪ VIẾT TẮT**:

| Viết tắt | Tiếng Anh | Tiếng Việt |
|---|---|---|
| API | Application Programming Interface | Giao diện lập trình ứng dụng |
| BR | Business Rule | Quy tắc nghiệp vụ |
| CSDL | — | Cơ sở dữ liệu |
| DI | Dependency Injection | Tiêm phụ thuộc |
| DOCX | — | Định dạng văn bản của Microsoft Word |
| DTO | Data Transfer Object | Đối tượng truyền dữ liệu |
| EDT | Event Dispatch Thread | Luồng sự kiện giao diện |
| ERD | Entity–Relationship Diagram | Sơ đồ thực thể – quan hệ |
| IP | Internet Protocol | Giao thức liên mạng |
| JDBC | Java Database Connectivity | Chuẩn kết nối cơ sở dữ liệu của Java |
| JDK | Java Development Kit | Bộ công cụ phát triển Java |
| JPA | Java Persistence API | Chuẩn ánh xạ đối tượng – quan hệ của Java |
| JRE | Java Runtime Environment | Môi trường chạy Java |
| JSON | JavaScript Object Notation | Định dạng dữ liệu JSON |
| JVM | Java Virtual Machine | Máy ảo Java |
| JWT | JSON Web Token | Chuỗi thông hành đã ký |
| LAN | Local Area Network | Mạng cục bộ |
| MVC | Model–View–Controller | Mô hình – khung nhìn – điều khiển |
| NFR | Non-Functional Requirement | Yêu cầu phi chức năng |
| ORM | Object–Relational Mapping | Ánh xạ đối tượng – quan hệ |
| PDF | Portable Document Format | Định dạng tài liệu di động |
| RBAC | Role-Based Access Control | Phân quyền theo vai trò |
| SQL | Structured Query Language | Ngôn ngữ truy vấn có cấu trúc |
| SRS | Software Requirements Specification | Đặc tả yêu cầu phần mềm |
| TCP | Transmission Control Protocol | Giao thức điều khiển truyền vận |
| TLS | Transport Layer Security | Bảo mật tầng vận chuyển |
| UC | Use Case | Trường hợp sử dụng |
| UI | User Interface | Giao diện người dùng |
| UTF-8 | Unicode Transformation Format 8-bit | Bảng mã UTF-8 |
| UUID | Universally Unique Identifier | Mã định danh duy nhất toàn cục |

---

## 8. Bốn chỗ dễ sai nhất

**Đừng viết "API", "endpoint", "REST" hay "mã trạng thái 401 / 403".** Hệ thống **không dùng HTTP**:
không có Tomcat (`web-application-type: none`), không có route, không có mã trạng thái. Một lượt gọi là
*một action trên socket*, và lỗi trả về là **mã lỗi dạng chuỗi** (`UNAUTHORIZED`, `FORBIDDEN`,
`VALIDATION_ERROR`), không phải số HTTP. Đây là lỗi dễ mắc nhất khi viết báo cáo vì phản xạ quen tay —
và là chỗ người đọc nhận ra ngay rằng báo cáo không khớp mã nguồn.

**`authentication` và `authorization` không phải một.** Xác thực là "anh là ai", phân quyền là "anh
được làm gì". Trong đồ án này chúng là hai bước liền nhau trong `SocketDispatcher.authorize()`: đọc
token rồi tìm tài khoản trong CSDL (xác thực → `UNAUTHORIZED`), sau đó so vai với vai tối thiểu của
action (phân quyền → `FORBIDDEN`). Dùng lẫn hai từ là mất điểm ngay ở chương phân tích.

**"đơn vị" mang hai nghĩa.** Mặc định là khoa / phòng ban (`department`). Khi nói *unit test* thì phải
viết đầy đủ "kiểm thử đơn vị (unit test)".

**"chỉ mục" khác "chỉ số".** `index` là chỉ mục — `idx_questions_topic`, `idx_messages_conversation`.
`metric` là chỉ số — độ trễ, thông lượng, số client đồng thời. Đảo hai từ này là lỗi hay gặp trong báo
cáo sinh viên.
