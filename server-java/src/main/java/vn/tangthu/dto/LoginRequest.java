package vn.tangthu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank @Size(max = 255) String login, @NotBlank @Size(max = 72) String password) {}
