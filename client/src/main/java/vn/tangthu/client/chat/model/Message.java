package vn.tangthu.client.chat.model;

import java.time.Instant;
import java.util.UUID;

/** Một lượt hỏi – đáp: câu hỏi sinh viên chọn, câu trả lời giáo vụ soạn, tài liệu nguồn. */
public record Message(UUID id, Instant sentAt, QuestionRef question, String answer, Source source) {
    public record QuestionRef(UUID id, String content) {}

    public record Source(UUID documentId, String title, int page) {}
}
