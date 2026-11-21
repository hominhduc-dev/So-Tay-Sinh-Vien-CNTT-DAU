package vn.tangthu.client.content.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import vn.tangthu.client.common.net.PageResult;
import vn.tangthu.client.common.view.Async;
import vn.tangthu.client.common.view.FileOpener;
import vn.tangthu.client.common.view.Ui;
import vn.tangthu.client.content.controller.DocumentController;
import vn.tangthu.client.content.model.Document;

/** Kho tài liệu. Sinh viên: tìm và mở. Giáo vụ: thêm tải lên (UC10), đổi tên (UC11), gỡ (UC12). */
public class DocumentsPanel extends JPanel {
    private static final int SIZE = 20;
    private final DocumentController documents;
    private final DocumentTable model = new DocumentTable();
    private final JTable table = new JTable(model);
    private final JTextField search = new JTextField(24);
    private final JLabel pageLabel = new JLabel();
    private int page;
    private int totalPages;

    public DocumentsPanel(DocumentController documents, boolean canEdit) {
        super(new BorderLayout(0, 8));
        this.documents = documents;
        Ui.pad(this, 10);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.add(new JLabel("Tìm theo tiêu đề"));
        bar.add(search);
        JButton find = new JButton("Tìm");
        bar.add(find);
        JButton open = new JButton("Mở tài liệu");
        bar.add(open);
        find.addActionListener(e -> load(0));
        search.addActionListener(e -> load(0));
        open.addActionListener(e -> openSelected());
        if (canEdit) {
            JButton upload = new JButton("Tải lên…");
            JButton rename = new JButton("Đổi tên…");
            JButton remove = new JButton("Gỡ");
            upload.addActionListener(e -> upload());
            rename.addActionListener(e -> rename());
            remove.addActionListener(e -> remove());
            bar.add(upload);
            bar.add(rename);
            bar.add(remove);
        }
        add(bar, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(0).setPreferredWidth(420);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) openSelected();
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel paging = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton prev = new JButton("‹ Trước");
        JButton next = new JButton("Sau ›");
        prev.addActionListener(e -> { if (page > 0) load(page - 1); });
        next.addActionListener(e -> { if (page + 1 < totalPages) load(page + 1); });
        paging.add(prev);
        paging.add(pageLabel);
        paging.add(next);
        add(paging, BorderLayout.SOUTH);

        load(0);
    }

    private void load(int p) {
        String q = search.getText().trim();
        Async.run(this, () -> documents.list(q.isEmpty() ? null : q, p, SIZE), (PageResult<Document> r) -> {
            page = r.page();
            totalPages = Math.max(1, r.totalPages());
            model.set(r.items());
            pageLabel.setText("Trang " + (page + 1) + " / " + totalPages + " · " + r.total() + " tài liệu");
        });
    }

    private Document selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Ui.info(this, "Chọn một tài liệu trong bảng trước.");
            return null;
        }
        return model.rows.get(table.convertRowIndexToModel(row));
    }

    private void openSelected() {
        Document d = selected();
        if (d != null) Async.run(this, () -> documents.download(d.id()), f -> FileOpener.open(this, f, null));
    }

    private void upload() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("PDF hoặc Word (*.pdf, *.docx)", "pdf", "docx"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path file = fc.getSelectedFile().toPath();
        String name = file.getFileName().toString();
        String title = JOptionPane.showInputDialog(this, "Tiêu đề tài liệu (3–500 ký tự)",
                name.replaceFirst("\\.[^.]+$", ""));
        if (title == null) return;
        Async.run(this, () -> documents.upload(title.trim(), file), d -> {
            Ui.info(this, "Đã tải lên \"" + d.title() + "\"" + (d.pageCount() == null ? "." : " — " + d.pageCount() + " trang."));
            load(0);
        });
    }

    private void rename() {
        Document d = selected();
        if (d == null) return;
        String title = JOptionPane.showInputDialog(this, "Tiêu đề mới", d.title());
        if (title == null || title.isBlank()) return;
        Async.run(this, () -> documents.rename(d.id(), title.trim()), x -> load(page));
    }

    private void remove() {
        Document d = selected();
        if (d == null || !Ui.confirm(this, "Gỡ tài liệu \"" + d.title() + "\"?")) return;
        Async.run(this, () -> {
            documents.delete(d.id());
            return null;
        }, x -> load(page));
    }

    private static final class DocumentTable extends AbstractTableModel {
        private static final String[] COLS = {"Tiêu đề", "Loại", "Số trang", "Người tải lên", "Ngày tải"};
        private List<Document> rows = new ArrayList<>();

        void set(List<Document> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Document d = rows.get(r);
            return switch (c) {
                case 0 -> d.title();
                case 1 -> d.kind();
                case 2 -> d.pageCount() == null ? "—" : d.pageCount();
                case 3 -> d.uploadedBy();
                default -> Ui.date(d.createdAt());
            };
        }
    }
}
