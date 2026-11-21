package vn.tangthu.chat.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AskRequest(@NotNull UUID questionId, UUID conversationId) {}
