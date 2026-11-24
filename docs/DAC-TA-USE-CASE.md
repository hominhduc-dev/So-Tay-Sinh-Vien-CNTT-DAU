# Đặc tả use case — Sổ Tay Sinh Viên CNTT

**Phiên bản:** 1.0 · 22/11/2026
**Phạm vi:** 17 use case của 3 tác nhân, đối chiếu với mã nguồn hiện tại (client Java Swing ↔ server Java qua
TCP socket).

Mỗi use case gồm: bảng thông tin, luồng chính, luồng thay thế, ngoại lệ. Sơ đồ đi kèm nằm trong
[`diagrams/so-do-he-thong.drawio`](diagrams/so-do-he-thong.drawio):

- **ACT UCxx**: sơ đồ hoạt động.
- **SEQ UCxx**: sơ đồ tuần tự.

Tên action và mã lỗi theo [`DAC-TA-TRIEN-KHAI.md`](DAC-TA-TRIEN-KHAI.md) mục 6.

## Quy ước chung

**Tác nhân và kế thừa**

| Tác nhân | Vai trong hệ thống | Use case gắn trực tiếp | Kế thừa | Tổng |
|---|---|---|---|---|
| Sinh viên | `USER` | UC01–UC08 (8) | — | 8 |
| Giáo vụ khoa | `CONTENT_ADMIN` | UC10, UC11, UC12, UC22, UC23 (5) | Sinh viên | 13 |
| Quản trị viên | `SYSTEM_ADMIN` | UC14, UC15, UC16, UC20 (4) | Giáo vụ khoa | 17 |

*Người dùng* là tên gọi chung cho cả ba tác nhân.

**Tiền điều kiện chung (TĐK-0).** Áp dụng cho mọi use case trừ UC01 và UC02:

- Người dùng đã đăng nhập; client giữ kết nối TCP và token JWT còn hạn.
- Ở **mỗi yêu cầu**, `SocketDispatcher` đọc lại tài khoản từ CSDL và kiểm tra: tài khoản đang hoạt động, thuộc khoa CNTT (BR01, BR02), có vai đủ cho action (BR03).
- Không thỏa:
  - sai hoặc thiếu token, tài khoản bị khóa, khác khoa → `UNAUTHORIZED`; client báo lỗi và quay về màn đăng nhập;
  - thiếu vai → `FORBIDDEN`.

**Ngoại lệ chung (NL-0).** Áp dụng cho mọi use case:

- Mất kết nối hoặc server không phản hồi sau 60 giây → client báo `NETWORK_ERROR`; lần thao tác sau tự kết nối lại.
- Lỗi máy chủ bất ngờ → `INTERNAL_ERROR`; kết nối vẫn mở.

---

## M1 · Tài khoản

### UC01 — Đăng nhập

| | |
|---|---|
| **Module** | M1 · Tài khoản (MOD-01-01) |
| **Tác nhân** | Người dùng (Sinh viên, Giáo vụ khoa, Quản trị viên) |
| **Mô tả** | Người dùng xác thực bằng mã hoặc email và mật khẩu để mở phiên làm việc trên một kết nối TCP |
| **Kích hoạt** | Mở ứng dụng client, bấm **Đăng nhập** |
| **Tiền điều kiện** | Chưa đăng nhập. Biết IP máy chủ; cổng 9090 mở trong LAN |
| **Hậu điều kiện** | Client giữ kết nối TCP và token JWT (hạn 8 giờ); hiện các tab theo vai |
| **Action** | `auth.login {login, password}` (công khai) |
| **Quy tắc** | BR01 |
| **Sơ đồ** | ACT UC01, SEQ UC01, SEQ Kết nối TCP |

**Luồng chính**
1. Người dùng nhập mã sinh viên / cán bộ hoặc email (≤ 255 ký tự), mật khẩu, IP và cổng máy chủ.
2. Client kiểm tra đã nhập đủ và cổng là số.
3. Client mở `Socket` tới `IP:cổng` (bắt tay TCP, chờ tối đa 5 giây).
4. Client gửi `auth.login`.
5. Server tìm tài khoản theo mã hoặc email; kiểm tra mật khẩu (BCrypt), trạng thái hoạt động và khoa CNTT.
6. Server cấp JWT chỉ chứa `userId`; trả token và thông tin người dùng.
7. Client lưu token trong `SocketClient` và dựng tab theo vai:
   - Sinh viên: Hỏi đáp, Tài liệu;
   - Giáo vụ: thêm Soạn nội dung;
   - Quản trị viên: thêm Tài khoản, Hệ thống.

