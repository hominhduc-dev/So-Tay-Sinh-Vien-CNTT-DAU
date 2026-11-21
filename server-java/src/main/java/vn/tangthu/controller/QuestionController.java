package vn.tangthu.controller;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.tangthu.dto.ContentDtos.QuestionDto;
import vn.tangthu.dto.QuestionRequest;
import vn.tangthu.service.QuestionService;

/** UC23 — chỉ Giáo vụ và Quản trị viên. */
@RestController
@PreAuthorize("hasRole('CONTENT_ADMIN')")
public class QuestionController {
    private final QuestionService questions;

    public QuestionController(QuestionService questions) {
        this.questions = questions;
    }

    @GetMapping("/api/admin/questions")
    public List<QuestionDto> list(@RequestParam UUID topicId) {
        return questions.listForEditor(topicId);
    }

    @PostMapping("/api/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionDto create(@Valid @RequestBody QuestionRequest req) {
        return questions.create(req.toInput());
    }

    @PatchMapping("/api/questions/{id}")
    public QuestionDto update(@PathVariable UUID id, @Valid @RequestBody QuestionRequest req) {
        return questions.update(id, req.toInput());
    }

    @DeleteMapping("/api/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hide(@PathVariable UUID id) {
        questions.hide(id);
    }
}
