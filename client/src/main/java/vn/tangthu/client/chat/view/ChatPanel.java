package vn.tangthu.client.chat.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import vn.tangthu.client.chat.controller.ChatController;
import vn.tangthu.client.chat.model.ConversationDetail;
import vn.tangthu.client.chat.model.ConversationSummary;
import vn.tangthu.client.chat.model.Message;
import vn.tangthu.client.common.view.Async;
import vn.tangthu.client.common.view.FileOpener;
import vn.tangthu.client.common.view.Ui;
import vn.tangthu.client.content.controller.DocumentController;
import vn.tangthu.client.content.controller.TopicController;
import vn.tangthu.client.content.model.DownloadedFile;
import vn.tangthu.client.content.model.QuestionButton;
import vn.tangthu.client.content.model.Topic;

/**
 * M2 · Hỏi đáp — bố cục ba cột cố định, mở/đóng tài liệu không làm xê dịch:
 * <ul>
 *   <li>Trái: lịch sử hội thoại (UC06).</li>
 *   <li>Giữa: các lượt hỏi – đáp; bên dưới là chọn chủ đề, rồi hàng nút câu hỏi cuộn ngang (UC05).</li>
 *   <li>Phải: cột nguồn — bấm vào nguồn để tải tệp gốc qua socket, PDF hiện đúng trang trích dẫn (UC08).</li>
 * </ul>
 */
public class ChatPanel extends JPanel {
    private final ChatController chat;
    private final TopicController topics;
    private final DocumentController documents;

