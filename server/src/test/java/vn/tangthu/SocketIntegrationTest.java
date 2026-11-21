package vn.tangthu;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import vn.tangthu.account.model.Role;
import vn.tangthu.account.model.User;
import vn.tangthu.account.repository.UserRepository;
import vn.tangthu.common.socket.FrameCodec;
import vn.tangthu.common.socket.SocketServer;

/**
 * Kiểm thử tích hợp qua TCP socket thật: mỗi ca mở kết nối tới {@link SocketServer} (cổng ngẫu nhiên)
 * và gửi khung [4 byte độ dài][JSON] như client desktop. Kịch bản theo docs/DAC-TA-TRIEN-KHAI.md mục 9.
 * Cần PostgreSQL: mặc định jdbc:postgresql://localhost:5432/tangthu_test (đổi bằng TEST_DATABASE_URL).
 */
@SpringBootTest(properties = "app.socket.port=0")
@ActiveProfiles("test")
class SocketIntegrationTest {
    static final String PWD = "Matkhau@123";

    @Autowired SocketServer server;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    User student, student2, teacher, admin;
    Client client;

    /** Client TCP tối giản: một kết nối, gửi yêu cầu rồi chờ đúng phản hồi của nó. */
    class Client implements AutoCloseable {
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;
        final AtomicInteger seq = new AtomicInteger();

        Client() throws Exception {
            socket = new Socket("localhost", server.getPort());
            socket.setSoTimeout(10_000);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        }

        JsonNode raw(byte[] payload) throws Exception {
            FrameCodec.write(out, payload);
            byte[] res = FrameCodec.read(in, 64 * 1024 * 1024);
            return res == null ? null : json.readTree(res);
        }

        JsonNode call(String action, String token, Map<String, ?> data) throws Exception {
            int id = seq.incrementAndGet();
            ObjectNode req = json.createObjectNode().put("id", id).put("action", action);
            if (token != null) req.put("token", token);
            if (data != null) req.set("data", json.valueToTree(data));
            JsonNode res = raw(json.writeValueAsBytes(req));
            assertThat(res.get("id").asInt()).as("phản hồi mang đúng id yêu cầu").isEqualTo(id);
            return res;
        }

        JsonNode ok(String action, String token, Map<String, ?> data) throws Exception {
            JsonNode res = call(action, token, data);
            assertThat(res.get("ok").asBoolean()).as(action + " → " + res).isTrue();
            return res.get("data");
        }

        void fails(String action, String token, Map<String, ?> data, String code) throws Exception {
            JsonNode res = call(action, token, data);
            assertThat(res.get("ok").asBoolean()).as(action + " phải lỗi").isFalse();
            assertThat(res.at("/error/code").asText()).as(action + " → " + res).isEqualTo(code);
        }

