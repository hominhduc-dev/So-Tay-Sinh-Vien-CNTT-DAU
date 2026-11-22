package vn.tangthu.client.common.view;

import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;
import vn.tangthu.client.common.net.ApiError;

/**
 * Chạy lời gọi mạng ở luồng nền (không treo giao diện), rồi trả kết quả về luồng Swing (EDT).
 * Lỗi {@code UNAUTHORIZED} — token hết hạn, tài khoản bị khóa (BR01) — đưa người dùng về màn đăng nhập.
 */
public final class Async {
    private static volatile Runnable onSessionExpired = () -> {};

    private Async() {}

    public static void setOnSessionExpired(Runnable handler) {
        onSessionExpired = handler;
    }

    public static <T> void run(Component owner, Callable<T> task, Consumer<T> onDone) {
        run(owner, task, onDone, e -> showError(owner, e));
    }

    public static <T> void run(Component owner, Callable<T> task, Consumer<T> onDone, Consumer<ApiError> onError) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    onDone.accept(get());
                } catch (ExecutionException e) {
                    Throwable c = e.getCause();
                    onError.accept(c instanceof ApiError a ? a : new ApiError("CLIENT_ERROR", String.valueOf(c)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    public static void showError(Component owner, ApiError e) {
        if (e.isAuthFailure()) {
            Ui.error(owner, e.getMessage() + "\nVui lòng đăng nhập lại.");
            onSessionExpired.run();
        } else {
            Ui.error(owner, e.getMessage());
        }
    }
}
