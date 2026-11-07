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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lớp Question: một nút câu hỏi, giữ câu trả lời soạn sẵn và nguồn (BR06). */
@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    private String content;

    /** +getAnswer(): string — getter do Lombok sinh. */
    private String answer;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id")
    private Document document;

    private int page;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "sort_order")
    private int sortOrder;

    public Question(String content, String answer, Document document, int page, int sortOrder) {
        this.content = content;
        this.answer = answer;
        this.document = document;
        this.page = page;
        this.sortOrder = sortOrder;
    }

    /** +getSource(): Document */
    public Document getSource() {
        return document;
    }

    public void setSource(Document document) {
        this.document = document;
    }
}
