package vn.tangthu.chat.controller;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import vn.tangthu.account.model.Role;
import vn.tangthu.chat.dto.AskRequest;
import vn.tangthu.chat.service.ChatService;
import vn.tangthu.common.dto.PageResponse;
import vn.tangthu.common.socket.SocketController;

/**
 * M2 · Hỏi đáp — UC05, UC06.
 *
 * <pre>
 * chat.ask           {questionId, conversationId?}  → {conversationId, message}   mọi vai
 * conversation.list  {page?, size?}                 → {items, page, size, total}  mọi vai (của mình)
 * conversation.get   {id}                           → {id, title, createdAt, messages}
 * </pre>
 */
@Component
public class ChatSocketController implements SocketController {
    private final ChatService chat;

    public ChatSocketController(ChatService chat) {
        this.chat = chat;
    }

    @Override
    public void register(Routes routes) {
        routes.secured("chat.ask", Role.USER, r -> {
            AskRequest req = r.body(AskRequest.class);
            return chat.ask(r.me().id(), req.questionId(), req.conversationId());
        });
        routes.secured("conversation.list", Role.USER, r -> chat.list(r.me().id(),
                PageResponse.request(r.integer("page", 0), r.integer("size", 20), Sort.by(Sort.Direction.DESC, "createdAt"))));
        routes.secured("conversation.get", Role.USER, r -> chat.detail(r.me().id(), r.uuid("id")));
    }
}
