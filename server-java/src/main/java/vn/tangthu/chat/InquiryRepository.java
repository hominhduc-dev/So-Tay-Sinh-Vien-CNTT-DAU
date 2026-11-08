package vn.tangthu.chat;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

    @EntityGraph(attributePaths = {"document"})
    Page<Inquiry> findByAskerId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"asker", "document"})
    Page<Inquiry> findByStatus(Inquiry.Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"asker", "document"})
    Page<Inquiry> findAllBy(Pageable pageable);

    long countByAskerIdAndStatus(UUID userId, Inquiry.Status status);

    long countByDocumentId(UUID documentId);
}
