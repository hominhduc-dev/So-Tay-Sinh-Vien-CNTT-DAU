package vn.tangthu.content;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.tangthu.common.ApiException;
import vn.tangthu.common.AuthUser;
import vn.tangthu.common.PageResponse;
import vn.tangthu.content.ContentDtos.DocumentDto;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documents;

    public DocumentController(DocumentService documents) {
        this.documents = documents;
    }

    public record RenameRequest(@Size(min = 3, max = 500) String title) {}

    /** UC07 */
    @GetMapping
    public PageResponse<DocumentDto> list(@RequestParam(required = false) String q,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        if (q != null && q.length() > 200) {
            throw ApiException.validation("q tối đa 200 ký tự.");
        }
        return documents.search(q, PageResponse.request(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{id}")
    public DocumentDto get(@PathVariable UUID id) {
        return documents.get(id);
    }

    /** UC08 — mở tệp gốc; client thêm #page=N để nhảy tới trang. */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable UUID id) {
        var f = documents.file(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(f.downloadName(), StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(f.path()));
    }

    /** UC10 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public DocumentDto upload(@AuthenticationPrincipal AuthUser me,
                              @RequestPart("file") MultipartFile file,
                              @RequestParam("title") String title) throws IOException {
        if (title == null || title.trim().length() < 3 || title.trim().length() > 500) {
            throw ApiException.validation("Tiêu đề 3–500 ký tự.");
        }
        return documents.upload(me.id(), title, file);
    }

    /** UC11 */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public DocumentDto rename(@PathVariable UUID id, @Valid @RequestBody RenameRequest req) {
        if (req.title() == null || req.title().isBlank()) {
            throw ApiException.validation("Tiêu đề 3–500 ký tự.");
        }
        return documents.rename(id, req.title());
    }

    /** UC12 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public void remove(@PathVariable UUID id) {
        documents.remove(id);
    }
}
