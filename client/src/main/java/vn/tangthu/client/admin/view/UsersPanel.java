package vn.tangthu.client.admin.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import vn.tangthu.client.account.model.Role;
import vn.tangthu.client.account.model.User;
import vn.tangthu.client.admin.controller.AdminController;
import vn.tangthu.client.common.view.Async;
import vn.tangthu.client.common.view.Ui;

/** UC14 tra cứu, UC15 khóa / mở, UC16 phân quyền. Quản trị viên không tự khóa / tự hạ vai (server chặn). */
public class UsersPanel extends JPanel {
    private static final int SIZE = 20;
    private final AdminController admin;
    private final UserTable model = new UserTable();
    private final JTable table = new JTable(model);
    private final JTextField search = new JTextField(22);
    private final JComboBox<Role> role = new JComboBox<>(Role.values());
    private final JLabel pageLabel = new JLabel();
    private int page;
    private int totalPages = 1;

    public UsersPanel(AdminController admin) {
        super(new BorderLayout(0, 8));
        this.admin = admin;
        Ui.pad(this, 10);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.add(new JLabel("Mã / email / họ tên"));
        bar.add(search);
        JButton find = new JButton("Tìm");
        bar.add(find);
        JButton toggle = new JButton("Khóa / Mở khóa");
        bar.add(toggle);
        bar.add(new JLabel("   Đổi vai thành"));
        bar.add(role);
        JButton apply = new JButton("Áp dụng");
        bar.add(apply);
        find.addActionListener(e -> load(0));
        search.addActionListener(e -> load(0));
        toggle.addActionListener(e -> toggle());
        apply.addActionListener(e -> changeRole());
        add(bar, BorderLayout.NORTH);

        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            User u = selectedOrNull();
            if (u != null) role.setSelectedItem(u.role());
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
        Async.run(this, () -> admin.users(q.isEmpty() ? null : q, p, SIZE), r -> {
            page = r.page();
            totalPages = Math.max(1, r.totalPages());
            model.set(r.items());
            pageLabel.setText("Trang " + (page + 1) + " / " + totalPages + " · " + r.total() + " tài khoản");
        });
    }

    private User selectedOrNull() {
        int row = table.getSelectedRow();
        return row < 0 ? null : model.rows.get(row);
    }

    private User selected() {
        User u = selectedOrNull();
        if (u == null) Ui.info(this, "Chọn một tài khoản trong bảng trước.");
        return u;
    }

    private void toggle() {
        User u = selected();
        if (u == null) return;
        String verb = u.active() ? "Khóa" : "Mở khóa";
        if (!Ui.confirm(this, verb + " tài khoản " + u.code() + " — " + u.fullName() + "?")) return;
        Async.run(this, () -> admin.setActive(u.id(), !u.active()), x -> load(page));
    }

    private void changeRole() {
        User u = selected();
        Role r = (Role) role.getSelectedItem();
        if (u == null || r == null || r == u.role()) return;
        Async.run(this, () -> admin.setRole(u.id(), r), x -> load(page));
    }

    private static final class UserTable extends AbstractTableModel {
        private static final String[] COLS = {"Mã", "Họ tên", "Email", "Vai", "Khoa", "Trạng thái"};
        private List<User> rows = new ArrayList<>();

        void set(List<User> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            User u = rows.get(r);
            return switch (c) {
                case 0 -> u.code();
                case 1 -> u.fullName();
                case 2 -> u.email();
                case 3 -> u.role();
                case 4 -> u.department();
                default -> u.active() ? "Đang hoạt động" : "Đã khóa";
            };
        }
    }
}
