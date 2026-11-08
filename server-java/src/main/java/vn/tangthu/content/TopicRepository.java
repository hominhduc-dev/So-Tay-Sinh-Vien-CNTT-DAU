package vn.tangthu.content;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    record TopicRow(UUID id, String name, int sortOrder, long questionCount) {}

    @Query("""
            select new vn.tangthu.content.TopicRepository$TopicRow(t.id, t.name, t.sortOrder,
                (select count(q) from Question q where q.topic = t and q.active = true))
            from Topic t order by t.sortOrder, t.name
            """)
    List<TopicRow> listWithActiveCount();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
