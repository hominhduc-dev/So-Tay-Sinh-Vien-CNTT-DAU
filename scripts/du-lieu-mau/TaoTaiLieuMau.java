import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

/**
 * Tạo 3 tài liệu PDF mẫu (tiếng Việt) rồi tải lên server qua TCP socket bằng tài khoản giáo vụ demo,
 * đúng như giáo vụ làm ở UC10 — server tự tính SHA-256, đếm trang và lưu tệp vào thư mục lưu trữ.
 * Tài liệu đã có (trùng tiêu đề) thì bỏ qua, nên chạy lại nhiều lần không sinh bản trùng.
 *
 * Chạy qua nap-du-lieu-mau.ps1. Tham số: [host] [port] [mã đăng nhập] [mật khẩu]
 */
public class TaoTaiLieuMau {
    static final String FOOTER = "Tài liệu minh họa cho đồ án — không phải văn bản chính thức của nhà trường";

    record Section(String heading, String... paragraphs) {}
    record Doc(String title, String fileName, List<Section> pages) {}

    static final List<Doc> DOCS = List.of(
        new Doc("[MẪU] Quy chế đào tạo theo tín chỉ", "quy-che-dao-tao-mau.pdf", List.of(
            new Section("Điều 1. Phạm vi và đối tượng áp dụng",
                "Quy chế mẫu này mô tả cách tổ chức đào tạo theo hệ thống tín chỉ cho sinh viên đại học chính quy ngành Công nghệ Thông tin.",
                "Sinh viên có trách nhiệm đọc kỹ quy chế, theo dõi thông báo của khoa và chủ động trao đổi với cố vấn học tập khi có thắc mắc.",
                "Các con số trong tài liệu chỉ dùng để minh họa cho hệ thống tra cứu; khi cần căn cứ chính thức, sinh viên đối chiếu văn bản do phòng Đào tạo ban hành."),
            new Section("Điều 2. Đăng ký khối lượng học tập",
                "Trong học kỳ chính, sinh viên đăng ký tối thiểu 14 tín chỉ và tối đa 25 tín chỉ. Học kỳ hè được đăng ký tối đa 12 tín chỉ.",
                "Sinh viên đang bị cảnh báo học vụ chỉ được đăng ký tối đa 14 tín chỉ trong học kỳ chính kế tiếp.",
                "Sinh viên phải hoàn thành học phần tiên quyết trước khi đăng ký học phần học sau. Kết quả đăng ký được xác nhận trên cổng thông tin đào tạo."),
            new Section("Điều 3. Rút bớt học phần, học lại và học cải thiện",
                "Sinh viên được rút bớt học phần đã đăng ký trong 2 tuần đầu của học kỳ. Học phần đã rút được ghi ký hiệu R và không tính vào điểm trung bình.",
                "Học phần bắt buộc bị điểm F phải đăng ký học lại. Với học phần tự chọn bị điểm F, sinh viên có thể học lại hoặc chọn học phần tự chọn khác cùng nhóm.",
                "Sinh viên đã đạt học phần được đăng ký học lại để cải thiện điểm; điểm cao nhất trong các lần học được dùng để tính điểm trung bình tích lũy."),
            new Section("Điều 4. Đánh giá học phần và thang điểm",
                "Điểm học phần gồm điểm chuyên cần, điểm giữa kỳ và điểm thi kết thúc học phần; trong đó điểm thi kết thúc chiếm ít nhất 50%.",
                "Điểm học phần chấm theo thang 10 rồi quy đổi sang điểm chữ: A từ 8,5 đến 10; B từ 7,0 đến 8,4; C từ 5,5 đến 6,9; D từ 4,0 đến 5,4; F dưới 4,0.",
                "Khi tính điểm trung bình, điểm chữ được quy đổi sang thang 4: A = 4, B = 3, C = 2, D = 1, F = 0."),
            new Section("Điều 5. Thi kết thúc học phần, hoãn thi và phúc khảo",
                "Sinh viên vắng thi có lý do chính đáng (ốm đau, tai nạn, việc gia đình đột xuất) nộp đơn kèm minh chứng cho khoa trong 3 ngày làm việc sau buổi thi để được dự thi bù.",
                "Vắng thi không có lý do chính đáng thì nhận điểm 0 cho lần thi đó.",
                "Sinh viên được nộp đơn phúc khảo bài thi trong 5 ngày làm việc kể từ ngày công bố điểm. Kết quả phúc khảo được thông báo trong 10 ngày làm việc."),
            new Section("Điều 6. Cảnh báo học vụ và buộc thôi học",
                "Sinh viên bị cảnh báo học vụ khi điểm trung bình học kỳ dưới 0,8 ở học kỳ đầu hoặc dưới 1,0 ở các học kỳ tiếp theo, hoặc khi tổng số tín chỉ bị điểm F vượt quá 24.",
                "Sinh viên bị cảnh báo học vụ hai học kỳ liên tiếp có thể bị xem xét buộc thôi học.",
                "Sinh viên nhận cảnh báo nên gặp cố vấn học tập để lập kế hoạch học tập, giảm khối lượng đăng ký và ưu tiên học lại các học phần bị điểm F."),
            new Section("Điều 7. Nghỉ học tạm thời và bảo lưu kết quả",
                "Sinh viên được nghỉ học tạm thời và bảo lưu kết quả khi được điều động vào lực lượng vũ trang, bị ốm đau phải điều trị dài ngày có xác nhận của cơ sở y tế, hoặc vì lý do cá nhân khác.",
                "Tổng thời gian nghỉ học tạm thời vì lý do cá nhân không quá 2 năm và không vượt quá thời gian tối đa của khóa học.",
                "Đơn xin nghỉ học tạm thời nộp cho khoa trước khi học kỳ bắt đầu. Khi muốn học tiếp, sinh viên nộp đơn trở lại học ít nhất 2 tuần trước học kỳ mới."),
            new Section("Điều 8. Xét và công nhận tốt nghiệp",
                "Sinh viên được công nhận tốt nghiệp khi tích lũy đủ số tín chỉ của chương trình, điểm trung bình tích lũy từ 2,0 trở lên (thang 4), đạt chuẩn đầu ra ngoại ngữ và tin học, có chứng chỉ Giáo dục quốc phòng – an ninh và hoàn thành Giáo dục thể chất.",
                "Hạng tốt nghiệp theo điểm trung bình tích lũy: Xuất sắc từ 3,60 đến 4,00; Giỏi từ 3,20 đến 3,59; Khá từ 2,50 đến 3,19; Trung bình từ 2,00 đến 2,49.",
                "Chuẩn đầu ra ngoại ngữ là điều kiện bắt buộc để được xét tốt nghiệp; sinh viên nộp minh chứng cho khoa trước thời hạn của mỗi đợt xét."))),

        new Doc("[MẪU] Hướng dẫn học phí, miễn giảm và học bổng", "hoc-phi-hoc-bong-mau.pdf", List.of(
            new Section("Mục 1. Cách tính học phí",
                "Học phí mỗi học kỳ bằng tổng số tín chỉ sinh viên đăng ký nhân với mức thu của một tín chỉ do nhà trường công bố cho từng năm học.",
                "Học phần học lại hoặc học cải thiện được tính học phí như học phần đăng ký mới.",
                "Sinh viên xem số tiền phải nộp trên cổng thông tin sau khi kết thúc đợt đăng ký học phần."),
            new Section("Mục 2. Thời hạn nộp và gia hạn học phí",
                "Học phí học kỳ chính nộp trong 4 tuần đầu của học kỳ; học kỳ hè nộp trước khi bắt đầu học.",
                "Sinh viên có hoàn cảnh khó khăn được làm đơn xin gia hạn trước thời hạn nộp; thời gian gia hạn tối đa 4 tuần.",
                "Sinh viên không nộp học phí đúng hạn và không được gia hạn có thể không được dự thi kết thúc học phần và bị khóa đăng ký học kỳ sau."),
            new Section("Mục 3. Miễn, giảm học phí",
                "Đối tượng được miễn hoặc giảm học phí gồm: con của người có công với cách mạng, sinh viên thuộc hộ nghèo hoặc cận nghèo, sinh viên khuyết tật, sinh viên mồ côi và các đối tượng khác theo quy định hiện hành.",
                "Hồ sơ gồm đơn đề nghị và giấy tờ chứng minh thuộc đối tượng, nộp cho phòng Công tác sinh viên trong 4 tuần đầu của học kỳ.",
                "Mỗi năm học sinh viên nộp lại giấy tờ còn hiệu lực để tiếp tục được hưởng chế độ."),
            new Section("Mục 4. Học bổng khuyến khích học tập",
                "Học bổng khuyến khích học tập xét theo từng học kỳ chính cho sinh viên đăng ký từ 15 tín chỉ trở lên, điểm trung bình học kỳ từ 3,2 (thang 4), điểm rèn luyện từ loại Tốt và không có học phần bị điểm F.",
                "Học bổng chia hai mức: loại Giỏi với điểm trung bình học kỳ từ 3,2 đến dưới 3,6 và loại Xuất sắc từ 3,6 trở lên.",
                "Học kỳ hè không thuộc diện xét học bổng khuyến khích học tập."),
            new Section("Mục 5. Học bổng tài trợ và hỗ trợ khó khăn",
                "Học bổng tài trợ do doanh nghiệp và các tổ chức trao qua nhà trường. Thông báo được đăng trên trang của khoa và gửi qua lớp trưởng các lớp.",
                "Mỗi học bổng tài trợ có tiêu chí riêng do nhà tài trợ đưa ra; sinh viên chuẩn bị đơn, bảng điểm và thư giới thiệu khi được yêu cầu.",
                "Sinh viên gặp khó khăn đột xuất có thể liên hệ phòng Công tác sinh viên để được xem xét hỗ trợ."))),

        new Doc("[MẪU] Hướng dẫn thực tập và đồ án tốt nghiệp ngành CNTT", "thuc-tap-do-an-mau.pdf", List.of(
            new Section("Phần 1. Điều kiện đi thực tập",
                "Sinh viên được đi thực tập tốt nghiệp khi đã tích lũy từ 100 tín chỉ trở lên và hoàn thành các học phần cơ sở ngành.",
                "Thực tập tốt nghiệp kéo dài 8 tuần, thường bố trí ở học kỳ 7.",
                "Khoa công bố danh sách sinh viên đủ điều kiện trước đợt thực tập ít nhất 4 tuần."),
            new Section("Phần 2. Chọn đơn vị thực tập",
                "Sinh viên được tự liên hệ đơn vị thực tập phù hợp với ngành Công nghệ Thông tin hoặc chọn từ danh sách đơn vị do khoa giới thiệu.",
                "Khi tự liên hệ, sinh viên nộp giấy tiếp nhận có xác nhận của đơn vị cho khoa trước ngày bắt đầu thực tập.",
                "Mỗi sinh viên được phân công một giảng viên theo dõi trong suốt thời gian thực tập."),
            new Section("Phần 3. Báo cáo và đánh giá thực tập",
                "Trong thời gian thực tập, sinh viên ghi nhật ký công việc hằng tuần và nộp báo cáo thực tập khi kết thúc.",
                "Điểm thực tập gồm 40% điểm đánh giá của đơn vị thực tập và 60% điểm của giảng viên chấm báo cáo.",
                "Báo cáo nộp chậm quá 1 tuần mà không có lý do chính đáng thì bị trừ điểm theo quy định của khoa."),
            new Section("Phần 4. Điều kiện nhận đồ án tốt nghiệp",
                "Sinh viên được giao đồ án tốt nghiệp khi điểm trung bình tích lũy từ 2,0 trở lên và số tín chỉ bị điểm F chưa trả nợ không quá 10.",
                "Sinh viên không đủ điều kiện làm đồ án sẽ học các học phần thay thế có tổng số tín chỉ tương đương theo danh sách khoa công bố.",
                "Danh sách sinh viên làm đồ án và học phần thay thế được công bố vào đầu học kỳ 8."),
            new Section("Phần 5. Đề cương, tiến độ và hồ sơ nộp đồ án",
                "Sinh viên đăng ký đề tài với giảng viên hướng dẫn và nộp đề cương trong 2 tuần đầu. Trong quá trình làm, sinh viên báo cáo tiến độ ít nhất 2 lần.",
                "Hồ sơ nộp đồ án gồm: quyển thuyết minh có chữ ký của giảng viên hướng dẫn, mã nguồn và hướng dẫn cài đặt, slide trình bày và bản điện tử của toàn bộ hồ sơ.",
                "Quyển thuyết minh được kiểm tra trùng lặp trước khi đưa ra hội đồng."),
            new Section("Phần 6. Bảo vệ và chấm điểm đồ án",
                "Mỗi sinh viên trình bày đồ án khoảng 15 phút, sau đó trả lời câu hỏi của hội đồng trong 10 đến 15 phút. Sinh viên nên chuẩn bị bản demo chạy được và phương án dự phòng.",
                "Điểm đồ án là trung bình có trọng số của điểm giảng viên hướng dẫn, điểm giảng viên phản biện và điểm hội đồng.",
                "Sinh viên không đạt được bảo vệ lại ở đợt kế tiếp theo thông báo của khoa.")))
    );

