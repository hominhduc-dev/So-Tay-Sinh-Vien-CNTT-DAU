package vn.tangthu.content;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @EntityGraph(attributePaths = {"uploadedBy"})
    Page<Document> findByTitleContainingIgnoreCase(String q, Pageable pageable);

    @EntityGraph(attributePaths = {"uploadedBy"})
    Page<Document> findAllBy(Pageable pageable);

    boolean existsByFileHash(String fileHash);
}
