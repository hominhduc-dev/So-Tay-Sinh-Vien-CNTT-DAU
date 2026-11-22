package vn.tangthu.client.chat.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.UUID;
import vn.tangthu.client.chat.model.ChatResponse;
import vn.tangthu.client.chat.model.ConversationDetail;
import vn.tangthu.client.chat.model.ConversationSummary;
import vn.tangthu.client.common.net.PageResult;
import vn.tangthu.client.common.net.SocketClient;

/** M2 · Hỏi đáp — UC05 chọn câu hỏi nhận câu trả lời, UC06 xem lại hội thoại. */
public class ChatController {
    private final SocketClient client;

    public ChatController(SocketClient client) {
        this.client = client;
    }

    /** {@code conversationId = null} → server mở hội thoại mới. */
    public ChatResponse ask(UUID questionId, UUID conversationId) {
        return client.call("chat.ask", SocketClient.params("questionId", questionId,
                "conversationId", conversationId), ChatResponse.class);
    }

    public PageResult<ConversationSummary> conversations(int page, int size) {
        return client.call("conversation.list", SocketClient.params("page", page, "size", size),
                new TypeReference<PageResult<ConversationSummary>>() {});
    }

    public ConversationDetail conversation(UUID id) {
        return client.call("conversation.get", SocketClient.params("id", id), ConversationDetail.class);
    }
}
