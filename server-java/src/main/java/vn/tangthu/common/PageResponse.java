package vn.tangthu.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Response phân trang: {@code { items, page, size, total }}. */
public record PageResponse<T>(List<T> items, int page, int size, long total) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    /** Kiểm tra tham số phân trang: page ≥ 0, 1 ≤ size ≤ 100. */
    public static Pageable request(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > 100) {
            throw ApiException.validation("page ≥ 0 và 1 ≤ size ≤ 100.");
        }
        return PageRequest.of(page, size, sort);
    }
}
