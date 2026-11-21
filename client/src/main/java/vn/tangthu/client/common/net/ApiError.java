package vn.tangthu.client.common.net;

/** Lỗi server trả về ({@code "ok": false}) hoặc lỗi mạng ({@code NETWORK_ERROR}). */
public class ApiError extends RuntimeException {
    private final String code;

    public ApiError(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }

    public boolean isAuthFailure() { return "UNAUTHORIZED".equals(code); }
}
