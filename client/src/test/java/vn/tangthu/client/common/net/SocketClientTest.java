package vn.tangthu.client.common.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Kiểm tra client với một server giả trên cổng ngẫu nhiên — không cần server thật. */
class SocketClientTest {

    @Test
    void guiNhanKhungVaBaoLoi() throws Exception {
        try (ServerSocket fake = new ServerSocket(0)) {
            Thread t = new Thread(() -> {
                try (Socket s = fake.accept();
                     var in = new DataInputStream(s.getInputStream());
                     var out = new DataOutputStream(s.getOutputStream())) {
                    for (int i = 0; i < 2; i++) {
                        JsonNode req = SocketClient.JSON.readTree(FrameCodec.read(in, 1 << 20));
                        int id = req.get("id").asInt();
                        String res = req.get("action").asText().equals("echo")
                                ? "{\"id\":" + id + ",\"ok\":true,\"data\":{\"nhan\":\"" + req.at("/data/chu").asText() + "\",\"token\":\"" + req.path("token").asText() + "\"}}"
                                : "{\"id\":" + id + ",\"ok\":false,\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"Không đủ quyền.\"}}";
                        FrameCodec.write(out, res.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                } catch (Exception ignored) {
                    // kết thúc server giả
                }
            });
            t.start();
            try (SocketClient client = new SocketClient("localhost", fake.getLocalPort())) {
                client.setToken("abc");
                JsonNode r = client.call("echo", Map.of("chu", "Xin chào — tiếng Việt có dấu"));
                assertEquals("Xin chào — tiếng Việt có dấu", r.get("nhan").asText());
                assertEquals("abc", r.get("token").asText());
                ApiError e = assertThrows(ApiError.class, () -> client.call("cam", null));
                assertEquals("FORBIDDEN", e.getCode());
            }
            t.join(2000);
        }
    }

    @Test
    void khongCoServerThiBaoLoiMang() {
        SocketClient client = new SocketClient("localhost", 1);
        ApiError e = assertThrows(ApiError.class, () -> client.call("system.ping", null));
        assertEquals("NETWORK_ERROR", e.getCode());
    }
}
