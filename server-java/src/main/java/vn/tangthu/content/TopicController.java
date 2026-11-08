package vn.tangthu.content;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.tangthu.content.ContentDtos.QuestionButton;
import vn.tangthu.content.ContentDtos.TopicDto;

@RestController
@RequestMapping("/api/topics")
public class TopicController {
    private final TopicService topics;

    public TopicController(TopicService topics) {
        this.topics = topics;
    }

    public record CreateTopic(@NotBlank @Size(max = 100) String name, Integer sortOrder) {}

    public record UpdateTopic(@Size(min = 1, max = 100) String name, Integer sortOrder) {}

    /** UC05 bước 1 — nút chủ đề. */
    @GetMapping
    public List<TopicDto> list() {
        return topics.list();
    }

    /** UC05 bước 2 — nút câu hỏi của chủ đề. */
    @GetMapping("/{id}/questions")
    public List<QuestionButton> questions(@PathVariable UUID id) {
        return topics.buttons(id);
    }

    /** UC22 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public TopicDto create(@Valid @RequestBody CreateTopic req) {
        return topics.create(req.name(), req.sortOrder());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public TopicDto update(@PathVariable UUID id, @Valid @RequestBody UpdateTopic req) {
        return topics.update(id, req.name(), req.sortOrder());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public void delete(@PathVariable UUID id) {
        topics.delete(id);
    }
}
