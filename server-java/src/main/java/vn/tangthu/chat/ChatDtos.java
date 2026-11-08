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
    public record MessageDto(UUID id, Instant sentAt, QuestionRef question, String answer, Source source) {
        public static MessageDto of(Message m) {
            Question q = m.getQuestion();
            Document d = q.getSource();
            return new MessageDto(m.getId(), m.getSentAt(), new QuestionRef(q.getId(), q.getContent()),
                    q.getAnswer(), new Source(d.getId(), d.getTitle(), q.getPage()));
        }
    }

    public record ChatResponse(UUID conversationId, MessageDto message) {}

    public record ConversationDetail(UUID id, String title, Instant createdAt, List<MessageDto> messages) {}
}
