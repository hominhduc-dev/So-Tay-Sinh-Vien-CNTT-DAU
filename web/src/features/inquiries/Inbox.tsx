"use client";

import { useCallback, useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { coVai, useCurrentUser } from "@/features/auth/useAuth";
import type { Inquiry, Page } from "@/features/chat/api";
import { layChuDe, taoCauHoi, type Topic } from "@/features/content/api";
import { danhSach, type KnowledgeDocument } from "@/features/documents/api";
import { ApiError, apiGet, apiPost } from "@/lib/api-client";
import { cn } from "@/lib/utils";

type Loc = "PENDING" | "ANSWERED";

const o = "w-full rounded-[8px] border border-border bg-surface px-3.5 text-[15px] outline-none focus:border-border-strong focus:ring-2 focus:ring-accent/15";

/** UC27 — giáo vụ trả lời câu hỏi sinh viên gửi; có thể lưu luôn thành câu hỏi có sẵn (UC23). */
function AnswerForm({ inquiry, documents, topics, onDone }: {
  inquiry: Inquiry; documents: KnowledgeDocument[]; topics: Topic[]; onDone: () => void;
}) {
  const [answer, setAnswer] = useState(inquiry.answer ?? "");
  const [documentId, setDocumentId] = useState(inquiry.source?.documentId ?? "");
  const [page, setPage] = useState(inquiry.source ? String(inquiry.source.page) : "");
  const [luuCauHoi, setLuuCauHoi] = useState(false);
  const [topicId, setTopicId] = useState(topics[0]?.id ?? "");
  const [nut, setNut] = useState(inquiry.content.slice(0, 200));
  const [dangGui, setDangGui] = useState(false);
  const [loi, setLoi] = useState<string | null>(null);
  const doc = documents.find((d) => d.id === documentId);

  async function gui(event: React.FormEvent) {
    event.preventDefault();
    if (!answer.trim()) return setLoi("Cần nhập câu trả lời.");
    const p = page ? Number(page) : undefined;
    if (documentId && (!p || p < 1 || (doc?.pageCount != null && p > doc.pageCount))) {
      return setLoi(`Số trang phải từ 1 đến ${doc?.pageCount ?? "số trang của tài liệu"}.`);
    }
    if (luuCauHoi && (!documentId || !topicId)) return setLoi("Muốn lưu thành câu hỏi có sẵn thì phải chọn chủ đề và tài liệu nguồn.");
    setDangGui(true);
    setLoi(null);
    try {
      await apiPost(`/inquiries/${inquiry.id}/answer`, { answer: answer.trim(), documentId: documentId || null, page: documentId ? p : null });
      if (luuCauHoi && p) {
        await taoCauHoi({ topicId, content: nut.trim(), answer: answer.trim(), documentId, page: p, sortOrder: 0, active: true });
      }
      onDone();
    } catch (e) {
      setLoi(e instanceof ApiError ? e.message : "Không gửi được câu trả lời.");
    } finally {
      setDangGui(false);
    }
  }

  return (
    <form onSubmit={gui} className="mt-4 border-t border-border pt-4" noValidate>
      <label className="block text-[13px] font-medium text-secondary">
        Câu trả lời
        <textarea className={`${o} mt-1 min-h-[110px] py-3 leading-[24px]`} maxLength={5000} value={answer} onChange={(e) => setAnswer(e.target.value)} />
      </label>
      <div className="mt-3 grid gap-3 sm:grid-cols-[1fr_120px]">
        <label className="block text-[13px] font-medium text-secondary">
          Tài liệu nguồn (nên có)
          <select className={`${o} mt-1 h-12`} value={documentId} onChange={(e) => setDocumentId(e.target.value)}>
            <option value="">— không kèm nguồn —</option>
            {documents.map((d) => <option key={d.id} value={d.id}>{d.title}{d.pageCount ? ` (${d.pageCount} trang)` : ""}</option>)}
          </select>
        </label>
        <label className="block text-[13px] font-medium text-secondary">
          Trang
          <Input className="mt-1" type="number" min={1} disabled={!documentId} value={page} onChange={(e) => setPage(e.target.value)} />
        </label>
      </div>
      <label className="mt-3 flex items-center gap-2 text-sm">
        <input type="checkbox" checked={luuCauHoi} onChange={(e) => setLuuCauHoi(e.target.checked)} className="size-4" />
        Lưu thành câu hỏi có sẵn để sinh viên khác bấm được
      </label>
      {luuCauHoi && (
        <div className="mt-3 grid gap-3 sm:grid-cols-[220px_1fr]">
          <label className="block text-[13px] font-medium text-secondary">
            Chủ đề
            <select className={`${o} mt-1 h-12`} value={topicId} onChange={(e) => setTopicId(e.target.value)}>
              {topics.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          </label>
          <label className="block text-[13px] font-medium text-secondary">
            Chữ trên nút
            <Input className="mt-1" maxLength={200} value={nut} onChange={(e) => setNut(e.target.value)} />
          </label>
        </div>
      )}
      {loi && <p role="alert" className="mt-3 text-sm text-danger">{loi}</p>}
      <Button type="submit" size="sm" className="mt-4" disabled={dangGui}>{dangGui ? "Đang gửi…" : inquiry.answer ? "Cập nhật câu trả lời" : "Gửi câu trả lời"}</Button>
    </form>
  );
}

export function Inbox() {
  const user = useCurrentUser();
  const [loc, setLoc] = useState<Loc>("PENDING");
  const [items, setItems] = useState<Inquiry[]>([]);
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([]);
  const [topics, setTopics] = useState<Topic[]>([]);
  const [mo, setMo] = useState<string | null>(null);
  const [loi, setLoi] = useState<string | null>(null);

  const nap = useCallback(async (l: Loc) => {
    try {
      const p = await apiGet<Page<Inquiry>>(`/inquiries?status=${l}&size=100`);
      setItems(p.items);
      setLoi(null);
    } catch (e) {
      setLoi(e instanceof ApiError ? e.message : "Không tải được hộp thư.");
    }
  }, []);

  useEffect(() => {
    if (!coVai(user, "CONTENT_ADMIN")) return;
    let huy = false;
    Promise.all([apiGet<Page<Inquiry>>(`/inquiries?status=PENDING&size=100`), danhSach({ size: 100 }), layChuDe()])
      .then(([p, d, t]) => {
        if (huy) return;
        setItems(p.items);
        setDocuments(d.items);
        setTopics(t);
      })
      .catch((e: unknown) => !huy && setLoi(e instanceof ApiError ? e.message : "Không tải được hộp thư."));
    return () => {
      huy = true;
    };
  }, [user]);

  if (!coVai(user, "CONTENT_ADMIN")) {
    return <p role="alert" className="mx-auto mt-10 max-w-[860px] px-6 text-secondary">Mục này chỉ dành cho giáo vụ khoa và quản trị viên.</p>;
  }

  return (
    <section className="mx-auto w-full max-w-[960px] px-6 pb-20 pt-10 sm:px-8">
      <h1 className="font-serif text-[30px] font-semibold leading-[38px]">Hộp thư câu hỏi</h1>
      <p className="mt-2 max-w-[70ch] text-[15px] leading-[26px] text-secondary">
        Câu sinh viên gõ mà không khớp câu hỏi soạn sẵn nào. Câu chờ lâu nhất ở trên cùng.
      </p>
      <nav className="mt-6 flex gap-1 border-b border-border" aria-label="Lọc">
        {(["PENDING", "ANSWERED"] as Loc[]).map((l) => (
          <button key={l} type="button" onClick={() => { setLoc(l); setMo(null); void nap(l); }}
            className={cn("-mb-px min-h-11 border-b-2 px-3.5 text-[15px]", loc === l ? "border-primary font-semibold" : "border-transparent text-secondary")}>
            {l === "PENDING" ? "Chờ trả lời" : "Đã trả lời"}
          </button>
        ))}
      </nav>
      {loi && <p role="alert" className="mt-4 text-sm text-danger">{loi}</p>}
      {items.length === 0 && <p className="mt-6 text-[15px] text-muted">{loc === "PENDING" ? "Không có câu hỏi nào đang chờ." : "Chưa trả lời câu nào."}</p>}
      <ul className="mt-5 flex flex-col gap-4">
        {items.map((i) => (
          <li key={i.id} className="rounded-[12px] border border-border bg-surface p-5">
            <div className="flex flex-wrap items-start gap-3">
              <div className="min-w-0 flex-1">
                <p className="m-0 text-[15px] font-semibold leading-6">{i.content}</p>
                <div className="mt-1 font-mono text-[12px] text-muted">
                  {i.askerName} · {i.askerCode} · {new Date(i.createdAt).toLocaleString("vi-VN")}
                </div>
                {i.answer && mo !== i.id && <p className="mt-3 whitespace-pre-line text-sm leading-[22px] text-secondary">{i.answer}</p>}
              </div>
              <Button type="button" size="sm" variant="outline" onClick={() => setMo(mo === i.id ? null : i.id)}>
                {mo === i.id ? "Đóng" : i.answer ? "Sửa" : "Trả lời"}
              </Button>
            </div>
            {mo === i.id && (
              <AnswerForm inquiry={i} documents={documents} topics={topics} onDone={() => { setMo(null); void nap(loc); }} />
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
