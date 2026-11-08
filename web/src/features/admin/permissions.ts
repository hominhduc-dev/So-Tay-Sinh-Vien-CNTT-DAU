import type { RoleCode } from "@/features/auth/useAuth";

export type { RoleCode };

export const ROLE_COLUMNS: { code: RoleCode; label: string }[] = [
  { code: "USER", label: "Sinh viên" },
  { code: "CONTENT_ADMIN", label: "Giáo vụ khoa" },
  { code: "SYSTEM_ADMIN", label: "Quản trị viên" },
];

export type PermissionRow = { name: string; allowed: RoleCode[] };

/** Ma trận quyền — khớp docs/SRS.md mục 7. */
export const PERMISSIONS: PermissionRow[] = [
  { name: "Đăng nhập / đăng xuất, đổi mật khẩu", allowed: ["USER", "CONTENT_ADMIN", "SYSTEM_ADMIN"] },
  { name: "Chọn câu hỏi, xem hội thoại của mình", allowed: ["USER", "CONTENT_ADMIN", "SYSTEM_ADMIN"] },
  { name: "Đọc tài liệu, mở tệp gốc", allowed: ["USER", "CONTENT_ADMIN", "SYSTEM_ADMIN"] },
  { name: "Quản lý tài liệu, chủ đề, câu hỏi – câu trả lời", allowed: ["CONTENT_ADMIN", "SYSTEM_ADMIN"] },
  { name: "Xem, khóa / mở, phân quyền tài khoản", allowed: ["SYSTEM_ADMIN"] },
  { name: "Kiểm tra trạng thái hệ thống", allowed: ["SYSTEM_ADMIN"] },
];

export function coQuyen(row: PermissionRow, role: RoleCode): boolean {
  return row.allowed.includes(role);
}