**Luồng thay thế**
- 2a. Thiếu thông tin hoặc cổng không phải số → client nhắc, quay lại bước 1. Không gửi gì lên server.

**Ngoại lệ**
- 3a. Không kết nối được máy chủ → `NETWORK_ERROR`, quay lại bước 1.
- 5a. Sai mật khẩu, tài khoản bị khóa hoặc không thuộc CNTT → `UNAUTHORIZED` với **cùng một thông báo** "Thông tin đăng nhập không đúng", không nói rõ lý do. Client đóng kết nối, quay lại bước 1.
- 4a. Thiếu trường → `VALIDATION_ERROR`.

### UC02 — Đăng xuất

| | |
|---|---|
| **Module** | M1 · Tài khoản (MOD-01-02) |
| **Tác nhân** | Người dùng |
| **Mô tả** | Kết thúc phiên: bỏ token và đóng kết nối TCP |
| **Kích hoạt** | Bấm **Đăng xuất** hoặc đóng cửa sổ ứng dụng |
| **Tiền điều kiện** | Đang đăng nhập |
| **Hậu điều kiện** | Client không còn token; kết nối TCP đóng; server giải phóng luồng phục vụ |
| **Action** | Không có — client gọi `Socket.close()` |
| **Sơ đồ** | ACT UC02, SEQ UC02 |

**Luồng chính**
1. Người dùng bấm Đăng xuất.
2. Client bỏ token, xóa trạng thái phiên và các tab.
3. Client đóng socket (gửi gói FIN).
4. Ở server, `ClientConnection` đọc được EOF, đóng socket và trả luồng về pool.
5. Client hiện màn đăng nhập.

**Ghi chú:** JWT đã cấp không bị thu hồi, tự hết hạn sau 8 giờ. Lần đăng nhập sau mở kết nối TCP mới.

### UC03 — Xem thông tin cá nhân

| | |
|---|---|
| **Module** | M1 · Tài khoản (MOD-01-03) |
| **Tác nhân** | Người dùng |
| **Mô tả** | Xem hồ sơ tài khoản của chính mình |
| **Kích hoạt** | Bấm **Thông tin cá nhân** trên thanh đầu cửa sổ |
| **Tiền điều kiện** | TĐK-0 |
| **Hậu điều kiện** | Không thay đổi dữ liệu |
| **Action** | `auth.me` |
| **Quy tắc** | BR02 |
| **Sơ đồ** | ACT UC03, SEQ UC03 |

**Luồng chính**
1. Người dùng bấm Thông tin cá nhân; client gửi `auth.me`.
2. Server đọc tài khoản theo `userId` trong token.
3. Server trả mã, họ tên, email, vai, khoa, trạng thái.
4. Client hiện hộp thoại thông tin.

**Ghi chú:** dữ liệu đọc mới từ CSDL nên phản ánh ngay việc bị đổi vai. Không sửa hồ sơ trong phạm vi đề tài.

### UC04 — Đổi mật khẩu

| | |
|---|---|
| **Module** | M1 · Tài khoản (MOD-01-04) |
| **Tác nhân** | Người dùng |
| **Mô tả** | Đổi mật khẩu đăng nhập của chính mình |
| **Kích hoạt** | Bấm **Đổi mật khẩu** |
| **Tiền điều kiện** | TĐK-0 |
| **Hậu điều kiện** | `users.password_hash` = BCrypt(mật khẩu mới); phiên hiện tại vẫn giữ |
| **Action** | `auth.changePassword {currentPassword, newPassword}` |
| **Sơ đồ** | ACT UC04, SEQ UC04 |

**Luồng chính**
1. Người dùng nhập mật khẩu hiện tại, mật khẩu mới và nhập lại.
2. Client kiểm tra hai lần nhập khớp, rồi gửi action.
3. Server kiểm tra mật khẩu mới dài 8–72 ký tự.
4. Server so mật khẩu hiện tại với `password_hash`.
5. Server lưu BCrypt(mật khẩu mới).
6. Client báo "Đã đổi mật khẩu".

