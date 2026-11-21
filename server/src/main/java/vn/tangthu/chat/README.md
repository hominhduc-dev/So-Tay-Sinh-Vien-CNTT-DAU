# M2 · Hỏi đáp — server

**Người phụ trách:** _Họ tên — @github_

**Use case:** UC05 Chọn câu hỏi, nhận trả lời, UC06 Xem lại hội thoại, UC08 Xem nguồn trích dẫn

**API:** `POST /api/chat`, `GET /api/conversations`, `GET /api/conversations/{id}`

| Thư mục | Tầng | Lớp |
|---|---|---|
| `model/` | Model — entity JPA | Conversation, Message |
| `repository/` | Model — truy vấn CSDL | ConversationRepository, MessageRepository |
| `service/` | Nghiệp vụ | ChatService |
| `controller/` | Controller — REST API | ChatController |
| `dto/` | Dữ liệu request / response JSON | AskRequest, ChatDtos |

Giao diện (View) của module: [`client/src/modules/chat/`](../../../../../../../client/src/modules/chat/).
