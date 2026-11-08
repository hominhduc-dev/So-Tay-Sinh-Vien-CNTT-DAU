# Đặc tả yêu cầu phần mềm (SRS) — Sổ Tay Sinh Viên CNTT

**Phiên bản:** 2.1 · 08/11/2026
**Học phần:** Lập trình mạng máy tính — Trường Đại học Kiến trúc Đà Nẵng
**Tài liệu liên quan:** [`DAC-TA-TRIEN-KHAI.md`](DAC-TA-TRIEN-KHAI.md) (lược đồ CSDL, API, hướng dẫn
Spring Boot), [`slides/SRS-tac-nhan-va-use-case.pptx`](slides/SRS-tac-nhan-va-use-case.pptx) (biểu đồ
use case, luồng), slide Chương 3 (BFD, IFD, DFD, ERD, biểu đồ lớp).

| Phiên bản | Ngày | Thay đổi |
|---|---|---|
| 1.0 | 09/2026 | Bản RAG: hỏi đáp tự do, sinh câu trả lời bằng Gemini |
| 2.0 | 07/11/2026 | Chatbot dạng nút chọn, câu trả lời soạn sẵn, bỏ AI; đơn vị và vai trò lưu trên tài khoản |
| 2.1 | 08/11/2026 | Sinh viên gõ được câu hỏi: hệ thống tìm câu soạn sẵn khớp nhất; không khớp thì gửi giáo vụ trả lời (UC24–UC27) |

---

## 1. Giới thiệu

### 1.1. Mục đích

Tài liệu này đặc tả đầy đủ yêu cầu chức năng và phi chức năng của hệ thống **Sổ Tay Sinh Viên CNTT**,
làm cơ sở cho thiết kế (Chương 3), hiện thực, kiểm thử và nghiệm thu. Người đọc: nhóm phát triển,
giảng viên hướng dẫn, giáo vụ khoa CNTT.

### 1.2. Phạm vi sản phẩm

Hệ thống web theo mô hình **client–server** giúp sinh viên ngành CNTT tra cứu quy định và quá trình
học tập: đăng ký học phần, điều kiện đồ án, chuẩn đầu ra, bảo lưu, cảnh báo học vụ, xét tốt nghiệp.

Sinh viên hỏi qua một **chatbot**, theo hai cách:

- **Bấm nút:** chọn chủ đề → chọn câu hỏi có sẵn → nhận câu trả lời do giáo vụ soạn, kèm tài liệu nguồn và
  số trang để tự kiểm chứng.
- **Gõ câu hỏi:** hệ thống tìm câu hỏi soạn sẵn khớp nhất theo từ khóa. Khớp rõ thì trả lời ngay; khớp
  vừa thì gợi ý vài câu gần nhất; không khớp thì sinh viên gửi câu hỏi cho giáo vụ, giáo vụ trả lời sau.

Mọi câu trả lời đều do giáo vụ viết; hệ thống **không dùng AI** để sinh nội dung.

**Thuộc phạm vi**

- Hỏi đáp qua nút câu hỏi có sẵn hoặc gõ câu hỏi, kèm nguồn; xem lại hội thoại cá nhân.
- Gửi câu hỏi chưa có câu trả lời cho giáo vụ; giáo vụ trả lời qua hộp thư.
- Kho tài liệu (PDF/DOCX) làm nguồn trích dẫn; giáo vụ soạn chủ đề và câu hỏi – câu trả lời.
- Quản lý trạng thái và vai trò tài khoản thuộc khoa CNTT.
- Kiểm tra trạng thái hệ thống; chạy trong mạng LAN để thực hành TCP/HTTP.

**Ngoài phạm vi**

- Dùng AI / mô hình ngôn ngữ để tự sinh câu trả lời; hiểu ngữ nghĩa câu hỏi ngoài so khớp từ khóa.
- Tra cứu điểm, công nợ học phí, lịch học, hồ sơ cá nhân; đăng ký tín chỉ.
- Quản trị nhiều khoa; tự đăng ký tài khoản; đăng nhập SSO; OCR tài liệu scan.

### 1.3. Định nghĩa và viết tắt