**Luồng thay thế**
- 2a. Hai lần nhập không khớp → client báo, quay lại bước 1.

**Ngoại lệ**
- 3a. Mật khẩu mới không hợp lệ → `VALIDATION_ERROR`, không lưu.
- 4a. Mật khẩu hiện tại sai → `WRONG_PASSWORD`, không lưu.

---

## M2 · Hỏi đáp

### UC05 — Chọn câu hỏi, nhận trả lời

| | |
|---|---|
| **Module** | M2 · Hỏi đáp (MOD-02-01) |
| **Tác nhân** | Sinh viên (Giáo vụ khoa, Quản trị viên kế thừa) |
| **Mô tả** | Chọn chủ đề, bấm nút câu hỏi để nhận câu trả lời soạn sẵn kèm tài liệu nguồn và số trang |
| **Kích hoạt** | Mở tab **Hỏi đáp** |
| **Tiền điều kiện** | TĐK-0 |
| **Hậu điều kiện** | Có thêm một `messages` trong hội thoại của người gọi; hội thoại mới (nếu có) lấy tiêu đề là câu hỏi đầu tiên |
| **Action** | `topic.list`, `topic.questions {topicId}`, `chat.ask {questionId, conversationId?}` |
| **Quy tắc** | BR05, BR06 |
| **Sơ đồ** | ACT UC05, SEQ UC05 |

**Luồng chính**
1. Client gửi `topic.list`; server trả chủ đề theo thứ tự hiển thị kèm số câu hỏi đang hiện.
2. Sinh viên chọn chủ đề; client gửi `topic.questions` và hiện các câu hỏi thành nút.
3. Sinh viên bấm một nút; client gửi `chat.ask` (không có `conversationId` nếu là hội thoại mới).
4. Server đọc câu hỏi đang hiện, câu trả lời, tài liệu nguồn và số trang.
5. Server tạo hội thoại nếu mới, hoặc kiểm tra hội thoại thuộc người gọi; lưu `messages` trong một transaction.
6. Server trả `conversationId` và lượt hỏi – đáp.
7. Client hiện bong bóng câu hỏi, câu trả lời và nhãn nguồn "Tên tài liệu — trang N", rồi cập nhật cột lịch sử. Sinh viên có thể bấm tiếp câu hỏi khác hoặc bấm nguồn (UC08).

**Luồng thay thế**
- 2a. Chủ đề chưa có câu hỏi → client hiện "Chủ đề này chưa có câu hỏi", quay lại bước 2.
- 3a. Bấm **+ Hội thoại mới** → client bỏ `conversationId`; câu hỏi kế tiếp mở hội thoại mới.

**Ngoại lệ**
- 4a. Câu hỏi vừa bị giáo vụ ẩn → `QUESTION_NOT_FOUND`; client báo và tải lại các nút của chủ đề.
- 5a. `conversationId` không thuộc người gọi → `CONVERSATION_NOT_FOUND` (BR05).

### UC06 — Xem lại hội thoại

| | |
|---|---|
| **Module** | M2 · Hỏi đáp (MOD-02-02) |
| **Tác nhân** | Sinh viên (kế thừa như UC05) |
| **Mô tả** | Xem danh sách hội thoại của mình và mở lại một hội thoại cũ |
| **Kích hoạt** | Mở tab Hỏi đáp (cột **Lịch sử hỏi đáp**), chọn một hội thoại |
| **Tiền điều kiện** | TĐK-0 |
| **Hậu điều kiện** | Không thay đổi dữ liệu; câu hỏi tiếp theo được thêm vào hội thoại đang mở |
| **Action** | `conversation.list {page, size}`, `conversation.get {id}` |
| **Quy tắc** | BR05 |
| **Sơ đồ** | ACT UC06, SEQ UC06 |

**Luồng chính**
1. Client gửi `conversation.list`; server trả hội thoại của chính người gọi, mới nhất trước, kèm số lượt hỏi.
2. Người dùng chọn một hội thoại; client gửi `conversation.get`.
3. Server trả các lượt hỏi – đáp theo thời gian, kèm nguồn.
4. Client hiện lại toàn bộ hội thoại.

