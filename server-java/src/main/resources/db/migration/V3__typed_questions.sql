-- UC24–UC27: sinh viên gõ câu hỏi; câu không khớp được gửi cho giáo vụ.

-- Tin nhắn ghi lại câu sinh viên đã gõ khi hệ thống trả lời bằng một câu hỏi soạn sẵn khớp.
-- NULL khi sinh viên bấm nút.
ALTER TABLE messages ADD COLUMN typed_text varchar(500);

-- Câu hỏi gửi giáo vụ (Inquiry): câu gõ không khớp câu hỏi soạn sẵn nào.
CREATE TABLE inquiries (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content     varchar(500)  NOT NULL,
    status      varchar(20)   NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ANSWERED')),
    answer      varchar(5000),
    document_id uuid          REFERENCES documents(id) ON DELETE RESTRICT,
    page        integer       CHECK (page >= 1),
    answered_by uuid          REFERENCES users(id) ON DELETE SET NULL,
    created_at  timestamptz   NOT NULL DEFAULT now(),
    answered_at timestamptz,
    CHECK ((status = 'PENDING' AND answer IS NULL) OR (status = 'ANSWERED' AND answer IS NOT NULL)),
    CHECK ((document_id IS NULL) = (page IS NULL))
);
CREATE INDEX idx_inquiries_user ON inquiries(user_id, created_at DESC);
CREATE INDEX idx_inquiries_pending ON inquiries(created_at) WHERE status = 'PENDING';
