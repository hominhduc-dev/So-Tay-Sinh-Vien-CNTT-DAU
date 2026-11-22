# M3 · Nội dung — client

**Người phụ trách:** _Họ tên — @github_

**Use case:** UC07 Tra cứu tài liệu, UC10–UC12 Tải lên / sửa / gỡ tài liệu, UC22 Quản lý chủ đề, UC23 Quản lý câu hỏi – câu trả lời

**Action gửi tới server:** `topic.*`, `question.*`, `document.*`

| Thư mục | Tầng | Lớp |
|---|---|---|
| `model/` | Model — record nhận từ JSON | Topic, QuestionButton, Question, Document, DownloadedFile |
| `controller/` | Controller — gửi action qua `SocketClient` | TopicController, QuestionController, DocumentController |
| `view/` | View — màn hình Swing | DocumentsPanel, ContentPanel |

Phía server: [`server/src/main/java/vn/tangthu/content/`](../../../../../../../../server/src/main/java/vn/tangthu/content/).
