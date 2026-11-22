package vn.tangthu.client.common.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * Đóng / tách khung thông điệp — giống hệt phía server (server/.../common/socket/FrameCodec).
 * Mỗi thông điệp: [4 byte độ dài N, big-endian][N byte JSON UTF-8].
 */
public final class FrameCodec {
    private FrameCodec() {}

    public static void write(DataOutputStream out, byte[] payload) throws IOException {
        out.writeInt(payload.length);
        out.write(payload);
        out.flush();
    }

    /** Trả {@code null} khi server đóng kết nối. */
    public static byte[] read(DataInputStream in, int maxBytes) throws IOException {
        int length;
        try {
            length = in.readInt();
        } catch (EOFException e) {
            return null;
        }
        if (length < 0 || length > maxBytes) {
            throw new IOException("Khung dài " + length + " byte, vượt giới hạn " + maxBytes);
        }
        byte[] payload = new byte[length];
        in.readFully(payload);
        return payload;
    }
}
