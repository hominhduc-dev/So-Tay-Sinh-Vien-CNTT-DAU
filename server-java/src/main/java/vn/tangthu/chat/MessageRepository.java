package vn.tangthu.chat;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
            select m from Message m join fetch m.question q join fetch q.document
            where m.conversation.id = :conversationId order by m.sentAt
            """)
    List<Message> findWithSource(@Param("conversationId") UUID conversationId);
}
