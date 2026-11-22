package vn.tangthu.client.chat.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.pdmodel.PDDocument;

final class CitationPdfViewer {
    private CitationPdfViewer() {}

    static BufferedImage renderPage(byte[] content, int pageNumber) throws IOException {
        if (pageNumber < 1) {
            throw new IOException("Số trang trích dẫn không hợp lệ.");
        }
        try (PDDocument document = Loader.loadPDF(content)) {
            if (pageNumber > document.getNumberOfPages()) {
                throw new IOException("Trang trích dẫn không tồn tại trong tài liệu.");
            }
            return new PDFRenderer(document).renderImageWithDPI(pageNumber - 1, 110, ImageType.RGB);
        }
    }

    static void show(Component owner, String fileName, int pageNumber, BufferedImage image) {
        JScrollPane page = new JScrollPane(new JLabel(new ImageIcon(image)));
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        page.setPreferredSize(new Dimension(Math.min(image.getWidth(), screen.width - 80),
                Math.min(image.getHeight(), screen.height - 120)));
        JOptionPane.showMessageDialog(owner, page, fileName + " — trang " + pageNumber,
                JOptionPane.PLAIN_MESSAGE);
    }
}
