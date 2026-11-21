# Server Spring Boot — Sổ Tay Sinh Viên CNTT

Hiện thực theo [`docs/DAC-TA-TRIEN-KHAI.md`](../docs/DAC-TA-TRIEN-KHAI.md) và [`docs/SRS.md`](../docs/SRS.md).

## Cấu trúc — module trước, tầng MVC sau

```
src/main/java/vn/tangthu/
├── TangThuApplication.java
├── common/      dùng chung: config/ security/ exception/ dto/ util/
├── account/     M1 · Tài khoản   UC01–UC04          model/ repository/ service/ controller/ dto/
├── chat/        M2 · Hỏi đáp     UC05, UC06         model/ repository/ service/ controller/ dto/
├── content/     M3 · Nội dung    UC07, UC08, UC10–UC12, UC22, UC23
│                                                    model/ repository/ service/ controller/ dto/
└── admin/       M4 · Quản trị    UC14–UC16, UC20    service/ controller/ dto/  (dùng User của M1)
src/main/resources/db/
├── migration/   V1__init.sql — lược đồ
└── demo/        dữ liệu demo: tài khoản, chủ đề, câu hỏi, tệp PDF mẫu
```

| Tầng MVC | Thư mục | Vai trò |
|---|---|---|
| **M**odel | `model/`, `repository/` | Entity JPA ánh xạ bảng; truy vấn CSDL |
| **C**ontroller | `controller/` | Nhận request REST, kiểm tra quyền (`@PreAuthorize`), gọi service, trả JSON |
| (nghiệp vụ) | `service/` | Quy tắc nghiệp vụ BR01–BR06, transaction |
| **V**iew | `dto/` → JSON; giao diện ở `client/` | Dữ liệu trả về client; client hiển thị |

Mỗi module có file `README.md` riêng ghi use case và người phụ trách.

## Chạy bằng Docker Compose (từ thư mục gốc repo)

```bash
cp .env.example .env      # rồi sửa DB_PASSWORD, JWT_SECRET
docker compose up --build
```

Web ở `http://localhost:3000`, API ở `http://localhost:8080/api`.

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
| `CORS_ORIGINS` | `*` | Origin được phép gọi API (mẫu, cách nhau bằng dấu phẩy) |
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
