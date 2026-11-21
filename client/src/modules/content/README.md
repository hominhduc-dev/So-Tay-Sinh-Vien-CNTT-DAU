# M3 · Nội dung — client

**Người phụ trách:** _Họ tên — @github_

**Trang:** `/documents`, `/content` · **Use case:** UC07 Tra cứu tài liệu, UC10–UC12 Tải lên / sửa / gỡ tài liệu, UC22 Quản lý chủ đề, UC23 Quản lý câu hỏi – câu trả lời

| Thư mục / tệp | Vai trò | Nội dung |
|---|---|---|
| `views/` | View — giao diện | ContentManager, DocumentManager, DocumentTable, QuestionForm, UploadDropzone |
| `api/documents.ts`, `api/topics-questions.ts` | Gọi REST API của module cùng tên ở server | |

Phía server: [`server/src/main/java/vn/tangthu/content/`](../../../../server/src/main/java/vn/tangthu/content/).
