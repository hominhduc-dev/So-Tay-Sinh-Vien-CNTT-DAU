package vn.tangthu.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.YearMonth;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lưu tệp tài liệu trong {@code app.storage-dir}. Tên tệp trên đĩa luôn là UUID ngẫu nhiên,
 * không dùng tên người dùng đặt, để tránh ghi đè và đường dẫn độc hại.
 */
@Component
public class FileStorage {
    private static final Logger log = LoggerFactory.getLogger(FileStorage.class);
    private final Path root;

    public FileStorage(AppProperties props) throws IOException {
        this.root = Path.of(props.storageDir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    /** Tệp tạm đã ghi xong kèm mã băm SHA-256 (hex, 64 ký tự). */
    public record Staged(Path tempFile, String sha256) {}

    public Staged stage(InputStream in) throws IOException {
        Path tmp = Files.createTempFile(root, "upload-", ".tmp");
        MessageDigest md = sha256();
        try (var din = new DigestInputStream(in, md)) {
            Files.copy(din, tmp, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
        return new Staged(tmp, HexFormat.of().formatHex(md.digest()));
    }

    /** Chuyển tệp tạm vào chỗ cố định {@code yyyy/MM/<uuid>.<ext>}; trả đường dẫn tương đối. */
    public String commit(Staged staged, String ext) throws IOException {
        YearMonth ym = YearMonth.now();
        String rel = "%d/%02d/%s.%s".formatted(ym.getYear(), ym.getMonthValue(), UUID.randomUUID(), ext);
        Path target = resolve(rel);
        Files.createDirectories(target.getParent());
        Files.move(staged.tempFile(), target, StandardCopyOption.ATOMIC_MOVE);
        return rel;
    }

    public void discard(Staged staged) {
        deleteQuietly(staged.tempFile());
    }

    public Path resolve(String relative) {
        Path p = root.resolve(relative).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Đường dẫn tệp nằm ngoài thư mục lưu trữ");
        }
        return p;
    }

    public void delete(String relative) {
        deleteQuietly(resolve(relative));
    }

    private static void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("Không xóa được tệp {}: {}", p, e.getMessage());
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }
}
