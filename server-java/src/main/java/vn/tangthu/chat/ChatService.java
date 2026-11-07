package vn.tangthu.chat;

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

/** M2 · Hỏi đáp: UC05, UC06. Không sinh câu trả lời — chỉ đọc câu trả lời soạn sẵn (BR06). */
@Service
public class ChatService {
    private final QuestionRepository questions;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final UserRepository users;

    public ChatService(QuestionRepository questions, ConversationRepository conversations,
                       MessageRepository messages, UserRepository users) {
        this.questions = questions;
        this.conversations = conversations;
        this.messages = messages;
        this.users = users;
    }

    /** UC05 bước 4–7. */
    @Transactional
    public ChatResponse ask(UUID userId, UUID questionId, UUID conversationId) {
        Question q = questions.findByIdAndActiveTrue(questionId)
                .orElseThrow(() -> ApiException.notFound("QUESTION_NOT_FOUND", "Câu hỏi không còn."));
        Conversation c = conversationId == null
                ? conversations.save(Conversation.start(users.getReferenceById(userId), q))
                : owned(userId, conversationId);
        Message m = messages.save(c.ask(q));
        return new ChatResponse(c.getId(), MessageDto.of(m));
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
