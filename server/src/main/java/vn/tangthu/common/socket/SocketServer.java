package vn.tangthu.common.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import vn.tangthu.common.config.AppProperties;

/**
 * Máy chủ TCP: mở {@link ServerSocket} ở cổng {@code app.socket.port} (mặc định 9090), một luồng
 * chấp nhận kết nối, mỗi kết nối được giao cho một luồng trong thread pool ({@link ClientConnection}).
 * Chạy cùng vòng đời ứng dụng Spring: mở khi khởi động, đóng gọn khi tắt.
 */
@Component
public class SocketServer implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(SocketServer.class);

    private final SocketDispatcher dispatcher;
    private final AppProperties.Socket cfg;
    private final AtomicInteger seq = new AtomicInteger();
    private ServerSocket server;
    private ExecutorService workers;
    private Thread acceptor;
    private volatile boolean running;

    public SocketServer(SocketDispatcher dispatcher, AppProperties props) {
        this.dispatcher = dispatcher;
        this.cfg = props.socket();
    }

    @Override
    public void start() {
        try {
            server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(cfg.port()), 100);
        } catch (IOException e) {
            throw new IllegalStateException("Không mở được cổng TCP " + cfg.port(), e);
        }
        workers = Executors.newFixedThreadPool(cfg.threads(), r -> {
            Thread t = new Thread(r, "socket-client-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        running = true;
        acceptor = new Thread(this::acceptLoop, "socket-acceptor");
        acceptor.start();
        log.info("Máy chủ TCP socket lắng nghe ở cổng {} ({} luồng phục vụ)", getPort(), cfg.threads());
    }

    private void acceptLoop() {
        int maxFrame = cfg.maxFrameMb() * 1024 * 1024;
        int idleMs = cfg.idleTimeout() == null ? 0 : (int) cfg.idleTimeout().toMillis();
        while (running) {
            try {
                Socket client = server.accept();          // chặn cho tới khi có client nối tới
                client.setTcpNoDelay(true);
                client.setSoTimeout(idleMs);
                workers.execute(new ClientConnection(client, dispatcher, maxFrame));
            } catch (SocketException e) {
                if (running) log.warn("Lỗi chấp nhận kết nối: {}", e.getMessage());
            } catch (IOException e) {
                log.warn("Lỗi chấp nhận kết nối: {}", e.getMessage());
            }
        }
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (server != null) server.close();
        } catch (IOException ignored) {
            // đang tắt
        }
        if (workers != null) {
            workers.shutdownNow();
            try {
                workers.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("Đã đóng máy chủ TCP socket");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /** Cổng thực sự đang lắng nghe (khác cấu hình khi cấu hình là 0). */
    public int getPort() {
        return server == null ? -1 : server.getLocalPort();
    }
}
