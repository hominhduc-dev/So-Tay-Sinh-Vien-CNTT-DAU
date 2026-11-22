package vn.tangthu.client.common.view;

import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import vn.tangthu.client.content.model.DownloadedFile;

/** Ghi tệp tải về vào thư mục tạm rồi mở bằng ứng dụng mặc định của máy (trình đọc PDF, Word…). */
public final class FileOpener {
    private FileOpener() {}

    public static void open(Component owner, DownloadedFile file, Integer page) {
        try {
            Path dir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "tangthu-client"));
            String safe = file.fileName().replaceAll("[\\\\/:*?\"<>|]", "_");
            Path target = dir.resolve(safe);
            Files.write(target, file.content());
            String where = page == null ? "" : "\nNội dung trích dẫn ở trang " + page + ".";
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(target.toFile());
                if (page != null) Ui.info(owner, "Đã mở \"" + file.fileName() + "\"." + where);
            } else {
                Ui.info(owner, "Đã lưu tệp tại:\n" + target + where);
            }
        } catch (IOException e) {
            Ui.error(owner, "Không mở được tệp: " + e.getMessage());
        }
    }
}
