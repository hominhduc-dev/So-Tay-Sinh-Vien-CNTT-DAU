package vn.tangthu.client.account.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import vn.tangthu.client.account.controller.AuthController;
import vn.tangthu.client.account.model.Session;
import vn.tangthu.client.common.net.SocketClient;
import vn.tangthu.client.common.view.Async;
import vn.tangthu.client.common.view.Ui;

/** UC01 — màn đăng nhập. Nhập luôn địa chỉ máy chủ để chạy được trong LAN. */
public class LoginPanel extends JPanel {
    private final JTextField host = new JTextField(14);
    private final JTextField port = new JTextField(5);
    private final JTextField login = new JTextField(20);
    private final JPasswordField password = new JPasswordField(20);
    private final JLabel status = new JLabel(" ");
    private final JButton submit = new JButton("Đăng nhập");

    public LoginPanel(String defaultHost, int defaultPort, Consumer<Session> onLoggedIn) {
        super(new GridBagLayout());
        host.setText(defaultHost);
        port.setText(String.valueOf(defaultPort));
        status.setForeground(new Color(0xB3261E));

        JPanel card = Ui.pad(new JPanel(new GridBagLayout()), 28);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        JLabel t = Ui.title("Sổ Tay Sinh Viên CNTT");
        t.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(t, g);
        g.gridy++;
        JLabel sub = new JLabel("Trường Đại học Kiến trúc Đà Nẵng — tra cứu quy định học vụ", SwingConstants.CENTER);
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN));
        card.add(sub, g);

        g.gridwidth = 1;
        row(card, g, "Mã SV / email", login);
        row(card, g, "Mật khẩu", password);
        JPanel server = new JPanel(new java.awt.BorderLayout(6, 0));
        server.add(host, java.awt.BorderLayout.CENTER);
        server.add(port, java.awt.BorderLayout.EAST);
        row(card, g, "Máy chủ (IP : cổng)", server);

        g.gridx = 0;
        g.gridy++;
        g.gridwidth = 2;
        card.add(submit, g);
        g.gridy++;
        card.add(status, g);
        add(card);

        submit.addActionListener(e -> submit(onLoggedIn));
        password.addActionListener(e -> submit(onLoggedIn));
        login.addActionListener(e -> password.requestFocusInWindow());
    }

    private static void row(JPanel p, GridBagConstraints g, String label, java.awt.Component field) {
        g.gridy++;
        g.gridx = 0;
        g.weightx = 0;
        p.add(new JLabel(label), g);
        g.gridx = 1;
        g.weightx = 1;
        p.add(field, g);
    }

    private void submit(Consumer<Session> onLoggedIn) {
        String user = login.getText().trim();
        String pass = new String(password.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            status.setText("Nhập tên đăng nhập và mật khẩu.");
            return;
        }
        int p;
        try {
            p = Integer.parseInt(port.getText().trim());
        } catch (NumberFormatException ex) {
            status.setText("Cổng phải là số.");
            return;
        }
        String h = host.getText().trim();
        submit.setEnabled(false);
        status.setText("Đang kết nối " + h + ":" + p + " …");
        Async.run(this, () -> {
            SocketClient client = new SocketClient(h, p);
            try {
                return new Session(client, new AuthController(client).login(user, pass).user());
            } catch (RuntimeException e) {
                client.close();
                throw e;
            }
        }, session -> {
            submit.setEnabled(true);
            status.setText(" ");
            password.setText("");
            onLoggedIn.accept(session);
        }, err -> {
            submit.setEnabled(true);
            status.setText(err.getMessage());
        });
    }

    public void focusLogin() {
        login.requestFocusInWindow();
    }
}
