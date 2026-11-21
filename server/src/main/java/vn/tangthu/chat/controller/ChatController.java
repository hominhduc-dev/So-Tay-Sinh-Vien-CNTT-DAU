package vn.tangthu.chat.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.tangthu.chat.dto.AskRequest;
import vn.tangthu.chat.dto.ChatDtos.ChatResponse;
import vn.tangthu.chat.dto.ChatDtos.ConversationDetail;
import vn.tangthu.common.dto.PageResponse;
import vn.tangthu.chat.repository.ConversationRepository;
import vn.tangthu.common.security.AuthUser;
import vn.tangthu.chat.service.ChatService;

@RestController
public class ChatController {
    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    /** UC05 — sinh viên bấm một nút câu hỏi. */
    @PostMapping("/api/chat")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatResponse ask(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody AskRequest req) {
        return chat.ask(me.id(), req.questionId(), req.conversationId());
    }

    /** UC06 */
    @GetMapping("/api/conversations")
    public PageResponse<ConversationRepository.Summary> list(@AuthenticationPrincipal AuthUser me,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return chat.list(me.id(), PageResponse.request(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/api/conversations/{id}")
    public ConversationDetail detail(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return chat.detail(me.id(), id);
    }
}
