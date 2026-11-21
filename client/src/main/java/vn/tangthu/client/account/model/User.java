package vn.tangthu.client.account.model;

import java.util.UUID;

public record User(UUID id, String code, String email, String fullName, Role role, String department,
                   boolean active) {}
