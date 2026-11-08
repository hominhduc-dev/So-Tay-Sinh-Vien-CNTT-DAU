// Gọi API tài liệu — docs/DAC-TA-TRIEN-KHAI.md mục 6.4.
import { apiDelete, apiGet, apiPatch, apiUpload } from "@/lib/api-client";

export type KnowledgeDocument = {
  id: string;
  title: string;
  mimeType: string;
  pageCount: number | null;
  uploadedBy: string;
  createdAt: string;
};

export type Page<T> = { items: T[]; page: number; size: number; total: number };

export function danhSach(params: { q?: string; page?: number; size?: number } = {}) {
  const qs = new URLSearchParams();
  if (params.q) qs.set("q", params.q);
  qs.set("page", String(params.page ?? 0));
  qs.set("size", String(params.size ?? 50));
  return apiGet<Page<KnowledgeDocument>>(`/documents?${qs}`);
}

export function taiLen(file: File, title: string) {
  const form = new FormData();
  form.append("file", file);
  form.append("title", title);
  return apiUpload<KnowledgeDocument>("/documents", form);
}

export const doiTen = (id: string, title: string) => apiPatch<KnowledgeDocument>(`/documents/${id}`, { title });
export const xoa = (id: string) => apiDelete(`/documents/${id}`);

export function loaiTep(mime: string): string {
  return mime === "application/pdf" ? "PDF" : "DOCX";
}

/** ISO 8601 → dd/MM/yyyy. */
export function dinhDangNgay(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
}
