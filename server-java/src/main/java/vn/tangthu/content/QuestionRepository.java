package vn.tangthu.content;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findByTopicIdAndActiveTrueOrderBySortOrderAscContentAsc(UUID topicId);

    @EntityGraph(attributePaths = {"document"})
    List<Question> findByTopicIdOrderBySortOrderAscContentAsc(UUID topicId);

    @EntityGraph(attributePaths = {"document"})
    Optional<Question> findByIdAndActiveTrue(UUID id);

    boolean existsByTopicId(UUID topicId);

    long countByDocumentId(UUID documentId);
}