| Thuật ngữ | Nghĩa |
|---|---|
| Chủ đề (Topic) | Nhóm câu hỏi, hiện thành một nút ở bước đầu của khung chat |
| Câu hỏi (Question) | Một nút câu hỏi có sẵn, gắn câu trả lời soạn sẵn và một nguồn |
| Nguồn | Tài liệu gốc và số trang chứa căn cứ của câu trả lời |
| Hội thoại (Conversation) | Chuỗi các lượt chọn câu hỏi của một sinh viên |
| Tin nhắn (Message) | Một lượt hỏi – đáp trong hội thoại: câu hỏi soạn sẵn được chọn (bấm nút hoặc khớp từ câu gõ) |
| Câu hỏi gửi giáo vụ (Inquiry) | Câu sinh viên gõ mà không khớp câu soạn sẵn nào, chờ giáo vụ trả lời |
| So khớp từ khóa | Bỏ dấu, bỏ từ hư ("là", "gì", "bao nhiêu"…), chấm điểm theo số từ khóa trùng giữa câu gõ và câu hỏi soạn sẵn |
| Ẩn câu hỏi | Gỡ câu hỏi khỏi danh sách nút nhưng giữ lại để lịch sử còn đọc được |
| JWT | JSON Web Token — mã xác thực client gửi kèm mỗi request |
| UC, BR, NFR | Use case, quy tắc nghiệp vụ (business rule), yêu cầu phi chức năng |

### 1.4. Tổng quan tài liệu

Mục 2 mô tả tổng quan sản phẩm và người dùng. Mục 3 đặc tả từng use case. Mục 4 là yêu cầu dữ liệu,
mục 5 yêu cầu giao diện ngoài, mục 6 yêu cầu phi chức năng, mục 7 quy tắc nghiệp vụ. Phụ lục A là ma
trận truy vết.

---

## 2. Mô tả tổng quan

### 2.1. Bối cảnh sản phẩm

Sản phẩm độc lập, không tích hợp hệ thống đào tạo của trường. Ba thành phần:

```
 Trình duyệt (client) ── HTTP request / response (JSON, JWT) ──▶ Server Spring Boot ──▶ PostgreSQL
 sinh viên, giáo vụ, quản trị viên                                 + thư mục lưu tệp tài liệu
```

Nội dung (tài liệu, câu trả lời) do giáo vụ khoa đưa vào từ văn bản của Phòng Đào tạo và Ban chủ nhiệm
khoa CNTT; hệ thống không tự sinh nội dung.

### 2.2. Chức năng chính

| Module | Chức năng |
|---|---|
| M1 · Tài khoản | Đăng nhập, đăng xuất, xem thông tin cá nhân, đổi mật khẩu |
| M2 · Hỏi đáp | Chọn hoặc gõ câu hỏi và nhận câu trả lời kèm nguồn; gửi câu hỏi cho giáo vụ và xem câu trả lời; xem lại hội thoại, mở nguồn |
| M3 · Nội dung | Tra cứu tài liệu; giáo vụ quản lý tài liệu, chủ đề, câu hỏi – câu trả lời, trả lời câu hỏi sinh viên gửi |
| M4 · Quản trị | Tra cứu, khóa / mở, phân quyền tài khoản; kiểm tra trạng thái hệ thống |

### 2.3. Tác nhân và người dùng

| Tác nhân | Vai | Đặc điểm | Tần suất |
|---|---|---|---|
| Sinh viên CNTT | `USER` | Quen dùng web và điện thoại, không cần hướng dẫn | Thường xuyên, tăng mạnh mùa đăng ký học phần, xét tốt nghiệp |
| Giáo vụ khoa CNTT | `CONTENT_ADMIN` | Nắm quy định, không chuyên kỹ thuật | Khi có văn bản mới hoặc quy định thay đổi |
| Quản trị viên | `SYSTEM_ADMIN` | Có kiến thức kỹ thuật cơ bản | Thỉnh thoảng |

Quyền kế thừa: Giáo vụ có mọi quyền của Sinh viên; Quản trị viên có mọi quyền của Giáo vụ.

**Đối tượng tham gia** (không thao tác trên hệ thống, không vẽ trong biểu đồ use case):

| Đối tượng | Mối quan tâm | Ràng buộc đưa vào SRS |
|---|---|---|
| Ban chủ nhiệm khoa CNTT | Giảm câu hỏi lặp lại cho giáo vụ | Chỉ nội dung dùng cho khoa CNTT (BR01, mục 1.2) |
| Nhà trường / Phòng Đào tạo | Quy định được dẫn đúng văn bản gốc | Mỗi câu trả lời phải kèm nguồn (BR06) |
| Giảng viên học phần Lập trình mạng | Sản phẩm minh họa client–server, HTTP, TCP | NFR-01 hiệu năng trên LAN |
| Nhóm phát triển | Dễ bảo trì, kiểm thử, triển khai lại | NFR-05, NFR-07 |

