package vn.tangthu.common.security;

import java.util.UUID;
import vn.tangthu.account.model.Role;

/** Người gọi hiện tại, lấy bằng {@code @AuthenticationPrincipal AuthUser me}. */
public record AuthUser(UUID id, Role role) {}
