"use client";

import { useCallback, useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api-client";
import { cn } from "@/lib/utils";
import { trangThai, type Health } from "./api";

function thoiLuong(giay: number): string {
  const d = Math.floor(giay / 86400);
  const h = Math.floor((giay % 86400) / 3600);
  const m = Math.floor((giay % 3600) / 60);
  return [d && `${d} ngày`, h && `${h} giờ`, `${m} phút`].filter(Boolean).join(" ");
}

/** UC20 — Kiểm tra trạng thái hệ thống. */
export function HealthPanel() {
  const [data, setData] = useState<Health | null>(null);
  const [loi, setLoi] = useState<string | null>(null);
  const [dangTai, setDangTai] = useState(false);

  const nap = useCallback(async () => {
    setDangTai(true);
    try {
      setData(await trangThai());
      setLoi(null);
    } catch (e) {
      setLoi(e instanceof ApiError ? e.message : "Không gọi được server.");
    } finally {
      setDangTai(false);
    }
  }, []);

  // Lần đầu: gán state trong callback của promise, không gán đồng bộ trong effect.
  useEffect(() => {
    let huy = false;
    trangThai()
      .then((d) => !huy && setData(d))
      .catch((e: unknown) => !huy && setLoi(e instanceof ApiError ? e.message : "Không gọi được server."));
    return () => {
      huy = true;
    };
  }, []);

  const o = (nhan: string, giaTri: string, tot?: boolean) => (
    <div className="rounded-[12px] border border-border bg-surface p-5">
      <div className="text-[13px] font-medium text-muted">{nhan}</div>
      <div className={cn("mt-1.5 font-mono text-[20px]", tot === true && "text-success", tot === false && "text-danger")}>{giaTri}</div>
    </div>
  );

  return (
    <div className="mt-7">
      {loi && <p role="alert" className="mb-4 rounded-[8px] border border-danger/30 bg-danger/5 px-4 py-3 text-sm text-danger">{loi} — server có thể đang dừng.</p>}
      {data && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {o("Server", data.status, data.status === "UP")}
          {o("Cơ sở dữ liệu", data.database, data.database === "UP")}
          {o("Thời gian chạy liên tục", thoiLuong(data.uptimeSeconds))}
          {o("Giờ server", new Date(data.time).toLocaleTimeString("vi-VN"))}
        </div>
      )}
      <Button type="button" variant="outline" size="sm" className="mt-5" disabled={dangTai} onClick={() => void nap()}>
        {dangTai ? "Đang kiểm tra…" : "Kiểm tra lại"}
      </Button>
    </div>
  );
}
