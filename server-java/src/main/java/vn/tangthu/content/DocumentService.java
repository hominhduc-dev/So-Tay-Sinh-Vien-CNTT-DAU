package vn.tangthu.content;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.tangthu.account.UserRepository;
import vn.tangthu.common.ApiException;
import vn.tangthu.common.FileStorage;
import vn.tangthu.common.PageResponse;
import vn.tangthu.content.ContentDtos.DocumentDto;

/** UC07, UC08, UC10, UC11, UC12. */
@Service
public class DocumentService {
    static final String PDF = "application/pdf";
    static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final DocumentRepository documents;
    private final QuestionRepository questions;
    private final UserRepository users;
    private final FileStorage storage;

    public DocumentService(DocumentRepository documents, QuestionRepository questions, UserRepository users,
                           FileStorage storage) {
        this.documents = documents;
        this.questions = questions;
        this.users = users;
        this.storage = storage;
    }

    /** UC07 */
    @Transactional(readOnly = true)
    public PageResponse<DocumentDto> search(String q, Pageable pageable) {
        String k = q == null ? "" : q.trim();
        var page = k.isEmpty() ? documents.findAllBy(pageable) : documents.findByTitleContainingIgnoreCase(k, pageable);
        return PageResponse.of(page, DocumentDto::of);
    }

    @Transactional(readOnly = true)
    public DocumentDto get(UUID id) {
        return DocumentDto.of(require(id));
    }

    public record StoredFile(Path path, String mimeType, String downloadName) {}

    /** UC08 */
    @Transactional(readOnly = true)
    public StoredFile file(UUID id) {
        Document d = require(id);
        Path p = storage.resolve(d.getFileUrl());
        if (!Files.exists(p)) {
            throw ApiException.notFound("FILE_NOT_FOUND", "Không tìm thấy tệp gốc.");
        }
        String ext = d.getMimeType().equals(PDF) ? ".pdf" : ".docx";
        return new StoredFile(p, d.getMimeType(), d.getTitle() + ext);
    }

    /** UC10 — +upload(in file: File): void của lớp Document. */
    @Transactional
    public DocumentDto upload(UUID uploaderId, String title, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw ApiException.validation("Chưa chọn tệp.");
        }
        String mime = detect(file);
        FileStorage.Staged staged;
        try (InputStream in = file.getInputStream()) {
            staged = storage.stage(in);
        }
        try {
            if (documents.existsByFileHash(staged.sha256())) {
                throw ApiException.conflict("DUPLICATE_DOCUMENT", "Tài liệu đã tồn tại.");
            }
            Integer pages = mime.equals(PDF) ? countPages(staged.tempFile()) : null;
            String rel = storage.commit(staged, mime.equals(PDF) ? "pdf" : "docx");
            try {
                Document d = documents.saveAndFlush(new Document(title.trim(), rel, staged.sha256(), mime, pages,
                        users.getReferenceById(uploaderId)));
                return DocumentDto.of(documents.findById(d.getId()).orElseThrow());
            } catch (RuntimeException e) {
                storage.delete(rel);
                throw e;
            }
        } finally {
            storage.discard(staged);
        }
    }

    /** UC11 */
    @Transactional
    public DocumentDto rename(UUID id, String title) {
        Document d = require(id);
        d.setTitle(title.trim());
        return DocumentDto.of(d);
    }

    /** UC12 — +remove(): void của lớp Document. Còn câu hỏi dẫn tới thì không gỡ (BR06). */
    @Transactional
    public void remove(UUID id) {
        Document d = require(id);
        long used = questions.countByDocumentId(id);
        if (used > 0) {
            throw ApiException.conflict("DOCUMENT_IN_USE",
                    "Tài liệu đang được dùng làm nguồn cho " + used + " câu hỏi.");
        }
        String rel = d.getFileUrl();
        documents.delete(d);
        documents.flush();
        storage.delete(rel);
    }

    private Document require(UUID id) {
        return documents.findById(id)
                .orElseThrow(() -> ApiException.notFound("DOCUMENT_NOT_FOUND", "Không tìm thấy tài liệu."));
    }

    /** Chỉ nhận PDF (bắt đầu bằng %PDF) và DOCX (tệp zip, đuôi .docx). */
    private static String detect(MultipartFile file) throws IOException {
        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(4);
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (head.length == 4 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F') {
            return PDF;
        }
        if (head.length == 4 && head[0] == 'P' && head[1] == 'K' && name.endsWith(".docx")) {
            return DOCX;
        }
        throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FILE", "Chỉ nhận tệp PDF hoặc DOCX.");
    }

    private static int countPages(Path pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            return doc.getNumberOfPages();
        } catch (IOException e) {
            throw ApiException.validation("Không đọc được tệp PDF.");
        }
    }
}
