package vn.tangthu.common;

import java.util.UUID;
import vn.tangthu.account.Role;

/** Người gọi hiện tại, lấy bằng {@code @AuthenticationPrincipal AuthUser me}. */
public record AuthUser(UUID id, Role role) {}
