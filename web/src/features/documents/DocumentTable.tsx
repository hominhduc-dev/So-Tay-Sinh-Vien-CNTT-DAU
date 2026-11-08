"use client";

import { Button } from "@/components/ui/button";
import { dinhDangNgay, loaiTep, type KnowledgeDocument } from "./api";

type DocumentTableProps = {
  documents: KnowledgeDocument[];
  canEdit: boolean;
  dangGhi: string | null;
  onOpen: (doc: KnowledgeDocument) => void;
  onRename: (doc: KnowledgeDocument) => void;
  onDelete: (doc: KnowledgeDocument) => void;
};

export function DocumentTable({ documents, canEdit, dangGhi, onOpen, onRename, onDelete }: DocumentTableProps) {
  if (!documents.length) {
    return <p className="my-6 text-[15px] leading-[26px] text-muted">Không có tài liệu nào khớp.</p>;
  }

  return (
    <div className="mt-5 overflow-x-auto">
      <table className="w-full min-w-[760px] border-collapse">
        <thead>
          <tr className="border-b border-border-strong text-left text-[13px] font-medium leading-5 text-muted">
            <th scope="col" className="pb-2.5 pr-3 font-medium">Tên</th>
            <th scope="col" className="px-3 pb-2.5 font-medium">Loại</th>
            <th scope="col" className="px-3 pb-2.5 text-right font-medium">Số trang</th>
            <th scope="col" className="px-3 pb-2.5 font-medium">Người tải lên</th>
            <th scope="col" className="px-3 pb-2.5 font-medium">Ngày</th>
            <th scope="col" className="pb-2.5 pl-3 text-right font-medium">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {documents.map((doc) => {
            const khoa = dangGhi === doc.id;
            return (
              <tr key={doc.id} className="h-14 border-b border-border hover:bg-sunken">
                <td className="max-w-[340px] py-2 pr-3 text-[15px] font-medium leading-6">{doc.title}</td>
                <td className="px-3 py-2 font-mono text-[13px] text-secondary">{loaiTep(doc.mimeType)}</td>
                <td className="px-3 py-2 text-right font-mono text-[13px]">{doc.pageCount ?? "—"}</td>
                <td className="px-3 py-2 text-sm leading-[22px] text-secondary">{doc.uploadedBy}</td>
                <td className="px-3 py-2 text-sm leading-[22px] text-secondary">{dinhDangNgay(doc.createdAt)}</td>
                <td className="py-2 pl-3 text-right">
                  <div className="inline-flex gap-1">
                    <Button type="button" variant="outline" size="sm" onClick={() => onOpen(doc)}>Mở</Button>
                    {canEdit && (
                      <>
                        <Button type="button" variant="ghost" size="sm" disabled={khoa} onClick={() => onRename(doc)} className="font-normal">Đổi tên</Button>
                        <Button type="button" variant="ghost" size="sm" disabled={khoa} onClick={() => onDelete(doc)} className="font-normal hover:text-danger">
                          {khoa ? "Đang xử lý…" : "Gỡ"}
                        </Button>
                      </>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
