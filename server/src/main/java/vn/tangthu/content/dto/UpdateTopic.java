package vn.tangthu.content.dto;

import jakarta.validation.constraints.Size;

public record UpdateTopic(@Size(min = 1, max = 100) String name, Integer sortOrder) {}
