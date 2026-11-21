package vn.tangthu.client.chat.model;

import java.time.Instant;
import java.util.UUID;

public record ConversationSummary(UUID id, String title, Instant createdAt, long messageCount) {}
