"use client";

import Link from "next/link";
import { coVai, useCurrentUser, type RoleCode } from "@/features/auth/useAuth";
import { cn } from "@/lib/utils";

export type Section = "chat" | "inquiries" | "documents" | "content" | "inbox" | "admin";

const LINKS: { key: Section; href: string; label: string; can: RoleCode }[] = [
  { key: "chat", href: "/chat", label: "Hỏi đáp", can: "USER" },
  { key: "inquiries", href: "/inquiries", label: "Câu hỏi đã gửi", can: "USER" },
  { key: "documents", href: "/documents", label: "Tài liệu", can: "USER" },
  { key: "content", href: "/content", label: "Nội dung", can: "CONTENT_ADMIN" },
  { key: "inbox", href: "/inbox", label: "Hộp thư", can: "CONTENT_ADMIN" },
  { key: "admin", href: "/admin/users", label: "Quản trị", can: "SYSTEM_ADMIN" },
];

/**
 * Liên kết theo vai. Ẩn liên kết KHÔNG phải biện pháp bảo mật — server chặn mọi
 * endpoint bằng @PreAuthorize; ẩn chỉ để người dùng không bấm vào màn chắc chắn lỗi.
 */
export function MainNav({ active, className }: { active: Section; className?: string }) {
  const user = useCurrentUser();
  return (
    <nav className={cn("items-center gap-1", className)} aria-label="Điều hướng chính">
      {LINKS.filter((l) => coVai(user, l.can)).map((l) => (
        <Link
          key={l.key}
          href={l.href}
          className={cn(
            "rounded-[8px] px-3 py-2 text-sm no-underline",
            active === l.key ? "bg-sunken font-medium text-primary" : "text-secondary hover:bg-sunken",
          )}
        >
          {l.label}
        </Link>
      ))}
    </nav>
  );
}
