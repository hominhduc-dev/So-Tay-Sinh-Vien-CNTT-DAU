package vn.tangthu.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Lớp User trong biểu đồ lớp: giữ cả vai trò (role) và đơn vị (department). */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String code;
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String department;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "created_at")
    private Instant createdAt;

    public User(String code, String email, String passwordHash, String fullName, Role role, String department) {
        this.code = code;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.department = department;
        this.active = true;
        this.createdAt = Instant.now();
    }

    /** +login(in pwd: string): boolean */
    public boolean login(String pwd, PasswordEncoder encoder) {
        return active && encoder.matches(pwd, passwordHash);
    }

    /** +assignRole(in role: Role): void */
    public void assignRole(Role role) {
        this.role = role;
    }

    /** BR01: đang hoạt động và thuộc khoa được phục vụ. */
    public boolean canUseSystem(String servedDepartment) {
        return active && servedDepartment.equalsIgnoreCase(department);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void changePassword(String newHash) {
        this.passwordHash = newHash;
    }
}
