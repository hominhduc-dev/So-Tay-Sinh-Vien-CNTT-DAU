"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ArrowLeft, Menu, Send } from "lucide-react";
import { Sidebar } from "@/components/layout/Sidebar";
import { ApiError } from "@/lib/api-client";
import { cn } from "@/lib/utils";
import { AnswerCard } from "./AnswerCard";
import { Textarea } from "@/components/ui/textarea";
import {
  chiTietHoiThoai,
  guiGiaoVu,
  hoi,
  hoiBangChu,
  layCauHoi,
  layChuDe,
  layHoiThoai,
  type ChatMessage,
  type TypedStatus,
  type ConversationSummary,
  type QuestionButton,
  type Topic,
} from "./api";

/** Một mục trong luồng chat: lượt hỏi – đáp đã lưu, hoặc phản hồi tạm của UC24 (không lưu ở server). */
type ThreadItem =
  | { kind: "answer"; message: ChatMessage }
  | { kind: "typed"; key: string; text: string; status: Exclude<TypedStatus, "ANSWERED">; suggestions: QuestionButton[]; sent: boolean };

/**
 * UC05 bấm nút: chủ đề → câu hỏi → câu trả lời soạn sẵn kèm nguồn.
 * UC24 gõ câu hỏi: server tìm câu soạn sẵn khớp nhất; khớp vừa thì gợi ý, không khớp thì mời
 * gửi cho giáo vụ (UC25). Không có câu trả lời nào do máy tự sinh (BR07).
 */
