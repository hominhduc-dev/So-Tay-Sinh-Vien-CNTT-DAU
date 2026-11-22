package vn.tangthu.client.account.model;

import vn.tangthu.client.common.net.SocketClient;

/** Phiên làm việc sau khi đăng nhập: kết nối TCP đang mở + người dùng hiện tại. */
public record Session(SocketClient client, User user) {}