### 2.4. Ràng buộc

- C1. Kiến trúc client–server; mọi giao tiếp qua HTTP, dữ liệu JSON (trừ tải tệp).
- C2. Server viết bằng Java 21, Spring Boot 3.3; CSDL PostgreSQL 16.
- C3. Không dùng dịch vụ AI hay API bên ngoài.
- C4. Chạy được trong một mạng LAN không có Internet (phòng máy).
- C5. Giao diện và nội dung bằng tiếng Việt.

### 2.5. Giả định và phụ thuộc

- A1. Tài khoản được cấp sẵn (seed / quản trị viên tạo trực tiếp trong CSDL); không có tự đăng ký.
- A2. Tài liệu PDF có lớp chữ hoặc là bản scan đều được, vì hệ thống không đọc nội dung tệp — chỉ đếm số trang.
- A3. Giáo vụ chịu trách nhiệm tính đúng của câu trả lời soạn sẵn.

---

## 3. Yêu cầu chức năng

Mỗi use case ghi: tác nhân, tiền điều kiện, luồng chính, luồng thay thế / ngoại lệ, hậu điều kiện.
Mọi use case trừ UC01 có tiền điều kiện chung **"đã đăng nhập, token hợp lệ, tài khoản đang hoạt động
và thuộc khoa CNTT"** (BR01, BR02); không thỏa → hệ thống trả lỗi xác thực, client chuyển về trang đăng nhập.
API tương ứng ghi ở [`DAC-TA-TRIEN-KHAI.md`](DAC-TA-TRIEN-KHAI.md) mục 6.

### 3.1. M1 · Tài khoản

**UC01 — Đăng nhập**

- **Tác nhân:** Sinh viên, Giáo vụ, Quản trị viên.
- **Tiền điều kiện:** chưa đăng nhập.
- **Luồng chính:**
  1. Người dùng nhập mã sinh viên / cán bộ hoặc email (≤ 255 ký tự) và mật khẩu.
  2. Hệ thống kiểm tra thông tin, trạng thái hoạt động và đơn vị CNTT.
  3. Hệ thống cấp token (hiệu lực 8 giờ), trả thông tin người dùng và vai.
  4. Client chuyển tới trang theo vai: khung chat (Sinh viên), trang nội dung (Giáo vụ), trang quản trị (Quản trị viên).
- **Ngoại lệ:** 2a. Sai mật khẩu, tài khoản khóa hoặc không thuộc CNTT → báo "Thông tin đăng nhập không đúng", không nói rõ lý do.
- **Hậu điều kiện:** client giữ token để gửi kèm các request sau.

**UC02 — Đăng xuất**

- **Luồng chính:** người dùng bấm Đăng xuất; client xóa token và về trang đăng nhập. Không gọi server.

**UC03 — Xem thông tin cá nhân**

- **Luồng chính:** hệ thống trả họ tên, mã, email, vai, đơn vị của người gọi; client hiển thị.

**UC04 — Đổi mật khẩu**

- **Luồng chính:**
  1. Người dùng nhập mật khẩu hiện tại và mật khẩu mới (8–72 ký tự).
  2. Hệ thống kiểm tra mật khẩu hiện tại, lưu mật khẩu mới dạng băm BCrypt.
- **Ngoại lệ:** 2a. Mật khẩu hiện tại sai hoặc mật khẩu mới không hợp lệ → báo lỗi, không đổi.

### 3.2. M2 · Hỏi đáp

**UC05 — Chọn câu hỏi, nhận trả lời**

- **Tác nhân:** Sinh viên (Giáo vụ, Quản trị viên kế thừa).
- **Luồng chính:**
  1. Sinh viên mở khung chat; hệ thống trả danh sách chủ đề theo thứ tự hiển thị.
  2. Sinh viên chọn một chủ đề; hệ thống trả các câu hỏi đang hiện của chủ đề đó.
  3. Sinh viên bấm một nút câu hỏi.
  4. Hệ thống đọc câu trả lời soạn sẵn, tài liệu nguồn và số trang của câu hỏi.
  5. Hệ thống lưu một tin nhắn vào hội thoại hiện tại (tạo hội thoại mới nếu chưa có, tiêu đề = nội dung câu hỏi đầu tiên).
  6. Hệ thống trả câu hỏi, câu trả lời và nguồn; client hiển thị thành một lượt hỏi – đáp và lại hiện các nút để chọn tiếp.
