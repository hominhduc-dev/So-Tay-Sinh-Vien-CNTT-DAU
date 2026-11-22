# Server — Sổ Tay Sinh Viên CNTT

Máy chủ **TCP socket** viết bằng Java 21 + Spring Boot 3.3 (chế độ không web). Client desktop kết nối
thẳng vào cổng **9090** bằng `java.net.Socket`; giao thức ở
[`docs/DAC-TA-TRIEN-KHAI.md` mục 6](../docs/DAC-TA-TRIEN-KHAI.md#6-giao-thức-tcp-socket).

## Cấu trúc — module trước, tầng MVC sau

```
src/main/java/vn/tangthu/
├── TangThuApplication.java
├── common/
│   ├── socket/  tầng mạng: SocketServer (ServerSocket + pool luồng), ClientConnection (một client),
│   │            FrameCodec (4 byte độ dài + JSON), SocketDispatcher (action → controller, kiểm quyền),
│   │            SocketController, SocketRequest
│   └── config/ security/ exception/ dto/ util/
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
| **C**ontroller | `controller/` | Đăng ký action socket (`routes.secured("chat.ask", Role.USER, …)`), đọc `data`, gọi service |
| (nghiệp vụ) | `service/` | Quy tắc nghiệp vụ BR01–BR06, transaction |
| **V**iew | `dto/` → JSON trong khung phản hồi; giao diện ở `client/` | Dữ liệu trả về client; client Swing hiển thị |

## Luồng một yêu cầu

```
ServerSocket.accept() ─▶ ClientConnection (luồng riêng) ─▶ FrameCodec.read ─▶ SocketDispatcher.handle
   ─▶ kiểm token + đọc lại user từ CSDL (BR01, BR02) ─▶ kiểm vai ─▶ <Module>SocketController ─▶ Service
   ─▶ { id, ok, data | error } ─▶ FrameCodec.write ─▶ chờ khung tiếp theo trên cùng socket
```

## Chạy bằng Docker Compose (từ thư mục gốc repo)

```bash
cp .env.example .env      # rồi sửa DB_PASSWORD, JWT_SECRET
docker compose up --build
```

## Chạy trực tiếp (cần Java 21, Maven, PostgreSQL 16)

```bash
# CSDL: tạo user/database tangthu (mật khẩu tangthu) hoặc đặt DATABASE_URL, DB_USER, DB_PASSWORD
export JWT_SECRET="chuoi-ngau-nhien-dai-it-nhat-32-ky-tu"
mvn spring-boot:run       # log: "Máy chủ TCP socket lắng nghe ở cổng 9090"
```

| Biến môi trường | Mặc định | Ý nghĩa |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/tangthu` | CSDL |
| `DB_USER`, `DB_PASSWORD` | `tangthu` / `tangthu` | Tài khoản CSDL |
| `JWT_SECRET` | — (bắt buộc) | Khóa ký token, ≥ 32 ký tự |
| `JWT_TTL` | `PT8H` | Thời hạn token |
| `SOCKET_PORT` | `9090` | Cổng TCP lắng nghe |
| `SOCKET_THREADS` | `64` | Số client phục vụ đồng thời |
| `SOCKET_IDLE_TIMEOUT` | `PT30M` | Đóng kết nối im lặng quá lâu |
| `STORAGE_DIR` | `./storage` | Thư mục lưu tệp tài liệu |
| `FLYWAY_LOCATIONS` | `classpath:db/migration,classpath:db/demo` | Bỏ `db/demo` khi triển khai thật |

Máy chủ trong LAN: mở cổng 9090 trên tường lửa để máy khác kết nối.

## Tài khoản demo

Mật khẩu chung `Matkhau@123` — chỉ dùng thử, đổi ngay khi triển khai.

| Đăng nhập | Vai |
|---|---|
| `SV001` / `sinhvien@tangthu.local` | Sinh viên (`USER`) |
| `GV001` / `giaovu@tangthu.local` | Giáo vụ (`CONTENT_ADMIN`) |
| `QTV001` / `quantri@tangthu.local` | Quản trị viên (`SYSTEM_ADMIN`) |

## Thử nhanh không cần client

Gửi một khung bằng Python (4 byte độ dài + JSON):

```bash
python3 - <<'PY'
import json, socket, struct
s = socket.create_connection(("localhost", 9090))
def call(msg):
    b = json.dumps(msg).encode()
    s.sendall(struct.pack(">I", len(b)) + b)
    n = struct.unpack(">I", s.recv(4, socket.MSG_WAITALL))[0]
    return json.loads(s.recv(n, socket.MSG_WAITALL))
r = call({"id": 1, "action": "auth.login", "data": {"login": "SV001", "password": "Matkhau@123"}})
print(call({"id": 2, "action": "topic.list", "token": r["data"]["token"]}))
PY
```

## Kiểm thử

`SocketIntegrationTest` — 19 ca: 4 ca giao thức (nhiều yêu cầu trên một kết nối, khung hỏng, khung quá lớn,
10 client đồng thời) và 15 ca nghiệp vụ theo mục 9 của đặc tả. Server mở cổng ngẫu nhiên, test nối bằng
socket thật, CSDL PostgreSQL thật:

```bash
createdb -O tangthu tangthu_test     # hoặc đặt TEST_DATABASE_URL, TEST_DB_USER, TEST_DB_PASSWORD
mvn test
```
