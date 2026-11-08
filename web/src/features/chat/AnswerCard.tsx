"use client";

import { useState } from "react";
import { FileText } from "lucide-react";
import { ApiError, openDocumentFile } from "@/lib/api-client";
import type { ChatMessage } from "./api";

/** Một lượt hỏi – đáp: bong bóng câu hỏi bên phải, câu trả lời kèm nguồn bên dưới. */
export function AnswerCard({ message }: { message: ChatMessage }) {
  const [loi, setLoi] = useState<string | null>(null);

  async function moNguon() {
    setLoi(null);
    try {
      await openDocumentFile(message.source.documentId, message.source.page);
    } catch (e) {
      setLoi(e instanceof ApiError ? e.message : "Không mở được tài liệu nguồn.");
    }
  }

  return (
    <div className="animate-message mb-9">
      <div className="mb-5 flex justify-end">
        <div className="max-w-[80%] rounded-[12px] bg-sunken px-4 py-3 text-[15px] leading-[26px]">{message.typedText ?? message.question.content}</div>
      </div>
      <article>
        <div className="mb-2.5 font-mono text-[11px] uppercase tracking-[0.12em] text-muted">Trả lời</div>
        {message.typedText && (
          <p className="mb-2 text-[13px] leading-5 text-secondary">
            Câu hỏi có sẵn gần nhất: <span className="font-medium text-primary">{message.question.content}</span>
          </p>
        )}
        <p className="reading-width m-0 whitespace-pre-line text-[17px] leading-8">{message.answer}</p>
        <button
          type="button"
          onClick={() => void moNguon()}
          className="mt-4 inline-flex min-h-11 items-center gap-2 rounded-[8px] border border-border bg-surface px-3.5 text-left text-sm hover:border-border-strong focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
          aria-label={`Mở nguồn: ${message.source.title}, trang ${message.source.page}`}
        >
          <FileText className="size-4 shrink-0 text-accent" aria-hidden="true" />
          <span className="font-medium">{message.source.title}</span>
          <span className="font-mono text-[13px] text-secondary">· trang {message.source.page}</span>
        </button>
        {loi && <p role="alert" className="mt-2 text-sm text-danger">{loi}</p>}
      </article>
    </div>
  );
}