- **Thay thế:** 2a. Chủ đề chưa có câu hỏi → client báo "Chủ đề chưa có nội dung", quay lại bước 1.
- **Ngoại lệ:**
  - 4a. Câu hỏi đã bị ẩn hoặc không tồn tại → báo "Câu hỏi không còn", tải lại danh sách câu hỏi.
  - 5a. Hội thoại không thuộc người gọi → báo không tìm thấy (BR05).
- **Hậu điều kiện:** có thêm một tin nhắn trong hội thoại của sinh viên.

**UC06 — Xem lại hội thoại**

- **Luồng chính:**
  1. Hệ thống trả danh sách hội thoại của chính người gọi, mới nhất trước, phân trang.
  2. Người dùng chọn một hội thoại; hệ thống trả các lượt hỏi – đáp theo thứ tự thời gian, kèm nguồn.
- **Ngoại lệ:** 2a. Hội thoại của người khác → báo không tìm thấy (BR05).
- **Ghi chú:** câu hỏi đã ẩn vẫn hiển thị trong lịch sử; nội dung là bản hiện hành của câu trả lời.

**UC24 — Gõ câu hỏi**

- **Tác nhân:** Sinh viên (Giáo vụ, Quản trị viên kế thừa).
- **Luồng chính:**
  1. Sinh viên gõ câu hỏi (3–500 ký tự) vào ô nhập của khung chat.
  2. Hệ thống chuẩn hóa câu gõ (chữ thường, bỏ dấu, bỏ từ hư) và chấm điểm với mọi câu hỏi đang hiện.
  3. Câu điểm cao nhất đạt ngưỡng khớp rõ và hơn hẳn câu thứ hai → hệ thống trả lời bằng câu trả lời soạn
     sẵn của câu đó, ghi rõ "câu hỏi có sẵn gần nhất", kèm nguồn; lưu tin nhắn cùng câu đã gõ.
  4. Client hiện thêm tối đa 2 câu hỏi liên quan để sinh viên bấm tiếp.
- **Thay thế:**
  - 3a. Có câu đạt ngưỡng gợi ý nhưng chưa khớp rõ → hiện "Có phải bạn muốn hỏi:" với tối đa 3 nút; sinh
    viên bấm một nút → như UC05 bước 3; hoặc chọn gửi giáo vụ → UC25. Không lưu tin nhắn.
  - 3b. Không câu nào đạt ngưỡng gợi ý → báo "Chưa có câu trả lời soạn sẵn", mời gửi giáo vụ → UC25.
- **Ngoại lệ:** 1a. Ít hơn 3 ký tự → báo lỗi, không gửi.
- **Hậu điều kiện:** 3 → có thêm một tin nhắn trong hội thoại; 3a, 3b → dữ liệu không đổi.

**UC25 — Gửi câu hỏi cho giáo vụ**

- **Tác nhân:** Sinh viên.
- **Tiền điều kiện:** vừa gõ câu hỏi ở UC24 mà không được trả lời.
- **Luồng chính:** sinh viên bấm "Gửi câu hỏi cho giáo vụ"; hệ thống lưu câu hỏi ở trạng thái **Chờ trả lời**
  và báo đã gửi.
- **Ngoại lệ:** đang có 5 câu chờ trả lời → từ chối, đề nghị đợi (BR08).

**UC26 — Xem câu trả lời của giáo vụ**

- **Tác nhân:** Sinh viên.
- **Luồng chính:** hệ thống trả các câu đã gửi của chính người gọi, mới nhất trước, kèm trạng thái; câu đã
  trả lời có nội dung trả lời và nguồn (nếu có); sinh viên mở nguồn như UC08.

**UC08 — Xem nguồn trích dẫn**

- **Luồng chính:** người dùng bấm nguồn của một câu trả lời; hệ thống trả tệp gốc; client mở PDF tại đúng trang được dẫn (DOCX: mở / tải về).
- **Ngoại lệ:** tài liệu không tồn tại → báo không tìm thấy.

### 3.3. M3 · Nội dung

**UC07 — Tra cứu kho tài liệu**

- **Tác nhân:** mọi vai.
- **Luồng chính:** người dùng nhập từ khóa (≤ 200 ký tự, có thể để trống); hệ thống trả danh sách tài liệu khớp tiêu đề, phân trang (≤ 100 mục / trang), gồm tiêu đề, loại tệp, số trang, người tải lên, ngày tải; người dùng mở tệp như UC08.

**UC10 — Tải lên tài liệu**

