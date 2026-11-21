package vn.tangthu.client.admin.model;

import java.time.Instant;

public record Health(String status, String database, long uptimeSeconds, Instant time) {}
