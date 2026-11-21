package vn.tangthu.common.socket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Phục vụ MỘT kết nối TCP trên một luồng riêng: lặp đọc khung yêu cầu → điều phối → ghi khung
 * phản hồi, cho tới khi client đóng kết nối, im lặng quá lâu, hoặc vi phạm giao thức.
 * Các yêu cầu trên cùng một kết nối được xử lý tuần tự, phản hồi về đúng thứ tự gửi.
 */
class ClientConnection implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClientConnection.class);

    private final Socket socket;
    private final SocketDispatcher dispatcher;
    private final int maxFrameBytes;

    ClientConnection(Socket socket, SocketDispatcher dispatcher, int maxFrameBytes) {
        this.socket = socket;
        this.dispatcher = dispatcher;
        this.maxFrameBytes = maxFrameBytes;
    }

    @Override
    public void run() {
        String peer = socket.getRemoteSocketAddress().toString();
        log.info("Kết nối mới từ {}", peer);
        int served = 0;
        try (socket;
             var in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             var out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {
            try {
                byte[] frame;
                while ((frame = FrameCodec.read(in, maxFrameBytes)) != null) {
                    FrameCodec.write(out, dispatcher.handle(frame));
                    served++;
                }
            } catch (FrameCodec.FrameTooLargeException e) {
                log.warn("{} vi phạm giao thức: {} — đóng kết nối", peer, e.getMessage());
                FrameCodec.write(out, dispatcher.error(null, "FRAME_TOO_LARGE", "Thông điệp vượt quá giới hạn."));
            }
        } catch (SocketTimeoutException e) {
            log.info("{} im lặng quá lâu — đóng kết nối", peer);
        } catch (IOException e) {
            log.info("{} ngắt kết nối: {}", peer, e.getMessage());
        } finally {
            log.info("Đóng kết nối {} sau {} yêu cầu", peer, served);
        }
    }
}
