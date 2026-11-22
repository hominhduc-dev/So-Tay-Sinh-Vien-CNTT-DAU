package vn.tangthu.client.content.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.UUID;
import vn.tangthu.client.common.net.SocketClient;
import vn.tangthu.client.content.model.QuestionButton;
import vn.tangthu.client.content.model.Topic;

/** M3 · Nội dung — chủ đề (UC07 xem, UC22 quản lý). */
public class TopicController {
    private final SocketClient client;

    public TopicController(SocketClient client) {
        this.client = client;
    }

    public List<Topic> list() {
        return client.call("topic.list", null, new TypeReference<List<Topic>>() {});
    }

    public List<QuestionButton> questions(UUID topicId) {
        return client.call("topic.questions", SocketClient.params("topicId", topicId),
                new TypeReference<List<QuestionButton>>() {});
    }

    public Topic create(String name, Integer sortOrder) {
        return client.call("topic.create", SocketClient.params("name", name, "sortOrder", sortOrder), Topic.class);
    }

    public Topic update(UUID id, String name, Integer sortOrder) {
        return client.call("topic.update", SocketClient.params("id", id, "name", name, "sortOrder", sortOrder),
                Topic.class);
    }

    public void delete(UUID id) {
        client.call("topic.delete", SocketClient.params("id", id));
    }
}
