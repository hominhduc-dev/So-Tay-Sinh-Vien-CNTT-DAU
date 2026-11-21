package vn.tangthu.security;

import java.util.UUID;
import vn.tangthu.model.Role;

/** Người gọi hiện tại, lấy bằng {@code @AuthenticationPrincipal AuthUser me}. */
public record AuthUser(UUID id, Role role) {}