export function ChatBox() {
  const [topics, setTopics] = useState<Topic[]>([]);
  const [topic, setTopic] = useState<Topic | null>(null);
  const [buttons, setButtons] = useState<QuestionButton[]>([]);
  const [items, setItems] = useState<ThreadItem[]>([]);
  const [input, setInput] = useState("");
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [dangTai, setDangTai] = useState(true);
  const [dangHoi, setDangHoi] = useState(false);
  const [loi, setLoi] = useState<string | null>(null);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const threadRef = useRef<HTMLDivElement>(null);

  const baoLoi = (e: unknown, macDinh: string) => setLoi(e instanceof ApiError ? e.message : macDinh);

  const napHoiThoai = useCallback(async () => {
    try {
      setConversations((await layHoiThoai()).items);
    } catch {
      // Danh sách bên trái hụt một lần không đáng chặn cả màn chat.
    }
  }, []);

  useEffect(() => {
    let huy = false;
    Promise.all([layChuDe(), layHoiThoai()])
      .then(([t, c]) => {
        if (huy) return;
        setTopics(t);
        setConversations(c.items);
      })
      .catch((e) => !huy && baoLoi(e, "Không tải được danh sách chủ đề."))
      .finally(() => !huy && setDangTai(false));
    return () => {
      huy = true;
    };
  }, []);

  useEffect(() => {
    const el = threadRef.current;
    if (el) el.scrollTo({ top: el.scrollHeight, behavior: "smooth" });
  }, [items, buttons]);

  async function chonChuDe(t: Topic) {
    setLoi(null);
    setTopic(t);
    setButtons([]);
    try {
      setButtons(await layCauHoi(t.id));
    } catch (e) {
      baoLoi(e, "Không tải được câu hỏi của chủ đề.");
    }
  }

  async function chonCauHoi(q: QuestionButton) {
    if (dangHoi) return;
    setDangHoi(true);
    setLoi(null);
    try {
      const kq = await hoi(q.id, conversationId);
      setItems((cur) => [...cur, { kind: "answer", message: kq.message }]);
      if (!conversationId) {
        setConversationId(kq.conversationId);
      }
      void napHoiThoai();
    } catch (e) {
      // Câu hỏi vừa bị giáo vụ ẩn: tải lại danh sách nút để không bấm lại vào nó.
      if (e instanceof ApiError && e.code === "QUESTION_NOT_FOUND" && topic?.id) {
        setButtons(await layCauHoi(topic.id).catch(() => []));
      }
      baoLoi(e, "Không lấy được câu trả lời. Vui lòng thử lại.");
    } finally {
      setDangHoi(false);
    }
  }

  async function moHoiThoai(id: string) {
    setSidebarOpen(false);
    setLoi(null);
    try {
      const ct = await chiTietHoiThoai(id);
      setConversationId(ct.id);
      setItems(ct.messages.map((m) => ({ kind: "answer", message: m })));
      setTopic(null);
      setButtons([]);
    } catch (e) {
      baoLoi(e, "Không mở được hội thoại.");
    }
  }

  function hoiThoaiMoi() {
    setConversationId(null);
    setItems([]);
    setInput("");
    setTopic(null);
    setButtons([]);
    setLoi(null);
    setSidebarOpen(false);
  }

  /** UC24 */
  async function gui() {
    const text = input.trim();
    if (text.length < 3 || dangHoi) {
      if (text.length > 0 && text.length < 3) setLoi("Câu hỏi cần ít nhất 3 ký tự.");
      return;
    }
    setDangHoi(true);
    setLoi(null);
    try {
      const kq = await hoiBangChu(text, conversationId);
      setInput("");
      if (kq.status === "ANSWERED" && kq.message) {
        const message = kq.message;
        setItems((cur) => [...cur, { kind: "answer", message }]);
        if (!conversationId && kq.conversationId) setConversationId(kq.conversationId);
        if (kq.suggestions.length) setButtons(kq.suggestions);
        if (kq.suggestions.length) setTopic({ id: "", name: "Câu hỏi liên quan", sortOrder: 0, questionCount: 0 });
        void napHoiThoai();
      } else {
        const status = kq.status === "SUGGESTIONS" ? "SUGGESTIONS" : "NO_MATCH";
        setItems((cur) => [...cur, { kind: "typed", key: crypto.randomUUID(), text, status, suggestions: kq.suggestions, sent: false }]);
      }
    } catch (e) {
      baoLoi(e, "Không gửi được câu hỏi. Vui lòng thử lại.");
    } finally {
      setDangHoi(false);
    }
  }

  /** UC25 */
  async function guiChoGiaoVu(key: string, text: string) {
    setLoi(null);
    try {
      await guiGiaoVu(text);
      setItems((cur) => cur.map((it) => (it.kind === "typed" && it.key === key ? { ...it, sent: true } : it)));
    } catch (e) {
      baoLoi(e, "Không gửi được câu hỏi cho giáo vụ.");
    }
  }

  return (
    <main className="flex h-dvh overflow-hidden bg-base">
      <Sidebar
        open={sidebarOpen}
        conversations={conversations}
        activeId={conversationId}
        onSelect={(id) => void moHoiThoai(id)}
        onNewChat={hoiThoaiMoi}
        onClose={() => setSidebarOpen(false)}
      />

      <section className="relative flex min-w-0 flex-1 flex-col">
        <div className="flex items-center gap-2.5 border-b border-border bg-surface px-4 py-2.5 md:hidden">
          <button type="button" onClick={() => setSidebarOpen(true)} className="flex size-11 items-center justify-center rounded-[8px] border border-border" aria-label="Mở menu"><Menu className="size-5" /></button>
          <span className="font-serif text-[15px] font-semibold">Hỏi đáp học vụ</span>
        </div>

        <div ref={threadRef} className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-auto max-w-[720px] px-6 pb-8 pt-10">
            {items.length === 0 && (
              <div className="pb-10 pt-6">
                <h1 className="max-w-[15em] font-serif text-[30px] font-semibold leading-10">Tra cứu quy định học vụ</h1>
                <p className="mt-3.5 max-w-[34em] text-[15px] leading-[26px] text-secondary">
                  Chọn chủ đề và câu hỏi có sẵn, hoặc gõ câu hỏi của bạn. Câu trả lời do giáo vụ khoa soạn và kèm đúng trang trong văn bản gốc.
                </p>
              </div>
            )}
            {items.map((it) =>
              it.kind === "answer" ? (
                <AnswerCard key={it.message.id} message={it.message} />
              ) : (
                <div key={it.key} className="animate-message mb-9">
                  <div className="mb-5 flex justify-end">
                    <div className="max-w-[80%] rounded-[12px] bg-sunken px-4 py-3 text-[15px] leading-[26px]">{it.text}</div>
                  </div>
                  <div className="mb-2.5 font-mono text-[11px] uppercase tracking-[0.12em] text-muted">Trả lời</div>
                  {it.status === "SUGGESTIONS" ? (
                    <>
                      <p className="m-0 text-[15px] leading-[26px]">Có phải bạn muốn hỏi:</p>
                      <div className="mt-3 flex flex-col items-stretch gap-2">
                        {it.suggestions.map((q) => (
                          <button key={q.id} type="button" disabled={dangHoi} onClick={() => void chonCauHoi(q)}
                            className="min-h-11 rounded-[8px] border border-border-strong bg-surface px-4 py-2.5 text-left text-[15px] leading-6 hover:border-accent hover:bg-accent-soft disabled:opacity-50">
                            {q.content}
                          </button>
                        ))}
                      </div>
                    </>
                  ) : (
                    <p className="m-0 text-[15px] leading-[26px]">Chưa có câu trả lời soạn sẵn cho câu hỏi này.</p>
                  )}
                  {it.sent ? (
                    <p className="mt-3 text-sm text-success">Đã gửi cho giáo vụ khoa. Câu trả lời sẽ hiện ở mục <a href="/inquiries">Câu hỏi đã gửi</a>.</p>
                  ) : (
                    <button type="button" onClick={() => void guiChoGiaoVu(it.key, it.text)}
                      className="mt-3 min-h-11 rounded-[8px] border border-border-strong px-4 text-sm font-medium hover:bg-surface">
                      {it.status === "SUGGESTIONS" ? "Không đúng — gửi câu hỏi cho giáo vụ" : "Gửi câu hỏi cho giáo vụ"}
                    </button>
                  )}
                </div>
              ),
            )}
            {dangHoi && <div role="status" className="pb-6 text-[13px] font-medium leading-5 text-muted">Đang lấy câu trả lời…</div>}
          </div>
        </div>

        <div className="border-t border-border bg-base">
          <div className="mx-auto max-h-[45dvh] max-w-[720px] overflow-y-auto px-4 pb-[22px] pt-4 sm:px-6">
            {loi && <p role="alert" className="mb-3 text-sm leading-[22px] text-danger">{loi}</p>}

            {dangTai ? (
              <p className="text-sm text-muted">Đang tải chủ đề…</p>
            ) : topic === null ? (
              <>
                <div className="mb-2.5 font-mono text-[11px] uppercase tracking-[0.12em] text-muted">Chọn chủ đề</div>
                {topics.length === 0 ? (
                  <p className="text-sm text-muted">Chưa có chủ đề nào.</p>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    {topics.map((t) => (
                      <button
                        key={t.id}
                        type="button"
                        onClick={() => void chonChuDe(t)}
                        className="min-h-11 rounded-[8px] border border-border-strong bg-surface px-4 py-2 text-left text-[15px] leading-6 hover:border-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
                      >
                        {t.name}
                        <span className="ml-2 font-mono text-[12px] text-muted">{t.questionCount}</span>
                      </button>
                    ))}
                  </div>
                )}
              </>
            ) : (
              <>
                <div className="mb-2.5 flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => { setTopic(null); setButtons([]); }}
                    className="inline-flex min-h-9 items-center gap-1.5 rounded-[8px] px-2 text-sm text-indigo hover:bg-sunken"
                  >
                    <ArrowLeft className="size-4" /> Chọn chủ đề khác
                  </button>
                  <span className="font-mono text-[11px] uppercase tracking-[0.12em] text-muted">{topic.name}</span>
                </div>
                {buttons.length === 0 ? (
                  <p className="text-sm text-muted">Chủ đề này chưa có câu hỏi.</p>
                ) : (
                  <div className="flex flex-col items-stretch gap-2">
                    {buttons.map((q) => (
                      <button
                        key={q.id}
                        type="button"
                        disabled={dangHoi}
                        onClick={() => void chonCauHoi(q)}
                        className={cn(
                          "min-h-11 rounded-[8px] border border-border-strong bg-surface px-4 py-2.5 text-left text-[15px] leading-6 hover:border-accent hover:bg-accent-soft focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent disabled:opacity-50",
                        )}
                      >
                        {q.content}
                      </button>
                    ))}
                  </div>
                )}
              </>
            )}

            <form
              className="relative mt-4 rounded-[12px] border border-border bg-surface py-3 pl-4 pr-14 focus-within:border-border-strong focus-within:ring-2 focus-within:ring-accent/10"
              onSubmit={(e) => { e.preventDefault(); void gui(); }}
            >
              <Textarea
                rows={1}
                value={input}
                maxLength={500}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); void gui(); }
                }}
                placeholder="Hoặc gõ câu hỏi của bạn…"
                className="max-h-[110px]"
                aria-label="Gõ câu hỏi"
              />
              <button type="submit" disabled={dangHoi || input.trim().length < 3} aria-label="Gửi câu hỏi"
                className="absolute bottom-2.5 right-2.5 flex size-[34px] items-center justify-center rounded-[8px] bg-accent text-white hover:bg-accent-hover disabled:opacity-40">
                <Send className="size-4" />
              </button>
            </form>
          </div>
        </div>
      </section>
    </main>
  );
}
