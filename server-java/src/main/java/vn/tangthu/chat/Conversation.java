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

    /** Hội thoại mới, tiêu đề là câu hỏi đầu tiên (câu đã gõ, hoặc chữ trên nút). */
    public static Conversation start(User user, String title) {
        Conversation c = new Conversation();
        c.user = user;
        c.title = title.length() > 200 ? title.substring(0, 197) + "…" : title;
        c.createdAt = Instant.now();
        return c;
    }

    /** +ask(in q: Question): Message — sinh viên bấm nút câu hỏi. */
    public Message ask(Question q) {
        return ask(q, null);
    }

    /** Sinh viên gõ câu hỏi và hệ thống chọn được câu soạn sẵn khớp nhất. */
    public Message ask(Question q, String typedText) {
        Message m = new Message(this, q, typedText);
        messages.add(m);
        return m;
    }
}
