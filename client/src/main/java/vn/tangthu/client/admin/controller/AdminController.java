package vn.tangthu.client.admin.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.UUID;
import vn.tangthu.client.account.model.Role;
import vn.tangthu.client.account.model.User;
import vn.tangthu.client.admin.model.Health;
import vn.tangthu.client.common.net.PageResult;
import vn.tangthu.client.common.net.SocketClient;

/** M4 · Quản trị — UC14 tra cứu, UC15 khóa / mở, UC16 phân quyền tài khoản, UC20 trạng thái hệ thống. */
public class AdminController {
    private final SocketClient client;

    public AdminController(SocketClient client) {
        this.client = client;
    }

    public PageResult<User> users(String q, int page, int size) {
        return client.call("user.list", SocketClient.params("q", q, "page", page, "size", size),
                new TypeReference<PageResult<User>>() {});
    }

    public User setActive(UUID id, boolean active) {
        return client.call("user.update", SocketClient.params("id", id, "active", active), User.class);
    }

    public User setRole(UUID id, Role role) {
        return client.call("user.update", SocketClient.params("id", id, "role", role.name()), User.class);
    }

    public Health health() {
        return client.call("system.health", null, Health.class);
    }

    /** Đo thời gian khứ hồi (round-trip) một khung qua TCP, đơn vị mili giây. */
    public double pingMillis() {
        long t = System.nanoTime();
        client.call("system.ping", null);
        return (System.nanoTime() - t) / 1_000_000.0;
    }
}
