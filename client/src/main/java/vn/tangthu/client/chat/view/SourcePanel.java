package vn.tangthu.client.chat.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import vn.tangthu.client.common.view.Async;
import vn.tangthu.client.common.view.FileOpener;
import vn.tangthu.client.content.model.DownloadedFile;

/**
 * Cột xem nguồn bên phải khung hội thoại (UC08). Cột luôn có mặt với bề rộng cố định để bố cục không xê dịch:
 * chưa mở nguồn thì hiện hướng dẫn; bấm nguồn thì hiện trang PDF được trích dẫn, lật được sang trang khác.
 * Trang được vẽ bằng PDFBox ở luồng nền và co theo bề rộng cột.
 */
class SourcePanel extends JPanel {
    private static final String EMPTY = "empty";
    private static final String PAGE = "page";
    private static final String NAV = "nav";
    private static final String HINT = "hint";

    private final JLabel title = new JLabel();
    private final JButton close = new JButton("✕");
    private final JLabel pageInfo = new JLabel();
    private final JLabel citedBadge = new JLabel("Trang trích dẫn");
    private final JButton prev = new JButton("◀");
    private final JButton next = new JButton("▶");
    private final JButton external = new JButton("Mở bằng ứng dụng khác");
    private final CardLayout toolbarCards = new CardLayout();
    private final JPanel toolbar = new JPanel(toolbarCards);
    private final CardLayout bodyCards = new CardLayout();
    private final JPanel body = new JPanel(bodyCards);
    private final PageView view = new PageView();
    private final JScrollPane scroll = new JScrollPane(view);

    private DownloadedFile file;
    private int pageCount;
    private int citedPage;
    private int page;
    /** Tăng mỗi lần đổi trang/tài liệu; kết quả vẽ của yêu cầu cũ bị bỏ qua. */
    private int generation;

    SourcePanel() {
        super(new BorderLayout(0, 8));

        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 1f));
        close.setToolTipText("Đóng tài liệu");
        prev.setToolTipText("Trang trước");
        next.setToolTipText("Trang sau");
        citedBadge.setOpaque(true);
        citedBadge.setBackground(new Color(0xFFF4CE));
        citedBadge.setForeground(new Color(0x7A5B00));
        citedBadge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        JPanel head = new JPanel(new BorderLayout(6, 6));
        head.add(title, BorderLayout.CENTER);
        head.add(close, BorderLayout.EAST);

        // Hai thẻ cùng chiều cao: thanh lật trang khi có tài liệu, dòng chú thích khi chưa có — không xê dịch.
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        nav.add(prev);
        nav.add(pageInfo);
        nav.add(next);
        nav.add(external);
        nav.add(citedBadge);                         // đặt cuối: ẩn/hiện không đẩy các nút khác
        JLabel hint = new JLabel("Tài liệu dẫn chứng cho câu trả lời sẽ hiện ở đây.");
        hint.setForeground(Color.GRAY);
        toolbar.add(nav, NAV);
        toolbar.add(hint, HINT);
        head.add(toolbar, BorderLayout.SOUTH);
        add(head, BorderLayout.NORTH);

        JLabel empty = new JLabel("<html><div style='text-align:center'>Bấm vào dòng <b>Nguồn: …</b> dưới một câu "
                + "trả lời<br>để xem tài liệu gốc, mở đúng trang được trích dẫn.</div></html>", SwingConstants.CENTER);
        empty.setForeground(new Color(0x6B7785));
        empty.setOpaque(true);
        empty.setBackground(new Color(0xF7F9FB));
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.setBorder(null);
        body.add(empty, EMPTY);
        body.add(scroll, PAGE);
        body.setBorder(BorderFactory.createLineBorder(new Color(0xC9D3DF)));
        add(body, BorderLayout.CENTER);

        prev.addActionListener(e -> showPage(page - 1));
        next.addActionListener(e -> showPage(page + 1));
        external.addActionListener(e -> {
            if (file != null) FileOpener.open(this, file, null);
        });
        close.addActionListener(e -> clear());
        clear();
    }

    /** Bề rộng cố định theo tỉ lệ cửa sổ (không theo nội dung), để mở/đóng tài liệu không làm xê dịch bố cục. */
    @Override
    public Dimension getPreferredSize() {
        int parent = getParent() == null ? 1400 : getParent().getWidth();
        int w = Math.max(360, Math.min(760, Math.round(parent * 0.36f)));
        return new Dimension(w, super.getPreferredSize().height);
    }

    /** Về trạng thái chưa mở tài liệu. */
    void clear() {
        generation++;
        file = null;
        title.setText("Tài liệu nguồn");
        title.setToolTipText(null);
        close.setEnabled(false);
        toolbarCards.show(toolbar, HINT);
        bodyCards.show(body, EMPTY);
    }

    /** Trạng thái chờ trong lúc tải tệp qua socket. */
    void loading(String what) {
        generation++;
        file = null;
        title.setText(what);
        close.setEnabled(true);
        toolbarCards.show(toolbar, HINT);
        view.message("Đang tải tài liệu…");
        bodyCards.show(body, PAGE);
    }

    /** Hiện tài liệu PDF đã tải về, mở ở trang được trích dẫn. */
    void show(DownloadedFile file, int pageCount, int citedPage) {
        this.file = file;
        this.pageCount = pageCount;
        this.citedPage = Math.max(1, Math.min(citedPage, pageCount));
        String name = file.fileName().replaceFirst("(?i)\\.pdf$", "");
        title.setText(name);                         // một dòng, dài thì JLabel tự thêm "…"
        title.setToolTipText(name);
        close.setEnabled(true);
        toolbarCards.show(toolbar, NAV);
        bodyCards.show(body, PAGE);
        showPage(this.citedPage);
    }

    private void showPage(int target) {
        if (file == null || target < 1 || target > pageCount) return;
        page = target;
        int ticket = ++generation;
        pageInfo.setText("Trang " + page + " / " + pageCount);
        citedBadge.setVisible(page == citedPage);
        prev.setEnabled(page > 1);
        next.setEnabled(page < pageCount);
        byte[] content = file.content();
        Async.run(this, () -> CitationPdfViewer.renderPage(content, target), image -> {
            if (ticket != generation) return;
            view.image(image);
            scroll.getVerticalScrollBar().setValue(0);
        });
    }

    /** Vẽ ảnh trang co theo bề rộng khung cuộn, giữ tỉ lệ. */
    private static final class PageView extends JComponent implements Scrollable {
        private BufferedImage image;
        private String message = "";

        void image(BufferedImage image) {
            this.image = image;
            this.message = "";
            revalidate();
            repaint();
        }

        void message(String message) {
            this.image = null;
            this.message = message;
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int w = getParent() == null ? 400 : Math.max(getParent().getWidth(), 100);
            if (image == null) return new Dimension(w, 200);
            return new Dimension(w, Math.round((float) w * image.getHeight() / image.getWidth()));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (image != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.drawImage(image, 0, 0, getWidth(), getPreferredSize().height, null);
            } else {
                g2.setColor(Color.GRAY);
                int tw = g2.getFontMetrics().stringWidth(message);
                g2.drawString(message, Math.max(8, (getWidth() - tw) / 2), 40);
            }
            g2.dispose();
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return o == SwingConstants.VERTICAL ? r.height - 24 : r.width;
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
