package vn.tangthu;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.tangthu.account.Role;
import vn.tangthu.account.User;
import vn.tangthu.account.UserRepository;

/**
 * Kiểm thử tích hợp theo docs/DAC-TA-TRIEN-KHAI.md mục 9.
 * Cần PostgreSQL: mặc định jdbc:postgresql://localhost:5432/tangthu_test (đổi bằng TEST_DATABASE_URL).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
    static final String PWD = "Matkhau@123";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    User student, student2, teacher, admin;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE messages, conversations, questions, topics, documents, users CASCADE");
        student = users.save(new User("SV1", "sv1@test", encoder.encode(PWD), "Sinh viên 1", Role.USER, "CNTT"));
        student2 = users.save(new User("SV2", "sv2@test", encoder.encode(PWD), "Sinh viên 2", Role.USER, "CNTT"));
        teacher = users.save(new User("GV1", "gv1@test", encoder.encode(PWD), "Giáo vụ 1", Role.CONTENT_ADMIN, "CNTT"));
        admin = users.save(new User("QT1", "qt1@test", encoder.encode(PWD), "Quản trị 1", Role.SYSTEM_ADMIN, "CNTT"));
    }

    // ---------- helpers ----------

    String token(String login) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("login", login, "password", PWD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return json.readTree(body).get("token").asText();
    }

    ResultActions call(MockHttpServletRequestBuilder req, String token, Object body) throws Exception {
        if (token != null) req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(req);
    }

    JsonNode read(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    static byte[] pdf(int pages) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) doc.addPage(new PDPage());
            doc.save(out);
            return out.toByteArray();
        }
    }

    String upload(String token, String title, byte[] content) throws Exception {
        var file = new MockMultipartFile("file", "tai-lieu.pdf", "application/pdf", content);
        var r = mvc.perform(multipart("/api/documents").file(file).param("title", title)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated());
        return read(r).get("id").asText();
    }

    /** Tạo chủ đề + tài liệu 10 trang + một câu hỏi; trả {topicId, documentId, questionId}. */
    String[] content(String gv) throws Exception {
        String topicId = read(call(post("/api/topics"), gv, Map.of("name", "Đăng ký học phần", "sortOrder", 1))
                .andExpect(status().isCreated())).get("id").asText();
        String docId = upload(gv, "Quy chế đào tạo", pdf(10));
        String qId = read(call(post("/api/questions"), gv, Map.of("topicId", topicId,
                        "content", "Tối đa bao nhiêu tín chỉ?", "answer", "Tối đa 25 tín chỉ mỗi học kỳ.",
                        "documentId", docId, "page", 4))
                .andExpect(status().isCreated())).get("id").asText();
        return new String[] {topicId, docId, qId};
    }

    // ---------- 14 kịch bản ----------

    @Test @DisplayName("1. Đăng nhập: đúng 200; sai mật khẩu, bị khóa, khác khoa 401")
    void login() throws Exception {
        token("SV1");
        token("sv1@test");
        call(post("/api/auth/login"), null, Map.of("login", "SV1", "password", "sai"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
        users.save(new User("KHOA", "khoa@test", encoder.encode(PWD), "Khoa khác", Role.USER, "KIENTRUC"));
        call(post("/api/auth/login"), null, Map.of("login", "KHOA", "password", PWD)).andExpect(status().isUnauthorized());
        jdbc.update("update users set is_active = false where code = 'SV2'");
        call(post("/api/auth/login"), null, Map.of("login", "SV2", "password", PWD)).andExpect(status().isUnauthorized());
        call(get("/api/auth/me"), null, null).andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("2. Khóa tài khoản: token cũ bị từ chối ngay request sau (BR02)")
    void lockTakesEffect() throws Exception {
        String sv = token("SV1"), qt = token("QT1");
        call(get("/api/auth/me"), sv, null).andExpect(status().isOk()).andExpect(jsonPath("$.user.role", is("USER")));
        call(patch("/api/users/" + student.getId()), qt, Map.of("active", false)).andExpect(status().isOk());
        call(get("/api/auth/me"), sv, null).andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("3. Đổi vai USER → CONTENT_ADMIN có hiệu lực với token cũ")
    void promoteTakesEffect() throws Exception {
        String sv = token("SV1"), qt = token("QT1");
        call(post("/api/topics"), sv, Map.of("name", "Thử")).andExpect(status().isForbidden());
        call(patch("/api/users/" + student.getId()), qt, Map.of("role", "CONTENT_ADMIN")).andExpect(status().isOk());
        call(post("/api/topics"), sv, Map.of("name", "Thử")).andExpect(status().isCreated());
    }

    @Test @DisplayName("4. Quản trị viên tự khóa / tự hạ vai → 400 SELF_MODIFICATION (BR04)")
    void selfModification() throws Exception {
        String qt = token("QT1");
        call(patch("/api/users/" + admin.getId()), qt, Map.of("active", false))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code", is("SELF_MODIFICATION")));
        call(patch("/api/users/" + admin.getId()), qt, Map.of("role", "USER"))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("5. Sinh viên tải tài liệu → 403; giáo vụ xem danh sách tài khoản → 403")
    void permissions() throws Exception {
        String sv = token("SV1"), gv = token("GV1");
        var file = new MockMultipartFile("file", "a.pdf", "application/pdf", pdf(1));
        mvc.perform(multipart("/api/documents").file(file).param("title", "Tài liệu")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sv))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
        call(get("/api/users"), gv, null).andExpect(status().isForbidden());
        call(get("/api/users"), token("QT1"), null).andExpect(status().isOk()).andExpect(jsonPath("$.total", is(4)));
    }

    @Test @DisplayName("6. Chọn câu hỏi → 201, câu trả lời và nguồn khớp")
    void chatAnswer() throws Exception {
        String[] c = content(token("GV1"));
        String sv = token("SV1");
        call(get("/api/topics"), sv, null).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionCount", is(1)));
        call(get("/api/topics/" + c[0] + "/questions"), sv, null)
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id", is(c[2])));
        JsonNode r = read(call(post("/api/chat"), sv, Map.of("questionId", c[2])).andExpect(status().isCreated()));
        org.assertj.core.api.Assertions.assertThat(r.at("/message/answer").asText()).isEqualTo("Tối đa 25 tín chỉ mỗi học kỳ.");
        org.assertj.core.api.Assertions.assertThat(r.at("/message/source/documentId").asText()).isEqualTo(c[1]);
        org.assertj.core.api.Assertions.assertThat(r.at("/message/source/page").asInt()).isEqualTo(4);
        String conv = r.get("conversationId").asText();
        call(post("/api/chat"), sv, Map.of("questionId", c[2], "conversationId", conv)).andExpect(status().isCreated());
        call(get("/api/conversations"), sv, null).andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].messageCount", is(2)));
        call(get("/api/documents/" + c[1] + "/file"), sv, null).andExpect(status().isOk());
    }

    @Test @DisplayName("7. Câu hỏi đã ẩn → 404 QUESTION_NOT_FOUND")
    void hiddenQuestion() throws Exception {
        String gv = token("GV1");
        String[] c = content(gv);
        call(delete("/api/questions/" + c[2]), gv, null).andExpect(status().isNoContent());
        call(post("/api/chat"), token("SV1"), Map.of("questionId", c[2]))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code", is("QUESTION_NOT_FOUND")));
        call(get("/api/topics/" + c[0] + "/questions"), token("SV1"), null).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test @DisplayName("8. Xem / ghi vào hội thoại của người khác → 404 (BR05)")
    void otherConversation() throws Exception {
        String[] c = content(token("GV1"));
        String conv = read(call(post("/api/chat"), token("SV1"), Map.of("questionId", c[2]))).get("conversationId").asText();
        String sv2 = token("SV2");
        call(get("/api/conversations/" + conv), sv2, null).andExpect(status().isNotFound());
        call(post("/api/chat"), sv2, Map.of("questionId", c[2], "conversationId", conv)).andExpect(status().isNotFound());
    }

    @Test @DisplayName("9. Tải lên cùng tệp hai lần → 409 DUPLICATE_DOCUMENT; sai định dạng → 415")
    void duplicateUpload() throws Exception {
        String gv = token("GV1");
        byte[] bytes = pdf(2);
        upload(gv, "Lần một", bytes);
        var again = new MockMultipartFile("file", "x.pdf", "application/pdf", bytes);
        mvc.perform(multipart("/api/documents").file(again).param("title", "Lần hai")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gv))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code", is("DUPLICATE_DOCUMENT")));
        var txt = new MockMultipartFile("file", "x.txt", "text/plain", "xin chao".getBytes());
        mvc.perform(multipart("/api/documents").file(txt).param("title", "Văn bản")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gv))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test @DisplayName("10. Gỡ tài liệu còn câu hỏi dẫn tới → 409 DOCUMENT_IN_USE")
    void documentInUse() throws Exception {
        String gv = token("GV1");
        String[] c = content(gv);
        call(delete("/api/documents/" + c[1]), gv, null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code", is("DOCUMENT_IN_USE")));
        String free = upload(gv, "Tài liệu rời", pdf(3));
        call(delete("/api/documents/" + free), gv, null).andExpect(status().isNoContent());
    }

    @Test @DisplayName("11. Xóa chủ đề còn câu hỏi → 409 TOPIC_NOT_EMPTY")
    void topicNotEmpty() throws Exception {
        String gv = token("GV1");
        String[] c = content(gv);
        call(delete("/api/topics/" + c[0]), gv, null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code", is("TOPIC_NOT_EMPTY")));
        call(post("/api/topics"), gv, Map.of("name", "Đăng ký học phần")).andExpect(status().isConflict());
    }

    @Test @DisplayName("12. Câu hỏi có page > số trang tài liệu → 400 VALIDATION_ERROR")
    void pageOutOfRange() throws Exception {
        String gv = token("GV1");
        String[] c = content(gv);
        call(post("/api/questions"), gv, Map.of("topicId", c[0], "content", "Q", "answer", "A",
                        "documentId", c[1], "page", 11))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code", is("VALIDATION_ERROR")));
        call(patch("/api/questions/" + c[2]), gv, Map.of("page", 10)).andExpect(status().isOk());
    }

    @Test @DisplayName("13. Ẩn câu hỏi rồi xem lại hội thoại cũ → vẫn hiển thị")
    void historyKeepsHiddenQuestion() throws Exception {
        String gv = token("GV1");
        String[] c = content(gv);
        String sv = token("SV1");
        String conv = read(call(post("/api/chat"), sv, Map.of("questionId", c[2]))).get("conversationId").asText();
        call(delete("/api/questions/" + c[2]), gv, null).andExpect(status().isNoContent());
        call(get("/api/conversations/" + conv), sv, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages[0].answer", is("Tối đa 25 tín chỉ mỗi học kỳ.")));
    }

    @Test @DisplayName("14. Trạng thái hệ thống: quản trị viên 200 UP; sinh viên 403")
    void health() throws Exception {
        call(get("/api/health"), token("QT1"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP"))).andExpect(jsonPath("$.database", is("UP")));
        call(get("/api/health"), token("SV1"), null).andExpect(status().isForbidden());
    }

    @Test @DisplayName("UC04. Đổi mật khẩu: sai mật khẩu cũ 400; đúng 204 và đăng nhập bằng mật khẩu mới")
    void changePassword() throws Exception {
        String sv = token("SV1");
        call(put("/api/auth/password"), sv, Map.of("currentPassword", "sai", "newPassword", "MatKhauMoi1"))
                .andExpect(status().isBadRequest());
        call(put("/api/auth/password"), sv, Map.of("currentPassword", PWD, "newPassword", "MatKhauMoi1"))
                .andExpect(status().isNoContent());
        call(post("/api/auth/login"), null, Map.of("login", "SV1", "password", "MatKhauMoi1")).andExpect(status().isOk());
    }
}