**Ngoại lệ**
- 2a. Hội thoại không tồn tại hoặc của người khác → `CONVERSATION_NOT_FOUND`. Server trả lời như không tồn tại để không lộ thông tin.

**Ghi chú:** câu hỏi đã ẩn vẫn hiện trong lịch sử. Câu trả lời hiển thị là bản hiện hành, vì `messages` không chụp lại nội dung tại thời điểm hỏi.

### UC08 — Xem nguồn trích dẫn

| | |
|---|---|
| **Module** | M2 · Hỏi đáp (MOD-02-03) |
| **Tác nhân** | Sinh viên (kế thừa như UC05) |
| **Mô tả** | Mở tệp gốc của tài liệu nguồn tại trang được trích dẫn |
| **Kích hoạt** | Bấm nhãn nguồn trong câu trả lời (từ UC05 / UC06), hoặc **Mở tài liệu** ở tab Tài liệu (UC07) |
| **Tiền điều kiện** | TĐK-0 |
| **Hậu điều kiện** | Tệp được ghi vào thư mục tạm của máy người dùng |
| **Action** | `document.download {id}` |
| **Quy tắc** | BR05 |
| **Sơ đồ** | ACT UC08, SEQ UC08 |

**Luồng chính**
1. Client gửi `document.download`.
2. Server đọc bản ghi tài liệu, kiểm tra tệp còn trong kho, đọc toàn bộ tệp.
3. Server trả `fileName`, `mimeType`, `size`, `contentBase64` (tệp ≤ 20 MB).
4. Client giải mã base64, ghi tệp vào thư mục tạm.
5. Client mở tệp bằng ứng dụng mặc định của máy (trình đọc PDF / Word), kèm thông báo số trang được trích dẫn.

**Ngoại lệ**
- 2a. Không có bản ghi → `DOCUMENT_NOT_FOUND`.
- 2b. Mất tệp gốc trong kho → `FILE_NOT_FOUND`.
- 5a. Máy không hỗ trợ mở tệp tự động → client báo đường dẫn tệp đã lưu.

---

## M3 · Nội dung

### UC07 — Tra cứu kho tài liệu

| | |
|---|---|
| **Module** | M3 · Nội dung (MOD-03-01) |
| **Tác nhân** | Sinh viên (Giáo vụ khoa, Quản trị viên kế thừa) |
| **Mô tả** | Tìm tài liệu theo tiêu đề, xem danh sách có phân trang |
| **Kích hoạt** | Mở tab **Tài liệu**, nhập từ khóa, bấm **Tìm** |
| **Tiền điều kiện** | TĐK-0 |
| **Hậu điều kiện** | Không thay đổi dữ liệu |
| **Action** | `document.list {q?, page, size}` |
| **Sơ đồ** | ACT UC07, SEQ UC07 |

**Luồng chính**
1. Người dùng nhập từ khóa (có thể để trống) và bấm Tìm.
2. Client gửi `document.list` (trang 20 mục).
3. Server tìm theo tiêu đề, không phân biệt hoa thường, mới nhất trước.
4. Server trả danh sách và tổng số.
5. Client hiện bảng: tiêu đề, loại (PDF / DOCX), số trang, người tải lên, ngày tải; có nút trang trước / sau.
6. Người dùng có thể mở một tài liệu (UC08).

**Ngoại lệ**
- 3a. Từ khóa > 200 ký tự, hoặc `size` ngoài khoảng 1–100 → `VALIDATION_ERROR`.

### UC10 — Tải lên tài liệu

| | |
|---|---|
| **Module** | M3 · Nội dung (MOD-03-02) |
| **Tác nhân** | Giáo vụ khoa (Quản trị viên kế thừa) |
| **Mô tả** | Đưa tệp PDF / DOCX vào kho làm nguồn trích dẫn |
| **Kích hoạt** | Tab Tài liệu → **Tải lên…** |
| **Tiền điều kiện** | TĐK-0 với vai ≥ `CONTENT_ADMIN` |
| **Hậu điều kiện** | Có bản ghi `documents` (kèm `page_count` nếu là PDF) và tệp trong `storage/yyyy/MM/<uuid>.<ext>` |
| **Action** | `document.upload {title, fileName, contentBase64}` |
| **Quy tắc** | BR03 |
| **Sơ đồ** | ACT UC10, SEQ UC10, Trạng thái · Tài liệu |