- **Tác nhân:** Giáo vụ (Quản trị viên kế thừa).
- **Luồng chính:**
  1. Giáo vụ chọn tệp PDF hoặc DOCX (≤ 20 MB), nhập tiêu đề (3–500 ký tự).
  2. Hệ thống kiểm quyền, kiểm định dạng và kích thước, tính mã băm SHA-256.
  3. Hệ thống lưu tệp với tên ngẫu nhiên, đếm số trang (PDF), lưu bản ghi tài liệu.
  4. Hệ thống trả tài liệu vừa tạo; tài liệu dùng được ngay làm nguồn ở UC23.
- **Ngoại lệ:** 2a. Trùng nội dung với tài liệu đã có → từ chối "Tài liệu đã tồn tại"; 2b. sai định dạng / quá lớn → từ chối.

**UC11 — Sửa thông tin tài liệu**

- **Luồng chính:** giáo vụ sửa tiêu đề (3–500 ký tự); hệ thống lưu. Không thay được tệp — muốn thay tệp thì tải lên tài liệu mới và trỏ câu hỏi sang.

**UC12 — Gỡ tài liệu**

- **Luồng chính:** giáo vụ chọn gỡ, xác nhận; hệ thống xóa bản ghi và tệp.
- **Ngoại lệ:** còn câu hỏi (kể cả đã ẩn) dẫn tới tài liệu → từ chối "Tài liệu đang được dùng làm nguồn"; giáo vụ phải trỏ các câu hỏi đó sang tài liệu khác trước.

**UC22 — Quản lý chủ đề**

- **Tác nhân:** Giáo vụ.
- **Luồng chính:** giáo vụ thêm chủ đề (tên ≤ 100 ký tự, không trùng), đổi tên, đổi thứ tự hiển thị, hoặc xóa; thay đổi có hiệu lực ngay ở lần sinh viên mở khung chat kế tiếp.
- **Ngoại lệ:** trùng tên → từ chối; xóa chủ đề còn câu hỏi → từ chối "Chủ đề còn câu hỏi".

**UC23 — Quản lý câu hỏi – câu trả lời**

- **Tác nhân:** Giáo vụ.
- **Luồng chính:**
  1. Giáo vụ chọn chủ đề; hệ thống trả mọi câu hỏi của chủ đề, kể cả câu hỏi đang ẩn.
  2. Giáo vụ thêm hoặc sửa câu hỏi: nội dung nút (≤ 200 ký tự), câu trả lời (≤ 5000 ký tự), tài liệu nguồn, số trang, thứ tự hiển thị.
  3. Hệ thống kiểm tra chủ đề và tài liệu tồn tại, số trang ≥ 1 và không vượt số trang của tài liệu (nếu biết).
  4. Hệ thống lưu; câu hỏi hiện thành nút ở UC05.
- **Thay thế:** 2a. Ẩn câu hỏi → câu hỏi không còn hiện thành nút, lịch sử vẫn giữ; 2b. hiện lại câu hỏi đã ẩn.
- **Ngoại lệ:** 3a. Dữ liệu không hợp lệ → báo lỗi từng trường, giáo vụ sửa và gửi lại.

**UC27 — Trả lời câu hỏi của sinh viên**

- **Tác nhân:** Giáo vụ (Quản trị viên kế thừa).
- **Luồng chính:**
  1. Giáo vụ mở hộp thư; hệ thống trả các câu **Chờ trả lời**, câu chờ lâu nhất trước, kèm tên và mã sinh viên.
  2. Giáo vụ viết câu trả lời (≤ 5000 ký tự), chọn tài liệu nguồn và số trang (khuyến khích, không bắt buộc).
  3. Hệ thống kiểm tra số trang, lưu câu trả lời, chuyển trạng thái **Đã trả lời**.
- **Thay thế:**
  - 2a. Giáo vụ chọn "Lưu thành câu hỏi có sẵn": chọn chủ đề, sửa chữ trên nút; bắt buộc có nguồn; hệ thống
    tạo thêm câu hỏi như UC23 — lần sau sinh viên gõ câu tương tự sẽ được trả lời ngay.
  - 2b. Sửa câu trả lời đã gửi: mở câu ở mục "Đã trả lời", sửa và gửi lại.
- **Ngoại lệ:** số trang không hợp lệ hoặc thiếu nội dung → báo lỗi, không lưu.

### 3.4. M4 · Quản trị

**UC14 — Tra cứu người dùng**

- **Tác nhân:** Quản trị viên.
- **Luồng chính:** quản trị viên nhập từ khóa (≤ 200 ký tự) theo mã, email hoặc họ tên; hệ thống trả danh sách tài khoản kèm vai, đơn vị, trạng thái, phân trang.

