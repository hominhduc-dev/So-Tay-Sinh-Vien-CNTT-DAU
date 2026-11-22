package vn.tangthu.common.socket;

import vn.tangthu.account.model.Role;

/**
 * Controller của giao thức socket — phần "C" trong MVC. Mỗi module có một hoặc vài controller,
 * đăng ký các action của mình (ví dụ {@code auth.login}, {@code chat.ask}) vào bộ điều phối.
 */
public interface SocketController {

    void register(Routes routes);

    @FunctionalInterface
    interface Handler {
        Object handle(SocketRequest request) throws Exception;
    }

    /** Bảng định tuyến: tên action → vai tối thiểu → hàm xử lý. */
    interface Routes {
        /** Action công khai, không cần token (chỉ dùng cho đăng nhập, ping). */
        void open(String action, Handler handler);

        /** Action cần đăng nhập với vai tối thiểu {@code minRole} (vai trên kế thừa vai dưới). */
        void secured(String action, Role minRole, Handler handler);
    }
}
