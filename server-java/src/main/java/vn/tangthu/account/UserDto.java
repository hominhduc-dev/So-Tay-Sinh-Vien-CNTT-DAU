package vn.tangthu.account;

import java.util.UUID;

public record UserDto(UUID id, String code, String email, String fullName, Role role, String department,
                      boolean active) {
    public static UserDto of(User u) {
        return new UserDto(u.getId(), u.getCode(), u.getEmail(), u.getFullName(), u.getRole(),
                u.getDepartment(), u.isActive());
    }
}
