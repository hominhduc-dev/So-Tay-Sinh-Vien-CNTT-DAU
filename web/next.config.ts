import type { NextConfig } from "next";

/**
 * Địa chỉ server Spring Boot mà Next chuyển tiếp `/api/*` tới.
 *
 * Trình duyệt chỉ gọi `/api/...` trên CHÍNH máy nó vừa tải trang, nên không vướng
 * CORS và chạy được cho localhost, IP LAN hay tên miền mà không phải build lại.
 * Giá trị được đọc LÚC BUILD (bản standalone đóng băng cấu hình), nên trong Docker
 * truyền bằng build arg `API_INTERNAL_URL`.
 */
const apiInternalUrl = process.env.API_INTERNAL_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // Gom mọi thứ cần để chạy vào .next/standalone — ảnh Docker không phải cài lại phụ thuộc.
  output: "standalone",
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${apiInternalUrl}/api/:path*` }];
  },
};

export default nextConfig;
