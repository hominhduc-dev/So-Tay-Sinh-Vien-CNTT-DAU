# M3 · Nội dung — server

**Người phụ trách:** _Họ tên — @github_

**Use case:** UC07 Tra cứu tài liệu, UC10–UC12 Tải lên / sửa / gỡ tài liệu, UC22 Quản lý chủ đề, UC23 Quản lý câu hỏi – câu trả lời

**Action socket:** `topic.*`, `question.*`, `document.*` (list, get, download, upload, rename, delete)

| Thư mục | Tầng | Lớp |
|---|---|---|
| `model/` | Model — entity JPA | Document, Question, Topic |
| `repository/` | Model — truy vấn CSDL | DocumentRepository, QuestionRepository, TopicRepository |
| `service/` | Nghiệp vụ | DocumentService, QuestionService, TopicService |
| `controller/` | Controller — đăng ký action socket | DocumentSocketController, QuestionSocketController, TopicSocketController |
| `dto/` | Dữ liệu `data` gửi / nhận (JSON) | ContentDtos, CreateTopic, QuestionRequest, RenameRequest, UpdateTopic |

Giao diện (View) của module: [`client/src/main/java/vn/tangthu/client/content/`](../../../../../../../client/src/main/java/vn/tangthu/client/content/).
