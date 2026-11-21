package vn.tangthu.client.content.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.UUID;
import vn.tangthu.client.common.net.SocketClient;
import vn.tangthu.client.content.model.Question;

/** M3 · Nội dung — UC22 soạn câu hỏi và câu trả lời, gắn tài liệu nguồn. */
public class QuestionController {
    private final SocketClient client;

    public QuestionController(SocketClient client) {
        this.client = client;
    }

    public List<Question> listForEditor(UUID topicId) {
        return client.call("question.listForEditor", SocketClient.params("topicId", topicId),
                new TypeReference<List<Question>>() {});
    }

    public Question create(UUID topicId, String content, String answer, UUID documentId, int page,
                           int sortOrder, boolean active) {
        return client.call("question.create", SocketClient.params("topicId", topicId, "content", content,
                "answer", answer, "documentId", documentId, "page", page, "sortOrder", sortOrder,
                "active", active), Question.class);
    }

    public Question update(UUID id, String content, String answer, UUID documentId, int page,
                           int sortOrder, boolean active) {
        return client.call("question.update", SocketClient.params("id", id, "content", content,
                "answer", answer, "documentId", documentId, "page", page, "sortOrder", sortOrder,
                "active", active), Question.class);
    }

    public void hide(UUID id) {
        client.call("question.hide", SocketClient.params("id", id));
    }
}
