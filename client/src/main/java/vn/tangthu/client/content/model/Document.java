package vn.tangthu.client.content.model;

import java.time.Instant;
import java.util.UUID;

public record Document(UUID id, String title, String mimeType, Integer pageCount, String uploadedBy,
                       Instant createdAt) {
    public String kind() {
        return mimeType != null && mimeType.contains("pdf") ? "PDF" : "DOCX";
    }

    @Override
    public String toString() {
        return title;
    }
}
