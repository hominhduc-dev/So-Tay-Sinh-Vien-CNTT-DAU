package vn.tangthu.client.common.net;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kết nối TCP tới server. Giữ MỘT socket lâu dài; mỗi lần gọi gửi một khung yêu cầu rồi chờ đúng
 * khung phản hồi có cùng {@code id}.
 *
 * <pre>
 * yêu cầu  {"id": 7, "action": "chat.ask", "token": "eyJ…", "data": {…}}
 * phản hồi {"id": 7, "ok": true, "data": …}  hoặc  {"id": 7, "ok": false, "error": {"code", "message"}}
 * </pre>
 *
 * Các hàm {@code call} có {@code synchronized}: nhiều luồng giao diện dùng chung một kết nối mà
 * không lẫn khung của nhau. Mất kết nối thì lần gọi sau tự nối lại.
 */
public class SocketClient implements AutoCloseable {
    private static final int MAX_FRAME = 64 * 1024 * 1024;

    public static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final String host;
    private final int port;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private int seq;
    private volatile String token;

    public SocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String address() { return host + ":" + port; }

    public void setToken(String token) { this.token = token; }

    /** Gọi một action, trả phần {@code data} của phản hồi. */
    public synchronized JsonNode call(String action, Object data) {
        try {
            ensureConnected();
            int id = ++seq;
            ObjectNode req = JSON.createObjectNode().put("id", id).put("action", action);
            if (token != null) req.put("token", token);
            if (data != null) req.set("data", JSON.valueToTree(data));
            FrameCodec.write(out, JSON.writeValueAsBytes(req));
            byte[] frame = FrameCodec.read(in, MAX_FRAME);
            if (frame == null) throw new IOException("Server đã đóng kết nối");
            JsonNode res = JSON.readTree(frame);
            if (res.path("id").asInt(-1) != id) throw new IOException("Phản hồi lệch thứ tự");
            if (!res.path("ok").asBoolean()) {
                JsonNode e = res.path("error");
                throw new ApiError(e.path("code").asText("UNKNOWN"), e.path("message").asText("Lỗi không rõ."));
            }
            return res.get("data");
        } catch (SocketTimeoutException e) {
            disconnect();
            throw new ApiError("NETWORK_ERROR", "Server không phản hồi (" + address() + ").");
        } catch (IOException e) {
            disconnect();
            throw new ApiError("NETWORK_ERROR", "Không kết nối được máy chủ " + address() + ". " + e.getMessage());
        }
    }

    public <T> T call(String action, Object data, Class<T> type) {
        return convert(call(action, data), type);
    }

    public <T> T call(String action, Object data, TypeReference<T> type) {
        return JSON.convertValue(call(action, data), type);
    }

    public static <T> T convert(JsonNode node, Class<T> type) {
        return JSON.convertValue(node, type);
    }

    /** Dựng phần {@code data} từ các cặp khóa–giá trị; bỏ qua giá trị {@code null}. */
    public static Map<String, Object> params(Object... keyValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null) m.put((String) keyValues[i], keyValues[i + 1]);
        }
        return m;
    }

    private void ensureConnected() throws IOException {
        if (socket != null && !socket.isClosed()) return;
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), 5_000);   // bắt tay TCP, tối đa 5 giây
        s.setTcpNoDelay(true);
        s.setSoTimeout(60_000);                                  // chờ phản hồi tối đa 60 giây
        socket = s;
        in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
    }

    private void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
            // đã đóng
        }
        socket = null;
    }

    /** UC02 — đăng xuất: bỏ token và đóng kết nối TCP. */
    @Override
    public synchronized void close() {
        token = null;
        disconnect();
    }
}