    public static void main(String[] args) throws Exception {
        System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true,
                StandardCharsets.UTF_8));
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;
        String login = args.length > 2 ? args[2] : "GV001";
        String password = args.length > 3 ? args[3] : "Matkhau@123";
        File font = new File(System.getenv().getOrDefault("SystemRoot", "C:\\Windows"), "Fonts\\arial.ttf");
        File fontBold = new File(font.getParentFile(), "arialbd.ttf");

        try (Client c = new Client(host, port)) {
            ObjectNode cred = c.json.createObjectNode().put("login", login).put("password", password);
            String token = c.call("auth.login", null, cred).path("token").asText();
            System.out.println("Đã đăng nhập " + login + " tại " + host + ":" + port);

            for (Doc d : DOCS) {
                JsonNode found = c.call("document.list", token, c.json.createObjectNode().put("q", d.title()).put("size", 50));
                boolean exists = false;
                for (JsonNode it : found.path("items")) exists |= d.title().equals(it.path("title").asText());
                if (exists) {
                    System.out.println("  bỏ qua (đã có): " + d.title());
                    continue;
                }
                byte[] pdf = render(d, font, fontBold);
                ObjectNode up = c.json.createObjectNode()
                        .put("title", d.title()).put("fileName", d.fileName())
                        .put("contentBase64", Base64.getEncoder().encodeToString(pdf));
                JsonNode doc = c.call("document.upload", token, up);
                System.out.println("  đã tải lên: " + d.title() + " (" + doc.path("pageCount").asText("?") + " trang)");
            }
        }
    }

    static byte[] render(Doc d, File font, File fontBold) throws Exception {
        try (PDDocument pdf = new PDDocument()) {
            PDType0Font regular = PDType0Font.load(pdf, font);
            PDType0Font bold = PDType0Font.load(pdf, fontBold);
            float margin = 60, width = PDRectangle.A4.getWidth() - 2 * margin;
            int n = d.pages().size();
            for (int i = 0; i < n; i++) {
                Section s = d.pages().get(i);
                PDPage page = new PDPage(PDRectangle.A4);
                pdf.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(pdf, page)) {
                    float y = PDRectangle.A4.getHeight() - margin;
                    y = text(cs, regular, 9, margin, y, width, d.title(), 12);
                    y -= 14;
                    y = text(cs, bold, 15, margin, y, width, s.heading(), 20);
                    y -= 10;
                    for (String p : s.paragraphs()) {
                        y = text(cs, regular, 11.5f, margin, y, width, p, 16);
                        y -= 10;
                    }
                    text(cs, regular, 8.5f, margin, 40, width, FOOTER + "   ·   Trang " + (i + 1) + "/" + n, 11);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            return out.toByteArray();
        }
    }

    /** Viết đoạn văn tự xuống dòng theo bề rộng; trả về tung độ sau đoạn. */
    static float text(PDPageContentStream cs, PDType0Font f, float size, float x, float y, float width,
                      String s, float leading) throws Exception {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String w : s.split(" ")) {
            String candidate = line.isEmpty() ? w : line + " " + w;
            if (f.getStringWidth(candidate) / 1000 * size > width && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(w);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        cs.beginText();
        cs.setFont(f, size);
        cs.setLeading(leading);
        cs.newLineAtOffset(x, y);
        for (String l : lines) {
            cs.showText(l);
            cs.newLine();
        }
        cs.endText();
        return y - leading * lines.size();
    }

    /** Khung: 4 byte độ dài big-endian + JSON UTF-8 (docs/DAC-TA-TRIEN-KHAI.md mục 6). */
    static final class Client implements AutoCloseable {
        final ObjectMapper json = new ObjectMapper();
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;
        int seq;

        Client(String host, int port) throws Exception {
            socket = new Socket(host, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        }

        JsonNode call(String action, String token, JsonNode data) throws Exception {
            ObjectNode req = json.createObjectNode().put("id", ++seq).put("action", action);
            if (token != null) req.put("token", token);
            req.set("data", data);
            byte[] payload = json.writeValueAsBytes(req);
            out.writeInt(payload.length);
            out.write(payload);
            out.flush();
            byte[] buf = new byte[in.readInt()];
            in.readFully(buf);
            JsonNode res = json.readTree(new String(buf, StandardCharsets.UTF_8));
            if (!res.path("ok").asBoolean()) {
                throw new IllegalStateException(action + " thất bại: " + res.path("error"));
            }
            return res.path("data");
        }

        @Override
        public void close() throws Exception {
            socket.close();
        }
    }
}
