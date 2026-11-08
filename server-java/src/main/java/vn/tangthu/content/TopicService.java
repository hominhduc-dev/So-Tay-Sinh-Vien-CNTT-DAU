package vn.tangthu.content;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tangthu.common.ApiException;
import vn.tangthu.content.ContentDtos.QuestionButton;
import vn.tangthu.content.ContentDtos.TopicDto;

/** Chủ đề: đọc cho UC05, quản lý cho UC22. */
@Service
public class TopicService {
    private final TopicRepository topics;
    private final QuestionRepository questions;

    public TopicService(TopicRepository topics, QuestionRepository questions) {
        this.topics = topics;
        this.questions = questions;
    }

    @Transactional(readOnly = true)
    public List<TopicDto> list() {
        return topics.listWithActiveCount().stream()
                .map(r -> new TopicDto(r.id(), r.name(), r.sortOrder(), r.questionCount()))
                .toList();
    }

    /** Các nút câu hỏi đang hiện của một chủ đề (UC05 bước 2). */
    @Transactional(readOnly = true)
    public List<QuestionButton> buttons(UUID topicId) {
        require(topicId);
        return questions.findByTopicIdAndActiveTrueOrderBySortOrderAscContentAsc(topicId).stream()
                .map(q -> new QuestionButton(q.getId(), q.getContent()))
                .toList();
    }

    @Transactional
    public TopicDto create(String name, Integer sortOrder) {
        String n = name.trim();
        if (topics.existsByNameIgnoreCase(n)) {
            throw ApiException.conflict("TOPIC_NAME_TAKEN", "Tên chủ đề đã tồn tại.");
        }
        Topic t = topics.save(new Topic(n, sortOrder == null ? 0 : sortOrder));
        return new TopicDto(t.getId(), t.getName(), t.getSortOrder(), 0);
    }

    @Transactional
    public TopicDto update(UUID id, String name, Integer sortOrder) {
        Topic t = require(id);
        if (name != null) {
            String n = name.trim();
            if (topics.existsByNameIgnoreCaseAndIdNot(n, id)) {
                throw ApiException.conflict("TOPIC_NAME_TAKEN", "Tên chủ đề đã tồn tại.");
            }
            t.setName(n);
        }
        if (sortOrder != null) {
            t.setSortOrder(sortOrder);
        }
        return new TopicDto(t.getId(), t.getName(), t.getSortOrder(), t.getQuestions().size());
    }

    @Transactional
    public void delete(UUID id) {
        Topic t = require(id);
        if (questions.existsByTopicId(id)) {
            throw ApiException.conflict("TOPIC_NOT_EMPTY", "Chủ đề còn câu hỏi.");
        }
        topics.delete(t);
    }

    Topic require(UUID id) {
        return topics.findById(id)
                .orElseThrow(() -> ApiException.notFound("TOPIC_NOT_FOUND", "Không tìm thấy chủ đề."));
    }
}
