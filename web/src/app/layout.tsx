import type { Metadata } from "next";
import "@fontsource/be-vietnam-pro/300.css";
import "@fontsource/be-vietnam-pro/400.css";
import "@fontsource/be-vietnam-pro/500.css";
import "@fontsource/be-vietnam-pro/600.css";
import "@fontsource/lora/400.css";
import "@fontsource/lora/500.css";
import "@fontsource/lora/600.css";
import "@fontsource/jetbrains-mono/400.css";
import "@fontsource/jetbrains-mono/500.css";
import "./globals.css";

// Font đóng gói sẵn từ npm (@fontsource), không tải từ Google lúc build hay lúc chạy:
// hệ thống phải chạy được trong LAN phòng máy không có Internet (NFR-07).

export const metadata: Metadata = {
  title: { default: "Sổ Tay Sinh Viên CNTT", template: "%s · Sổ Tay Sinh Viên CNTT" },
  description: "Tra cứu quy định học vụ qua câu hỏi có sẵn, kèm nguồn, cho sinh viên ngành CNTT — Đại học Kiến trúc Đà Nẵng.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="vi">
      <body>{children}</body>
    </html>
  );
}