**UC15 — Khóa / mở tài khoản**

- **Luồng chính:** quản trị viên chọn khóa hoặc mở một tài khoản; hệ thống cập nhật trạng thái. Tài khoản bị khóa bị từ chối ngay ở request kế tiếp (BR02).
- **Ngoại lệ:** tự khóa chính mình → từ chối (BR04).

**UC16 — Phân quyền**

- **Luồng chính:** quản trị viên chọn một trong ba vai cho tài khoản; hệ thống cập nhật. Vai mới có hiệu lực ngay ở request kế tiếp.
- **Ngoại lệ:** tự đổi vai của chính mình → từ chối (BR04).

**UC20 — Kiểm tra trạng thái hệ thống**

- **Luồng chính:** quản trị viên mở trang trạng thái; hệ thống trả trạng thái server, kết nối CSDL, thời gian đã chạy liên tục và giờ server.
- **Thay thế:** CSDL không phản hồi → vẫn trả kết quả, đánh dấu CSDL "DOWN".

### 3.5. Use case đã bỏ so với bản 1.0

| Mã | Tên | Lý do |
|---|---|---|
| UC09 | Theo dõi xử lý tài liệu | Tài liệu không còn qua bước xử lý tự động |
| UC13 | Chạy lại khi lỗi | Như trên |
| UC18 | Tra cứu lai qua API | Không còn tìm kiếm ngữ nghĩa |
| UC19 | Đánh giá truy hồi | Không còn truy hồi |
| UC21 | Cấu hình dịch vụ AI | Không dùng AI |

---

## 4. Yêu cầu dữ liệu

Bảy thực thể, khớp ERD và biểu đồ lớp Chương 3. Lược đồ chi tiết: `DAC-TA-TRIEN-KHAI.md` mục 5.

| Thực thể | Bảng | Thuộc tính chính | Ràng buộc |
|---|---|---|---|
| Tài khoản | `users` | mã, email, mật khẩu băm, họ tên, vai, đơn vị, trạng thái | Mã và email duy nhất; vai ∈ {USER, CONTENT_ADMIN, SYSTEM_ADMIN} |
| Chủ đề | `topics` | tên, thứ tự | Tên duy nhất |
| Câu hỏi | `questions` | chủ đề, nội dung nút, câu trả lời, tài liệu nguồn, trang, đang hiện, thứ tự | Thuộc đúng một chủ đề; dẫn tới đúng một tài liệu; trang ≥ 1 |
| Tài liệu | `documents` | tiêu đề, đường dẫn tệp, mã băm, loại tệp, số trang, người tải lên | Mã băm duy nhất |
| Hội thoại | `conversations` | người tạo, tiêu đề, thời điểm tạo | Thuộc đúng một tài khoản |
| Tin nhắn | `messages` | hội thoại, câu hỏi được chọn, câu đã gõ (nếu có), thời điểm | Xóa hội thoại thì xóa tin nhắn |
| Câu hỏi gửi giáo vụ | `inquiries` | người gửi, nội dung, trạng thái, câu trả lời, tài liệu nguồn, trang, người trả lời, thời điểm gửi / trả lời | Trạng thái ∈ {Chờ trả lời, Đã trả lời}; đã trả lời thì phải có câu trả lời; có tài liệu thì phải có trang |

Quan hệ: Tài khoản 1–n Hội thoại; Hội thoại 1–n Tin nhắn; Câu hỏi 1–n Tin nhắn; Chủ đề 1–n Câu hỏi;
Tài liệu 1–n Câu hỏi; Tài khoản 1–n Tài liệu (người tải lên); Tài khoản 1–n Câu hỏi gửi giáo vụ (người
gửi); Tài liệu 1–n Câu hỏi gửi giáo vụ (nguồn của câu trả lời, có thể không có).

**Lưu giữ:** câu hỏi không bị xóa vĩnh viễn (chỉ ẩn); hội thoại giữ đến khi tài khoản bị xóa khỏi CSDL;
tệp tài liệu xóa cùng bản ghi ở UC12. Sao lưu CSDL và thư mục tệp hằng tuần.

---

## 5. Yêu cầu giao diện ngoài

### 5.1. Giao diện người dùng

- UI-1. Trang đăng nhập; sau đăng nhập có thanh điều hướng theo vai.
- UI-2. **Khung chat:** hàng nút chủ đề → hàng nút câu hỏi → bong bóng câu trả lời có nhãn nguồn
  "Tên tài liệu · trang N"; bấm nhãn mở tệp gốc. Luôn có nút "Chọn chủ đề khác" và ô "Hoặc gõ câu hỏi
  của bạn"; câu gõ không khớp hiện nút "Gửi câu hỏi cho giáo vụ".
