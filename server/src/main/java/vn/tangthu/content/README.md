# M3 · Nội dung — server

**Người phụ trách:** _Họ tên — @github_

**Use case:** UC07 Tra cứu tài liệu, UC10–UC12 Tải lên / sửa / gỡ tài liệu, UC22 Quản lý chủ đề, UC23 Quản lý câu hỏi – câu trả lời

**API:** `/api/documents/**`, `/api/topics/**`, `/api/questions/**`, `/api/admin/questions`

| Thư mục | Tầng | Lớp |
|---|---|---|
| `model/` | Model — entity JPA | Document, Question, Topic |
| `repository/` | Model — truy vấn CSDL | DocumentRepository, QuestionRepository, TopicRepository |
| `service/` | Nghiệp vụ | DocumentService, QuestionService, TopicService |
| `controller/` | Controller — REST API | DocumentController, QuestionController, TopicController |
| `dto/` | Dữ liệu request / response JSON | ContentDtos, CreateTopic, QuestionRequest, RenameRequest, UpdateTopic |

Giao diện (View) của module: [`client/src/modules/content/`](../../../../../../../client/src/modules/content/).
