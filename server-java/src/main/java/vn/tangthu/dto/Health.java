package vn.tangthu.dto;

import java.time.Instant;

public record Health(String status, String database, long uptimeSeconds, Instant time) {}
