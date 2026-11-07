package vn.tangthu.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lớp Topic: một nhóm nút câu hỏi trong khung chat. */
@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @Column(name = "sort_order")
    private int sortOrder;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "topic")
    @OrderBy("sortOrder")
    private List<Question> questions = new ArrayList<>();

    public Topic(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }

    /** +getQuestions(): Question[] — chỉ các câu hỏi đang hiện. */
    public List<Question> getQuestions() {
        return questions.stream().filter(Question::isActive).toList();
    }

    /** +addQuestion(in q: Question): void */
    public void addQuestion(Question q) {
        q.setTopic(this);
        questions.add(q);
    }
}
