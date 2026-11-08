// ---------------------------------------------------------------------------
// Cầu nối duy nhất tới API Spring Boot. Không component nào gọi `fetch` trực tiếp.
//
// Thành công: thân response là dữ liệu JSON (hoặc rỗng với 204).
// Thất bại:   { "error": { "code": "...", "message": "..." } }  — docs/DAC-TA-TRIEN-KHAI.md mục 6.1
//
// Gọi qua đường dẫn tương đối `/api/...`; next.config.ts chuyển tiếp sang server.
// ---------------------------------------------------------------------------

export const API_BASE = "/api";

/** Khóa localStorage giữ token. Chỉ `features/auth` và file này đụng tới. */
export const TOKEN_KEY = "tang-thu-token";

/** Lỗi có mã, giữ nguyên `code` server trả về để giao diện xử lý đúng từng trường hợp. */
export class ApiError extends Error {
  readonly code: string;
  readonly status: number;

  constructor(code: string, message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
  }

  /** Token hỏng, hết hạn, hoặc tài khoản bị khóa — đưa về màn đăng nhập. */
  get isAuthFailure(): boolean {
    return this.status === 401;
  }
}

export function readToken(): string | null {
  if (typeof window === "undefined") return null;
  try {
    return window.localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

function authHeader(): Record<string, string> {
  const token = readToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function send(path: string, init: RequestInit): Promise<Response> {
  try {
    return await fetch(`${API_BASE}${path}`, init);
  } catch {
    throw new ApiError("NETWORK_ERROR", "Không kết nối được máy chủ. Kiểm tra mạng hoặc server đã chạy chưa.", 0);
  }
}

async function toError(res: Response): Promise<ApiError> {
  const body = (await res.json().catch(() => null)) as { error?: { code?: string; message?: string } } | null;
  return new ApiError(
    body?.error?.code ?? "UNKNOWN",
    body?.error?.message ?? `Yêu cầu thất bại (HTTP ${res.status}).`,
    res.status,
  );
}

export async function apiClient<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await send(path, {
    ...init,
    headers: {
      ...(init.body && !(init.body instanceof FormData) ? { "Content-Type": "application/json" } : {}),
      ...authHeader(),
      ...init.headers,
    },
  });
  if (!res.ok) throw await toError(res);
  if (res.status === 204) return null as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : null) as T;
}

export const apiGet = <T>(path: string) => apiClient<T>(path);
export const apiPost = <T>(path: string, body: unknown) =>
  apiClient<T>(path, { method: "POST", body: JSON.stringify(body) });
export const apiPut = <T>(path: string, body: unknown) =>
  apiClient<T>(path, { method: "PUT", body: JSON.stringify(body) });
export const apiPatch = <T>(path: string, body: unknown) =>
  apiClient<T>(path, { method: "PATCH", body: JSON.stringify(body) });
export const apiDelete = (path: string) => apiClient<null>(path, { method: "DELETE" });

/** Gửi multipart/form-data — trình duyệt tự đặt Content-Type kèm boundary. */
export const apiUpload = <T>(path: string, form: FormData) => apiClient<T>(path, { method: "POST", body: form });

/**
 * Mở tệp gốc ở tab mới, nhảy tới trang `page` (UC08).
 *
 * Endpoint tệp cần header Authorization nên không dùng được thẻ <a> thường: tải về
 * dạng blob rồi mở URL tạm. Mở tab NGAY trong lúc xử lý cú bấm để trình duyệt
 * không chặn popup, sau đó mới gán địa chỉ.
 */
export async function openDocumentFile(documentId: string, page?: number): Promise<void> {
  const tab = window.open("", "_blank");
  try {
    const res = await send(`/documents/${documentId}/file`, { headers: authHeader() });
    if (!res.ok) throw await toError(res);
    const url = URL.createObjectURL(await res.blob());
    const target = page ? `${url}#page=${page}` : url;
    if (tab) tab.location.href = target;
    else window.location.href = target;
  } catch (error) {
    tab?.close();
    throw error;
  }
}
