package vn.tangthu.content.controller;

import java.util.Map;
import org.springframework.stereotype.Component;
import vn.tangthu.account.model.Role;
import vn.tangthu.common.socket.SocketController;
import vn.tangthu.content.dto.QuestionRequest;
import vn.tangthu.content.service.QuestionService;

/**
 * M3 · Nội dung — UC23 quản lý câu hỏi – câu trả lời (giáo vụ).
 *
 * <pre>
 * question.listForEditor  {topicId}                                      → [ question ] (kể cả đã ẩn)
 * question.create         {topicId, content, answer, documentId, page, sortOrder?, active?} → question
 * question.update         {id, …các trường như create, đều tùy chọn}     → question
 * question.hide           {id}                                           → {}
 * </pre>
 */
@Component
public class QuestionSocketController implements SocketController {
    private final QuestionService questions;

    public QuestionSocketController(QuestionService questions) {
        this.questions = questions;
    }

    @Override
    public void register(Routes routes) {
        routes.secured("question.listForEditor", Role.CONTENT_ADMIN, r -> questions.listForEditor(r.uuid("topicId")));
        routes.secured("question.create", Role.CONTENT_ADMIN, r -> questions.create(r.body(QuestionRequest.class).toInput()));
        routes.secured("question.update", Role.CONTENT_ADMIN,
                r -> questions.update(r.uuid("id"), r.body(QuestionRequest.class).toInput()));
        routes.secured("question.hide", Role.CONTENT_ADMIN, r -> {
            questions.hide(r.uuid("id"));
            return Map.of();
        });
    }
}
