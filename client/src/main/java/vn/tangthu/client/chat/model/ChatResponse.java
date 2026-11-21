package vn.tangthu.client.chat.model;

import java.util.UUID;

public record ChatResponse(UUID conversationId, Message message) {}
