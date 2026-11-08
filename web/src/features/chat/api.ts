// Gọi API hỏi đáp — docs/DAC-TA-TRIEN-KHAI.md mục 6.3.
import { apiGet, apiPost } from "@/lib/api-client";

export type Topic = { id: string; name: string; sortOrder: number; questionCount: number };
export type QuestionButton = { id: string; content: string };
export type Source = { documentId: string; title: string; page: number };

/** Một lượt hỏi – đáp: câu hỏi đã chọn, câu trả lời soạn sẵn và nguồn. */
export type ChatMessage = {
  id: string;
  sentAt: string;
  /** Câu sinh viên đã gõ (UC24); null khi bấm nút. */
  typedText: string | null;
  question: { id: string; content: string };
  answer: string;
  source: Source;
};

export type ConversationSummary = { id: string; title: string; createdAt: string; messageCount: number };
export type ConversationDetail = { id: string; title: string; createdAt: string; messages: ChatMessage[] };
export type Page<T> = { items: T[]; page: number; size: number; total: number };

export const layChuDe = () => apiGet<Topic[]>("/topics");
export const layCauHoi = (topicId: string) => apiGet<QuestionButton[]>(`/topics/${topicId}/questions`);
export const hoi = (questionId: string, conversationId: string | null) =>
  apiPost<{ conversationId: string; message: ChatMessage }>("/chat", { questionId, conversationId });
export const layHoiThoai = () => apiGet<Page<ConversationSummary>>("/conversations?size=50");
export const chiTietHoiThoai = (id: string) => apiGet<ConversationDetail>(`/conversations/${id}`);

// --- UC24: gõ câu hỏi ---------------------------------------------------------
export type TypedStatus = "ANSWERED" | "SUGGESTIONS" | "NO_MATCH";
export type TypedResponse = {
  status: TypedStatus;
  conversationId: string | null;
  message: ChatMessage | null;
  suggestions: QuestionButton[];
};
export const hoiBangChu = (text: string, conversationId: string | null) =>
  apiPost<TypedResponse>("/chat/typed", { text, conversationId });

// --- UC25, UC26: câu hỏi gửi giáo vụ ------------------------------------------
export type Inquiry = {
  id: string;
  content: string;
  status: "PENDING" | "ANSWERED";
  answer: string | null;
  source: Source | null;
  createdAt: string;
  answeredAt: string | null;
  askerName: string | null;
  askerCode: string | null;
};
export const guiGiaoVu = (content: string) => apiPost<Inquiry>("/inquiries", { content });
export const cauHoiDaGui = () => apiGet<Page<Inquiry>>("/inquiries/mine?size=50");