- UI-2b. Trang "Câu hỏi đã gửi" của sinh viên; trang "Hộp thư" của giáo vụ với hai mục Chờ trả lời / Đã trả lời.
- UI-3. Danh sách hội thoại bên trái (máy tính) hoặc trong menu (điện thoại).
- UI-4. Trang giáo vụ: bảng tài liệu (tải lên, sửa tiêu đề, gỡ), danh sách chủ đề kéo thả thứ tự, biểu
  mẫu câu hỏi chọn tài liệu nguồn từ danh sách và nhập số trang.
- UI-5. Trang quản trị: bảng tài khoản có ô tìm kiếm, công tắc khóa / mở, ô chọn vai; trang trạng thái hệ thống.
- UI-6. Thông báo lỗi bằng tiếng Việt, nói rõ việc người dùng cần làm.

### 5.2. Giao diện phần mềm

- Server ↔ CSDL PostgreSQL 16 qua JDBC; migration bằng Flyway.
- Server ↔ hệ thống tệp: thư mục lưu tài liệu cấu hình bằng biến môi trường.
- Không có giao diện với hệ thống ngoài.

### 5.3. Giao diện truyền thông

- HTTP/1.1 trên TCP, cổng mặc định 8080; HTTPS khi triển khai ngoài LAN.
- REST, dữ liệu JSON UTF-8; tải lên `multipart/form-data`; tải tệp trả luồng nhị phân.
- Xác thực bằng header `Authorization: Bearer <token>`.
- Mỗi thao tác là một cặp request / response; không dùng kết nối giữ lâu (SSE, WebSocket).

---

## 6. Yêu cầu phi chức năng

| Mã | Loại | Yêu cầu | Cách kiểm |
|---|---|---|---|
| NFR-01 | Hiệu năng | Phân vị 95 (p95) thời gian phản hồi < 2 giây cho mọi request trừ tải lên, với 10 client đồng thời trong LAN | JMeter 10 luồng; đối chiếu bằng Wireshark |
| NFR-02 | Bảo mật | Mật khẩu băm BCrypt; token ký HMAC, hết hạn 8 giờ; mọi kiểm quyền ở server; không tiết lộ lý do đăng nhập thất bại; tệp lưu tên ngẫu nhiên, chỉ nhận PDF/DOCX ≤ 20 MB | Kịch bản kiểm thử 1–5, 9 (`DAC-TA-TRIEN-KHAI.md` mục 9) |
| NFR-03 | Toàn vẹn | Lưu tin nhắn, tài liệu, đổi vai trong transaction; ràng buộc khóa ngoại ở CSDL | Kịch bản 10–13 |
| NFR-04 | Khả dụng | Từ lúc mở khung chat tới khi thấy câu trả lời ≤ 3 lần bấm; dùng được trên điện thoại (màn hình ≥ 360 px) | Kiểm thử thủ công |
| NFR-05 | Bảo trì | Mã chia theo 4 module M1–M4; lược đồ quản lý bằng migration; có bộ kiểm thử tích hợp cho 14 kịch bản | Review mã, `mvn test` |
| NFR-06 | Tương thích | Chrome, Edge, Firefox bản hiện hành; Java 21; PostgreSQL 16 | Kiểm thử thủ công |
| NFR-07 | Triển khai | Chạy toàn bộ bằng một lệnh `docker compose up` trong LAN không có Internet | Thử trên máy phòng lab |
| NFR-08 | Tin cậy | Có trang trạng thái hệ thống (UC20); lỗi CSDL không làm server dừng | Kịch bản 14 |
| NFR-09 | Chất lượng so khớp | Gõ không dấu cho cùng kết quả với có dấu; "kỳ" và "kì" như nhau; câu gõ gần nguyên văn câu hỏi soạn sẵn phải được trả lời ngay | Kiểm thử đơn vị `QuestionMatcherTest`, kịch bản UC24 |

---

## 7. Quy tắc nghiệp vụ

