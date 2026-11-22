package vn.tangthu.client.common.view;

import java.awt.Component;
import java.awt.Font;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/** Tiện ích giao diện dùng chung. */
public final class Ui {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private Ui() {}

    public static String date(Instant t) {
        return t == null ? "" : DATE.format(t);
    }

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, l.getFont().getSize2D() + 6f));
        return l;
    }

    public static <T extends JComponent> T pad(T c, int px) {
        c.setBorder(BorderFactory.createEmptyBorder(px, px, px, px));
        return c;
    }

    public static void info(Component owner, String message) {
        JOptionPane.showMessageDialog(owner, message, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component owner, String message) {
        JOptionPane.showMessageDialog(owner, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component owner, String message) {
        return JOptionPane.showConfirmDialog(owner, message, "Xác nhận",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }
}
