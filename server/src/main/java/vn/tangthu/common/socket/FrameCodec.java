package vn.tangthu.common.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * Đóng / tách khung thông điệp trên luồng TCP.
 *
 * <p>TCP là luồng byte liên tục, không có ranh giới thông điệp: một lần {@code read()} có thể nhận
 * nửa thông điệp hoặc hai thông điệp dính nhau. Vì vậy mỗi thông điệp được đóng khung:
 * <pre>
 *   ┌────────────────────────┬──────────────────────────────┐
 *   │ 4 byte độ dài N        │ N byte nội dung JSON (UTF-8)   │
 *   │ (số nguyên big-endian) │                              │
 *   └────────────────────────┴──────────────────────────────┘
 * </pre>
 * Bên nhận đọc đủ 4 byte để biết N, rồi đọc đúng N byte ({@link DataInputStream#readFully}).
 */
public final class FrameCodec {
    private FrameCodec() {}

    /** Khung vượt giới hạn hoặc độ dài âm — vi phạm giao thức, phải đóng kết nối. */
    public static class FrameTooLargeException extends IOException {
        public FrameTooLargeException(String message) { super(message); }
    }

    /** Ghi một khung. Gọi {@code flush()} để đẩy ngay xuống socket. */
    public static void write(DataOutputStream out, byte[] payload) throws IOException {
        out.writeInt(payload.length);
        out.write(payload);
        out.flush();
    }

    /** Đọc một khung; trả {@code null} khi bên kia đóng kết nối đúng ở ranh giới khung. */
    public static byte[] read(DataInputStream in, int maxBytes) throws IOException {
        int length;
        try {
            length = in.readInt();
        } catch (EOFException e) {
            return null;
        }
        if (length < 0 || length > maxBytes) {
            throw new FrameTooLargeException("Khung dài " + length + " byte, vượt giới hạn " + maxBytes);
        }
        byte[] payload = new byte[length];
        in.readFully(payload);
        return payload;
    }
}
