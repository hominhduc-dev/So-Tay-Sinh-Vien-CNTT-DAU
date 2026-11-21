package vn.tangthu.client.account.view;

import java.awt.GridLayout;
import java.awt.Window;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import vn.tangthu.client.account.controller.AuthController;
import vn.tangthu.client.common.view.Async;
import vn.tangthu.client.common.view.Ui;

/** UC03 — đổi mật khẩu. */
public final class ChangePasswordDialog {
    private ChangePasswordDialog() {}

    public static void show(Window owner, AuthController auth) {
        JPasswordField current = new JPasswordField(18);
        JPasswordField next = new JPasswordField(18);
        JPasswordField again = new JPasswordField(18);
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Mật khẩu hiện tại"));
        p.add(current);
        p.add(new JLabel("Mật khẩu mới (≥ 8 ký tự)"));
        p.add(next);
        p.add(new JLabel("Nhập lại mật khẩu mới"));
        p.add(again);
        int ok = JOptionPane.showConfirmDialog(owner, p, "Đổi mật khẩu", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        String n = new String(next.getPassword());
        if (!n.equals(new String(again.getPassword()))) {
            Ui.error(owner, "Hai lần nhập mật khẩu mới không khớp.");
            return;
        }
        String c = new String(current.getPassword());
        Async.run(owner, () -> {
            auth.changePassword(c, n);
            return null;
        }, v -> Ui.info(owner, "Đã đổi mật khẩu."));
    }
}
