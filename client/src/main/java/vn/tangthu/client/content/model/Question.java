package vn.tangthu.client.content.model;

import java.util.UUID;

/** Câu hỏi đầy đủ cho giáo vụ soạn (UC23). */
public record Question(UUID id, UUID topicId, String content, String answer, UUID documentId,
                       String documentTitle, int page, boolean active, int sortOrder) {}
