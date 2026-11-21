package vn.tangthu.dto;

import vn.tangthu.model.Role;

public record UpdateUserRequest(Boolean active, Role role) {}
