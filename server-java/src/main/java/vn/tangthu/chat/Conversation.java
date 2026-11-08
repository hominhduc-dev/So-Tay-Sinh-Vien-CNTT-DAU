package vn.tangthu.chat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.tangthu.account.User;
import vn.tangthu.content.Question;

/** Lớp Conversation. Hợp thành ◆ với Message: xóa hội thoại thì xóa tin nhắn. */
@Entity
@Table(name = "conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private String title;

    @Column(name = "created_at")
    private Instant createdAt;

    /** +getMessages(): Message[] — getter do Lombok sinh. */
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sentAt")
    private List<Message> messages = new ArrayList<>();

    /** Hội thoại mới, tiêu đề là nội dung câu hỏi đầu tiên. */
    public static Conversation start(User user, Question first) {
        Conversation c = new Conversation();
        c.user = user;
        c.title = first.getContent();
        c.createdAt = Instant.now();
        return c;
    }

    /** +ask(in q: Question): Message */
    public Message ask(Question q) {
        Message m = new Message(this, q);
        messages.add(m);
        return m;
    }
}
