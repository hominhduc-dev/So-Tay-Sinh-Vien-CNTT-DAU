-- Lược đồ Sổ Tay Sinh Viên CNTT — 6 bảng, khớp ERD Chương 3.
-- Đặc tả: docs/DAC-TA-TRIEN-KHAI.md mục 5.

CREATE TABLE users (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code          varchar(50)  NOT NULL UNIQUE,          -- mã sinh viên / cán bộ
    email         varchar(255) NOT NULL UNIQUE,
    password_hash varchar(100) NOT NULL,
    full_name     varchar(200) NOT NULL,
    role          varchar(20)  NOT NULL DEFAULT 'USER'
                  CHECK (role IN ('USER', 'CONTENT_ADMIN', 'SYSTEM_ADMIN')),
    department    varchar(50)  NOT NULL,
    is_active     boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE documents (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    title       varchar(500) NOT NULL,
    file_url    varchar(500) NOT NULL,                   -- đường dẫn tương đối trong thư mục lưu tệp
    file_hash   varchar(64)  NOT NULL UNIQUE,            -- SHA-256, chặn tải trùng
    mime_type   varchar(100) NOT NULL,
    page_count  integer,                                 -- NULL với DOCX
    uploaded_by uuid         NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE topics (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name       varchar(100) NOT NULL UNIQUE,
    sort_order integer      NOT NULL DEFAULT 0
);

CREATE TABLE questions (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id    uuid          NOT NULL REFERENCES topics(id)    ON DELETE RESTRICT,
    content     varchar(200)  NOT NULL,                  -- chữ hiện trên nút
    answer      varchar(5000) NOT NULL,                  -- câu trả lời soạn sẵn
    document_id uuid          NOT NULL REFERENCES documents(id) ON DELETE RESTRICT,
    page        integer       NOT NULL CHECK (page >= 1),
    is_active   boolean       NOT NULL DEFAULT true,     -- gỡ câu hỏi = false
    sort_order  integer       NOT NULL DEFAULT 0
);
CREATE INDEX idx_questions_topic ON questions(topic_id) WHERE is_active;
CREATE INDEX idx_questions_document ON questions(document_id);

CREATE TABLE conversations (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title      varchar(200) NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_conversations_user ON conversations(user_id, created_at DESC);

CREATE TABLE messages (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id uuid        NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    question_id     uuid        NOT NULL REFERENCES questions(id)     ON DELETE RESTRICT,
    sent_at         timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_conversation ON messages(conversation_id, sent_at);
