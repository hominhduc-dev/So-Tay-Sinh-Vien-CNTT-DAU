package vn.tangthu.client.content.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.AbstractTableModel;
import vn.tangthu.client.common.view.Async;
import vn.tangthu.client.common.view.Ui;
import vn.tangthu.client.content.controller.DocumentController;
import vn.tangthu.client.content.controller.QuestionController;
import vn.tangthu.client.content.controller.TopicController;
import vn.tangthu.client.content.model.Document;
import vn.tangthu.client.content.model.Question;
import vn.tangthu.client.content.model.Topic;

/** Giáo vụ soạn nội dung: chủ đề (UC22) và câu hỏi – câu trả lời gắn nguồn (UC23). */
public class ContentPanel extends JPanel {
    private final TopicController topics;
    private final QuestionController questions;
    private final DocumentController documents;

    private final DefaultListModel<Topic> topicModel = new DefaultListModel<>();
    private final JList<Topic> topicList = new JList<>(topicModel);
    private final QuestionTable questionModel = new QuestionTable();
    private final JTable questionTable = new JTable(questionModel);

    private final JTextField content = new JTextField();
    private final JTextArea answer = new JTextArea(6, 40);
    private final JComboBox<Document> document = new JComboBox<>();
    private final JSpinner page = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private final JSpinner order = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
    private final JCheckBox active = new JCheckBox("Hiển thị cho sinh viên", true);
    private final JLabel formTitle = new JLabel();
    private UUID editingId;

    public ContentPanel(TopicController topics, QuestionController questions, DocumentController documents) {
        super(new BorderLayout(10, 10));
        this.topics = topics;
        this.questions = questions;
        this.documents = documents;
        Ui.pad(this, 10);

        add(buildTopics(), BorderLayout.WEST);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildQuestions(), buildForm());
        split.setResizeWeight(0.45);
        add(split, BorderLayout.CENTER);

