"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { KnowledgeDocument } from "@/features/documents/api";
import type { EditorQuestion, QuestionInput } from "./api";

type Props = {
  topicId: string;
  documents: KnowledgeDocument[];
  initial: EditorQuestion | null;
  dangLuu: boolean;
  onSave: (input: QuestionInput) => void;
  onCancel: () => void;
};

const o = "mt-1 w-full rounded-[8px] border border-border bg-surface px-3.5 text-[15px] outline-none focus:border-border-strong focus:ring-2 focus:ring-accent/15";

/** Biểu mẫu soạn một nút câu hỏi: nội dung nút, câu trả lời, tài liệu nguồn và trang (BR06). */
export function QuestionForm({ topicId, documents, initial, dangLuu, onSave, onCancel }: Props) {
  const [content, setContent] = useState(initial?.content ?? "");
  const [answer, setAnswer] = useState(initial?.answer ?? "");
  const [documentId, setDocumentId] = useState(initial?.documentId ?? documents[0]?.id ?? "");
  const [page, setPage] = useState(String(initial?.page ?? 1));
  const [sortOrder, setSortOrder] = useState(String(initial?.sortOrder ?? 0));
  const [active, setActive] = useState(initial?.active ?? true);
  const [loi, setLoi] = useState<string | null>(null);

  const doc = documents.find((d) => d.id === documentId);

  function submit(event: React.FormEvent) {
    event.preventDefault();
    const p = Number(page);
    if (!content.trim() || !answer.trim()) return setLoi("Cần nhập nội dung nút và câu trả lời.");
    if (!documentId) return setLoi("Chọn tài liệu nguồn — mỗi câu trả lời phải có nguồn.");
    if (!Number.isInteger(p) || p < 1 || (doc?.pageCount != null && p > doc.pageCount)) {
      return setLoi(`Số trang phải từ 1 đến ${doc?.pageCount ?? "số trang của tài liệu"}.`);
    }
    setLoi(null);
    onSave({ topicId, content: content.trim(), answer: answer.trim(), documentId, page: p, sortOrder: Number(sortOrder) || 0, active });
  }

  return (
    <form onSubmit={submit} className="rounded-[12px] border border-border-strong bg-surface p-5" noValidate>
      <h3 className="font-serif text-[19px] font-semibold">{initial ? "Sửa câu hỏi" : "Câu hỏi mới"}</h3>

      <label className="mt-4 block text-[13px] font-medium text-secondary">
        Nội dung nút ({content.length}/200)
        <Input className="mt-1" maxLength={200} value={content} onChange={(e) => setContent(e.target.value)} placeholder="Mỗi học kỳ đăng ký tối đa bao nhiêu tín chỉ?" />
      </label>

      <label className="mt-4 block text-[13px] font-medium text-secondary">
        Câu trả lời ({answer.length}/5000)
        <textarea className={`${o} min-h-[140px] py-3 leading-[24px]`} maxLength={5000} value={answer} onChange={(e) => setAnswer(e.target.value)} />
      </label>

      <div className="mt-4 grid gap-3 sm:grid-cols-[1fr_120px_120px]">
        <label className="block text-[13px] font-medium text-secondary">
          Tài liệu nguồn
          <select className={`${o} h-12`} value={documentId} onChange={(e) => setDocumentId(e.target.value)}>
            {documents.length === 0 && <option value="">— chưa có tài liệu, tải lên ở trang Tài liệu —</option>}
            {documents.map((d) => (
              <option key={d.id} value={d.id}>{d.title}{d.pageCount ? ` (${d.pageCount} trang)` : ""}</option>
            ))}
          </select>
        </label>
        <label className="block text-[13px] font-medium text-secondary">
          Trang
          <Input className="mt-1" type="number" min={1} max={doc?.pageCount ?? undefined} value={page} onChange={(e) => setPage(e.target.value)} />
        </label>
        <label className="block text-[13px] font-medium text-secondary">
          Thứ tự
          <Input className="mt-1" type="number" value={sortOrder} onChange={(e) => setSortOrder(e.target.value)} />
        </label>
      </div>

      <label className="mt-4 flex items-center gap-2 text-sm">
        <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} className="size-4 accent-[var(--accent)]" />
        Hiện thành nút cho sinh viên
      </label>

      {loi && <p role="alert" className="mt-3 text-sm text-danger">{loi}</p>}

      <div className="mt-5 flex gap-2">
        <Button type="submit" size="sm" disabled={dangLuu}>{dangLuu ? "Đang lưu…" : "Lưu"}</Button>
        <Button type="button" size="sm" variant="outline" onClick={onCancel} disabled={dangLuu}>Hủy</Button>
      </div>
    </form>
  );
}
