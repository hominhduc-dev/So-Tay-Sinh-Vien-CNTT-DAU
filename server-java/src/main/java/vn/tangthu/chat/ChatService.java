package vn.tangthu.chat;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tangthu.account.UserRepository;
import vn.tangthu.common.ApiException;
import vn.tangthu.common.PageResponse;
import vn.tangthu.content.Question;
import vn.tangthu.content.QuestionRepository;
import vn.tangthu.chat.ChatDtos.ChatResponse;
import vn.tangthu.chat.ChatDtos.ConversationDetail;
import vn.tangthu.chat.ChatDtos.MessageDto;
import vn.tangthu.chat.ChatDtos.QuestionRef;
import vn.tangthu.chat.ChatDtos.TypedResponse;

/** M2 · Hỏi đáp: UC05, UC06. Không sinh câu trả lời — chỉ đọc câu trả lời soạn sẵn (BR06). */
@Service
public class ChatService {
    private final QuestionRepository questions;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final UserRepository users;
    private final QuestionMatcher matcher;

    public ChatService(QuestionRepository questions, ConversationRepository conversations,
                       MessageRepository messages, UserRepository users, QuestionMatcher matcher) {
        this.questions = questions;
        this.conversations = conversations;
        this.messages = messages;
        this.users = users;
        this.matcher = matcher;
    }

    /** UC05 bước 4–7. */
    @Transactional
    public ChatResponse ask(UUID userId, UUID questionId, UUID conversationId) {
        Question q = questions.findByIdAndActiveTrue(questionId)
                .orElseThrow(() -> ApiException.notFound("QUESTION_NOT_FOUND", "Câu hỏi không còn."));
        Conversation c = conversationOf(userId, conversationId, q.getContent());
        Message m = messages.save(c.ask(q));
        return new ChatResponse(c.getId(), MessageDto.of(m));
    }

    /** UC24 — sinh viên gõ câu hỏi. Chỉ trả lời bằng câu soạn sẵn khớp; không tự sinh nội dung (BR07). */
    @Transactional
    public TypedResponse askTyped(UUID userId, String text, UUID conversationId) {
        String typed = text.trim();
        if (conversationId != null) {
            owned(userId, conversationId); // BR05: kiểm tra trước cả khi tìm khớp
        }
        QuestionMatcher.Result r = matcher.match(typed, questions.findByActiveTrue());
        List<QuestionRef> suggestions = r.suggestions().stream()
                .map(s -> new QuestionRef(s.question().getId(), s.question().getContent()))
                .toList();
        if (r.kind() != QuestionMatcher.Kind.ANSWERED) {
            return new TypedResponse(r.kind().name(), conversationId, null, suggestions);
        }
        Conversation c = conversationOf(userId, conversationId, typed);
        Message m = messages.save(c.ask(r.best().question(), typed));
        return new TypedResponse(r.kind().name(), c.getId(), MessageDto.of(m), suggestions);
    }

    private Conversation conversationOf(UUID userId, UUID conversationId, String title) {
        return conversationId == null
                ? conversations.save(Conversation.start(users.getReferenceById(userId), title))
                : owned(userId, conversationId);
    }

    /** UC06 — danh sách hội thoại của chính mình, mới nhất trước. */
    @Transactional(readOnly = true)
    public PageResponse<ConversationRepository.Summary> list(UUID userId, Pageable pageable) {
        return PageResponse.of(conversations.summaries(userId, pageable), s -> s);
    }

    /** UC06 — chi tiết; câu hỏi đã ẩn vẫn hiển thị. */
    @Transactional(readOnly = true)
    public ConversationDetail detail(UUID userId, UUID conversationId) {
        Conversation c = owned(userId, conversationId);
        var items = messages.findWithSource(c.getId()).stream().map(MessageDto::of).toList();
        return new ConversationDetail(c.getId(), c.getTitle(), c.getCreatedAt(), items);
    }

    /** BR05: hội thoại của người khác coi như không tồn tại. */
    private Conversation owned(UUID userId, UUID conversationId) {
        return conversations.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> ApiException.notFound("CONVERSATION_NOT_FOUND", "Không tìm thấy hội thoại."));
    }
}
