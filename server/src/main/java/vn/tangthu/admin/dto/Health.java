package vn.tangthu.admin.dto;

import java.time.Instant;

public record Health(String status, String database, long uptimeSeconds, Instant time) {}
