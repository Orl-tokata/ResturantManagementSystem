import axios, {
  type AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from "axios";

/**
 * Single axios instance for the whole app. Components must not call `fetch`
 * directly — see PROJECT-SPEC.md §11.
 */

export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8081/api";

/* -------------------------------------------------------------------------
   Access token lives in memory only. The refresh token is an httpOnly cookie
   set by the backend, so it is never readable from JavaScript.
   ------------------------------------------------------------------------- */

let accessToken: string | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function getAccessToken() {
  return accessToken;
}

export const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
  timeout: 15_000,
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) {
    config.headers.set("Authorization", `Bearer ${accessToken}`);
  }
  return config;
});

/* ---- Refresh-on-401, with a single in-flight refresh ---------------------- */

let refreshing: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  try {
    const res = await axios.post<ApiResponse<{ accessToken: string }>>(
      `${BASE_URL}/auth/refresh`,
      {},
      { withCredentials: true },
    );
    const token = res.data.data.accessToken;
    setAccessToken(token);
    return token;
  } catch {
    setAccessToken(null);
    return null;
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as
      | (InternalAxiosRequestConfig & { _retried?: boolean })
      | undefined;

    const isAuthCall = original?.url?.includes("/auth/");

    if (error.response?.status === 401 && original && !original._retried && !isAuthCall) {
      original._retried = true;

      // Collapse concurrent 401s into one refresh call.
      refreshing ??= refreshAccessToken().finally(() => {
        refreshing = null;
      });

      const token = await refreshing;
      if (token) {
        original.headers.set("Authorization", `Bearer ${token}`);
        return api(original);
      }

      // Refresh failed too — the session is genuinely over.
      if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
        window.location.href = "/login?reason=expired";
      }
    }

    return Promise.reject(error);
  },
);

/* ---- Thin helpers that unwrap the ApiResponse envelope -------------------- */

export async function get<T>(url: string, params?: object): Promise<T> {
  const res = await api.get<ApiResponse<T>>(url, { params });
  return res.data.data;
}

export async function post<T>(url: string, body?: unknown): Promise<T> {
  const res = await api.post<ApiResponse<T>>(url, body);
  return res.data.data;
}

export async function put<T>(url: string, body?: unknown): Promise<T> {
  const res = await api.put<ApiResponse<T>>(url, body);
  return res.data.data;
}

export async function patch<T>(url: string, body?: unknown): Promise<T> {
  const res = await api.patch<ApiResponse<T>>(url, body);
  return res.data.data;
}

export async function del<T>(url: string): Promise<T> {
  const res = await api.delete<ApiResponse<T>>(url);
  return res.data.data;
}
