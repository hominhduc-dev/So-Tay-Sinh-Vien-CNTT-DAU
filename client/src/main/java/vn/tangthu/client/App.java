package vn.tangthu.client;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.SwingUtilities;
import vn.tangthu.client.common.view.MainFrame;

/**
 * Điểm khởi động client desktop.
 *
 * <pre>
 * java -jar tangthu-client.jar                    # localhost:9090
 * java -jar tangthu-client.jar 192.168.1.10 9090  # máy chủ trong LAN
 * </pre>
 * Hoặc đặt biến môi trường {@code TANGTHU_HOST}, {@code TANGTHU_PORT}.
 */
public final class App {
    private App() {}

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : env("TANGTHU_HOST", "localhost");
        int port = Integer.parseInt(args.length > 1 ? args[1] : env("TANGTHU_PORT", "9090"));
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new MainFrame(host, port).setVisible(true));
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? fallback : v;
    }
}
