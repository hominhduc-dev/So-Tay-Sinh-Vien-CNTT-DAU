package vn.tangthu.chat;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /** BR05: luôn lọc theo chủ hội thoại. */
    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    record Summary(UUID id, String title, java.time.Instant createdAt, long messageCount) {}

    @Query(value = """
            select new vn.tangthu.chat.ConversationRepository$Summary(c.id, c.title, c.createdAt, count(m))
            from Conversation c left join c.messages m
            where c.user.id = :userId
            group by c.id, c.title, c.createdAt
            """,
            countQuery = "select count(c) from Conversation c where c.user.id = :userId")
    Page<Summary> summaries(@Param("userId") UUID userId, Pageable pageable);
}