**Luồng chính**
1. Giáo vụ chọn tệp (bộ lọc `.pdf`, `.docx`) và nhập tiêu đề; mặc định là tên tệp bỏ đuôi.
2. Client kiểm tra tệp ≤ 20 MB, mã hóa base64, gửi action.
3. Server kiểm tra tiêu đề 3–500 ký tự và kích thước.
4. Server nhận diện loại tệp theo **nội dung**: bắt đầu bằng `%PDF`, hoặc là ZIP và có đuôi `.docx`.
5. Server ghi tệp tạm, tính SHA-256, kiểm tra chưa có tài liệu trùng mã băm.
6. Với PDF: đếm số trang bằng PDFBox.
7. Server chuyển tệp vào kho bằng tên ngẫu nhiên và lưu bản ghi.
8. Client hiện tài liệu mới trong bảng; tài liệu dùng được ngay làm nguồn ở UC23.

**Ngoại lệ**
- 2a / 3a. Tệp > 20 MB → `FILE_TOO_LARGE`.
- 3b. Tiêu đề sai độ dài → `VALIDATION_ERROR`.
- 4a. Không phải PDF / DOCX → `UNSUPPORTED_FILE`.
- 5a. Trùng nội dung với tài liệu đã có → `DUPLICATE_DOCUMENT`.
- 6a. PDF hỏng → `VALIDATION_ERROR`.
- 7a. Lưu bản ghi lỗi → xóa tệp vừa chuyển.

Mọi trường hợp đều xóa tệp tạm.

### UC11 — Sửa thông tin tài liệu

| | |
|---|---|
| **Module** | M3 · Nội dung (MOD-03-03) |
| **Tác nhân** | Giáo vụ khoa (Quản trị viên kế thừa) |
| **Mô tả** | Đổi tiêu đề tài liệu |
| **Kích hoạt** | Tab Tài liệu → chọn dòng → **Đổi tên…** |
| **Tiền điều kiện** | TĐK-0 với vai ≥ `CONTENT_ADMIN` |
| **Hậu điều kiện** | `documents.title` mới (đã bỏ khoảng trắng hai đầu) |
| **Action** | `document.rename {id, title}` |
| **Sơ đồ** | ACT UC11, SEQ UC11 |

**Luồng chính**
1. Giáo vụ nhập tiêu đề mới; client gửi action.
2. Server kiểm tra tiêu đề 3–500 ký tự.
3. Server tìm tài liệu, cập nhật tiêu đề.
4. Client tải lại bảng.

**Ngoại lệ**
- 2a. Tiêu đề sai → `VALIDATION_ERROR`.
- 3a. Không có tài liệu → `DOCUMENT_NOT_FOUND`.

**Ghi chú:** không thay nội dung tệp. Muốn thay tệp thì tải lên tài liệu mới (UC10) và trỏ câu hỏi sang (UC23).

### UC12 — Gỡ tài liệu

| | |
|---|---|
| **Module** | M3 · Nội dung (MOD-03-04) |
| **Tác nhân** | Giáo vụ khoa (Quản trị viên kế thừa) |
| **Mô tả** | Xóa tài liệu không còn được câu hỏi nào dùng làm nguồn |
| **Kích hoạt** | Tab Tài liệu → chọn dòng → **Gỡ** |
| **Tiền điều kiện** | TĐK-0 với vai ≥ `CONTENT_ADMIN` |
| **Hậu điều kiện** | Không còn bản ghi; tệp gốc bị xóa (lỗi xóa tệp chỉ ghi log) |
| **Action** | `document.delete {id}` |
| **Quy tắc** | BR06 |
| **Sơ đồ** | ACT UC12, SEQ UC12, Trạng thái · Tài liệu |

**Luồng chính**
1. Giáo vụ bấm Gỡ; client hỏi xác nhận.
2. Client gửi action.
3. Server tìm tài liệu, đếm câu hỏi dẫn tới (kể cả câu hỏi đã ẩn).
4. Không còn câu hỏi nào: xóa bản ghi, rồi xóa tệp.
5. Client tải lại bảng.

**Luồng thay thế**
- 1a. Giáo vụ hủy xác nhận → không gửi gì.

