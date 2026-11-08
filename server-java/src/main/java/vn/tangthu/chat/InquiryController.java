package vn.tangthu.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import vn.tangthu.chat.InquiryService.InquiryDto;

@RestController
public class InquiryController {
    private final InquiryService inquiries;

    public InquiryController(InquiryService inquiries) {
        this.inquiries = inquiries;
    }

    public record SendRequest(@NotBlank @Size(min = 3, max = 500) String content) {}

    public record AnswerRequest(@NotBlank @Size(max = 5000) String answer, UUID documentId, @Min(1) Integer page) {}

    /** UC25 */
    @PostMapping("/api/inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    public InquiryDto send(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody SendRequest req) {
        return inquiries.send(me.id(), req.content());
    }

    /** UC26 */
    @GetMapping("/api/inquiries/mine")
    public PageResponse<InquiryDto> mine(@AuthenticationPrincipal AuthUser me,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return inquiries.mine(me.id(), PageResponse.request(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /** Hộp thư giáo vụ — câu chờ trả lời cũ nhất lên đầu. */
    @GetMapping("/api/inquiries")
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public PageResponse<InquiryDto> inbox(@RequestParam(required = false) Inquiry.Status status,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        Sort sort = status == Inquiry.Status.PENDING ? Sort.by("createdAt") : Sort.by(Sort.Direction.DESC, "createdAt");
        return inquiries.inbox(status, PageResponse.request(page, size, sort));
    }

    /** UC27 */
    @PostMapping("/api/inquiries/{id}/answer")
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public InquiryDto answer(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                             @Valid @RequestBody AnswerRequest req) {
        return inquiries.answer(me.id(), id, req.answer(), req.documentId(), req.page());
    }
}
