# Sổ Tay Sinh Viên CNTT

Hệ thống hỏi đáp học vụ cho sinh viên **ngành Công nghệ Thông tin, Trường Đại học Kiến trúc Đà Nẵng**.
Đồ án học phần **Lập trình mạng máy tính** — mô hình client–server.

Sinh viên tra cứu quy định học vụ (đăng ký học phần, đồ án, chuẩn đầu ra, bảo lưu, cảnh báo học vụ,
xét tốt nghiệp) qua **chatbot dạng nút chọn**: chọn chủ đề → chọn câu hỏi → nhận câu trả lời do giáo vụ
soạn sẵn, kèm tài liệu nguồn và số trang. Hệ thống không dùng AI.

## Vai trò

- `USER` — Sinh viên CNTT: chọn câu hỏi, xem nguồn, xem lại hội thoại, tra cứu tài liệu.
- `CONTENT_ADMIN` — Giáo vụ khoa: thêm quản lý tài liệu, chủ đề, câu hỏi – câu trả lời.
- `SYSTEM_ADMIN` — Quản trị viên: thêm quản lý tài khoản, phân quyền, kiểm tra trạng thái hệ thống.

## Công nghệ

Java 21 · Spring Boot 3.3 (Web, Data JPA, Security, Validation) · PostgreSQL 16 · Flyway · JWT ·
Docker Compose.

## Tài liệu

| Tệp | Nội dung |
|---|---|
| [`docs/DAC-TA-TRIEN-KHAI.md`](docs/DAC-TA-TRIEN-KHAI.md) | **Đặc tả chính thức**: use case, quy tắc nghiệp vụ, lược đồ CSDL, API REST, luồng xử lý, hướng dẫn Spring Boot, kịch bản kiểm thử |
| [`docs/slides/SRS-tac-nhan-va-use-case.pptx`](docs/slides/SRS-tac-nhan-va-use-case.pptx) | Slide SRS: tác nhân, đối tượng tham gia, use case, ma trận quyền, input/output, luồng |
| Slide Chương 3 (artifact) | BFD, IFD, DFD, ERD, biểu đồ lớp, CSDL |

## Trạng thái

Mã nguồn bản cũ (Node.js/Next.js, RAG với Gemini) đã được gỡ khỏi nhánh này; vẫn xem lại được trong
lịch sử git. Server Spring Boot sẽ được hiện thực trong thư mục `server-java/` theo mục 8 của đặc tả.
