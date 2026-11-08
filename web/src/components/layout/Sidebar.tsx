"use client";

import Image from "next/image";
import { X } from "lucide-react";
import type { ConversationSummary } from "@/features/chat/api";
import { cn } from "@/lib/utils";
import { MainNav } from "./MainNav";
import { UserMenu } from "./UserMenu";

type SidebarProps = {
  open: boolean;
  conversations: ConversationSummary[];
  activeId: string | null;
  onSelect: (id: string) => void;
  onNewChat: () => void;
  onClose: () => void;
};

function thoiGian(iso: string): string {
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? ""
    : d.toLocaleString("vi-VN", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
}

export function Sidebar({ open, conversations, activeId, onSelect, onNewChat, onClose }: SidebarProps) {
  return (
    <>
      {open && <button type="button" aria-label="Đóng thanh điều hướng" className="fixed inset-0 z-40 bg-primary/20 md:hidden" onClick={onClose} />}
      <aside className={cn(
        "fixed inset-y-0 left-0 z-50 flex w-[260px] shrink-0 flex-col overflow-hidden border-r border-border bg-sunken transition-transform md:static md:z-auto md:translate-x-0",
        open ? "translate-x-0 overlay-shadow" : "-translate-x-full",
      )}>
        <div className="flex h-[57px] items-center gap-2.5 border-b border-border px-4">
          <Image src="/logo-dau.png" alt="DAU" width={24} height={24} />
          <span className="font-serif text-[15px] font-semibold leading-5">Sổ Tay Sinh Viên CNTT</span>
          <button type="button" onClick={onClose} className="ml-auto flex size-11 items-center justify-center rounded-[8px] text-secondary hover:bg-surface md:hidden" aria-label="Đóng menu"><X className="size-5" /></button>
        </div>

        <MainNav active="chat" className="flex flex-wrap border-b border-border px-3 py-2" />

        <div className="p-4 pb-3.5">
          <button type="button" onClick={onNewChat} className="min-h-11 w-full rounded-[8px] border border-border-strong px-3.5 text-sm font-medium hover:bg-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent">Hội thoại mới</button>
        </div>
        <div className="px-4 pb-2 font-mono text-[11px] uppercase tracking-[0.12em] text-muted">Gần đây</div>
        <div className="flex-1 overflow-y-auto px-2 pb-4">
          {conversations.length === 0 && <p className="px-2.5 text-[13px] leading-5 text-muted">Chưa có hội thoại nào.</p>}
          {conversations.map((c) => (
            <button
              key={c.id}
              type="button"
              onClick={() => onSelect(c.id)}
              className={cn(
                "block min-h-11 w-full rounded-[8px] px-2.5 py-2 text-left hover:bg-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent",
                activeId === c.id && "bg-surface",
              )}
            >
              <span className="block truncate text-sm leading-[21px]">{c.title}</span>
              <span className="mt-0.5 block text-[13px] leading-[18px] text-muted">{thoiGian(c.createdAt)} · {c.messageCount} lượt</span>
            </button>
          ))}
        </div>
        <div className="border-t border-border p-3.5 px-4"><UserMenu /></div>
      </aside>
    </>
  );
}
