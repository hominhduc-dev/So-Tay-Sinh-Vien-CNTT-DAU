package vn.tangthu.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuestionMatcherTest {

    @Test
    void chuanHoaBoDauVaTuHu() {
        assertThat(QuestionMatcher.keywords("Học kỳ này đăng ký tối đa bao nhiêu tín chỉ?"))
                .containsExactlyInAnyOrder("hoc", "ki", "dang", "toi", "da", "tin", "chi");
        // Từ có nghĩa không bị nhầm là từ hư sau khi bỏ dấu.
        assertThat(QuestionMatcher.keywords("đồ án, bảo lưu, đề tài")).contains("do", "an", "bao", "luu", "de", "tai");
        // Gõ không dấu cho cùng kết quả với gõ có dấu.
        assertThat(QuestionMatcher.keywords("dang ky toi da bao nhieu tin chi"))
                .containsAll(QuestionMatcher.keywords("đăng ký tối đa tín chỉ"));
        assertThat(QuestionMatcher.keywords("học kì")).isEqualTo(QuestionMatcher.keywords("HỌC KỲ"));
        assertThat(QuestionMatcher.keywords("Điểm rèn luyện")).contains("diem", "ren", "luien");
    }

    @Test
    void diemCaoKhiTrungTrongTam() {
        var q = QuestionMatcher.keywords("đăng ký tín chỉ");
        double dung = QuestionMatcher.score(q, QuestionMatcher.keywords("Đăng ký tối đa bao nhiêu tín chỉ?"),
                QuestionMatcher.keywords(""));
        double lech = QuestionMatcher.score(q, QuestionMatcher.keywords("Khi nào mở cổng đăng ký học phần?"),
                QuestionMatcher.keywords(""));
        assertThat(dung).isGreaterThan(lech);
    }
}
