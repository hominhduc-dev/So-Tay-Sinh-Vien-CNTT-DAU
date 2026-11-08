"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Input } from "@/components/ui/input";
import { nhanVai, useCurrentUser, type RoleCode } from "@/features/auth/useAuth";
import { ApiError } from "@/lib/api-client";
import { cn } from "@/lib/utils";
import { danhSachNguoiDung, doiVai, khoaMo, type UserItem } from "./api";

/** UC14 tra cứu, UC15 khóa / mở, UC16 đổi vai. Mỗi tài khoản có đúng một vai và một đơn vị. */
export function UserTable() {
  const me = useCurrentUser();
  const [items, setItems] = useState<UserItem[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState("");
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState<string | null>(null);
  /** Id đang có yêu cầu ghi dở — khóa nút để không bấm hai lần. */
  const [dangGhi, setDangGhi] = useState<string | null>(null);

  // Đánh số lượt gọi: phản hồi của lần gõ trước có thể về SAU lần gõ sau và ghi
  // đè kết quả mới bằng kết quả cũ.
  const luotGoi = useRef(0);

  const nap = useCallback(async () => {
    const luot = ++luotGoi.current;
    setDangTai(true);
    try {
      const kq = await danhSachNguoiDung({ q: query.trim() || undefined });
      if (luot !== luotGoi.current) return;
      setItems(kq.items);
      setTotal(kq.total);
      setLoi(null);
    } catch (e: unknown) {
      if (luot !== luotGoi.current) return;
      setLoi(e instanceof ApiError ? e.message : "Không tải được danh sách người dùng.");
    } finally {
      if (luot === luotGoi.current) setDangTai(false);
    }
  }, [query]);

  useEffect(() => {
    const t = setTimeout(nap, query ? 300 : 0);
    return () => clearTimeout(t);
  }, [nap, query]);

  async function xuLy(id: string, viec: () => Promise<unknown>) {
    setDangGhi(id);
    try {
      await viec();
      await nap();
      setLoi(null);
    } catch (e: unknown) {
      setLoi(e instanceof ApiError ? e.message : "Thao tác thất bại.");
    } finally {
      setDangGhi(null);
    }
  }

  return (
    <div className="mt-7">
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Tìm theo tên, email hoặc mã số"
          aria-label="Tìm người dùng"
          className="max-w-[320px]"
        />
        <span className="font-mono text-[13px] text-muted">
          {dangTai ? "Đang tải…" : `${total} người dùng`}
        </span>
      </div>

      {loi && (
        <p
          role="alert"
          className="mb-4 rounded-[8px] border border-danger/30 bg-danger/5 px-4 py-3 text-sm leading-[22px] text-danger"
        >
          {loi}
        </p>
      )}

      <div className="overflow-x-auto">
        <table className="w-full min-w-[860px] border-collapse">
          <thead>
            <tr className="border-b border-border-strong text-left text-[13px] leading-5 text-muted">
              <th scope="col" className="pb-2.5 pr-3 font-medium">
                Họ tên
              </th>
              <th scope="col" className="px-3 pb-2.5 font-medium">
                Email
              </th>
              <th scope="col" className="px-3 pb-2.5 font-medium">
                Đơn vị
              </th>
              <th scope="col" className="px-3 pb-2.5 font-medium">
                Vai
              </th>
              <th scope="col" className="px-3 pb-2.5 font-medium">
                Trạng thái
              </th>
              <th scope="col" className="pb-2.5 pl-3 text-right font-medium">
                Thao tác
              </th>
            </tr>
          </thead>
          <tbody>
            {items.map((user) => {
              const laToi = user.id === me?.id;
              const khoa = dangGhi === user.id;
              return (
                <tr
                  key={user.id}
                  className={cn(
                    "border-b border-border align-top hover:bg-sunken",
                    !user.active && "opacity-60",
                  )}
                >
                  <td className="py-3 pr-3 text-[15px] font-medium leading-6">
                    {user.fullName}
                    {user.code && (
                      <span className="ml-2 font-mono text-[12px] text-muted">{user.code}</span>
                    )}
                  </td>
                  <td className="px-3 py-3 font-mono text-[13px] text-secondary">{user.email}</td>
                  <td className="px-3 py-3 text-sm text-secondary">{user.department}</td>
                  <td className="px-3 py-3">
                    <select
                      aria-label={`Vai của ${user.fullName}`}
                      value={user.role}
                      disabled={khoa || laToi}
                      title={laToi ? "Không thể tự đổi vai của chính mình" : undefined}
                      onChange={(event) => {
                        const role = event.target.value as RoleCode;
                        void xuLy(user.id, () => doiVai(user.id, role));
                      }}
                      className="min-h-8 rounded-[6px] border border-border-strong px-2 text-[13px] disabled:opacity-50"
                    >
                      <option value="USER">{nhanVai("USER")}</option>
                      <option value="CONTENT_ADMIN">{nhanVai("CONTENT_ADMIN")}</option>
                      <option value="SYSTEM_ADMIN">{nhanVai("SYSTEM_ADMIN")}</option>
                    </select>
                  </td>
                  <td className="px-3 py-3 text-sm leading-[22px]">
                    <span className={user.active ? "text-success" : "text-danger"}>
                      {user.active ? "Đang hoạt động" : "Đã khóa"}
                    </span>
                  </td>
                  <td className="py-3 pl-3 text-right">
                    <button
                      type="button"
                      disabled={khoa || laToi}
                      title={
                        laToi ? "Không thể tự khóa tài khoản của chính mình" : undefined
                      }
                      onClick={() => xuLy(user.id, () => khoaMo(user.id, !user.active))}
                      className="min-h-10 rounded-[8px] border border-border-strong px-3 text-sm hover:bg-surface disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {user.active ? "Khóa" : "Mở khóa"}
                    </button>
                  </td>
                </tr>
              );
            })}
            {!dangTai && items.length === 0 && (
              <tr>
                <td colSpan={6} className="py-8 text-center text-[15px] leading-6 text-muted">
                  Không có người dùng nào khớp.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <p className="mt-5 max-w-[64ch] text-sm leading-[22px] text-muted">
        Khóa KHÔNG xóa tài khoản — lịch sử hội thoại và tài liệu đã tải lên vẫn giữ. Thay đổi có hiệu lực ngay ở
        request kế tiếp của người đó. Tài khoản mới được cấp trực tiếp trong CSDL; hệ thống không có tự đăng ký.
      </p>
    </div>
  );
}