        reloadDocuments();
        reloadTopics(null);
        clearForm();
    }

    // ---------- chủ đề ----------
    private JPanel buildTopics() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setPreferredSize(new Dimension(240, 0));
        topicList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        topicList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                reloadQuestions();
                clearForm();
            }
        });
        JScrollPane s = new JScrollPane(topicList);
        s.setBorder(BorderFactory.createTitledBorder("Chủ đề"));
        p.add(s, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new java.awt.GridLayout(1, 3, 4, 0));
        JButton add = new JButton("Thêm");
        JButton edit = new JButton("Sửa");
        JButton del = new JButton("Xóa");
        add.addActionListener(e -> addTopic());
        edit.addActionListener(e -> editTopic());
        del.addActionListener(e -> deleteTopic());
        buttons.add(add);
        buttons.add(edit);
        buttons.add(del);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    private void reloadTopics(UUID select) {
        Topic current = topicList.getSelectedValue();
        UUID keep = select != null ? select : current == null ? null : current.id();
        Async.run(this, topics::list, list -> {
            topicModel.clear();
            list.forEach(topicModel::addElement);
            int idx = 0;
            for (int i = 0; i < list.size(); i++) if (list.get(i).id().equals(keep)) idx = i;
            if (!list.isEmpty()) topicList.setSelectedIndex(idx);
        });
    }

    private void addTopic() {
        TopicDialog.TopicInput input = TopicDialog.show(this, "Thêm chủ đề mới", null);
        if (input == null) return;
        Async.run(this, () -> topics.create(input.name(), input.sortOrder()), t -> reloadTopics(t.id()));
    }

    private void editTopic() {
        Topic t = topicList.getSelectedValue();
        if (t == null) {
            Ui.info(this, "Vui lòng chọn một chủ đề trong danh sách để sửa.");
            return;
        }
        TopicDialog.TopicInput input = TopicDialog.show(this, "Sửa chủ đề", t);
        if (input == null) return;
        Async.run(this, () -> topics.update(t.id(), input.name(), input.sortOrder()), x -> reloadTopics(t.id()));
    }

    private void deleteTopic() {
        Topic t = topicList.getSelectedValue();
        if (t == null) {
            Ui.info(this, "Vui lòng chọn một chủ đề trong danh sách để xóa.");
            return;
        }
        if (!Ui.confirm(this, "Xóa chủ đề \"" + t.name() + "\"?\n(Lưu ý: Chỉ xóa được chủ đề chưa có câu hỏi nào)")) {
            return;
        }
        Async.run(this, () -> {
            topics.delete(t.id());
            return null;
        }, x -> reloadTopics(null), err -> {
            if ("TOPIC_NOT_EMPTY".equals(err.getCode())) {
                Ui.error(this, "Chủ đề này đang chứa câu hỏi (kể cả câu hỏi đã ẩn).\nVui lòng xóa hoặc chuyển các câu hỏi sang chủ đề khác trước khi xóa chủ đề.");
            } else {
                Async.showError(this, err);
            }
        });
    }

    // ---------- câu hỏi ----------
    private JPanel buildQuestions() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        questionTable.setRowHeight(26);
        questionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        questionTable.getColumnModel().getColumn(0).setPreferredWidth(420);
        questionTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = questionTable.getSelectedRow();
            if (row >= 0) fillForm(questionModel.rows.get(row));
        });
        JScrollPane s = new JScrollPane(questionTable);
        s.setBorder(BorderFactory.createTitledBorder("Câu hỏi của chủ đề"));
        p.add(s, BorderLayout.CENTER);
        return p;
    }

    private void reloadQuestions() {
        Topic t = topicList.getSelectedValue();
        if (t == null) {
            questionModel.set(List.of());
            return;
        }
        Async.run(this, () -> questions.listForEditor(t.id()), questionModel::set);
    }

    private void reloadDocuments() {
        Async.run(this, () -> documents.list(null, 0, 100), r -> {
            document.removeAllItems();
            r.items().forEach(document::addItem);
        });
    }

    // ---------- biểu mẫu ----------
    private JPanel buildForm() {
        JPanel p = Ui.pad(new JPanel(new GridBagLayout()), 6);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 4;
        formTitle.setFont(formTitle.getFont().deriveFont(java.awt.Font.BOLD));
        p.add(formTitle, g);
        g.gridwidth = 1;

        g.gridy = 1; g.gridx = 0; g.weightx = 0; p.add(new JLabel("Câu hỏi"), g);
        g.gridx = 1; g.gridwidth = 3; g.weightx = 1; p.add(content, g);
        g.gridy = 2; g.gridx = 0; g.gridwidth = 1; g.weightx = 0;
        g.anchor = GridBagConstraints.NORTHWEST; p.add(new JLabel("Câu trả lời"), g);
        answer.setLineWrap(true);
        answer.setWrapStyleWord(true);
        g.gridx = 1; g.gridwidth = 3; g.weightx = 1; g.weighty = 1; g.fill = GridBagConstraints.BOTH;
        p.add(new JScrollPane(answer), g);
        g.weighty = 0; g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.WEST;

        g.gridy = 3; g.gridx = 0; g.gridwidth = 1; g.weightx = 0; p.add(new JLabel("Tài liệu nguồn"), g);
        g.gridx = 1; g.weightx = 1; p.add(document, g);
        g.gridx = 2; g.weightx = 0; p.add(new JLabel("Trang"), g);
        g.gridx = 3; p.add(page, g);

        g.gridy = 4; g.gridx = 0; p.add(new JLabel("Thứ tự"), g);
        g.gridx = 1; p.add(order, g);
        g.gridx = 2; g.gridwidth = 2; p.add(active, g);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton fresh = new JButton("Câu hỏi mới");
        JButton hide = new JButton("Ẩn câu hỏi");
        JButton save = new JButton("Lưu");
        fresh.addActionListener(e -> {
            questionTable.clearSelection();
            clearForm();
        });
        hide.addActionListener(e -> hideQuestion());
        save.addActionListener(e -> save());
        buttons.add(fresh);
        buttons.add(hide);
        buttons.add(save);
        g.gridy = 5; g.gridx = 0; g.gridwidth = 4; p.add(buttons, g);
        return p;
    }

    private void clearForm() {
        editingId = null;
        formTitle.setText("Thêm câu hỏi vào chủ đề đang chọn");
        content.setText("");
        answer.setText("");
        page.setValue(1);
        order.setValue(0);
        active.setSelected(true);
    }

    private void fillForm(Question q) {
        editingId = q.id();
        formTitle.setText("Sửa câu hỏi");
        content.setText(q.content());
        answer.setText(q.answer());
        page.setValue(q.page());
        order.setValue(q.sortOrder());
        active.setSelected(q.active());
        for (int i = 0; i < document.getItemCount(); i++) {
            if (document.getItemAt(i).id().equals(q.documentId())) document.setSelectedIndex(i);
        }
    }

    private void save() {
        Topic t = topicList.getSelectedValue();
        Document d = (Document) document.getSelectedItem();
        if (t == null) { Ui.info(this, "Chọn chủ đề trước."); return; }
        if (d == null) { Ui.info(this, "Mỗi câu trả lời phải có tài liệu nguồn — tải tài liệu lên trước."); return; }
        String c = content.getText().trim();
        String a = answer.getText().trim();
        int pg = (Integer) page.getValue();
        int so = (Integer) order.getValue();
        boolean on = active.isSelected();
        UUID id = editingId;
        Async.run(this, () -> id == null
                ? questions.create(t.id(), c, a, d.id(), pg, so, on)
                : questions.update(id, c, a, d.id(), pg, so, on), q -> {
            Ui.info(this, "Đã lưu câu hỏi.");
            reloadTopics(t.id());
        });
    }

    private void hideQuestion() {
        if (editingId == null) { Ui.info(this, "Chọn một câu hỏi trong bảng trước."); return; }
        UUID id = editingId;
        Async.run(this, () -> {
            questions.hide(id);
            return null;
        }, x -> reloadQuestions());
    }

    private static final class QuestionTable extends AbstractTableModel {
        private static final String[] COLS = {"Câu hỏi", "Tài liệu nguồn", "Trang", "Hiển thị"};
        private List<Question> rows = new ArrayList<>();

        void set(List<Question> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Question q = rows.get(r);
            return switch (c) {
                case 0 -> q.content();
                case 1 -> q.documentTitle();
                case 2 -> q.page();
                default -> q.active() ? "Có" : "Đã ẩn";
            };
        }
    }
}
