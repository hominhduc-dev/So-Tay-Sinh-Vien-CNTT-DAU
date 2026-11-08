package vn.tangthu.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.tangthu.account.User;
import vn.tangthu.content.Document;

/** Lớp Inquiry: câu sinh viên gõ mà không khớp câu hỏi soạn sẵn nào, gửi cho giáo vụ trả lời. */
@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry {
    public enum Status { PENDING, ANSWERED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User asker;

    private String content;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    private Integer page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by")
    private User answeredBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "answered_at")
    private Instant answeredAt;

    public Inquiry(User asker, String content) {
        this.asker = asker;
        this.content = content;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }

    /** +answer(in text: string, in source: Document, in page: int): void — trả lời được nhiều lần (sửa câu trả lời). */
    public void answer(User by, String text, Document source, Integer page) {
        this.answer = text;
        this.document = source;
        this.page = source == null ? null : page;
        this.answeredBy = by;
        this.answeredAt = Instant.now();
        this.status = Status.ANSWERED;
    }

    /** +getSource(): Document — có thể null: giáo vụ trả lời không kèm văn bản. */
    public Document getSource() {
        return document;
    }
}
