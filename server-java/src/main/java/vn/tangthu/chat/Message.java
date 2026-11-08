package vn.tangthu.chat;

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
import vn.tangthu.content.Question;

/** Lớp Message: một lượt chọn câu hỏi trong hội thoại. */
@Entity
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    /** +getQuestion(): Question — getter do Lombok sinh. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "sent_at")
    private Instant sentAt;

    /** Câu sinh viên đã gõ khi hệ thống trả lời bằng câu hỏi soạn sẵn khớp nhất (UC24); null khi bấm nút. */
    @Column(name = "typed_text")
    private String typedText;

    Message(Conversation conversation, Question question, String typedText) {
        this.conversation = conversation;
        this.question = question;
        this.typedText = typedText;
        this.sentAt = Instant.now();
    }
}
