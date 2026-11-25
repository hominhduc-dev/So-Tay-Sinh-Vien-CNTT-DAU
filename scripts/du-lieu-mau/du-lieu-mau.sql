-- Dữ liệu mẫu bổ sung cho môi trường chạy thử (KHÔNG dùng khi triển khai thật).
-- Chạy SAU TaoTaiLieuMau.java: câu hỏi gắn nguồn vào 3 tài liệu [MẪU] mà chương trình đó tải lên.
-- Chạy lại nhiều lần không sinh bản trùng. Mật khẩu mọi tài khoản mới: Matkhau@123

BEGIN;

-- 1. Tài khoản: 12 sinh viên (SV013 bị khóa) và 1 giáo vụ -----------------------------------------
INSERT INTO users (code, email, password_hash, full_name, role, department, is_active, created_at)
SELECT seed.code, seed.email, '$2a$10$vZufdqRgtRC4i1RHC3R9kuVW4tEWh0L6UUOAEYJqGrett8v.2UgYy',
       seed.full_name, seed.role, 'CNTT', seed.active, now() - make_interval(days => seed.days)
FROM (VALUES
    ('SV002', 'sv002@tangthu.local', 'Nguyễn Văn An',       'USER',          true,  95),
    ('SV003', 'sv003@tangthu.local', 'Trần Thị Bình',       'USER',          true,  93),
    ('SV004', 'sv004@tangthu.local', 'Lê Hoàng Cường',      'USER',          true,  90),
    ('SV005', 'sv005@tangthu.local', 'Phạm Thu Dung',       'USER',          true,  88),
    ('SV006', 'sv006@tangthu.local', 'Hoàng Minh Đạt',      'USER',          true,  80),
    ('SV007', 'sv007@tangthu.local', 'Võ Thị Hà',           'USER',          true,  75),
    ('SV008', 'sv008@tangthu.local', 'Đặng Quốc Huy',       'USER',          true,  60),
    ('SV009', 'sv009@tangthu.local', 'Bùi Ngọc Lan',        'USER',          true,  45),
    ('SV010', 'sv010@tangthu.local', 'Đỗ Thanh Long',       'USER',          true,  40),
    ('SV011', 'sv011@tangthu.local', 'Ngô Thị Mai',         'USER',          true,  30),
    ('SV012', 'sv012@tangthu.local', 'Huỳnh Gia Phúc',      'USER',          true,  20),
    ('SV013', 'sv013@tangthu.local', 'Phan Thị Quỳnh',      'USER',          false, 70),
    ('GV002', 'gv002@tangthu.local', 'Lê Thị Thu Hương',    'CONTENT_ADMIN', true, 100)
) AS seed(code, email, full_name, role, active, days)
ON CONFLICT DO NOTHING;

-- 2. Chủ đề mới (6 chủ đề demo cũ giữ thứ tự 1–6) -----------------------------------------------
INSERT INTO topics (name, sort_order) VALUES
    ('Học phí – miễn giảm', 7),
    ('Học bổng',            8),
    ('Thực tập',            9),
    ('Thi – kiểm tra',      10)
ON CONFLICT (name) DO NOTHING;