        @Override public void close() throws Exception { socket.close(); }
    }

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE messages, conversations, questions, topics, documents, users CASCADE");
        student = users.save(new User("SV1", "sv1@test", encoder.encode(PWD), "Sinh viên 1", Role.USER, "CNTT"));
        student2 = users.save(new User("SV2", "sv2@test", encoder.encode(PWD), "Sinh viên 2", Role.USER, "CNTT"));
        teacher = users.save(new User("GV1", "gv1@test", encoder.encode(PWD), "Giáo vụ 1", Role.CONTENT_ADMIN, "CNTT"));
        admin = users.save(new User("QT1", "qt1@test", encoder.encode(PWD), "Quản trị 1", Role.SYSTEM_ADMIN, "CNTT"));
        client = new Client();
    }

    @AfterEach
    void tearDown() throws Exception {
        client.close();
    }

    // ---------- helpers ----------

    String token(String login) throws Exception {
        return client.ok("auth.login", null, Map.of("login", login, "password", PWD)).get("token").asText();
    }

    static byte[] pdf(int pages) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) doc.addPage(new PDPage());
            doc.save(out);
            return out.toByteArray();
        }
    }

    static Map<String, Object> file(String title, String fileName, byte[] bytes) {
        return Map.of("title", title, "fileName", fileName, "contentBase64", Base64.getEncoder().encodeToString(bytes));
    }

    String upload(String token, String title, byte[] content) throws Exception {
        return client.ok("document.upload", token, file(title, "tai-lieu.pdf", content)).get("id").asText();
    }

    /** Tạo chủ đề + tài liệu 10 trang + một câu hỏi; trả {topicId, documentId, questionId}. */
    String[] content(String gv) throws Exception {
        String topicId = client.ok("topic.create", gv, Map.of("name", "Đăng ký học phần", "sortOrder", 1)).get("id").asText();
        String docId = upload(gv, "Quy chế đào tạo", pdf(10));
        String qId = client.ok("question.create", gv, Map.of("topicId", topicId,
                "content", "Tối đa bao nhiêu tín chỉ?", "answer", "Tối đa 25 tín chỉ mỗi học kỳ.",
                "documentId", docId, "page", 4)).get("id").asText();
        return new String[] {topicId, docId, qId};
    }

    // ---------- giao thức ----------

    @Test @DisplayName("P1. Nhiều yêu cầu trên một kết nối; ping công khai; id phản hồi khớp yêu cầu")
    void persistentConnection() throws Exception {
        for (int i = 0; i < 5; i++) assertThat(client.ok("system.ping", null, null).get("pong").asBoolean()).isTrue();
        token("SV1");
        client.ok("system.ping", null, null);
    }

    @Test @DisplayName("P2. JSON hỏng → BAD_FRAME; action lạ → UNKNOWN_ACTION; kết nối vẫn dùng tiếp được")
    void badRequests() throws Exception {
        JsonNode r = client.raw("{không phải json".getBytes(StandardCharsets.UTF_8));
        assertThat(r.at("/error/code").asText()).isEqualTo("BAD_FRAME");
        client.fails("khong.co", null, null, "UNKNOWN_ACTION");
        client.ok("system.ping", null, null);
    }

    @Test @DisplayName("P3. Khung vượt giới hạn → FRAME_TOO_LARGE rồi server đóng kết nối")
    void frameTooLarge() throws Exception {
        client.out.writeInt(64 * 1024 * 1024);   // khai báo 64 MB, quá giới hạn 32 MB
        client.out.flush();
        byte[] res = FrameCodec.read(client.in, 1024 * 1024);
        assertThat(json.readTree(res).at("/error/code").asText()).isEqualTo("FRAME_TOO_LARGE");
        assertThat(client.in.read()).as("server đã đóng kết nối").isEqualTo(-1);
    }

    @Test @DisplayName("P4. 10 client kết nối đồng thời, mỗi client đăng nhập và gửi 20 yêu cầu")
    void concurrentClients() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Callable<Integer>> jobs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            jobs.add(() -> {
                try (Client c = new Client()) {
                    String t = c.ok("auth.login", null, Map.of("login", "SV1", "password", PWD)).get("token").asText();
                    int n = 0;
                    for (int k = 0; k < 20; k++) {
                        c.ok("auth.me", t, null);
                        n++;
                    }
                    return n;
                }
            });
        }
        int total = 0;
        for (Future<Integer> f : pool.invokeAll(jobs)) total += f.get();
        pool.shutdown();
        assertThat(total).isEqualTo(200);
    }

    // ---------- nghiệp vụ ----------

    @Test @DisplayName("1. Đăng nhập: đúng → token; sai mật khẩu, bị khóa, khác khoa → UNAUTHORIZED")
    void login() throws Exception {
        token("SV1");
        token("sv1@test");
        client.fails("auth.login", null, Map.of("login", "SV1", "password", "sai"), "UNAUTHORIZED");
        users.save(new User("KHOA", "khoa@test", encoder.encode(PWD), "Khoa khác", Role.USER, "KIENTRUC"));
        client.fails("auth.login", null, Map.of("login", "KHOA", "password", PWD), "UNAUTHORIZED");
        jdbc.update("update users set is_active = false where code = 'SV2'");
        client.fails("auth.login", null, Map.of("login", "SV2", "password", PWD), "UNAUTHORIZED");
        client.fails("auth.me", null, null, "UNAUTHORIZED");
        client.fails("auth.login", null, Map.of("login", ""), "VALIDATION_ERROR");
    }

    @Test @DisplayName("2. Khóa tài khoản: token cũ bị từ chối ngay yêu cầu sau (BR02)")
    void lockTakesEffect() throws Exception {
        String sv = token("SV1"), qt = token("QT1");
        assertThat(client.ok("auth.me", sv, null).at("/user/role").asText()).isEqualTo("USER");
        client.ok("user.update", qt, Map.of("id", student.getId(), "active", false));
        client.fails("auth.me", sv, null, "UNAUTHORIZED");
    }

    @Test @DisplayName("3. Đổi vai USER → CONTENT_ADMIN có hiệu lực với token cũ")
    void promoteTakesEffect() throws Exception {
        String sv = token("SV1"), qt = token("QT1");
        client.fails("topic.create", sv, Map.of("name", "Thử"), "FORBIDDEN");
        client.ok("user.update", qt, Map.of("id", student.getId(), "role", "CONTENT_ADMIN"));
        client.ok("topic.create", sv, Map.of("name", "Thử"));
    }

    @Test @DisplayName("4. Quản trị viên tự khóa / tự hạ vai → SELF_MODIFICATION (BR04)")
    void selfModification() throws Exception {
        String qt = token("QT1");
        client.fails("user.update", qt, Map.of("id", admin.getId(), "active", false), "SELF_MODIFICATION");
        client.fails("user.update", qt, Map.of("id", admin.getId(), "role", "USER"), "SELF_MODIFICATION");
    }

    @Test @DisplayName("5. Sinh viên tải tài liệu → FORBIDDEN; giáo vụ xem tài khoản → FORBIDDEN")
    void permissions() throws Exception {
        String sv = token("SV1"), gv = token("GV1");
        client.fails("document.upload", sv, file("Tài liệu", "a.pdf", pdf(1)), "FORBIDDEN");
        client.fails("user.list", gv, null, "FORBIDDEN");
        assertThat(client.ok("user.list", token("QT1"), null).get("total").asInt()).isEqualTo(4);
    }

    @Test @DisplayName("6. Chọn câu hỏi → câu trả lời và nguồn khớp; tải tệp gốc qua socket")
    void chatAnswer() throws Exception {
        String[] c = content(token("GV1"));
        String sv = token("SV1");
        assertThat(client.ok("topic.list", sv, null).get(0).get("questionCount").asInt()).isEqualTo(1);
        JsonNode buttons = client.ok("topic.questions", sv, Map.of("topicId", c[0]));
        assertThat(buttons).hasSize(1);
        assertThat(buttons.get(0).get("id").asText()).isEqualTo(c[2]);
        JsonNode r = client.ok("chat.ask", sv, Map.of("questionId", c[2]));
        assertThat(r.at("/message/answer").asText()).isEqualTo("Tối đa 25 tín chỉ mỗi học kỳ.");
        assertThat(r.at("/message/source/documentId").asText()).isEqualTo(c[1]);
        assertThat(r.at("/message/source/page").asInt()).isEqualTo(4);
        String conv = r.get("conversationId").asText();
        client.ok("chat.ask", sv, Map.of("questionId", c[2], "conversationId", conv));
        JsonNode list = client.ok("conversation.list", sv, null);
        assertThat(list.get("items")).hasSize(1);
        assertThat(list.at("/items/0/messageCount").asInt()).isEqualTo(2);
        JsonNode f = client.ok("document.download", sv, Map.of("id", c[1]));
        byte[] bytes = Base64.getDecoder().decode(f.get("contentBase64").asText());
        assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(f.get("size").asInt()).isEqualTo(bytes.length);
    }

    @Test @DisplayName("7. Câu hỏi đã ẩn → QUESTION_NOT_FOUND, không còn hiện thành nút")
    void hiddenQuestion() throws Exception {
        String gv = token("GV1");
        String[] c = content(gv);
        client.ok("question.hide", gv, Map.of("id", c[2]));
        String sv = token("SV1");
        client.fails("chat.ask", sv, Map.of("questionId", c[2]), "QUESTION_NOT_FOUND");
        assertThat(client.ok("topic.questions", sv, Map.of("topicId", c[0]))).isEmpty();
    }

    @Test @DisplayName("8. Xem / ghi vào hội thoại của người khác → CONVERSATION_NOT_FOUND (BR05)")
    void otherConversation() throws Exception {
        String[] c = content(token("GV1"));
        String conv = client.ok("chat.ask", token("SV1"), Map.of("questionId", c[2])).get("conversationId").asText();
        String sv2 = token("SV2");
        client.fails("conversation.get", sv2, Map.of("id", conv), "CONVERSATION_NOT_FOUND");
        client.fails("chat.ask", sv2, Map.of("questionId", c[2], "conversationId", conv), "CONVERSATION_NOT_FOUND");
    }

    @Test @DisplayName("9. Tải cùng tệp hai lần → DUPLICATE_DOCUMENT; sai định dạng → UNSUPPORTED_FILE")
    void duplicateUpload() throws Exception {
        String gv = token("GV1");
        byte[] bytes = pdf(2);
        upload(gv, "Lần một", bytes);
        client.fails("document.upload", gv, file("Lần hai", "x.pdf", bytes), "DUPLICATE_DOCUMENT");
        client.fails("document.upload", gv, file("Văn bản", "x.txt", "xin chao".getBytes()), "UNSUPPORTED_FILE");
    }

    @Test @DisplayName("10. Gỡ tài liệu còn câu hỏi dẫn tới → DOCUMENT_IN_USE")
    void documentInUse() throws Exception {
        String gv = token("GV1");
        String[] c = content(gv);
        client.fails("document.delete", gv, Map.of("id", c[1]), "DOCUMENT_IN_USE");
        String free = upload(gv, "Tài liệu rời", pdf(3));
        client.ok("document.delete", gv, Map.of("id", free));
    }

    @Test @DisplayName("11. Xóa chủ đề còn câu hỏi → TOPIC_NOT_EMPTY; trùng tên → TOPIC_NAME_TAKEN")
    void topicNotEmpty() throws Exception {
        String gv = token("GV1");
        String[] c = content(gv);
        client.fails("topic.delete", gv, Map.of("id", c[0]), "TOPIC_NOT_EMPTY");
        client.fails("topic.create", gv, Map.of("name", "Đăng ký học phần"), "TOPIC_NAME_TAKEN");
    }

    @Test @DisplayName("12. Câu hỏi có page > số trang tài liệu → VALIDATION_ERROR")
    void pageOutOfRange() throws Exception {
        String gv = token("GV1");
        String[] c = content(gv);
        Map<String, Object> q = new HashMap<>(Map.of("topicId", c[0], "content", "Q", "answer", "A", "documentId", c[1], "page", 11));
        client.fails("question.create", gv, q, "VALIDATION_ERROR");
        client.ok("question.update", gv, Map.of("id", c[2], "page", 10));
    }

    @Test @DisplayName("13. Ẩn câu hỏi rồi xem lại hội thoại cũ → vẫn hiển thị")
    void historyKeepsHiddenQuestion() throws Exception {
        String gv = token("GV1");
        String[] c = content(gv);
        String sv = token("SV1");
        String conv = client.ok("chat.ask", sv, Map.of("questionId", c[2])).get("conversationId").asText();
        client.ok("question.hide", gv, Map.of("id", c[2]));
        JsonNode d = client.ok("conversation.get", sv, Map.of("id", conv));
        assertThat(d.get("messages")).hasSize(1);
        assertThat(d.at("/messages/0/answer").asText()).isEqualTo("Tối đa 25 tín chỉ mỗi học kỳ.");
    }

    @Test @DisplayName("14. Trạng thái hệ thống: quản trị viên UP; sinh viên FORBIDDEN")
    void health() throws Exception {
        JsonNode h = client.ok("system.health", token("QT1"), null);
        assertThat(h.get("status").asText()).isEqualTo("UP");
        assertThat(h.get("database").asText()).isEqualTo("UP");
        client.fails("system.health", token("SV1"), null, "FORBIDDEN");
    }

    @Test @DisplayName("UC04. Đổi mật khẩu: sai mật khẩu cũ → WRONG_PASSWORD; đúng → đăng nhập bằng mật khẩu mới")
    void changePassword() throws Exception {
        String sv = token("SV1");
        client.fails("auth.changePassword", sv, Map.of("currentPassword", "sai", "newPassword", "MatKhauMoi1"), "WRONG_PASSWORD");
        client.ok("auth.changePassword", sv, Map.of("currentPassword", PWD, "newPassword", "MatKhauMoi1"));
        client.ok("auth.login", null, Map.of("login", "SV1", "password", "MatKhauMoi1"));
    }
}
