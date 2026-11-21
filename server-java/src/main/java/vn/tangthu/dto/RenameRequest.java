package vn.tangthu.dto;

import jakarta.validation.constraints.Size;

public record RenameRequest(@Size(min = 3, max = 500) String title) {}
