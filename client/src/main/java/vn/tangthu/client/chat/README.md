# M2 · Hỏi đáp — client

**Người phụ trách:** _Họ tên — @github_

**Use case:** UC05 Chọn câu hỏi, nhận trả lời, UC06 Xem lại hội thoại, UC08 Mở tài liệu nguồn

**Action gửi tới server:** `chat.ask`, `conversation.list`, `conversation.get` (+ `topic.*`, `document.download` của M3)

| Thư mục | Tầng | Lớp |
|---|---|---|
| `model/` | Model — record nhận từ JSON | Message, ConversationSummary, ConversationDetail, ChatResponse |
| `controller/` | Controller — gửi action qua `SocketClient` | ChatController |
| `view/` | View — màn hình Swing | ChatPanel |

Phía server: [`server/src/main/java/vn/tangthu/chat/`](../../../../../../../../server/src/main/java/vn/tangthu/chat/).

Bấm nguồn PDF trong câu trả lời: tài liệu hiện ở khung bên cạnh hội thoại (`SourcePanel`), mở đúng trang được trích dẫn, lật được sang trang khác. Tệp DOCX mở bằng ứng dụng ngoài.
