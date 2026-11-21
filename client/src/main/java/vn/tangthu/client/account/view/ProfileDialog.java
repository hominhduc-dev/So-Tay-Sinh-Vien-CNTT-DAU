package vn.tangthu.client.account.view;

import java.awt.GridLayout;
import java.awt.Window;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import vn.tangthu.client.account.controller.AuthController;
import vn.tangthu.client.account.model.User;
import vn.tangthu.client.common.view.Async;

/** UC03 — xem thông tin cá nhân: gửi {@code auth.me}, hiện hồ sơ server trả về (luôn là dữ liệu mới nhất trong CSDL). */
public final class ProfileDialog {
    private ProfileDialog() {}

    public static void show(Window owner, AuthController auth) {
        Async.run(owner, auth::me, (User u) -> {
            JPanel p = new JPanel(new GridLayout(0, 2, 12, 8));
            row(p, "Mã", u.code());
            row(p, "Họ tên", u.fullName());
            row(p, "Email", u.email());
            row(p, "Vai", u.role().toString());
            row(p, "Khoa", u.department());
            row(p, "Trạng thái", u.active() ? "Đang hoạt động" : "Đã khóa");
            JOptionPane.showMessageDialog(owner, p, "Thông tin cá nhân", JOptionPane.PLAIN_MESSAGE);
        });
    }

    private static void row(JPanel p, String label, String value) {
        p.add(new JLabel(label));
        p.add(new JLabel("<html><b>" + vn.tangthu.client.common.view.Ui.esc(value) + "</b></html>"));
    }
}