-- 3. Câu hỏi – câu trả lời, nguồn là đúng trang trong tài liệu [MẪU] -----------------------------
INSERT INTO questions (topic_id, content, answer, document_id, page, sort_order, is_active)
SELECT t.id, seed.content, seed.answer, d.id, seed.page, seed.sort_order, seed.active
FROM (VALUES
    -- Quy chế đào tạo theo tín chỉ
    ('Đăng ký học phần', '[MẪU] Quy chế đào tạo theo tín chỉ', 2, 3, true,
     'Mỗi học kỳ được đăng ký tối đa bao nhiêu tín chỉ?',
     'Học kỳ chính được đăng ký từ 14 đến 25 tín chỉ; học kỳ hè tối đa 12 tín chỉ. Nếu đang bị cảnh báo học vụ thì học kỳ chính kế tiếp chỉ được đăng ký tối đa 14 tín chỉ.'),
    ('Đăng ký học phần', '[MẪU] Quy chế đào tạo theo tín chỉ', 3, 4, true,
     'Có được rút học phần sau khi đã đăng ký không?',
     'Được, nhưng chỉ trong 2 tuần đầu của học kỳ. Học phần đã rút ghi ký hiệu R và không tính vào điểm trung bình.'),
    ('Thi – kiểm tra', '[MẪU] Quy chế đào tạo theo tín chỉ', 4, 1, true,
     'Điểm học phần được quy đổi sang điểm chữ như thế nào?',
     'Điểm thang 10 quy đổi: A từ 8,5 đến 10; B từ 7,0 đến 8,4; C từ 5,5 đến 6,9; D từ 4,0 đến 5,4; F dưới 4,0. Khi tính điểm trung bình, A = 4, B = 3, C = 2, D = 1, F = 0.'),
    ('Thi – kiểm tra', '[MẪU] Quy chế đào tạo theo tín chỉ', 5, 2, true,
     'Vắng thi cuối kỳ có lý do thì phải làm gì?',
     'Nộp đơn kèm minh chứng (giấy khám bệnh, xác nhận…) cho khoa trong 3 ngày làm việc sau buổi thi để được dự thi bù. Vắng không có lý do chính đáng sẽ nhận điểm 0.'),
    ('Thi – kiểm tra', '[MẪU] Quy chế đào tạo theo tín chỉ', 5, 3, true,
     'Muốn phúc khảo bài thi thì nộp đơn khi nào?',
     'Nộp đơn phúc khảo trong 5 ngày làm việc kể từ ngày công bố điểm. Kết quả được thông báo trong 10 ngày làm việc.'),
    ('Thi – kiểm tra', '[MẪU] Quy chế đào tạo theo tín chỉ', 3, 4, true,
     'Bị điểm F thì có phải học lại không?',
     'Học phần bắt buộc bị điểm F phải đăng ký học lại. Nếu là học phần tự chọn, bạn có thể học lại hoặc chọn học phần tự chọn khác cùng nhóm.'),
    ('Cảnh báo học vụ', '[MẪU] Quy chế đào tạo theo tín chỉ', 6, 3, true,
     'Khi nào sinh viên bị cảnh báo học vụ?',
     'Khi điểm trung bình học kỳ dưới 0,8 ở học kỳ đầu hoặc dưới 1,0 ở các học kỳ sau, hoặc khi tổng số tín chỉ bị điểm F vượt quá 24. Bị cảnh báo hai học kỳ liên tiếp có thể bị xem xét buộc thôi học.'),
    ('Cảnh báo học vụ', '[MẪU] Quy chế đào tạo theo tín chỉ', 2, 4, true,
     'Bị cảnh báo học vụ thì được đăng ký bao nhiêu tín chỉ?',
     'Tối đa 14 tín chỉ trong học kỳ chính kế tiếp. Nên ưu tiên học lại các học phần bị điểm F.'),
    ('Bảo lưu – nghỉ học', '[MẪU] Quy chế đào tạo theo tín chỉ', 7, 3, true,
     'Thời gian nghỉ học tạm thời tối đa là bao lâu?',
     'Nghỉ vì lý do cá nhân tổng cộng không quá 2 năm và không vượt thời gian tối đa của khóa học. Đơn nộp cho khoa trước khi học kỳ bắt đầu.'),
    ('Xét tốt nghiệp', '[MẪU] Quy chế đào tạo theo tín chỉ', 8, 3, true,
     'Điều kiện để được công nhận tốt nghiệp là gì?',
     'Tích lũy đủ tín chỉ của chương trình, điểm trung bình tích lũy từ 2,0 (thang 4), đạt chuẩn đầu ra ngoại ngữ và tin học, có chứng chỉ Giáo dục quốc phòng – an ninh và hoàn thành Giáo dục thể chất.'),
    ('Xét tốt nghiệp', '[MẪU] Quy chế đào tạo theo tín chỉ', 8, 4, true,
     'Hạng tốt nghiệp được xếp theo điểm trung bình thế nào?',
     'Xuất sắc từ 3,60 đến 4,00; Giỏi từ 3,20 đến 3,59; Khá từ 2,50 đến 3,19; Trung bình từ 2,00 đến 2,49.'),
    ('Chuẩn đầu ra', '[MẪU] Quy chế đào tạo theo tín chỉ', 8, 3, true,
     'Chuẩn đầu ra ngoại ngữ có bắt buộc để tốt nghiệp không?',
     'Có. Đạt chuẩn đầu ra ngoại ngữ là điều kiện bắt buộc để được xét tốt nghiệp; nộp minh chứng cho khoa trước thời hạn của mỗi đợt xét.'),
    -- Học phí, miễn giảm và học bổng
    ('Học phí – miễn giảm', '[MẪU] Hướng dẫn học phí, miễn giảm và học bổng', 1, 1, true,
     'Học phí mỗi học kỳ được tính như thế nào?',
     'Bằng tổng số tín chỉ đăng ký nhân với mức thu một tín chỉ của năm học. Học phần học lại hoặc học cải thiện tính như đăng ký mới. Số tiền hiện trên cổng thông tin sau đợt đăng ký.'),
    ('Học phí – miễn giảm', '[MẪU] Hướng dẫn học phí, miễn giảm và học bổng', 2, 2, true,
     'Hạn nộp học phí là khi nào?',
     'Học kỳ chính nộp trong 4 tuần đầu của học kỳ; học kỳ hè nộp trước khi bắt đầu học.'),
    ('Học phí – miễn giảm', '[MẪU] Hướng dẫn học phí, miễn giảm và học bổng', 2, 3, true,
     'Không nộp kịp học phí thì có được gia hạn không?',
     'Được nếu có hoàn cảnh khó khăn: làm đơn xin gia hạn trước thời hạn nộp, tối đa 4 tuần. Quá hạn mà không được gia hạn có thể không được dự thi và bị khóa đăng ký học kỳ sau.'),
    ('Học phí – miễn giảm', '[MẪU] Hướng dẫn học phí, miễn giảm và học bổng', 3, 4, true,
     'Ai được miễn, giảm học phí?',
     'Con của người có công với cách mạng, sinh viên thuộc hộ nghèo hoặc cận nghèo, sinh viên khuyết tật, mồ côi và các đối tượng khác theo quy định. Hồ sơ nộp phòng Công tác sinh viên trong 4 tuần đầu học kỳ.'),
    ('Học bổng', '[MẪU] Hướng dẫn học phí, miễn giảm và học bổng', 4, 1, true,
     'Điều kiện nhận học bổng khuyến khích học tập là gì?',
     'Đăng ký từ 15 tín chỉ trong học kỳ chính, điểm trung bình học kỳ từ 3,2 (thang 4), điểm rèn luyện loại Tốt trở lên và không có học phần bị điểm F. Loại Giỏi từ 3,2 đến dưới 3,6; loại Xuất sắc từ 3,6.'),
    ('Học bổng', '[MẪU] Hướng dẫn học phí, miễn giảm và học bổng', 5, 2, true,
     'Học bổng tài trợ của doanh nghiệp được thông báo ở đâu?',
     'Trên trang của khoa và qua lớp trưởng các lớp. Mỗi học bổng có tiêu chí riêng; chuẩn bị đơn, bảng điểm và thư giới thiệu khi được yêu cầu.'),
    -- Câu hỏi đã gỡ (is_active = false): không còn hiện ở nút chọn nhưng vẫn thấy trong lịch sử hội thoại
    ('Học bổng', '[MẪU] Hướng dẫn học phí, miễn giảm và học bổng', 4, 3, false,
     'Học bổng có được xét cho học kỳ hè không?',
     'Không. Học kỳ hè không thuộc diện xét học bổng khuyến khích học tập.'),
    -- Thực tập và đồ án tốt nghiệp
    ('Thực tập', '[MẪU] Hướng dẫn thực tập và đồ án tốt nghiệp ngành CNTT', 1, 1, true,
     'Khi nào sinh viên đủ điều kiện đi thực tập?',
     'Khi đã tích lũy từ 100 tín chỉ và hoàn thành các học phần cơ sở ngành. Thực tập kéo dài 8 tuần, thường ở học kỳ 7.'),
    ('Thực tập', '[MẪU] Hướng dẫn thực tập và đồ án tốt nghiệp ngành CNTT', 2, 2, true,
     'Có được tự tìm nơi thực tập không?',
     'Được. Bạn tự liên hệ đơn vị phù hợp với ngành hoặc chọn từ danh sách khoa giới thiệu; nếu tự liên hệ thì nộp giấy tiếp nhận có xác nhận của đơn vị trước ngày bắt đầu.'),
    ('Thực tập', '[MẪU] Hướng dẫn thực tập và đồ án tốt nghiệp ngành CNTT', 3, 3, true,
     'Điểm thực tập được tính như thế nào?',
     '40% điểm đánh giá của đơn vị thực tập và 60% điểm của giảng viên chấm báo cáo. Báo cáo nộp chậm quá 1 tuần không lý do sẽ bị trừ điểm.'),
    ('Đồ án tốt nghiệp', '[MẪU] Hướng dẫn thực tập và đồ án tốt nghiệp ngành CNTT', 4, 3, true,
     'Không đủ điều kiện làm đồ án thì học gì thay thế?',
     'Học các học phần thay thế có tổng tín chỉ tương đương theo danh sách khoa công bố đầu học kỳ 8. Điều kiện làm đồ án: điểm trung bình tích lũy từ 2,0 và nợ không quá 10 tín chỉ.'),
    ('Đồ án tốt nghiệp', '[MẪU] Hướng dẫn thực tập và đồ án tốt nghiệp ngành CNTT', 5, 4, true,
     'Hồ sơ nộp đồ án tốt nghiệp gồm những gì?',
     'Quyển thuyết minh có chữ ký giảng viên hướng dẫn, mã nguồn kèm hướng dẫn cài đặt, slide trình bày và bản điện tử của toàn bộ hồ sơ. Thuyết minh được kiểm tra trùng lặp trước khi ra hội đồng.'),
    ('Đồ án tốt nghiệp', '[MẪU] Hướng dẫn thực tập và đồ án tốt nghiệp ngành CNTT', 6, 5, true,
     'Buổi bảo vệ đồ án diễn ra như thế nào?',
     'Trình bày khoảng 15 phút rồi trả lời câu hỏi của hội đồng 10–15 phút. Điểm là trung bình có trọng số của giảng viên hướng dẫn, phản biện và hội đồng.')
) AS seed(topic_name, doc_title, page, sort_order, active, content, answer)
JOIN topics t ON t.name = seed.topic_name
JOIN documents d ON d.title = seed.doc_title
WHERE NOT EXISTS (SELECT 1 FROM questions q WHERE q.topic_id = t.id AND q.content = seed.content);

