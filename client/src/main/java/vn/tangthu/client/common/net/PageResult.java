package vn.tangthu.client.common.net;

import java.util.List;

/** Một trang kết quả — khớp với {@code PageResponse} của server. */
public record PageResult<T>(List<T> items, int page, int size, long total) {
    public int totalPages() {
        return size == 0 ? 0 : (int) ((total + size - 1) / size);
    }
}
