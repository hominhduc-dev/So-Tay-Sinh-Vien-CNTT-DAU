// Gọi API quản trị — docs/DAC-TA-TRIEN-KHAI.md mục 6.5.
import type { AuthUser, RoleCode } from "@/features/auth/useAuth";
import { apiGet, apiPatch } from "@/lib/api-client";

export type UserItem = AuthUser;
export type Page<T> = { items: T[]; page: number; size: number; total: number };
export type Health = { status: "UP" | "DEGRADED"; database: "UP" | "DOWN"; uptimeSeconds: number; time: string };

export function danhSachNguoiDung(params: { q?: string; page?: number } = {}) {
  const qs = new URLSearchParams();
  if (params.q) qs.set("q", params.q);
  qs.set("page", String(params.page ?? 0));
  qs.set("size", "100");
  return apiGet<Page<UserItem>>(`/users?${qs}`);
}

/** UC15 — khóa / mở. Server chặn tự khóa chính mình (BR04). */
export const khoaMo = (id: string, active: boolean) => apiPatch<UserItem>(`/users/${id}`, { active });

/** UC16 — đổi vai. Server chặn tự đổi vai của chính mình (BR04). */
export const doiVai = (id: string, role: RoleCode) => apiPatch<UserItem>(`/users/${id}`, { role });

/** UC20 */
export const trangThai = () => apiGet<Health>("/health");
