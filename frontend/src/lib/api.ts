import axios, {
  type AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from "axios";
import { DEFAULT_LOCALE, LOCALE_COOKIE, isLocale } from "@/i18n/config";

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

/**
 * Which language the API should answer in.
 *
 * <p>Read from the cookie rather than from next-intl: this module is not a
 * component, so there is no useLocale() to call, and the interceptor has to
 * serve requests fired from event handlers and react-query alike. It is the
 * same cookie the server reads, so the two cannot disagree about the locale.
 */
function preferredLanguage(): string {
  if (typeof document === "undefined") return DEFAULT_LOCALE;
  const prefix = LOCALE_COOKIE + "=";
  const value = document.cookie
    .split(";")
    .map((c) => c.trim())
    .find((c) => c.startsWith(prefix))
    ?.slice(prefix.length);
  return isLocale(value) ? value : DEFAULT_LOCALE;
}

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) {
    config.headers.set("Authorization", `Bearer ${accessToken}`);
  }
  // Without this the backend answers in its default language, and a Khmer
  // cashier reads English validation messages under a Khmer form.
  config.headers.set("Accept-Language", preferredLanguage());
  return config;
});

/* ---- Refresh-on-401, with a single in-flight refresh ---------------------- */

let refreshing: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  try {
    const res = await axios.post<ApiResponse<{ accessToken: string }>>(
      `${BASE_URL}/auth/refresh`,
      {},
      // Bare axios, not the instance: the instance would try to refresh
      // again on a 401 from here. So the language header is set by hand.
      { withCredentials: true, headers: { "Accept-Language": preferredLanguage() } },
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
      //
      // A hard navigation is intentional here, not router.push(): this runs
      // outside React, and a full reload is the only way to guarantee every
      // cached query and in-memory token from the dead session is discarded.
      if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
        // eslint-disable-next-line @next/next/no-location-assign-relative-destination
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
