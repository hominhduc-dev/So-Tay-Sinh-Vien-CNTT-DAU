package vn.tangthu.admin.dto;

import vn.tangthu.account.model.Role;

public record UpdateUserRequest(Boolean active, Role role) {}
