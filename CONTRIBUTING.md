# Hướng dẫn làm việc nhóm trên GitHub

Quy trình chung: **clone → tạo nhánh riêng → commit → push → mở Pull Request (PR) → trưởng nhóm duyệt và merge**.
Không ai push thẳng vào `main`.

## 0. Chuẩn bị (làm một lần)

1. Có tài khoản GitHub và đã **chấp nhận lời mời** vào repo (email mời hoặc `github.com/notifications`).
2. Cài **Git**, **Java 21**, **Maven 3.9**. Kiểm tra:
   ```bash
   git --version && java -version && mvn -v
   ```
3. Khai báo tên và email trùng tài khoản GitHub. Sai email thì commit không được tính cho bạn:
   ```bash
   git config --global user.name  "Họ Tên"
   git config --global user.email "email-dang-ky-github@example.com"
   ```
   Nếu muốn ẩn email: GitHub → **Settings → Emails** → chép địa chỉ dạng `12345678+username@users.noreply.github.com`.

## 1. Clone dự án về máy

```bash
git clone https://github.com/hominhduc-dev/So-Tay-Sinh-Vien-CNTT-DAU.git
cd So-Tay-Sinh-Vien-CNTT-DAU
```

Lần push đầu tiên Git sẽ hỏi đăng nhập:
- **Windows / macOS:** cửa sổ đăng nhập GitHub hiện ra (Git Credential Manager), đăng nhập là xong.
- **Linux:** username là tên GitHub; **password là Personal Access Token**, không phải mật khẩu GitHub.
  Tạo token ở GitHub → **Settings → Developer settings → Personal access tokens → Tokens (classic)**,
  tích quyền `repo`.

## 2. Tạo nhánh mới cho phần việc của mình

Luôn tạo nhánh từ `main` **mới nhất**:

```bash
git checkout main
git pull origin main
git checkout -b feature/m2-chat-lich-su
```

Đặt tên nhánh: `feature/<module>-<việc-cần-làm>`, viết không dấu, nối bằng gạch ngang.

| Module | Tên nhánh ví dụ | Chỉ sửa trong |
|---|---|---|
| M1 · Tài khoản | `feature/m1-account-doi-mat-khau` | `server/.../account/`, `client/.../client/account/` |
| M2 · Hỏi đáp | `feature/m2-chat-lich-su` | `server/.../chat/`, `client/.../client/chat/` |
| M3 · Nội dung | `feature/m3-content-tai-len` | `server/.../content/`, `client/.../client/content/` |
| M4 · Quản trị | `feature/m4-admin-khoa-tai-khoan` | `server/.../admin/`, `client/.../client/admin/` |
| Sửa lỗi | `fix/<module>-<lỗi>` | |
| Tài liệu | `docs/<nội-dung>` | `docs/`, các `README.md` |

Cần sửa `common/` (tầng mạng, cấu hình) thì báo trưởng nhóm trước, vì phần này ảnh hưởng mọi người.

## 3. Làm việc, kiểm tra, commit

```bash
# kiểm tra code biên dịch được trước khi commit
cd server && mvn -q compile && cd ..
cd client && mvn -q package && cd ..     # build xong chạy thử: java -jar client/target/tangthu-client.jar

git status                               # xem đã sửa những tệp nào
git add server/src/main/java/vn/tangthu/chat client/src/main/java/vn/tangthu/client/chat
git commit -m "M2: hiện số câu hỏi trong lịch sử hội thoại"
```

- Commit nhỏ, mỗi commit một ý. Ghi message bằng tiếng Việt, mở đầu bằng mã module (`M1:` … `M4:`).
- **Không commit:** thư mục `target/`, tệp `.env`, mật khẩu, tệp của IDE (`.idea/`, `.vscode/`). `.gitignore` đã chặn sẵn.
- Server test cần PostgreSQL có database `tangthu_test` (xem `server/README.md`): `cd server && mvn test`.

## 4. Đẩy nhánh lên GitHub

```bash
git push -u origin feature/m2-chat-lich-su      # lần đầu
git push                                        # các lần sau
```

## 5. Mở Pull Request

1. Vào trang repo trên GitHub. Sẽ có thanh vàng **"feature/… had recent pushes"** → bấm **Compare & pull request**.
   (Hoặc vào tab **Pull requests → New pull request**, chọn `base: main` ← `compare: nhánh của bạn`.)
2. Kiểm tra **base là `main`**, **compare là nhánh của bạn**.
3. Tiêu đề: `M2: hiện số câu hỏi trong lịch sử hội thoại`. Mô tả ngắn gọn:
   ```
   ## Làm gì
   - ...
   ## Đã kiểm tra
   - [ ] mvn compile / mvn package chạy được
   - [ ] chạy thử client với tài khoản SV001 / GV001 / QTV001
   ```
4. Ô **Reviewers** bên phải: chọn trưởng nhóm → **Create pull request**.
5. Có góp ý thì sửa tiếp trên **cùng nhánh**, rồi `git add` → `git commit` → `git push`. PR tự cập nhật.

## 6. Sau khi PR được merge

```bash
git checkout main
git pull origin main
git branch -d feature/m2-chat-lich-su           # xóa nhánh cũ trên máy
git checkout -b feature/m2-viec-tiep-theo       # việc mới = nhánh mới
```

## Cập nhật nhánh khi `main` đã có code mới

Làm việc này trước khi mở PR, hoặc khi GitHub báo **"This branch has conflicts"**:

```bash
git checkout feature/m2-chat-lich-su
git pull origin main          # kéo main mới vào nhánh của mình
# nếu có xung đột: mở tệp bị báo, giữ phần đúng, xóa các dòng <<<<<<< ======= >>>>>>>
git add <tệp đã sửa>
git commit                    # hoàn tất merge
git push
```

## Lỗi hay gặp

| Thông báo | Cách xử lý |
|---|---|
| `Permission denied` / `403` khi push | Chưa chấp nhận lời mời vào repo, hoặc nhập mật khẩu thay vì token |
| `! [rejected] … (fetch first)` | Nhánh trên GitHub có commit mới: `git pull` rồi `git push` lại |
| Lỡ commit vào `main` trên máy | `git checkout -b feature/ten-nhanh` (mang commit sang nhánh mới), rồi `git checkout main && git reset --hard origin/main` |
| Commit không hiện avatar của mình | `user.email` không trùng email trên GitHub — sửa ở bước 0.3 cho các commit sau |