**Ngoại lệ**
- 3a. Không có tài liệu → `DOCUMENT_NOT_FOUND`.
- 3b. Còn câu hỏi dẫn tới → `DOCUMENT_IN_USE` (khóa ngoại RESTRICT). Giáo vụ phải trỏ các câu hỏi đó sang tài liệu khác trước.

### UC22 — Quản lý chủ đề

| | |
|---|---|
| **Module** | M3 · Nội dung (MOD-03-05) |
| **Tác nhân** | Giáo vụ khoa (Quản trị viên kế thừa) |
| **Mô tả** | Thêm, đổi tên, đổi thứ tự hoặc xóa chủ đề |
| **Kích hoạt** | Tab **Soạn nội dung** → khung Chủ đề → **Thêm / Sửa / Xóa** |
| **Tiền điều kiện** | TĐK-0 với vai ≥ `CONTENT_ADMIN` |
| **Hậu điều kiện** | Bảng `topics` cập nhật; sinh viên thấy thay đổi ở lần mở khung chat kế tiếp |
| **Action** | `topic.list`, `topic.create {name, sortOrder?}`, `topic.update {id, name?, sortOrder?}`, `topic.delete {id}` |
| **Quy tắc** | BR03 |
| **Sơ đồ** | ACT UC22, SEQ UC22 |

**Luồng chính**
1. Giáo vụ chọn thao tác và nhập tên chủ đề.
2. Client gửi action tương ứng.
3. Server kiểm tra:
   - tên 1–100 ký tự;
   - với sửa / xóa: chủ đề tồn tại;
   - với thêm / sửa: tên chưa bị dùng (không phân biệt hoa thường);
   - với xóa: chủ đề không còn câu hỏi.
4. Server lưu thay đổi.
5. Client tải lại danh sách chủ đề.

**Ngoại lệ**
- 3a. Tên sai độ dài → `VALIDATION_ERROR`.
- 3b. Không có chủ đề → `TOPIC_NOT_FOUND`.
- 3c. Trùng tên → `TOPIC_NAME_TAKEN`.
- 3d. Còn câu hỏi → `TOPIC_NOT_EMPTY`.

### UC23 — Quản lý câu hỏi – câu trả lời

| | |
|---|---|
| **Module** | M3 · Nội dung (MOD-03-06) |
| **Tác nhân** | Giáo vụ khoa (Quản trị viên kế thừa) |
| **Mô tả** | Soạn câu hỏi (nội dung nút) và câu trả lời, gắn tài liệu nguồn, số trang; ẩn / hiện câu hỏi |
| **Kích hoạt** | Tab Soạn nội dung → chọn chủ đề → bảng câu hỏi và biểu mẫu |
| **Tiền điều kiện** | TĐK-0 với vai ≥ `CONTENT_ADMIN`; đã có ít nhất một tài liệu (UC10) |
| **Hậu điều kiện** | Bảng `questions` cập nhật; câu hỏi đang hiện thành nút ở UC05 |
| **Action** | `question.listForEditor {topicId}`, `question.create`, `question.update {id, …}`, `question.hide {id}` |
| **Quy tắc** | BR03, BR06 |
| **Sơ đồ** | ACT UC23, SEQ UC23, Trạng thái · Câu hỏi |

**Luồng chính**
1. Giáo vụ chọn chủ đề; server trả mọi câu hỏi của chủ đề, kể cả câu hỏi đã ẩn.
2. Giáo vụ chọn một câu hỏi để sửa, hoặc bấm **Câu hỏi mới**. Nhập:
   - nội dung nút (≤ 200 ký tự);
   - câu trả lời (≤ 5000 ký tự);
   - tài liệu nguồn;
   - số trang;
   - thứ tự hiển thị;
   - hiển thị / ẩn.
3. Client kiểm tra đã chọn chủ đề và tài liệu nguồn, rồi gửi `question.create` hoặc `question.update`.
4. Server kiểm tra:
   - đủ trường, đúng độ dài;
   - chủ đề và tài liệu tồn tại;
   - 1 ≤ trang ≤ số trang của tài liệu (nếu biết số trang).
5. Server lưu câu hỏi.
6. Client báo "Đã lưu câu hỏi" và tải lại bảng.