-- 4. Lịch sử hội thoại của sinh viên (chỉ thêm cho người chưa có hội thoại nào) --------------------
-- Tiêu đề hội thoại = câu hỏi đầu tiên, giống ChatService khi sinh viên hỏi thật.
WITH seed(code, days, hours, qs) AS (VALUES
    ('SV002', 20, 3, ARRAY['Mỗi học kỳ được đăng ký tối đa bao nhiêu tín chỉ?', 'Có được rút học phần sau khi đã đăng ký không?', 'Học phí mỗi học kỳ được tính như thế nào?']),
    ('SV002',  3, 5, ARRAY['Hạn nộp học phí là khi nào?', 'Không nộp kịp học phí thì có được gia hạn không?']),
    ('SV003', 30, 2, ARRAY['Học bổng có được xét cho học kỳ hè không?']),
    ('SV003', 15, 4, ARRAY['Điều kiện nhận học bổng khuyến khích học tập là gì?', 'Học bổng tài trợ của doanh nghiệp được thông báo ở đâu?']),
    ('SV004',  9, 1, ARRAY['Khi nào sinh viên bị cảnh báo học vụ?', 'Bị cảnh báo học vụ thì được đăng ký bao nhiêu tín chỉ?', 'Lập kế hoạch cải thiện kết quả học tập thế nào?']),
    ('SV005', 25, 6, ARRAY['Khi nào sinh viên đủ điều kiện đi thực tập?', 'Có được tự tìm nơi thực tập không?', 'Điểm thực tập được tính như thế nào?']),
    ('SV005',  2, 2, ARRAY['Hồ sơ nộp đồ án tốt nghiệp gồm những gì?', 'Buổi bảo vệ đồ án diễn ra như thế nào?', 'Nên chuẩn bị gì cho buổi bảo vệ đồ án?']),
    ('SV006',  7, 8, ARRAY['Vắng thi cuối kỳ có lý do thì phải làm gì?', 'Muốn phúc khảo bài thi thì nộp đơn khi nào?']),
    ('SV007', 12, 3, ARRAY['Điều kiện để được công nhận tốt nghiệp là gì?', 'Hạng tốt nghiệp được xếp theo điểm trung bình thế nào?', 'Chuẩn đầu ra ngoại ngữ có bắt buộc để tốt nghiệp không?']),
    ('SV008',  5, 7, ARRAY['Thời gian nghỉ học tạm thời tối đa là bao lâu?', 'Cần hỏi ai trước khi xin bảo lưu?', 'Làm gì khi muốn học lại sau thời gian bảo lưu?']),
    ('SV009',  1, 4, ARRAY['Bị điểm F thì có phải học lại không?', 'Điểm học phần được quy đổi sang điểm chữ như thế nào?']),
    ('SV010', 18, 2, ARRAY['Ai được miễn, giảm học phí?']),
    ('SV011',  0, 2, ARRAY['Cần chuẩn bị gì trước khi đăng ký học phần?', 'Làm gì khi hai lớp bị trùng lịch?', 'Mỗi học kỳ được đăng ký tối đa bao nhiêu tín chỉ?']),
    ('SV012',  6, 5, ARRAY['Không đủ điều kiện làm đồ án thì học gì thay thế?']),
    ('SV013', 40, 3, ARRAY['Làm sao để chuẩn bị đề cương đồ án?'])
),
conv AS MATERIALIZED (
    SELECT gen_random_uuid() AS id, u.id AS user_id, s.qs,
           now() - make_interval(days => s.days, hours => s.hours) AS created_at
    FROM seed s
    JOIN users u ON u.code = s.code
    WHERE NOT EXISTS (SELECT 1 FROM conversations c WHERE c.user_id = u.id)
),
ins AS (
    INSERT INTO conversations (id, user_id, title, created_at)
    SELECT id, user_id, qs[1], created_at FROM conv
    RETURNING id
)
INSERT INTO messages (conversation_id, question_id, sent_at)
SELECT conv.id, q.id, conv.created_at + make_interval(mins => (x.ord - 1)::int * 2)
FROM conv
CROSS JOIN LATERAL unnest(conv.qs) WITH ORDINALITY AS x(content, ord)
JOIN questions q ON q.content = x.content
WHERE conv.id IN (SELECT id FROM ins);

COMMIT;

-- Tổng kết
SELECT 'users' AS bang, count(*) FROM users
UNION ALL SELECT 'topics', count(*) FROM topics
UNION ALL SELECT 'questions', count(*) FROM questions
UNION ALL SELECT 'documents', count(*) FROM documents
UNION ALL SELECT 'conversations', count(*) FROM conversations
UNION ALL SELECT 'messages', count(*) FROM messages;
