"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { coVai, useCurrentUser } from "@/features/auth/useAuth";
import { ApiError, openDocumentFile } from "@/lib/api-client";
import { cn } from "@/lib/utils";
import { danhSach, doiTen, taiLen, xoa, type KnowledgeDocument } from "./api";
import { DocumentTable } from "./DocumentTable";
import { UploadDropzone } from "./UploadDropzone";

type UploadItem = { key: string; name: string; state: "dang-tai" | "xong" | "loi"; message: string | null };

/** UC07 tra cứu (mọi vai), UC08 mở tệp gốc, UC10–UC12 tải lên / đổi tên / gỡ (giáo vụ trở lên). */
export function DocumentManager() {
  const user = useCurrentUser();
  const quanLy = coVai(user, "CONTENT_ADMIN");

  const [query, setQuery] = useState("");
  const [items, setItems] = useState<KnowledgeDocument[]>([]);
  const [total, setTotal] = useState(0);
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState<string | null>(null);
  const [dangGhi, setDangGhi] = useState<string | null>(null);
  const [uploads, setUploads] = useState<UploadItem[]>([]);

  // Chỉ nhận kết quả của lần gọi mới nhất: gõ nhanh thì phản hồi cũ có thể về sau.
  const luotGoi = useRef(0);
  const nap = useCallback(async () => {
    const luot = ++luotGoi.current;
    try {
      const kq = await danhSach({ q: query.trim() || undefined });
      if (luot !== luotGoi.current) return;
      setItems(kq.items);
      setTotal(kq.total);
      setLoi(null);
    } catch (e) {
      if (luot === luotGoi.current) setLoi(e instanceof ApiError ? e.message : "Không tải được danh sách tài liệu.");
    } finally {
      if (luot === luotGoi.current) setDangTai(false);
    }
  }, [query]);

  useEffect(() => {
    const hen = window.setTimeout(() => void nap(), 250);
    return () => window.clearTimeout(hen);
  }, [nap]);

  async function themTep(files: File[]) {
    for (const file of files) {
      const key = crypto.randomUUID();
      const title = file.name.replace(/\.(pdf|docx)$/i, "").trim() || file.name;
      setUploads((cur) => [{ key, name: file.name, state: "dang-tai", message: null }, ...cur]);
      try {
        await taiLen(file, title.length < 3 ? `Tài liệu ${title}` : title);
        setUploads((cur) => cur.map((x) => (x.key === key ? { ...x, state: "xong" } : x)));
      } catch (e) {
        const message = e instanceof ApiError ? e.message : "Tải lên thất bại.";
        setUploads((cur) => cur.map((x) => (x.key === key ? { ...x, state: "loi", message } : x)));
      }
    }
    void nap();
  }

  async function lam(id: string, viec: () => Promise<unknown>) {
    setDangGhi(id);
    try {
      await viec();
      await nap();
    } catch (e) {
      setLoi(e instanceof ApiError ? e.message : "Thao tác thất bại.");
    } finally {
      setDangGhi(null);
    }
  }

  function moTep(doc: KnowledgeDocument) {
    openDocumentFile(doc.id).catch((e) => setLoi(e instanceof ApiError ? e.message : "Không mở được tệp."));
  }

  function doiTenTaiLieu(doc: KnowledgeDocument) {
    const moi = window.prompt("Tên mới của tài liệu (3–500 ký tự):", doc.title)?.trim();
    if (!moi || moi === doc.title) return;
    if (moi.length < 3 || moi.length > 500) {
      setLoi("Tên tài liệu phải từ 3 đến 500 ký tự.");
      return;
    }
    void lam(doc.id, () => doiTen(doc.id, moi));
  }

  function goTaiLieu(doc: KnowledgeDocument) {
    if (!window.confirm(`Gỡ "${doc.title}"? Tệp sẽ bị xóa và không hoàn tác được.`)) return;
    void lam(doc.id, () => xoa(doc.id));
  }

  return (
    <section className="mx-auto w-full max-w-[1120px] px-6 pb-20 pt-10 sm:px-8">
      <div className="flex flex-wrap items-end gap-5">
        <div>
          <h1 className="font-serif text-[30px] font-semibold leading-[38px]">Tài liệu</h1>
          <p className="mt-2 text-[15px] leading-[26px] text-secondary">
            Văn bản gốc làm nguồn cho các câu trả lời{quanLy ? " · bạn có quyền quản lý" : ""}.
          </p>
        </div>
        {quanLy && (
          <>
            <Button className="ml-auto" onClick={() => document.getElementById("chon-tep")?.click()}>
              <Upload className="size-4" /> Tải tài liệu lên
            </Button>
            <input
              id="chon-tep"
              type="file"
              accept=".pdf,.docx"
              multiple
              className="hidden"
              onChange={(event) => {
                void themTep(Array.from(event.target.files ?? []));
                event.target.value = "";
              }}
            />
          </>
        )}
      </div>

      {quanLy && <UploadDropzone onFiles={(files) => void themTep(files)} />}

      {uploads.length > 0 && (
        <div className="mt-5 overflow-hidden rounded-[12px] border border-border bg-surface">
          {uploads.map((u) => (
            <div key={u.key} className="flex flex-wrap items-center gap-3 border-b border-sunken px-[18px] py-3 last:border-0">
              <div className="min-w-0 flex-1 truncate text-[15px] font-medium leading-6">{u.name}</div>
              <div className={cn("font-mono text-[13px]", u.state === "loi" ? "text-danger" : u.state === "xong" ? "text-success" : "text-secondary")}>
                {u.state === "dang-tai" ? "Đang tải lên…" : u.state === "xong" ? "Đã lưu" : u.message}
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="mt-8 flex flex-wrap items-center gap-3">
        <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Tìm theo tên tài liệu…" className="min-w-[220px] flex-1" maxLength={200} />
      </div>

      {loi && <p role="alert" className="mt-5 text-sm leading-[22px] text-danger">{loi}</p>}

      {dangTai ? (
        <p className="my-6 text-[15px] leading-[26px] text-muted">Đang tải…</p>
      ) : (
        <>
          <DocumentTable
            documents={items}
            canEdit={quanLy}
            dangGhi={dangGhi}
            onOpen={moTep}
            onRename={doiTenTaiLieu}
            onDelete={goTaiLieu}
          />
          <p className="mt-5 text-[13px] leading-5 text-muted">
            {total} tài liệu.{quanLy && " Không gỡ được tài liệu đang làm nguồn cho câu hỏi — đổi nguồn của câu hỏi trước."}
          </p>
        </>
      )}
    </section>
  );
}
