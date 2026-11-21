package vn.tangthu.content.controller;

import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import vn.tangthu.account.model.Role;
import vn.tangthu.common.dto.PageResponse;
import vn.tangthu.common.exception.ApiException;
import vn.tangthu.common.socket.SocketController;
import vn.tangthu.content.dto.RenameRequest;
import vn.tangthu.content.service.DocumentService;

/**
 * M3 · Nội dung — tài liệu: UC07, UC08, UC10–UC12. Nội dung tệp đi trong JSON dạng base64.
 *
 * <pre>
 * document.list      {q?, page?, size?}                → {items, page, size, total}   mọi vai
 * document.get       {id}                              → document                     mọi vai
 * document.download  {id}                              → {fileName, mimeType, size, contentBase64}
 * document.upload    {title, fileName, contentBase64}  → document                     giáo vụ
 * document.rename    {id, title}                       → document                     giáo vụ
 * document.delete    {id}                              → {}                           giáo vụ
 * </pre>
 */
@Component
public class DocumentSocketController implements SocketController {
    private final DocumentService documents;

    public DocumentSocketController(DocumentService documents) {
        this.documents = documents;
    }

    @Override
    public void register(Routes routes) {
        routes.secured("document.list", Role.USER, r -> {
            String q = r.text("q");
            if (q != null && q.length() > 200) throw ApiException.validation("q tối đa 200 ký tự.");
            return documents.search(q, PageResponse.request(r.integer("page", 0), r.integer("size", 20),
                    Sort.by(Sort.Direction.DESC, "createdAt")));
        });
        routes.secured("document.get", Role.USER, r -> documents.get(r.uuid("id")));
        routes.secured("document.download", Role.USER, r -> {
            var f = documents.file(r.uuid("id"));
            byte[] bytes;
            try (InputStream in = f.resource().getInputStream()) {
                bytes = in.readAllBytes();
            }
            return Map.of("fileName", f.downloadName(), "mimeType", f.mimeType(), "size", bytes.length,
                    "contentBase64", Base64.getEncoder().encodeToString(bytes));
        });
        routes.secured("document.upload", Role.CONTENT_ADMIN, r -> {
            String title = r.text("title");
            if (title == null || title.trim().length() < 3 || title.trim().length() > 500) {
                throw ApiException.validation("Tiêu đề 3–500 ký tự.");
            }
            byte[] content;
            try {
                String b64 = r.text("contentBase64");
                content = Base64.getDecoder().decode(b64 == null ? "" : b64);
            } catch (IllegalArgumentException e) {
                throw ApiException.validation("contentBase64 không phải base64 hợp lệ.");
            }
            return documents.upload(r.me().id(), title, r.text("fileName"), content);
        });
        routes.secured("document.rename", Role.CONTENT_ADMIN, r -> {
            RenameRequest req = r.body(RenameRequest.class);
            if (req.title() == null || req.title().isBlank()) throw ApiException.validation("Tiêu đề 3–500 ký tự.");
            return documents.rename(r.uuid("id"), req.title());
        });
        routes.secured("document.delete", Role.CONTENT_ADMIN, r -> {
            documents.remove(r.uuid("id"));
            return Map.of();
        });
    }
}
