package vn.tangthu.chat;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import vn.tangthu.content.Question;

/**
 * UC24 — tìm câu hỏi soạn sẵn gần nhất với câu sinh viên gõ. KHÔNG dùng AI: chấm điểm theo mức
 * trùng từ khóa sau khi bỏ dấu và bỏ các từ hư ("là", "gì", "bao nhiêu"…).
 *
 * <p>Điểm của một câu hỏi = 0,7 × (tỉ lệ từ khóa của câu gõ có mặt trong câu hỏi)
 * + 0,3 × (tỉ lệ từ khóa của câu hỏi có mặt trong câu gõ). Từ chỉ xuất hiện trong câu trả lời
 * được tính nửa điểm. Hệ số thứ hai giúp phân biệt câu hỏi ngắn, đúng trọng tâm với câu dài
 * chỉ tình cờ chứa vài từ trùng.
 */
@Component
public class QuestionMatcher {
    /** Từ trở lên ngưỡng này và bỏ xa câu thứ hai: trả lời ngay. */
    static final double ANSWER_THRESHOLD = 0.6;
    /** Khoảng cách tối thiểu giữa câu nhất và câu nhì để coi là khớp rõ. */
    static final double ANSWER_MARGIN = 0.1;
    /** Từ ngưỡng này: đưa vào danh sách "Có phải bạn muốn hỏi". */
    static final double SUGGEST_THRESHOLD = 0.25;
    static final int MAX_SUGGESTIONS = 3;

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");

    /**
     * Từ hư tiếng Việt, so trên chữ CÒN DẤU. Bỏ dấu trước rồi mới so thì đụng từ có nghĩa:
     * "đang" = "đăng" (ký), "để" = "đề" (tài), "do" = "đồ" (án), "bao" (nhiêu) = "bảo" (lưu).
     */
    private static final Set<String> STOP = Set.of(
            "là", "gì", "nào", "bao", "nhiêu", "mấy", "có", "không", "của", "cho", "thì", "và", "tôi", "em",
            "mình", "được", "phải", "như", "thế", "sao", "ở", "đâu", "khi", "một", "các", "những", "với", "để",
            "về", "trong", "hãy", "à", "ạ", "ơi", "nhé", "vậy", "bạn", "cần", "muốn", "hỏi", "ai", "nếu", "thôi",
            "lúc", "này", "đó", "đi", "làm", "sẽ", "đã", "đang", "bị", "bởi", "từ", "đến", "hay", "còn", "mà");

    /**
     * Dùng thêm khi người dùng gõ KHÔNG DẤU. Đã bỏ các từ trùng với từ có nghĩa khi mất dấu
     * (dang, de, do, bao, ma, toi, da, co, con, hoi, tu, the).
     */
    private static final Set<String> STOP_KHONG_DAU = Set.of(
            "la", "gi", "nao", "nhieu", "may", "khong", "cua", "cho", "thi", "va", "em", "minh", "duoc", "phai",
            "nhu", "sao", "dau", "khi", "mot", "cac", "nhung", "voi", "ve", "trong", "hay", "nhe", "vay", "ban",
            "can", "muon", "ai", "neu", "thoi", "luc", "nay", "di", "lam", "se", "bi", "boi", "den");

    public record Scored(Question question, double score) {}

    public enum Kind { ANSWERED, SUGGESTIONS, NO_MATCH }

    public record Result(Kind kind, Scored best, List<Scored> suggestions) {}

    public Result match(String typed, List<Question> candidates) {
        Set<String> q = keywords(typed);
        if (q.isEmpty()) {
            return new Result(Kind.NO_MATCH, null, List.of());
        }
        List<Scored> ranked = candidates.stream()
                .map(c -> new Scored(c, score(q, keywords(c.getContent()), keywords(c.getAnswer()))))
                .filter(s -> s.score() >= SUGGEST_THRESHOLD)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .toList();
        if (ranked.isEmpty()) {
            return new Result(Kind.NO_MATCH, null, List.of());
        }
        Scored best = ranked.get(0);
        double second = ranked.size() > 1 ? ranked.get(1).score() : 0;
        if (best.score() >= ANSWER_THRESHOLD && best.score() - second >= ANSWER_MARGIN) {
            return new Result(Kind.ANSWERED, best, ranked.subList(1, Math.min(ranked.size(), MAX_SUGGESTIONS)));
        }
        return new Result(Kind.SUGGESTIONS, null, ranked.subList(0, Math.min(ranked.size(), MAX_SUGGESTIONS)));
    }

    static double score(Set<String> query, Set<String> content, Set<String> answer) {
        double hit = 0;
        for (String w : query) {
            if (content.contains(w)) hit += 1;
            else if (answer.contains(w)) hit += 0.5;
        }
        double queryCoverage = hit / query.size();
        long contentHit = content.stream().filter(query::contains).count();
        double contentCoverage = content.isEmpty() ? 0 : (double) contentHit / content.size();
        return Math.round((0.7 * queryCoverage + 0.3 * contentCoverage) * 1000) / 1000.0;
    }

    /** Từ khóa: chữ thường, bỏ từ hư, bỏ dấu, đ → d, y → i (học kỳ = học kì). */
    static Set<String> keywords(String text) {
        if (text == null) return Set.of();
        Set<String> out = new HashSet<>();
        for (String w : NON_WORD.split(Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFC))) {
            if (w.isEmpty() || STOP.contains(w)) continue;
            String base = MARKS.matcher(Normalizer.normalize(w, Normalizer.Form.NFD)).replaceAll("").replace('đ', 'd');
            if (base.equals(w) && STOP_KHONG_DAU.contains(base)) continue;
            base = base.replace('y', 'i');
            if (base.length() >= 2 || Character.isDigit(base.charAt(0))) out.add(base);
        }
        return out;
    }
}
