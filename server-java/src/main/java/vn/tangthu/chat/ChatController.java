package vn.tangthu.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
import vn.tangthu.common.AuthUser;
import vn.tangthu.common.PageResponse;
import vn.tangthu.chat.ChatDtos.ChatResponse;
import vn.tangthu.chat.ChatDtos.ConversationDetail;
import vn.tangthu.chat.ChatDtos.TypedResponse;

@RestController
public class ChatController {
    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    public record AskRequest(@NotNull UUID questionId, UUID conversationId) {}

    public record TypedRequest(@NotBlank @Size(min = 3, max = 500) String text, UUID conversationId) {}

    /** UC24 — sinh viên gõ câu hỏi; server tìm câu hỏi soạn sẵn khớp nhất. */
    @PostMapping("/api/chat/typed")
    public TypedResponse typed(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody TypedRequest req) {
        return chat.askTyped(me.id(), req.text(), req.conversationId());
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
