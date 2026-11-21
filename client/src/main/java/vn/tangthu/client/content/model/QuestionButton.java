package vn.tangthu.client.content.model;

import java.util.UUID;

/** Câu hỏi hiển thị thành nút bấm trong khung chat (chỉ câu đang hiển thị). */
public record QuestionButton(UUID id, String content) {}
