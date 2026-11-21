package vn.tangthu.content.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import vn.tangthu.content.service.QuestionService;

public record QuestionRequest(UUID topicId,
                              @Size(max = 200) String content,
                              @Size(max = 5000) String answer,
                              UUID documentId,
                              @Min(1) Integer page,
                              Integer sortOrder,
                              Boolean active) {
    public QuestionService.QuestionInput toInput() {
        return new QuestionService.QuestionInput(topicId, content, answer, documentId, page, sortOrder, active);
    }
}