**Luồng thay thế**
- 2a. Bấm **Ẩn câu hỏi** → client gửi `question.hide`, server đặt `is_active = false`. Câu hỏi không còn là nút nhưng lịch sử hội thoại vẫn giữ.
- 2b. Hiện lại câu hỏi đã ẩn → sửa, tích "Hiển thị cho sinh viên" (`active = true`).

**Ngoại lệ**
- 3a. Chưa có tài liệu nguồn → client nhắc tải tài liệu lên trước (BR06).
- 4a. Dữ liệu hoặc số trang sai → `VALIDATION_ERROR`; giáo vụ sửa và gửi lại.
- 4b. Thiếu chủ đề / tài liệu / câu hỏi → `TOPIC_NOT_FOUND` / `DOCUMENT_NOT_FOUND` / `QUESTION_NOT_FOUND`.

---

## M4 · Quản trị

### UC14 — Tra cứu người dùng

| | |
|---|---|
| **Module** | M4 · Quản trị (MOD-04-01) |
| **Tác nhân** | Quản trị viên |
| **Mô tả** | Tìm tài khoản theo mã, email hoặc họ tên |
| **Kích hoạt** | Tab **Tài khoản**, nhập từ khóa, bấm **Tìm** |
| **Tiền điều kiện** | TĐK-0 với vai `SYSTEM_ADMIN` |
| **Hậu điều kiện** | Không thay đổi dữ liệu |
| **Action** | `user.list {q?, page, size}` |
| **Quy tắc** | BR03 |
| **Sơ đồ** | ACT UC14, SEQ UC14 |

**Luồng chính**
1. Client gửi `user.list` (trang 20 mục).
2. Server tìm trong mã, email, họ tên (không phân biệt hoa thường), sắp theo mã.
3. Client hiện bảng: mã, họ tên, email, vai, khoa, trạng thái; có phân trang.

**Ngoại lệ**
- 2a. Từ khóa > 200 ký tự → `VALIDATION_ERROR`.

### UC15 — Khóa / mở tài khoản

| | |
|---|---|
| **Module** | M4 · Quản trị (MOD-04-02) |
| **Tác nhân** | Quản trị viên |
| **Mô tả** | Khóa tài khoản để chặn truy cập, hoặc mở lại |
| **Kích hoạt** | Tab Tài khoản → chọn dòng → **Khóa / Mở khóa** |
| **Tiền điều kiện** | TĐK-0 với vai `SYSTEM_ADMIN` |
| **Hậu điều kiện** | `users.is_active` đổi; tài khoản bị khóa bị từ chối **ngay ở yêu cầu kế tiếp** |
| **Action** | `user.update {id, active}` |
| **Quy tắc** | BR02, BR04 |
| **Sơ đồ** | ACT UC15, SEQ UC15, Trạng thái · Tài khoản |

**Luồng chính**
1. Quản trị viên bấm Khóa / Mở khóa; client hỏi xác nhận.
2. Client gửi action với giá trị ngược trạng thái hiện tại.
3. Server kiểm tra tài khoản đích khác chính người gọi và có tồn tại.
4. Server cập nhật trạng thái.
5. Client tải lại bảng.
6. Yêu cầu kế tiếp của tài khoản vừa bị khóa → `UNAUTHORIZED`, client của người đó về màn đăng nhập.

**Luồng thay thế**
- 1a. Hủy xác nhận → không gửi gì.

**Ngoại lệ**
- 3a. Tự khóa chính mình → `SELF_MODIFICATION` (BR04).
- 3b. Không có tài khoản → `USER_NOT_FOUND`.
- 2a. Không có `active` lẫn `role` → `VALIDATION_ERROR`.

### UC16 — Phân quyền

| | |
|---|---|
| **Module** | M4 · Quản trị (MOD-04-03) |
| **Tác nhân** | Quản trị viên |
| **Mô tả** | Đổi vai của một tài khoản sang `USER`, `CONTENT_ADMIN` hoặc `SYSTEM_ADMIN` |
| **Kích hoạt** | Tab Tài khoản → chọn dòng → chọn vai → **Áp dụng** |
| **Tiền điều kiện** | TĐK-0 với vai `SYSTEM_ADMIN` |
| **Hậu điều kiện** | `users.role` đổi; vai mới có hiệu lực ngay ở yêu cầu kế tiếp |
| **Action** | `user.update {id, role}` |
| **Quy tắc** | BR02, BR04 |
| **Sơ đồ** | ACT UC16, SEQ UC16, Trạng thái · Tài khoản |

