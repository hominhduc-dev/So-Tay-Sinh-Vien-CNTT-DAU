package vn.tangthu.client.content.view;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import vn.tangthu.client.common.view.Ui;
import vn.tangthu.client.content.model.Topic;

/** Hộp thoại thêm / sửa chủ đề (UC22). */
public final class TopicDialog {
    public record TopicInput(String name, int sortOrder) {}

    private TopicDialog() {}

    /**
     * Mở hộp thoại nhập thông tin chủ đề.
     *
     * @param parent component cha để định vị dialog
     * @param title tiêu đề dialog ("Thêm chủ đề mới" hoặc "Sửa chủ đề")
     * @param current chủ đề hiện tại (nếu sửa) hoặc null (nếu thêm mới)
     * @return {@link TopicInput} nếu người dùng bấm OK và hợp lệ, hoặc null nếu hủy
     */
    public static TopicInput show(Component parent, String title, Topic current) {
        JTextField nameField = new JTextField(current != null ? current.name() : "", 22);
        int initialOrder = current != null ? current.sortOrder() : 0;
        JSpinner orderSpinner = new JSpinner(new SpinnerNumberModel(initialOrder, 0, 9999, 1));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 0;
        panel.add(new JLabel("Tên chủ đề:"), g);

        g.gridx = 1;
        g.weightx = 1.0;
        panel.add(nameField, g);

        g.gridx = 0;
        g.gridy = 1;
        g.weightx = 0;
        panel.add(new JLabel("Thứ tự hiển thị:"), g);

        g.gridx = 1;
        g.weightx = 1.0;
        panel.add(orderSpinner, g);

        int result = JOptionPane.showConfirmDialog(parent, panel, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            Ui.error(parent, "Tên chủ đề không được để trống.");
            return null;
        }
        if (name.length() > 100) {
            Ui.error(parent, "Tên chủ đề không được vượt quá 100 ký tự (hiện tại: " + name.length() + " ký tự).");
            return null;
        }

        int order = (Integer) orderSpinner.getValue();
        return new TopicInput(name, order);
    }
}
