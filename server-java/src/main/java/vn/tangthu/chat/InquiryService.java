package vn.tangthu.chat;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tangthu.account.UserRepository;
import vn.tangthu.common.ApiException;
import vn.tangthu.common.PageResponse;
import vn.tangthu.content.Document;
import vn.tangthu.content.DocumentRepository;

/** UC25 gửi câu hỏi cho giáo vụ, UC26 xem câu trả lời, UC27 giáo vụ trả lời. */
@Service
public class InquiryService {
    /** BR08: mỗi sinh viên có tối đa chừng này câu hỏi đang chờ, tránh gửi tràn hộp thư. */
    static final int MAX_PENDING = 5;

    private final InquiryRepository inquiries;
    private final UserRepository users;
    private final DocumentRepository documents;

    public InquiryService(InquiryRepository inquiries, UserRepository users, DocumentRepository documents) {
        this.inquiries = inquiries;
        this.users = users;
        this.documents = documents;
    }

    public record Source(UUID documentId, String title, int page) {}

    public record InquiryDto(UUID id, String content, Inquiry.Status status, String answer, Source source,
                             Instant createdAt, Instant answeredAt, String askerName, String askerCode) {
        static InquiryDto of(Inquiry i, boolean withAsker) {
            Document d = i.getSource();
            return new InquiryDto(i.getId(), i.getContent(), i.getStatus(), i.getAnswer(),
                    d == null ? null : new Source(d.getId(), d.getTitle(), i.getPage()),
                    i.getCreatedAt(), i.getAnsweredAt(),
                    withAsker ? i.getAsker().getFullName() : null, withAsker ? i.getAsker().getCode() : null);
        }
    }

    /** UC25 */
    @Transactional
    public InquiryDto send(UUID userId, String content) {
        if (inquiries.countByAskerIdAndStatus(userId, Inquiry.Status.PENDING) >= MAX_PENDING) {
            throw ApiException.conflict("TOO_MANY_PENDING",
                    "Bạn đang có " + MAX_PENDING + " câu hỏi chờ giáo vụ trả lời. Vui lòng đợi trước khi gửi thêm.");
        }
        Inquiry i = inquiries.save(new Inquiry(users.getReferenceById(userId), content.trim()));
        return InquiryDto.of(i, false);
    }

    /** UC26 — chỉ câu hỏi của chính mình (BR05). */
    @Transactional(readOnly = true)
    public PageResponse<InquiryDto> mine(UUID userId, Pageable pageable) {
        return PageResponse.of(inquiries.findByAskerId(userId, pageable), i -> InquiryDto.of(i, false));
    }

    /** Hộp thư giáo vụ. status = null: tất cả. */
    @Transactional(readOnly = true)
    public PageResponse<InquiryDto> inbox(Inquiry.Status status, Pageable pageable) {
        var page = status == null ? inquiries.findAllBy(pageable) : inquiries.findByStatus(status, pageable);
        return PageResponse.of(page, i -> InquiryDto.of(i, true));
    }

    /** UC27 */
    @Transactional
    public InquiryDto answer(UUID staffId, UUID inquiryId, String text, UUID documentId, Integer page) {
        Inquiry i = inquiries.findById(inquiryId)
                .orElseThrow(() -> ApiException.notFound("INQUIRY_NOT_FOUND", "Không tìm thấy câu hỏi."));
        if (text == null || text.isBlank()) {
            throw ApiException.validation("Cần nhập câu trả lời.");
        }
        Document doc = null;
        if (documentId != null) {
            doc = documents.findById(documentId)
                    .orElseThrow(() -> ApiException.validation("Tài liệu nguồn không tồn tại."));
            if (page == null || page < 1 || (doc.getPageCount() != null && page > doc.getPageCount())) {
                throw ApiException.validation("Số trang phải từ 1 đến số trang của tài liệu.");
            }
        } else if (page != null) {
            throw ApiException.validation("Chọn tài liệu nguồn trước khi nhập số trang.");
        }
        i.answer(users.getReferenceById(staffId), text.trim(), doc, page);
        return InquiryDto.of(i, true);
    }
}