**Luồng chính**
1. Quản trị viên chọn vai mới, bấm Áp dụng.
2. Client gửi action nếu vai mới khác vai hiện tại.
3. Server kiểm tra như UC15, rồi cập nhật vai.
4. Client tải lại bảng.

**Luồng thay thế**
- 2a. Vai mới trùng vai hiện tại → client không gửi gì.

**Ngoại lệ**
- 3a. Tự đổi vai chính mình → `SELF_MODIFICATION`.
- 3b. Không có tài khoản → `USER_NOT_FOUND`.

**Ghi chú:** tab của người bị đổi vai cập nhật ở lần đăng nhập sau. Quyền thì đổi ngay vì server kiểm ở mỗi yêu cầu.

### UC20 — Kiểm tra trạng thái hệ thống

| | |
|---|---|
| **Module** | M4 · Quản trị (MOD-04-04) |
| **Tác nhân** | Quản trị viên |
| **Mô tả** | Xem trạng thái server, kết nối CSDL, thời gian chạy, giờ server và độ trễ TCP |
| **Kích hoạt** | Mở tab **Hệ thống** hoặc bấm **Kiểm tra lại** |
| **Tiền điều kiện** | TĐK-0 với vai `SYSTEM_ADMIN` (riêng `system.ping` là công khai) |
| **Hậu điều kiện** | Không thay đổi dữ liệu |
| **Action** | `system.ping` × 5, `system.health` |
| **Sơ đồ** | ACT UC20, SEQ UC20 |

**Luồng chính**
1. Client gửi `system.ping` 5 lần, đo thời gian khứ hồi trung bình.
2. Client gửi `system.health`.
3. Server chạy `SELECT 1`.
4. Server trả `status`, `database`, thời gian chạy (giây), giờ server.
5. Client hiện bảng trạng thái và độ trễ (ms).

**Luồng thay thế**
- 3a. Truy vấn lỗi → vẫn trả kết quả với `status = DEGRADED`, `database = DOWN`.

**Ngoại lệ**
- 2a. CSDL hỏng ngay ở bước đọc tài khoản khi kiểm token → `INTERNAL_ERROR` trước khi tới controller.

---

## Ma trận use case ↔ sơ đồ

| UC | Hoạt động | Tuần tự | Trạng thái liên quan |
|---|---|---|---|
| UC01 | ACT UC01 | SEQ UC01, SEQ Kết nối TCP | Phiên client, Kết nối TCP |
| UC02 | ACT UC02 | SEQ UC02 | Phiên client, Kết nối TCP |
| UC03 | ACT UC03 | SEQ UC03 | — |
| UC04 | ACT UC04 | SEQ UC04 | — |
| UC05 | ACT UC05 | SEQ UC05 | Câu hỏi |
| UC06 | ACT UC06 | SEQ UC06 | — |
| UC07 | ACT UC07 | SEQ UC07 | — |
| UC08 | ACT UC08 | SEQ UC08 | — |
| UC10 | ACT UC10 | SEQ UC10 | Tài liệu |
| UC11 | ACT UC11 | SEQ UC11 | Tài liệu |
| UC12 | ACT UC12 | SEQ UC12 | Tài liệu |
| UC14 | ACT UC14 | SEQ UC14 | — |
| UC15 | ACT UC15 | SEQ UC15 | Tài khoản |
| UC16 | ACT UC16 | SEQ UC16 | Tài khoản |
| UC20 | ACT UC20 | SEQ UC20 | — |
| UC22 | ACT UC22 | SEQ UC22 | — |
| UC23 | ACT UC23 | SEQ UC23 | Câu hỏi |

Sơ đồ cấp hệ thống:
- Use case tổng quan và theo module M1–M4;
- Sơ đồ ngữ cảnh;
- Kiến trúc TCP socket;
- Cấu trúc MVC client – server;
- **Sơ đồ thành phần**, **Sơ đồ triển khai**, **Sơ đồ gói**;
- Sơ đồ lớp, ERD;
- BFD.
