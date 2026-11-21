package vn.tangthu.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTopic(@NotBlank @Size(max = 100) String name, Integer sortOrder) {}
