-- Dữ liệu demo: một tài khoản mỗi vai và sáu chủ đề.
-- Mật khẩu chung: Matkhau@123 — ĐỔI NGAY khi triển khai thật, hoặc bỏ thư mục db/demo
-- khỏi FLYWAY_LOCATIONS (chỉ để classpath:db/migration).
-- Câu hỏi cần tài liệu nguồn nên giáo vụ nhập qua giao diện (UC10 → UC23).

INSERT INTO users (code, email, password_hash, full_name, role, department) VALUES
  ('SV001',  'sinhvien@tangthu.local', '$2a$10$vZufdqRgtRC4i1RHC3R9kuVW4tEWh0L6UUOAEYJqGrett8v.2UgYy', 'Sinh viên Demo',     'USER',          'CNTT'),
  ('GV001',  'giaovu@tangthu.local',   '$2a$10$vZufdqRgtRC4i1RHC3R9kuVW4tEWh0L6UUOAEYJqGrett8v.2UgYy', 'Giáo vụ Demo',       'CONTENT_ADMIN', 'CNTT'),
  ('QTV001', 'quantri@tangthu.local',  '$2a$10$vZufdqRgtRC4i1RHC3R9kuVW4tEWh0L6UUOAEYJqGrett8v.2UgYy', 'Quản trị viên Demo', 'SYSTEM_ADMIN',  'CNTT');

INSERT INTO topics (name, sort_order) VALUES
  ('Đăng ký học phần', 1),
  ('Đồ án tốt nghiệp', 2),
  ('Chuẩn đầu ra', 3),
  ('Bảo lưu – nghỉ học', 4),
  ('Cảnh báo học vụ', 5),
  ('Xét tốt nghiệp', 6);
