# Server Spring Boot — Sổ Tay Sinh Viên CNTT

Hiện thực theo [`docs/DAC-TA-TRIEN-KHAI.md`](../docs/DAC-TA-TRIEN-KHAI.md) và [`docs/SRS.md`](../docs/SRS.md).

## Cấu trúc

```
src/main/java/vn/tangthu/
├── common/    cấu hình, JWT, bảo mật, xử lý lỗi, lưu tệp
├── account/   M1 · Tài khoản      — UC01–UC04
├── chat/      M2 · Hỏi đáp        — UC05, UC06
├── content/   M3 · Nội dung       — UC07, UC08, UC10–UC12, UC22, UC23
└── admin/     M4 · Quản trị       — UC14–UC16, UC20
src/main/resources/db/
├── migration/ V1__init.sql — lược đồ 6 bảng
└── demo/      V2__demo_data.sql — 3 tài khoản demo, 6 chủ đề
```

## Chạy bằng Docker Compose (từ thư mục gốc repo)

```bash
cp .env.example .env      # rồi sửa DB_PASSWORD, JWT_SECRET
docker compose up --build
```

API ở `http://localhost:8080/api`.

## Chạy trực tiếp (cần Java 21, Maven, PostgreSQL 16)

```bash
# CSDL: tạo user/database tangthu (mật khẩu tangthu) hoặc đặt DATABASE_URL, DB_USER, DB_PASSWORD
export JWT_SECRET="chuoi-ngau-nhien-dai-it-nhat-32-ky-tu"
mvn spring-boot:run
```

| Biến môi trường | Mặc định | Ý nghĩa |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/tangthu` | CSDL |
| `DB_USER`, `DB_PASSWORD` | `tangthu` / `tangthu` | Tài khoản CSDL |
| `JWT_SECRET` | — (bắt buộc) | Khóa ký token, ≥ 32 ký tự |
| `JWT_TTL` | `PT8H` | Thời hạn token |
| `STORAGE_DIR` | `./storage` | Thư mục lưu tệp tài liệu |
| `CORS_ORIGINS` | `http://localhost:3000` | Web client được phép gọi |
| `FLYWAY_LOCATIONS` | `classpath:db/migration,classpath:db/demo` | Bỏ `db/demo` khi triển khai thật |
| `PORT` | `8080` | Cổng HTTP |

## Tài khoản demo

Mật khẩu chung `Matkhau@123` — chỉ dùng thử, đổi ngay khi triển khai.

| Đăng nhập | Vai |
|---|---|
| `SV001` / `sinhvien@tangthu.local` | Sinh viên (`USER`) |
| `GV001` / `giaovu@tangthu.local` | Giáo vụ (`CONTENT_ADMIN`) |
| `QTV001` / `quantri@tangthu.local` | Quản trị viên (`SYSTEM_ADMIN`) |

Có sẵn 6 chủ đề nhưng chưa có câu hỏi: giáo vụ tải tài liệu (UC10) rồi soạn câu hỏi (UC23).

## Thử nhanh bằng curl

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"login":"GV001","password":"Matkhau@123"}' | jq -r .token)
curl -s localhost:8080/api/topics -H "Authorization: Bearer $TOKEN"
curl -s -X POST localhost:8080/api/documents -H "Authorization: Bearer $TOKEN" \
  -F "file=@quy-che.pdf;type=application/pdf" -F "title=Quy chế đào tạo"
```

## Kiểm thử

Kiểm thử tích hợp (`ApiIntegrationTest`, 15 ca theo mục 9 của đặc tả) chạy trên PostgreSQL thật:

```bash
# CSDL riêng cho test, sẽ bị xóa dữ liệu mỗi ca
createdb -O tangthu tangthu_test     # hoặc đặt TEST_DATABASE_URL, TEST_DB_USER, TEST_DB_PASSWORD
mvn test
```
