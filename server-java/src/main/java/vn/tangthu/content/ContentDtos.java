package vn.tangthu.content;

import java.time.Instant;
import java.util.UUID;

/** DTO trả về client của module M3 · Nội dung. */
public final class ContentDtos {
    private ContentDtos() {}

    public record TopicDto(UUID id, String name, int sortOrder, long questionCount) {}

    public record QuestionButton(UUID id, String content) {}

    public record QuestionDto(UUID id, UUID topicId, String content, String answer, UUID documentId,
                              String documentTitle, int page, boolean active, int sortOrder) {
        public static QuestionDto of(Question q) {
            return new QuestionDto(q.getId(), q.getTopic().getId(), q.getContent(), q.getAnswer(),
                    q.getSource().getId(), q.getSource().getTitle(), q.getPage(), q.isActive(), q.getSortOrder());
        }
    }

    public record DocumentDto(UUID id, String title, String mimeType, Integer pageCount, String uploadedBy,
                              Instant createdAt) {
        public static DocumentDto of(Document d) {
            return new DocumentDto(d.getId(), d.getTitle(), d.getMimeType(), d.getPageCount(),
                    d.getUploadedBy().getFullName(), d.getCreatedAt());
        }
    }
}
