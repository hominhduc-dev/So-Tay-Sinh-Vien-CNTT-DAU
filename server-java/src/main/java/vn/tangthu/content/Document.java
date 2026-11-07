package vn.tangthu.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.Setter;
import vn.tangthu.account.User;

/**
 * Lớp Document: tệp nguồn trích dẫn. Hai phương thức trong biểu đồ lớp cần hạ tầng lưu tệp nên
 * đặt ở service: +upload(in file: File) → {@link DocumentService#upload}, +remove() → {@link DocumentService#remove}.
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_hash")
    private String fileHash;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "page_count")
    private Integer pageCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(name = "created_at")
    private Instant createdAt;

    public Document(String title, String fileUrl, String fileHash, String mimeType, Integer pageCount, User uploadedBy) {
        this.title = title;
        this.fileUrl = fileUrl;
        this.fileHash = fileHash;
        this.mimeType = mimeType;
        this.pageCount = pageCount;
        this.uploadedBy = uploadedBy;
        this.createdAt = Instant.now();
    }
}
