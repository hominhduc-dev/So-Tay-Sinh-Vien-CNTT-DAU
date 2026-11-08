package vn.tangthu.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.tangthu.content.Document;
import vn.tangthu.content.Question;

/** DTO của module M2 · Hỏi đáp. */
public final class ChatDtos {
    private ChatDtos() {}

    public record QuestionRef(UUID id, String content) {}

    public record Source(UUID documentId, String title, int page) {}

    /** Một lượt hỏi – đáp: câu hỏi đã chọn, câu trả lời soạn sẵn và nguồn. */
    public record MessageDto(UUID id, Instant sentAt, String typedText, QuestionRef question, String answer,
                             Source source) {
        public static MessageDto of(Message m) {
            Question q = m.getQuestion();
            Document d = q.getSource();
            return new MessageDto(m.getId(), m.getSentAt(), m.getTypedText(), new QuestionRef(q.getId(), q.getContent()),
                    q.getAnswer(), new Source(d.getId(), d.getTitle(), q.getPage()));
        }
    }

    /**
     * Kết quả UC24. ANSWERED: đã trả lời bằng câu khớp nhất (message khác null), kèm câu liên quan.
     * SUGGESTIONS: chưa trả lời, đưa 1–3 câu gần nhất để sinh viên chọn. NO_MATCH: không có câu nào —
     * client mời sinh viên gửi câu hỏi cho giáo vụ (UC25).
     */
    public record TypedResponse(String status, UUID conversationId, MessageDto message, List<QuestionRef> suggestions) {}

    public record ChatResponse(UUID conversationId, MessageDto message) {}

    public record ConversationDetail(UUID id, String title, Instant createdAt, List<MessageDto> messages) {}
}