| Mã | Quy tắc |
|---|---|
| BR01 | Tài khoản phải đang hoạt động và thuộc khoa CNTT mới được đăng nhập và sử dụng. |
| BR02 | Mỗi request kiểm tra lại quyền từ CSDL; khóa tài khoản hoặc đổi vai có hiệu lực ngay ở request sau. |
| BR03 | Sinh viên chỉ đọc; giáo vụ và quản trị viên mới ghi nội dung; chỉ quản trị viên đổi tài khoản. |
| BR04 | Quản trị viên không tự khóa hay tự hạ vai của chính mình. |
| BR05 | Hội thoại thuộc riêng người tạo; mở nguồn và tệp gốc phải đăng nhập. |
| BR06 | Mỗi câu hỏi soạn sẵn phải gắn tài liệu nguồn và số trang. |
| BR07 | Hệ thống không tự sinh câu trả lời: câu gõ chỉ được trả lời bằng câu soạn sẵn khớp, hoặc do giáo vụ viết. |
| BR08 | Mỗi sinh viên có tối đa 5 câu hỏi đang chờ giáo vụ trả lời. |

### Ma trận quyền

| Chức năng | Sinh viên | Giáo vụ | Quản trị viên |
|---|---|---|---|
| Đăng nhập / đăng xuất, đổi mật khẩu | Có | Có | Có |
| Chọn hoặc gõ câu hỏi, xem hội thoại của mình | Có | Có | Có |
| Gửi câu hỏi cho giáo vụ, xem câu trả lời | Có | Có | Có |
| Đọc tài liệu, mở tệp gốc | Có | Có | Có |
| Quản lý tài liệu, chủ đề, câu hỏi – câu trả lời | — | Có | Có |
| Trả lời câu hỏi sinh viên gửi | — | Có | Có |
| Xem, khóa / mở, phân quyền tài khoản | — | — | Có |
| Kiểm tra trạng thái hệ thống | — | — | Có |
| Tự khóa hoặc tự hạ vai của chính mình | — | — | — |

---

## Phụ lục A. Ma trận truy vết

| Use case | Module | API (`DAC-TA-TRIEN-KHAI.md` mục 6) | Bảng | Quy tắc |
|---|---|---|---|---|
| UC01 Đăng nhập | M1 | `POST /api/auth/login` | users | BR01 |
| UC02 Đăng xuất | M1 | — (client) | — | — |
| UC03 Xem thông tin cá nhân | M1 | `GET /api/auth/me` | users | BR02 |
| UC04 Đổi mật khẩu | M1 | `PUT /api/auth/password` | users | BR02 |
| UC05 Chọn câu hỏi, nhận trả lời | M2 | `GET /api/topics`, `GET /api/topics/{id}/questions`, `POST /api/chat` | topics, questions, documents, conversations, messages | BR05, BR06 |
| UC06 Xem lại hội thoại | M2 | `GET /api/conversations`, `GET /api/conversations/{id}` | conversations, messages, questions | BR05 |
| UC24 Gõ câu hỏi | M2 | `POST /api/chat/typed` | questions, conversations, messages | BR07 |
| UC25 Gửi câu hỏi cho giáo vụ | M2 | `POST /api/inquiries` | inquiries | BR08 |
| UC26 Xem câu trả lời của giáo vụ | M2 | `GET /api/inquiries/mine` | inquiries, documents | BR05 |
| UC08 Xem nguồn trích dẫn | M2 | `GET /api/documents/{id}/file` | documents | BR05 |
| UC07 Tra cứu kho tài liệu | M3 | `GET /api/documents` | documents | — |
| UC10 Tải lên tài liệu | M3 | `POST /api/documents` | documents | BR03 |
| UC11 Sửa thông tin tài liệu | M3 | `PATCH /api/documents/{id}` | documents | BR03 |
| UC12 Gỡ tài liệu | M3 | `DELETE /api/documents/{id}` | documents, questions | BR03, BR06 |
| UC22 Quản lý chủ đề | M3 | `POST/PATCH/DELETE /api/topics` | topics | BR03 |
| UC27 Trả lời câu hỏi của sinh viên | M3 | `GET /api/inquiries`, `POST /api/inquiries/{id}/answer` | inquiries, questions | BR03, BR07 |
| UC23 Quản lý câu hỏi – câu trả lời | M3 | `GET /api/admin/questions`, `POST/PATCH/DELETE /api/questions` | questions | BR03, BR06 |
| UC14 Tra cứu người dùng | M4 | `GET /api/users` | users | BR03 |
| UC15 Khóa / mở tài khoản | M4 | `PATCH /api/users/{id}` | users | BR02, BR04 |
| UC16 Phân quyền | M4 | `PATCH /api/users/{id}` | users | BR02, BR04 |
| UC20 Kiểm tra trạng thái hệ thống | M4 | `GET /api/health` | — | BR03 |
