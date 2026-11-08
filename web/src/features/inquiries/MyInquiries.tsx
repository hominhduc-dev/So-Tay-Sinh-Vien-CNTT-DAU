"use client";

import { useEffect, useState } from "react";
import { FileText } from "lucide-react";
import { cauHoiDaGui, type Inquiry } from "@/features/chat/api";
import { ApiError, openDocumentFile } from "@/lib/api-client";
import { cn } from "@/lib/utils";

function ngayGio(iso: string | null): string {
  if (!iso) return "";
  return new Date(iso).toLocaleString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

/** UC26 — sinh viên xem các câu đã gửi giáo vụ và câu trả lời. */
export function MyInquiries() {
  const [items, setItems] = useState<Inquiry[] | null>(null);
  const [loi, setLoi] = useState<string | null>(null);

  useEffect(() => {
    let huy = false;
    cauHoiDaGui()
      .then((p) => !huy && setItems(p.items))
      .catch((e: unknown) => !huy && setLoi(e instanceof ApiError ? e.message : "Không tải được danh sách."));
    return () => {
      huy = true;
    };
  }, []);

  return (
    <section className="mx-auto w-full max-w-[860px] px-6 pb-20 pt-10 sm:px-8">
      <h1 className="font-serif text-[30px] font-semibold leading-[38px]">Câu hỏi đã gửi giáo vụ</h1>
      <p className="mt-2 text-[15px] leading-[26px] text-secondary">
        Những câu bạn gõ mà chưa có câu trả lời soạn sẵn. Giáo vụ khoa sẽ trả lời tại đây.
      </p>
      {loi && <p role="alert" className="mt-4 text-sm text-danger">{loi}</p>}
      {items === null && !loi && <p className="mt-6 text-muted">Đang tải…</p>}
      {items?.length === 0 && <p className="mt-6 text-[15px] text-muted">Bạn chưa gửi câu hỏi nào. Gõ câu hỏi ở trang Hỏi đáp để bắt đầu.</p>}
      <ul className="mt-6 flex flex-col gap-4">
        {items?.map((i) => (
          <li key={i.id} className="rounded-[12px] border border-border bg-surface p-5">
            <div className="flex flex-wrap items-baseline gap-3">
              <p className="m-0 flex-1 text-[15px] font-semibold leading-6">{i.content}</p>
              <span className={cn("rounded-[4px] px-2 py-0.5 font-mono text-[12px]", i.status === "ANSWERED" ? "bg-success/10 text-success" : "bg-warning/10 text-warning")}>
                {i.status === "ANSWERED" ? "Đã trả lời" : "Chờ trả lời"}
              </span>
            </div>
            <div className="mt-1 font-mono text-[12px] text-muted">Gửi lúc {ngayGio(i.createdAt)}</div>
            {i.answer && (
              <div className="mt-4 border-l-2 border-accent pl-4">
                <p className="m-0 whitespace-pre-line text-[15px] leading-[26px]">{i.answer}</p>
                {i.source && (
                  <button
                    type="button"
                    onClick={() => openDocumentFile(i.source!.documentId, i.source!.page).catch((e) => setLoi(e instanceof ApiError ? e.message : "Không mở được tài liệu."))}
                    className="mt-3 inline-flex min-h-10 items-center gap-2 rounded-[8px] border border-border px-3 text-sm hover:border-border-strong"
                  >
                    <FileText className="size-4 text-accent" /> <span className="font-medium">{i.source.title}</span>
                    <span className="font-mono text-[13px] text-secondary">· trang {i.source.page}</span>
                  </button>
                )}
                <div className="mt-2 font-mono text-[12px] text-muted">Trả lời lúc {ngayGio(i.answeredAt)}</div>
              </div>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
