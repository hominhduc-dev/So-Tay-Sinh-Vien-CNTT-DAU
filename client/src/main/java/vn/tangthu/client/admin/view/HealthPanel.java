package vn.tangthu.client.admin.view;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Duration;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import vn.tangthu.client.admin.controller.AdminController;
import vn.tangthu.client.admin.model.Health;
import vn.tangthu.client.common.view.Async;
import vn.tangthu.client.common.view.Ui;

/** UC20 — trạng thái hệ thống, kèm đo độ trễ khứ hồi của kết nối TCP. */
public class HealthPanel extends JPanel {
    private final AdminController admin;
    private final JLabel server = new JLabel("—");
    private final JLabel status = new JLabel("—");
    private final JLabel database = new JLabel("—");
    private final JLabel uptime = new JLabel("—");
    private final JLabel time = new JLabel("—");
    private final JLabel latency = new JLabel("—");

    public HealthPanel(AdminController admin, String address) {
        super(new FlowLayout(FlowLayout.LEFT));
        this.admin = admin;
        server.setText(address + "  (TCP, khung 4 byte độ dài + JSON)");
        JPanel grid = Ui.pad(new JPanel(new GridBagLayout()), 16);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 24);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        grid.add(Ui.title("Trạng thái hệ thống"), g);
        g.gridwidth = 1;
        row(grid, g, "Máy chủ", server);
        row(grid, g, "Trạng thái", status);
        row(grid, g, "Cơ sở dữ liệu", database);
        row(grid, g, "Thời gian chạy", uptime);
        row(grid, g, "Giờ máy chủ", time);
        row(grid, g, "Độ trễ khứ hồi (5 lần ping)", latency);
        JButton check = new JButton("Kiểm tra lại");
        check.addActionListener(e -> refresh());
        g.gridy++;
        g.gridx = 0;
        grid.add(check, g);
        add(grid);
        refresh();
    }

    private static void row(JPanel p, GridBagConstraints g, String label, JLabel value) {
        g.gridy++;
        g.gridx = 0;
        p.add(new JLabel(label), g);
        g.gridx = 1;
        p.add(value, g);
    }

    private void refresh() {
        Async.run(this, () -> {
            double sum = 0;
            for (int i = 0; i < 5; i++) sum += admin.pingMillis();
            return new Object[] {admin.health(), sum / 5};
        }, r -> {
            Health h = (Health) r[0];
            status.setText(h.status());
            database.setText(h.database());
            Duration d = Duration.ofSeconds(h.uptimeSeconds());
            uptime.setText(String.format("%d giờ %02d phút %02d giây", d.toHours(), d.toMinutesPart(), d.toSecondsPart()));
            time.setText(Ui.date(h.time()));
            latency.setText(String.format("%.2f ms", (Double) r[1]));
        });
    }
}
