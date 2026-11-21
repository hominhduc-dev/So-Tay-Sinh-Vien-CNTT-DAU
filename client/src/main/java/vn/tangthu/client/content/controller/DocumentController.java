package vn.tangthu.client.content.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;
import vn.tangthu.client.common.net.ApiError;
import vn.tangthu.client.common.net.PageResult;
import vn.tangthu.client.common.net.SocketClient;
import vn.tangthu.client.content.model.Document;
import vn.tangthu.client.content.model.DownloadedFile;

/** M3 · Nội dung — kho tài liệu: UC08 mở tệp nguồn, UC10 tải lên, UC11 đổi tên, UC12 gỡ. */
public class DocumentController {
    /** Giống giới hạn server (DocumentService.MAX_BYTES): kiểm tra trước để khỏi gửi tệp quá lớn qua mạng. */
    public static final long MAX_BYTES = 20L * 1024 * 1024;

    private final SocketClient client;

    public DocumentController(SocketClient client) {
        this.client = client;
    }

    public PageResult<Document> list(String q, int page, int size) {
        return client.call("document.list", SocketClient.params("q", q, "page", page, "size", size),
                new TypeReference<PageResult<Document>>() {});
    }

    public DownloadedFile download(UUID id) {
        JsonNode d = client.call("document.download", SocketClient.params("id", id));
        return new DownloadedFile(d.path("fileName").asText(), d.path("mimeType").asText(),
                Base64.getDecoder().decode(d.path("contentBase64").asText()));
    }

    public Document upload(String title, Path file) {
        try {
            if (Files.size(file) > MAX_BYTES) throw new ApiError("FILE_TOO_LARGE", "Tệp vượt quá 20 MB.");
            String b64 = Base64.getEncoder().encodeToString(Files.readAllBytes(file));
            return client.call("document.upload", SocketClient.params("title", title,
                    "fileName", file.getFileName().toString(), "contentBase64", b64), Document.class);
        } catch (IOException e) {
            throw new ApiError("CLIENT_ERROR", "Không đọc được tệp: " + e.getMessage());
        }
    }

    public Document rename(UUID id, String title) {
        return client.call("document.rename", SocketClient.params("id", id, "title", title), Document.class);
    }

    public void delete(UUID id) {
        client.call("document.delete", SocketClient.params("id", id));
    }
}
