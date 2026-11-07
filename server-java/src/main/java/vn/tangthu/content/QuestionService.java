package vn.tangthu.content;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tangthu.common.ApiException;
import vn.tangthu.content.ContentDtos.QuestionDto;

/** UC23 — Quản lý câu hỏi – câu trả lời. */
@Service
public class QuestionService {
    private final QuestionRepository questions;
    private final TopicRepository topics;
    private final DocumentRepository documents;

    public QuestionService(QuestionRepository questions, TopicRepository topics, DocumentRepository documents) {
        this.questions = questions;
        this.topics = topics;
        this.documents = documents;
    }

    public record QuestionInput(UUID topicId, String content, String answer, UUID documentId, Integer page,
                                Integer sortOrder, Boolean active) {}

    /** Mọi câu hỏi của chủ đề, kể cả đã ẩn (màn hình giáo vụ). */
    @Transactional(readOnly = true)
    public List<QuestionDto> listForEditor(UUID topicId) {
        topics.findById(topicId)
                .orElseThrow(() -> ApiException.notFound("TOPIC_NOT_FOUND", "Không tìm thấy chủ đề."));
        return questions.findByTopicIdOrderBySortOrderAscContentAsc(topicId).stream().map(QuestionDto::of).toList();
    }

    @Transactional
    public QuestionDto create(QuestionInput in) {
        if (in.topicId() == null || in.documentId() == null || in.page() == null
                || blank(in.content()) || blank(in.answer())) {
            throw ApiException.validation("Cần đủ topicId, content, answer, documentId, page.");
        }
        Topic topic = topic(in.topicId());
        Document doc = document(in.documentId());
        checkPage(doc, in.page());
        Question q = new Question(in.content().trim(), in.answer().trim(), doc, in.page(),
                in.sortOrder() == null ? 0 : in.sortOrder());
        topic.addQuestion(q);
        if (in.active() != null) {
            q.setActive(in.active());
        }
        return QuestionDto.of(questions.save(q));
    }

    @Transactional
    public QuestionDto update(UUID id, QuestionInput in) {
        Question q = questions.findById(id)
                .orElseThrow(() -> ApiException.notFound("QUESTION_NOT_FOUND", "Không tìm thấy câu hỏi."));
        if (in.topicId() != null) {
            q.setTopic(topic(in.topicId()));
        }
        if (in.content() != null) {
            if (blank(in.content())) throw ApiException.validation("content không được rỗng.");
            q.setContent(in.content().trim());
        }
        if (in.answer() != null) {
            if (blank(in.answer())) throw ApiException.validation("answer không được rỗng.");
            q.setAnswer(in.answer().trim());
        }
        if (in.documentId() != null) {
            q.setSource(document(in.documentId()));
        }
        if (in.page() != null) {
            q.setPage(in.page());
        }
        checkPage(q.getSource(), q.getPage());
        if (in.sortOrder() != null) {
            q.setSortOrder(in.sortOrder());
        }
        if (in.active() != null) {
            q.setActive(in.active());
        }
        return QuestionDto.of(q);
    }

    /** Gỡ = ẩn, không xóa dòng, để lịch sử hội thoại vẫn đọc được. */
    @Transactional
    public void hide(UUID id) {
        Question q = questions.findById(id)
                .orElseThrow(() -> ApiException.notFound("QUESTION_NOT_FOUND", "Không tìm thấy câu hỏi."));
        q.setActive(false);
    }

    private Topic topic(UUID id) {
        return topics.findById(id).orElseThrow(() -> ApiException.validation("Chủ đề không tồn tại."));
    }

    private Document document(UUID id) {
        return documents.findById(id).orElseThrow(() -> ApiException.validation("Tài liệu nguồn không tồn tại."));
    }

    private static void checkPage(Document doc, int page) {
        if (page < 1 || (doc.getPageCount() != null && page > doc.getPageCount())) {
            throw ApiException.validation("Số trang phải từ 1 đến số trang của tài liệu.");
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
