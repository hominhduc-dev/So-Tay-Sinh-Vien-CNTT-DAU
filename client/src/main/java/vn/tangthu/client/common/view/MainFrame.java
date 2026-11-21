package vn.tangthu.client.common.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import vn.tangthu.client.account.controller.AuthController;
import vn.tangthu.client.account.model.Role;
import vn.tangthu.client.account.model.Session;
import vn.tangthu.client.account.model.User;
import vn.tangthu.client.account.view.ChangePasswordDialog;
import vn.tangthu.client.account.view.LoginPanel;
import vn.tangthu.client.admin.controller.AdminController;
import vn.tangthu.client.admin.view.HealthPanel;
import vn.tangthu.client.admin.view.UsersPanel;
import vn.tangthu.client.chat.controller.ChatController;
import vn.tangthu.client.chat.view.ChatPanel;
import vn.tangthu.client.common.net.SocketClient;
import vn.tangthu.client.content.controller.DocumentController;
import vn.tangthu.client.content.controller.QuestionController;
import vn.tangthu.client.content.controller.TopicController;
import vn.tangthu.client.content.view.ContentPanel;
import vn.tangthu.client.content.view.DocumentsPanel;

/** Cửa sổ chính: màn đăng nhập ↔ các tab chức năng, hiện theo vai người dùng. */
public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final LoginPanel loginPanel;
    private JPanel mainPanel;
    private Session session;

    public MainFrame(String host, int port) {
        super("Sổ Tay Sinh Viên CNTT");
        loginPanel = new LoginPanel(host, port, this::showMain);
        root.add(loginPanel, "login");
        setContentPane(root);
        setSize(1240, 780);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Async.setOnSessionExpired(this::logout);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (session != null) session.client().close();
            }
        });
    }

    private void showMain(Session s) {
        this.session = s;
        SocketClient c = s.client();
        User u = s.user();
        AuthController auth = new AuthController(c);
        TopicController topics = new TopicController(c);
        DocumentController documents = new DocumentController(c);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Hỏi đáp", new ChatPanel(new ChatController(c), topics, documents));
        tabs.addTab("Tài liệu", new DocumentsPanel(documents, u.role().atLeast(Role.CONTENT_ADMIN)));
        if (u.role().atLeast(Role.CONTENT_ADMIN)) {
            tabs.addTab("Soạn nội dung", new ContentPanel(topics, new QuestionController(c), documents));
        }
        if (u.role().atLeast(Role.SYSTEM_ADMIN)) {
            AdminController admin = new AdminController(c);
            tabs.addTab("Tài khoản", new UsersPanel(admin));
            tabs.addTab("Hệ thống", new HealthPanel(admin, c.address()));
        }

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        header.add(Ui.title("Sổ Tay Sinh Viên CNTT"), BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.add(new JLabel("<html><b>" + Ui.esc(u.fullName()) + "</b> · " + u.role() + " · " + Ui.esc(u.code())
                + " &nbsp; <font color='#888'>máy chủ " + c.address() + "</font></html>"));
        JButton pw = new JButton("Đổi mật khẩu");
        JButton out = new JButton("Đăng xuất");
        pw.addActionListener(e -> ChangePasswordDialog.show(this, auth));
        out.addActionListener(e -> logout());
        right.add(pw);
        right.add(out);
        header.add(right, BorderLayout.EAST);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(tabs, BorderLayout.CENTER);
        root.add(mainPanel, "main");
        cards.show(root, "main");
        setTitle("Sổ Tay Sinh Viên CNTT — " + u.fullName());
    }

    /** UC02 — đăng xuất: đóng kết nối TCP, quay về màn đăng nhập. */
    public void logout() {
        if (session == null) return;
        session.client().close();
        session = null;
        if (mainPanel != null) root.remove(mainPanel);
        mainPanel = null;
        cards.show(root, "login");
        setTitle("Sổ Tay Sinh Viên CNTT");
        loginPanel.focusLogin();
    }
}
