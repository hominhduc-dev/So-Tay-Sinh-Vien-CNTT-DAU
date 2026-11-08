// Gọi API quản lý nội dung — UC22, UC23 (docs/DAC-TA-TRIEN-KHAI.md mục 6.4).
import { apiDelete, apiGet, apiPatch, apiPost } from "@/lib/api-client";
import type { Topic } from "@/features/chat/api";

export type { Topic };

export type EditorQuestion = {
  id: string;
  topicId: string;
  content: string;
  answer: string;
  documentId: string;
  documentTitle: string;
  page: number;
  active: boolean;
  sortOrder: number;
};

export type QuestionInput = {
  topicId: string;
  content: string;
  answer: string;
  documentId: string;
  page: number;
  sortOrder: number;
  active: boolean;
};

export const layChuDe = () => apiGet<Topic[]>("/topics");
export const taoChuDe = (name: string, sortOrder: number) => apiPost<Topic>("/topics", { name, sortOrder });
export const suaChuDe = (id: string, body: { name?: string; sortOrder?: number }) => apiPatch<Topic>(`/topics/${id}`, body);
export const xoaChuDe = (id: string) => apiDelete(`/topics/${id}`);

export const layCauHoiSoan = (topicId: string) => apiGet<EditorQuestion[]>(`/admin/questions?topicId=${topicId}`);
export const taoCauHoi = (input: QuestionInput) => apiPost<EditorQuestion>("/questions", input);
export const suaCauHoi = (id: string, input: Partial<QuestionInput>) => apiPatch<EditorQuestion>(`/questions/${id}`, input);
export const anCauHoi = (id: string) => apiDelete(`/questions/${id}`);
