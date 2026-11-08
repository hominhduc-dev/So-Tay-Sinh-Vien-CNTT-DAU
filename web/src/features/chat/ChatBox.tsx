"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ArrowLeft, Menu } from "lucide-react";
import { Sidebar } from "@/components/layout/Sidebar";
import { ApiError } from "@/lib/api-client";
import { cn } from "@/lib/utils";
import { AnswerCard } from "./AnswerCard";
import {
  chiTietHoiThoai,
  hoi,
  layCauHoi,
  layChuDe,
  layHoiThoai,
  type ChatMessage,
  type ConversationSummary,
  type QuestionButton,
  type Topic,
} from "./api";

/**
 * UC05 — chatbot dạng nút chọn: chủ đề → câu hỏi → câu trả lời soạn sẵn kèm nguồn.
 * Không có ô nhập tự do: hệ thống chỉ trả lời các câu hỏi giáo vụ đã soạn (BR06).
 */
export function ChatBox() {
  const [topics, setTopics] = useState<Topic[]>([]);
  const [topic, setTopic] = useState<Topic | null>(null);
  const [buttons, setButtons] = useState<QuestionButton[]>([]);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
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
  }, [messages, buttons]);

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
      setMessages((cur) => [...cur, kq.message]);
      if (!conversationId) {
        setConversationId(kq.conversationId);
      }
      void napHoiThoai();
    } catch (e) {
      // Câu hỏi vừa bị giáo vụ ẩn: tải lại danh sách nút để không bấm lại vào nó.
      if (e instanceof ApiError && e.code === "QUESTION_NOT_FOUND" && topic) {
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
      setMessages(ct.messages);
      setTopic(null);
      setButtons([]);
    } catch (e) {
      baoLoi(e, "Không mở được hội thoại.");
    }
  }

  function hoiThoaiMoi() {
    setConversationId(null);
    setMessages([]);
    setTopic(null);
    setButtons([]);
    setLoi(null);
    setSidebarOpen(false);
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
            {messages.length === 0 && (
              <div className="pb-10 pt-6">
                <h1 className="max-w-[15em] font-serif text-[30px] font-semibold leading-10">Tra cứu quy định học vụ</h1>
                <p className="mt-3.5 max-w-[34em] text-[15px] leading-[26px] text-secondary">
                  Chọn một chủ đề, rồi chọn câu hỏi. Câu trả lời do giáo vụ khoa soạn và luôn kèm đúng trang trong văn bản gốc.
                </p>
              </div>
            )}
            {messages.map((m) => <AnswerCard key={m.id} message={m} />)}
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
          </div>
        </div>
      </section>
    </main>
  );
}
