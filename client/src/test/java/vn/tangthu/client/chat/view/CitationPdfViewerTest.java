package vn.tangthu.client.chat.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

class CitationPdfViewerTest {

    @Test
    void rendersTheRequestedOneBasedPage() throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(new PDRectangle(100, 100)));
            document.addPage(new PDPage(new PDRectangle(200, 100)));
            document.save(output);
            pdf = output.toByteArray();
        }

        var image = CitationPdfViewer.renderPage(pdf, 2);

        assertTrue(image.getWidth() > 250);
        assertTrue(image.getWidth() > image.getHeight());
    }

    @Test
    void countsPages() throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int i = 0; i < 3; i++) document.addPage(new PDPage());
            document.save(output);
            pdf = output.toByteArray();
        }

        assertEquals(3, CitationPdfViewer.pageCount(pdf));
    }

    @Test
    void rejectsPageNumbersOutsideThePdf() throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            pdf = output.toByteArray();
        }

        assertThrows(IOException.class, () -> CitationPdfViewer.renderPage(pdf, 2));
    }
}
