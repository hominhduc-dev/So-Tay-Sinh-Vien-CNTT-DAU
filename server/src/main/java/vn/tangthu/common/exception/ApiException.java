package vn.tangthu.common.exception;

/**
 * Lỗi nghiệp vụ. Bộ điều phối socket chuyển thành phản hồi
 * {@code {"ok": false, "error": {"code", "message"}}} gửi về client.
 */
public class ApiException extends RuntimeException {
    private final String code;

    public ApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(code, message);
    }
    public static ApiException validation(String message) {
        return new ApiException("VALIDATION_ERROR", message);
    }
    public static ApiException unauthorized(String message) {
        return new ApiException("UNAUTHORIZED", message);
    }
    public static ApiException forbidden(String message) {
        return new ApiException("FORBIDDEN", message);
    }
    public static ApiException notFound(String code, String message) {
        return new ApiException(code, message);
    }
    public static ApiException conflict(String code, String message) {
        return new ApiException(code, message);
    }
}
