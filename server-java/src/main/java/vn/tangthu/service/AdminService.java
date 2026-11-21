package vn.tangthu.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tangthu.dto.PageResponse;
import vn.tangthu.dto.UserDto;
import vn.tangthu.exception.ApiException;
import vn.tangthu.model.Role;
import vn.tangthu.model.User;
import vn.tangthu.repository.UserRepository;

/** M4 · Quản trị tài khoản: UC14, UC15, UC16. */
@Service
public class AdminService {
    private final UserRepository users;

    public AdminService(UserRepository users) {
        this.users = users;
    }

    /** UC14 */
    @Transactional(readOnly = true)
    public PageResponse<UserDto> search(String q, Pageable pageable) {
        String k = q == null ? "" : q.trim();
        Page<User> page = k.isEmpty()
                ? users.findAll(pageable)
                : users.findByCodeContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                        k, k, k, pageable);
        return PageResponse.of(page, UserDto::of);
    }

    /** UC15 (active) và UC16 (role). BR04: không thao tác lên chính mình. */
    @Transactional
    public UserDto update(UUID actorId, UUID targetId, Boolean active, Role role) {
        if (active == null && role == null) {
            throw ApiException.validation("Cần ít nhất một trường: active hoặc role.");
        }
        if (actorId.equals(targetId)) {
            throw ApiException.badRequest("SELF_MODIFICATION", "Không được tự khóa hay tự đổi vai của chính mình.");
        }
        User u = users.findById(targetId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Không tìm thấy tài khoản."));
        if (active != null) {
            u.setActive(active);
        }
        if (role != null) {
            u.assignRole(role);
        }
        return UserDto.of(u);
    }
}
