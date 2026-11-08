"use client";

import { useCallback, useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { coVai, useCurrentUser } from "@/features/auth/useAuth";
import { danhSach, type KnowledgeDocument } from "@/features/documents/api";
import { ApiError } from "@/lib/api-client";
import { cn } from "@/lib/utils";
import {
  anCauHoi,
  layCauHoiSoan,
  layChuDe,
  suaCauHoi,
  suaChuDe,
  taoCauHoi,
  taoChuDe,
  xoaChuDe,
  type EditorQuestion,
  type QuestionInput,
  type Topic,
} from "./api";
import { QuestionForm } from "./QuestionForm";

/** UC22 quản lý chủ đề + UC23 quản lý câu hỏi – câu trả lời. Chỉ Giáo vụ và Quản trị viên. */
export function ContentManager() {
  const user = useCurrentUser();
  const [topics, setTopics] = useState<Topic[]>([]);
  const [chon, setChon] = useState<Topic | null>(null);
  const [questions, setQuestions] = useState<EditorQuestion[]>([]);
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([]);
  const [dangSua, setDangSua] = useState<EditorQuestion | "moi" | null>(null);
  const [tenMoi, setTenMoi] = useState("");
  const [dangLuu, setDangLuu] = useState(false);
  const [loi, setLoi] = useState<string | null>(null);

  const baoLoi = (e: unknown, macDinh: string) => setLoi(e instanceof ApiError ? e.message : macDinh);

  const napChuDe = useCallback(async () => {
    const t = await layChuDe();
    setTopics(t);
    return t;
  }, []);

  const napCauHoi = useCallback(async (topicId: string) => {
    setQuestions(await layCauHoiSoan(topicId));
  }, []);

  useEffect(() => {
    if (!coVai(user, "CONTENT_ADMIN")) return;
    let huy = false;
    Promise.all([layChuDe(), danhSach({ size: 100 })])
      .then(async ([t, d]) => {
        if (huy) return;
        setTopics(t);
        setDocuments(d.items);
        if (t.length) {
          setChon(t[0]);
          const q = await layCauHoiSoan(t[0].id);
          if (!huy) setQuestions(q);
        }
      })
      .catch((e: unknown) => !huy && setLoi(e instanceof ApiError ? e.message : "Không tải được nội dung."));
    return () => {
      huy = true;
    };
  }, [user]);

  if (!coVai(user, "CONTENT_ADMIN")) {
    return (
      <section className="mx-auto max-w-[1120px] px-6 pt-10 sm:px-8">
        <p role="alert" className="rounded-[8px] border border-border bg-surface px-4 py-3.5 text-[15px] text-secondary">
          Mục này chỉ dành cho giáo vụ khoa và quản trị viên.
        </p>
      </section>
    );
  }

  async function lam(viec: () => Promise<unknown>, macDinh: string) {
    setDangLuu(true);
    setLoi(null);
    try {
      await viec();
      return true;
    } catch (e) {
      baoLoi(e, macDinh);
      return false;
    } finally {
      setDangLuu(false);
    }
  }

  async function chonChuDe(t: Topic) {
    setChon(t);
    setDangSua(null);
    setLoi(null);
    try {
      await napCauHoi(t.id);
    } catch (e) {
      baoLoi(e, "Không tải được câu hỏi.");
    }
  }

  async function themChuDe(event: React.FormEvent) {
    event.preventDefault();
    const name = tenMoi.trim();
    if (!name) return;
    const ok = await lam(async () => {
      const t = await taoChuDe(name, topics.length + 1);
      await napChuDe();
      setTenMoi("");
      await chonChuDe(t);
    }, "Không tạo được chủ đề.");
    if (!ok) return;
  }

  function doiTenChuDe(t: Topic) {
    const name = window.prompt("Tên chủ đề:", t.name)?.trim();
    if (!name || name === t.name) return;
    void lam(async () => {
      const moi = await suaChuDe(t.id, { name });
      await napChuDe();
      if (chon?.id === t.id) setChon(moi);
    }, "Không đổi được tên chủ đề.");
  }

  function doiThuTu(t: Topic, delta: number) {
    void lam(async () => {
      await suaChuDe(t.id, { sortOrder: t.sortOrder + delta });
      await napChuDe();
    }, "Không đổi được thứ tự.");
  }

  function xoaChuDeNay(t: Topic) {
    if (!window.confirm(`Xóa chủ đề "${t.name}"? Chỉ xóa được khi chủ đề không còn câu hỏi nào.`)) return;
    void lam(async () => {
      await xoaChuDe(t.id);
      const ds = await napChuDe();
      if (chon?.id === t.id) {
        setChon(ds[0] ?? null);
        if (ds[0]) await napCauHoi(ds[0].id);
        else setQuestions([]);
      }
    }, "Không xóa được chủ đề.");
  }

  async function luuCauHoi(input: QuestionInput) {
    const dangSuaQ = dangSua;
    const ok = await lam(
      () => (dangSuaQ && dangSuaQ !== "moi" ? suaCauHoi(dangSuaQ.id, input) : taoCauHoi(input)),
      "Không lưu được câu hỏi.",
    );
    if (ok && chon) {
      setDangSua(null);
      await napCauHoi(chon.id);
      await napChuDe();
    }
  }

  function anHien(q: EditorQuestion) {
    if (!chon) return;
    void lam(async () => {
      if (q.active) await anCauHoi(q.id);
      else await suaCauHoi(q.id, { active: true });
      await napCauHoi(chon.id);
      await napChuDe();
    }, "Không đổi được trạng thái câu hỏi.");
  }

  return (
    <section className="mx-auto w-full max-w-[1184px] px-6 pb-20 pt-10 sm:px-8">
      <h1 className="font-serif text-[30px] font-semibold leading-[38px]">Nội dung hỏi đáp</h1>
      <p className="mt-2 max-w-[70ch] text-[15px] leading-[26px] text-secondary">
        Soạn các nút chủ đề và câu hỏi mà sinh viên thấy trong khung chat. Mỗi câu trả lời phải dẫn tới một tài liệu và số trang.
      </p>
      {loi && <p role="alert" className="mt-4 rounded-[8px] border border-danger/30 bg-danger/5 px-4 py-3 text-sm text-danger">{loi}</p>}

      <div className="mt-7 grid gap-8 lg:grid-cols-[300px_1fr]">
        {/* --- UC22: chủ đề --- */}
        <div>
          <div className="mb-2 font-mono text-[11px] uppercase tracking-[0.12em] text-muted">Chủ đề</div>
          <ul className="flex flex-col gap-1">
            {topics.map((t, i) => (
              <li key={t.id} className={cn("rounded-[8px] border", chon?.id === t.id ? "border-border-strong bg-surface" : "border-transparent")}>
                <button type="button" onClick={() => void chonChuDe(t)} className="flex min-h-11 w-full items-center gap-2 px-3 text-left">
                  <span className="flex-1 text-[15px] leading-6">{t.name}</span>
                  <span className="font-mono text-[12px] text-muted">{t.questionCount}</span>
                </button>
                {chon?.id === t.id && (
                  <div className="flex flex-wrap gap-1 border-t border-border px-2 py-1.5 text-[13px]">
                    <button type="button" disabled={dangLuu || i === 0} onClick={() => doiThuTu(t, -(t.sortOrder - (topics[i - 1]?.sortOrder ?? t.sortOrder) + 1))} className="rounded px-2 py-1 hover:bg-sunken disabled:opacity-40">Lên</button>
                    <button type="button" disabled={dangLuu || i === topics.length - 1} onClick={() => doiThuTu(t, (topics[i + 1]?.sortOrder ?? t.sortOrder) - t.sortOrder + 1)} className="rounded px-2 py-1 hover:bg-sunken disabled:opacity-40">Xuống</button>
                    <button type="button" disabled={dangLuu} onClick={() => doiTenChuDe(t)} className="rounded px-2 py-1 hover:bg-sunken">Đổi tên</button>
                    <button type="button" disabled={dangLuu} onClick={() => xoaChuDeNay(t)} className="ml-auto rounded px-2 py-1 text-danger hover:bg-sunken">Xóa</button>
                  </div>
                )}
              </li>
            ))}
          </ul>
          <form onSubmit={themChuDe} className="mt-3 flex gap-2">
            <Input value={tenMoi} onChange={(e) => setTenMoi(e.target.value)} maxLength={100} placeholder="Chủ đề mới" className="h-11" aria-label="Tên chủ đề mới" />
            <Button type="submit" size="icon" disabled={dangLuu || !tenMoi.trim()} aria-label="Thêm chủ đề"><Plus className="size-4" /></Button>
          </form>
        </div>

        {/* --- UC23: câu hỏi của chủ đề --- */}
        <div>
          {chon === null ? (
            <p className="text-[15px] text-muted">Tạo một chủ đề để bắt đầu.</p>
          ) : (
            <>
              <div className="mb-3 flex flex-wrap items-center gap-3">
                <h2 className="font-serif text-[22px] font-semibold">{chon.name}</h2>
                {dangSua === null && (
                  <Button type="button" size="sm" className="ml-auto" onClick={() => setDangSua("moi")} disabled={documents.length === 0}>
                    <Plus className="size-4" /> Thêm câu hỏi
                  </Button>
                )}
              </div>
              {documents.length === 0 && (
                <p className="mb-4 text-sm text-warning">Chưa có tài liệu nguồn. Tải tài liệu lên ở trang Tài liệu trước khi soạn câu hỏi.</p>
              )}

              {dangSua !== null && (
                <div className="mb-5">
                  <QuestionForm
                    key={dangSua === "moi" ? "moi" : dangSua.id}
                    topicId={chon.id}
                    documents={documents}
                    initial={dangSua === "moi" ? null : dangSua}
                    dangLuu={dangLuu}
                    onSave={(input) => void luuCauHoi(input)}
                    onCancel={() => setDangSua(null)}
                  />
                </div>
              )}

              {questions.length === 0 ? (
                <p className="text-[15px] text-muted">Chủ đề này chưa có câu hỏi.</p>
              ) : (
                <ul className="flex flex-col gap-3">
                  {questions.map((q) => (
                    <li key={q.id} className={cn("rounded-[12px] border border-border bg-surface p-4", !q.active && "opacity-60")}>
                      <div className="flex flex-wrap items-start gap-3">
                        <div className="min-w-0 flex-1">
                          <div className="text-[15px] font-semibold leading-6">{q.content}</div>
                          <p className="mt-1.5 line-clamp-3 whitespace-pre-line text-sm leading-[22px] text-secondary">{q.answer}</p>
                          <div className="mt-2 font-mono text-[12px] text-muted">
                            Nguồn: {q.documentTitle} · trang {q.page}{!q.active && " · đang ẩn"}
                          </div>
                        </div>
                        <div className="flex gap-1">
                          <Button type="button" size="sm" variant="outline" disabled={dangLuu} onClick={() => setDangSua(q)}>Sửa</Button>
                          <Button type="button" size="sm" variant="ghost" disabled={dangLuu} onClick={() => anHien(q)} className="font-normal">
                            {q.active ? "Ẩn" : "Hiện"}
                          </Button>
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </div>
      </div>
    </section>
  );
}
