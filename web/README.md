# Web client — Sổ Tay Sinh Viên CNTT

Next.js 16 · React 19 · Tailwind 4. Gọi API Spring Boot ở [`../server-java`](../server-java/README.md)
qua đường dẫn tương đối `/api` — `next.config.ts` chuyển tiếp sang server, nên trình duyệt ở máy nào
trong LAN cũng gọi đúng mà không vướng CORS.

| Trang | Vai | Use case |
|---|---|---|
| `/login` | mọi vai | UC01 |
| `/chat` | mọi vai | UC05 chọn chủ đề → câu hỏi, UC24 gõ câu hỏi, UC25 gửi giáo vụ, UC06 lịch sử, UC08 mở nguồn đúng trang |
| `/inquiries` | mọi vai | UC26 câu hỏi đã gửi và câu trả lời của giáo vụ |
| `/documents` | mọi vai đọc; giáo vụ tải lên, đổi tên, gỡ | UC07, UC08, UC10–UC12 |
| `/content` | giáo vụ, quản trị viên | UC22 chủ đề, UC23 câu hỏi – câu trả lời |
| `/inbox` | giáo vụ, quản trị viên | UC27 trả lời câu hỏi sinh viên gửi, lưu thành câu hỏi có sẵn |
| `/admin/users`, `/admin/health`, `/admin/permissions` | quản trị viên | UC14–UC16, UC20 |

## Chạy khi phát triển

```bash
# server-java đang chạy ở :8080
npm install
npm run dev            # http://localhost:3000
```

Đổi địa chỉ server bằng `API_INTERNAL_URL` (xem `.env.local.example`).

## Kiểm tra

```bash
npm run typecheck && npm run lint && npm run build
```

Font đóng gói qua `@fontsource`, không tải từ Google — build và chạy được trong LAN không có Internet.
