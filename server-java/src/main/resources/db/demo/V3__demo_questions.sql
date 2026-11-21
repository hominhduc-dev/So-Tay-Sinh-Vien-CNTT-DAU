-- Tài liệu mẫu được đóng gói cùng ứng dụng. Dùng lại bản đã tải lên nếu trùng SHA-256.
INSERT INTO documents (title, file_url, file_hash, mime_type, page_count, uploaded_by)
SELECT '[DEMO] Sổ tay học vụ mẫu', 'demo/so-tay-hoc-vu-demo.pdf',
       '74fe6023afaf84e02d57356e1fdf52be159feca24a7bb7bd4161903c353dda72', 'application/pdf', 6, id
FROM users WHERE code = 'GV001'
ON CONFLICT (file_hash) DO NOTHING;

INSERT INTO questions (topic_id, content, answer, document_id, page, sort_order)
SELECT t.id, seed.content, seed.answer, d.id, seed.page, seed.sort_order
FROM (VALUES
    ('Đăng ký học phần', 'Cần chuẩn bị gì trước khi đăng ký học phần?', 'Kiểm tra kế hoạch học tập, học phần tiên quyết và lịch dự kiến. Chuẩn bị một lớp dự phòng cho mỗi học phần có ít chỗ.', 1, 1),
    ('Đăng ký học phần', 'Làm gì khi hai lớp bị trùng lịch?', 'So sánh lịch học và lịch thực hành, chọn lớp khác hoặc đổi học phần tự chọn. Kiểm tra lại thời khóa biểu sau khi lưu.', 1, 2),
    ('Đồ án tốt nghiệp', 'Làm sao để chuẩn bị đề cương đồ án?', 'Tóm tắt bài toán, mục tiêu, phạm vi, sản phẩm dự kiến và kế hoạch theo tuần. Trao đổi với giảng viên hướng dẫn trước khi chốt.', 2, 1),
    ('Đồ án tốt nghiệp', 'Nên chuẩn bị gì cho buổi bảo vệ đồ án?', 'Chuẩn bị slide, bản demo và phương án trình diễn dự phòng. Tập trình bày bài toán, kết quả và các giới hạn của giải pháp.', 2, 2),
    ('Chuẩn đầu ra', 'Xem chuẩn đầu ra của khóa mình ở đâu?', 'Tra cứu chương trình đào tạo áp dụng cho khóa tuyển sinh của bạn và đối chiếu mục chuẩn đầu ra. Hỏi giáo vụ nếu có nhiều phiên bản.', 3, 1),
    ('Chuẩn đầu ra', 'Nên lưu minh chứng chuẩn đầu ra như thế nào?', 'Lưu chứng chỉ và các minh chứng theo từng nhóm yêu cầu. Ghi lại ngày cấp, thời hạn và trạng thái đã nộp để dễ đối chiếu.', 3, 2),
    ('Bảo lưu – nghỉ học', 'Cần hỏi ai trước khi xin bảo lưu?', 'Trao đổi với cố vấn học tập hoặc giáo vụ để xác định mẫu đơn, giấy tờ và thời gian tiếp nhận phù hợp với trường hợp của bạn.', 4, 1),
    ('Bảo lưu – nghỉ học', 'Làm gì khi muốn học lại sau thời gian bảo lưu?', 'Liên hệ giáo vụ để xác nhận quy trình tiếp nhận, lịch đăng ký và kế hoạch học tập trước khi quay lại.', 4, 2),
    ('Cảnh báo học vụ', 'Nên kiểm tra gì khi nhận cảnh báo học vụ?', 'Đối chiếu kết quả từng học phần và số tín chỉ tích lũy. Ghi lại những điểm cần làm rõ để trao đổi với cố vấn học tập.', 5, 1),
    ('Cảnh báo học vụ', 'Lập kế hoạch cải thiện kết quả học tập thế nào?', 'Ưu tiên học phần nền tảng hoặc tiên quyết, chọn khối lượng phù hợp và thống nhất các mốc theo dõi với cố vấn học tập.', 5, 2),
    ('Xét tốt nghiệp', 'Trước đợt xét tốt nghiệp cần rà soát những gì?', 'Đối chiếu bảng điểm, tín chỉ, minh chứng chuẩn đầu ra và thông tin cá nhân với hướng dẫn đang áp dụng cho khóa của bạn.', 6, 1),
    ('Xét tốt nghiệp', 'Làm gì nếu thông tin hồ sơ tốt nghiệp bị sai?', 'Ghi rõ mục cần sửa và chuẩn bị giấy tờ đối chiếu. Liên hệ bộ phận tiếp nhận hồ sơ theo thông báo của đợt xét.', 6, 2)
) AS seed(topic_name, content, answer, page, sort_order)
JOIN topics t ON t.name = seed.topic_name
JOIN documents d ON d.file_hash = '74fe6023afaf84e02d57356e1fdf52be159feca24a7bb7bd4161903c353dda72'
WHERE NOT EXISTS (
    SELECT 1 FROM questions q
    WHERE q.topic_id = t.id AND q.document_id = d.id AND q.content = seed.content
);
