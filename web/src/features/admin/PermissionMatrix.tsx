import { coQuyen, PERMISSIONS, ROLE_COLUMNS } from "./permissions";

export function PermissionMatrix() {
  return (
    <div className="mt-7">
      <div className="overflow-x-auto">
        <table className="w-full min-w-[520px] border-collapse">
          <caption className="sr-only">
            Ma trận phân quyền theo vai. Chi tiết ở docs/SRS.md mục 7.
          </caption>
          <thead>
            <tr className="border-b border-border-strong text-[13px] leading-5 text-muted">
              <th scope="col" className="pb-3 pr-4 text-left font-medium">
                Quyền
              </th>
              {ROLE_COLUMNS.map((role) => (
                <th key={role.code} scope="col" className="px-3 pb-3 text-center font-medium">
                  {role.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {PERMISSIONS.map((row) => (
              <tr key={row.name} className="h-[52px] border-b border-border">
                <th scope="row" className="py-2 pr-4 text-left text-[15px] font-normal leading-6">
                  {row.name}
                </th>
                {ROLE_COLUMNS.map((role) => {
                  const duoc = coQuyen(row, role.code);
                  return (
                    <td key={role.code} className="px-3 py-2 text-center text-[15px]">
                      {/* Dấu ✓ và — là hình ảnh; trình đọc màn hình đọc phần chữ ẩn. */}
                      <span aria-hidden="true" className={duoc ? "text-primary" : "text-muted"}>
                        {duoc ? "✓" : "—"}
                      </span>
                      <span className="sr-only">{duoc ? "Có quyền" : "Không có quyền"}</span>
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-5 max-w-[64ch] space-y-2 text-sm leading-[22px] text-muted">
        <p>Vai trên có mọi quyền của vai dưới: Quản trị viên ⊃ Giáo vụ khoa ⊃ Sinh viên.</p>
        <p>Server đọc lại vai từ CSDL ở mỗi request, nên khóa tài khoản hoặc đổi vai có hiệu lực ngay.</p>
        <p>Quản trị viên không tự khóa hay tự hạ vai của chính mình.</p>
      </div>
    </div>
  );
}
