package vn.tangthu.client.chat.view;

import java.awt.image.BufferedImage;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/** Vẽ một trang PDF thành ảnh bằng PDFBox, để hiện nguồn trích dẫn ngay trong ứng dụng (UC08). */
final class CitationPdfViewer {
    /** Đủ nét khi co theo bề rộng khung xem nguồn trên màn hình độ phân giải cao. */
    static final int DPI = 150;

    private CitationPdfViewer() {}

    static int pageCount(byte[] content) throws IOException {
        try (PDDocument document = Loader.loadPDF(content)) {
            return document.getNumberOfPages();
        }
    }

    /** {@code pageNumber} tính từ 1, như số trang lưu trong câu hỏi. */
    static BufferedImage renderPage(byte[] content, int pageNumber) throws IOException {
        if (pageNumber < 1) {
            throw new IOException("Số trang trích dẫn không hợp lệ.");
        }
        try (PDDocument document = Loader.loadPDF(content)) {
            if (pageNumber > document.getNumberOfPages()) {
                throw new IOException("Trang trích dẫn không tồn tại trong tài liệu.");
            }
            return new PDFRenderer(document).renderImageWithDPI(pageNumber - 1, DPI, ImageType.RGB);
        }
    }
}
