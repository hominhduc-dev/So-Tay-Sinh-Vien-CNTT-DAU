package vn.tangthu.account;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("select u from User u where u.code = :login or lower(u.email) = lower(:login)")
    Optional<User> findByLogin(@Param("login") String login);

    Page<User> findByCodeContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String code, String email, String fullName, Pageable pageable);
}