    private final DefaultListModel<ConversationSummary> history = new DefaultListModel<>();
    private final JList<ConversationSummary> historyList = new JList<>(history);
    private final JEditorPane transcript = new JEditorPane("text/html", "");
    private final JLabel heading = new JLabel();
    private final JComboBox<Topic> topicBox = new JComboBox<>();
    private final JPanel questionButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JScrollPane questionScroll = new JScrollPane(questionButtons,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    private final SourcePanel source = new SourcePanel();
    private final StringBuilder html = new StringBuilder();
    private UUID conversationId;
    private boolean loadingHistory;

    public ChatPanel(ChatController chat, TopicController topics, DocumentController documents) {
        super(new BorderLayout(10, 10));
        this.chat = chat;
        this.topics = topics;
        this.documents = documents;
        Ui.pad(this, 10);

        add(buildHistory(), BorderLayout.WEST);
        JPanel conversation = buildTranscript();
        conversation.add(buildQuestions(), BorderLayout.SOUTH);
        add(conversation, BorderLayout.CENTER);
        add(source, BorderLayout.EAST);

        newConversation();
        reloadTopics();
        reloadHistory();
    }

    // ---------- trái: lịch sử ----------
    private JPanel buildHistory() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setPreferredSize(new Dimension(230, 0));
        JButton fresh = new JButton("+ Hội thoại mới");
        fresh.addActionListener(e -> {
            historyList.clearSelection();
            newConversation();
        });
        p.add(fresh, BorderLayout.NORTH);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                ConversationSummary c = (ConversationSummary) value;
                String text = "<html><b>" + Ui.esc(c.title()) + "</b><br><small>" + Ui.date(c.createdAt())
                        + " · " + c.messageCount() + " câu</small></html>";
                JLabel l = (JLabel) super.getListCellRendererComponent(list, text, index, selected, focus);
                l.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                return l;
            }
        });
        historyList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || loadingHistory) return;
            ConversationSummary c = historyList.getSelectedValue();
            if (c != null) openConversation(c.id());
        });
        JScrollPane s = new JScrollPane(historyList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        s.setBorder(BorderFactory.createTitledBorder("Lịch sử hỏi đáp"));
        p.add(s, BorderLayout.CENTER);
        return p;
    }

    // ---------- giữa: hội thoại ----------
    private JPanel buildTranscript() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD, heading.getFont().getSize2D() + 3f));
        p.add(heading, BorderLayout.NORTH);
        transcript.setEditable(false);
        transcript.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        transcript.addHyperlinkListener(e -> {
            if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) return;
            String[] parts = e.getDescription().split(":");      // doc:<uuid>:<trang>
            if (parts.length == 3 && parts[0].equals("doc")) {
                openSource(UUID.fromString(parts[1]), Integer.parseInt(parts[2]));
            }
        });
        p.add(new JScrollPane(transcript), BorderLayout.CENTER);
        return p;
    }

    // ---------- dưới khung chat: chủ đề ở trên, hàng nút câu hỏi cuộn ngang ở dưới ----------
    private JPanel buildQuestions() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Bấm vào câu hỏi"),
                BorderFactory.createEmptyBorder(2, 6, 4, 6)));

        JPanel topicRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topicRow.add(new JLabel("Chủ đề"));
        topicBox.setPreferredSize(new Dimension(320, topicBox.getPreferredSize().height));
        topicRow.add(topicBox);
        p.add(topicRow, BorderLayout.NORTH);
        topicBox.addActionListener(e -> reloadQuestions());

        // Một hàng nút đủ chữ; dài hơn bề ngang thì cuộn ngang (kéo thanh cuộn, lăn chuột hoặc Shift + lăn).
        // Chiều cao cố định = một nút + thanh cuộn ngang, để thanh cuộn hiện/ẩn cũng không đẩy bố cục.
        int buttonHeight = new JButton("Câu hỏi").getPreferredSize().height;
        int barHeight = questionScroll.getHorizontalScrollBar().getPreferredSize().height;
        questionScroll.setPreferredSize(new Dimension(0, buttonHeight + barHeight + 8));
        questionScroll.setBorder(null);
        questionScroll.getHorizontalScrollBar().setUnitIncrement(32);
        p.add(questionScroll, BorderLayout.CENTER);
        return p;
    }

    private void reloadTopics() {
        Async.run(this, topics::list, list -> {
            topicBox.removeAllItems();
            list.forEach(topicBox::addItem);
            if (list.isEmpty()) showButtons(List.of());
        });
    }

    private void reloadQuestions() {
        Topic t = (Topic) topicBox.getSelectedItem();
        if (t == null) return;
        Async.run(this, () -> topics.questions(t.id()), this::showButtons);
    }

    private void showButtons(List<QuestionButton> list) {
        questionButtons.removeAll();
        if (list.isEmpty()) questionButtons.add(new JLabel("Chủ đề này chưa có câu hỏi."));
        for (QuestionButton q : list) {
            JButton b = new JButton(q.content());
            b.addActionListener(e -> ask(q));
            questionButtons.add(b);
        }
        questionButtons.revalidate();
        questionButtons.repaint();
        SwingUtilities.invokeLater(() -> questionScroll.getHorizontalScrollBar().setValue(0));
    }

    // ---------- hành động ----------
    private void ask(QuestionButton q) {
        Async.run(this, () -> chat.ask(q.id(), conversationId), r -> {
            boolean isNew = conversationId == null;
            conversationId = r.conversationId();
            append(r.message());
            if (isNew) {
                heading.setText(q.content());
            }
            reloadHistory();
        }, err -> {
            Async.showError(this, err);
            // UC05 ngoại lệ 4a: câu hỏi vừa bị giáo vụ ẩn → tải lại các nút của chủ đề
            if ("QUESTION_NOT_FOUND".equals(err.getCode())) reloadQuestions();
        });
    }

    private void newConversation() {
        conversationId = null;
        heading.setText("Hội thoại mới");
        html.setLength(0);
        render("<p style='color:#666'>Chọn một chủ đề rồi bấm câu hỏi ở khung bên dưới. Mỗi câu trả lời do giáo vụ "
                + "khoa soạn sẵn, kèm tài liệu nguồn và số trang để bạn tự kiểm chứng.</p>");
    }

    private void openConversation(UUID id) {
        Async.run(this, () -> chat.conversation(id), (ConversationDetail d) -> {
            conversationId = d.id();
            heading.setText(d.title());
            html.setLength(0);
            d.messages().forEach(this::appendNoRender);
            render("");
        });
    }

    private void reloadHistory() {
        Async.run(this, () -> chat.conversations(0, 50), page -> {
            loadingHistory = true;
            history.clear();
            page.items().forEach(history::addElement);
            for (int i = 0; i < history.size(); i++) {
                if (history.get(i).id().equals(conversationId)) historyList.setSelectedIndex(i);
            }
            loadingHistory = false;
        });
    }

    /** UC08: PDF hiện ở cột nguồn, mở đúng trang trích dẫn; DOCX mở bằng ứng dụng ngoài. */
    private void openSource(UUID documentId, int page) {
        source.loading("Nguồn — trang " + page);
        Async.run(this, () -> {
            DownloadedFile file = documents.download(documentId);
            int pages = "application/pdf".equals(file.mimeType()) ? CitationPdfViewer.pageCount(file.content()) : 0;
            return new LoadedSource(file, pages);
        }, loaded -> {
            if (loaded.pageCount() > 0) {
                source.show(loaded.file(), loaded.pageCount(), page);
            } else {
                source.clear();
                FileOpener.open(this, loaded.file(), page);
            }
        }, err -> {
            source.clear();
            Async.showError(this, err);
        });
    }

    private record LoadedSource(DownloadedFile file, int pageCount) {}

    private void append(Message m) {
        appendNoRender(m);
        render("");
    }

    private void appendNoRender(Message m) {
        html.append("<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='right'>")
            .append("<table cellpadding='8' bgcolor='#1F4E79'><tr><td><font color='white'>")
            .append(Ui.esc(m.question().content())).append("</font></td></tr></table>")
            .append("</td></tr></table>")
            .append("<table width='100%' cellpadding='10' bgcolor='#F1F4F8' style='margin-top:6px;margin-bottom:14px'>")
            .append("<tr><td>").append(Ui.esc(m.answer()).replace("\n", "<br>"));
        if (m.source() != null) {
            html.append("<br><br><font color='#555'>Nguồn: </font><a href='doc:").append(m.source().documentId())
                .append(':').append(m.source().page()).append("'>").append(Ui.esc(m.source().title()))
                .append(" — trang ").append(m.source().page()).append("</a>");
        }
        html.append("<br><font size='2' color='#888'>").append(Ui.date(m.sentAt())).append("</font></td></tr></table>");
    }

    private void render(String extra) {
        transcript.setText("<html><body style='font-family:sans-serif;padding:6px'>" + html + extra + "</body></html>");
        SwingUtilities.invokeLater(() -> transcript.setCaretPosition(transcript.getDocument().getLength()));
    }
}
