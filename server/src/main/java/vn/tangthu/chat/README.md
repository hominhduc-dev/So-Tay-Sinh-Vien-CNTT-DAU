# M2 · Hỏi đáp — server

**Người phụ trách:** _Họ tên — @github_

**Use case:** UC05 Chọn câu hỏi, nhận trả lời, UC06 Xem lại hội thoại, UC08 Xem nguồn trích dẫn

**Action socket:** `chat.ask`, `conversation.list`, `conversation.get`

| Thư mục | Tầng | Lớp |
|---|---|---|
| `model/` | Model — entity JPA | Conversation, Message |
| `repository/` | Model — truy vấn CSDL | ConversationRepository, MessageRepository |
| `service/` | Nghiệp vụ | ChatService |
| `controller/` | Controller — đăng ký action socket | ChatSocketController |
| `dto/` | Dữ liệu `data` gửi / nhận (JSON) | AskRequest, ChatDtos |

Giao diện (View) của module: [`client/src/main/java/vn/tangthu/client/chat/`](../../../../../../../client/src/main/java/vn/tangthu/client/chat/).
