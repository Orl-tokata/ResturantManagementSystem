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
  /**
   * Present on failures only: the server's message key, e.g.
   * `order.table.occupied`. Branch on this rather than on `message`, which is
   * prose and changes with the request's language.
   */
  code?: string;
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

/**
 * The same base, for URLs a browser fetches by itself.
 *
 * <p>An `<img src>` is not an axios request, so it carries none of the headers
 * the interceptor adds — which is why product photographs are served from a
 * path that needs no token.
 */
export const API_BASE_URL = BASE_URL;

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

/* ---- Idempotency ---------------------------------------------------------
   The server refuses these endpoints without an `Idempotency-Key`, so one
   repeated request cannot become two bills. Mirrors IdempotencyFilter.REQUIRED
   on the backend — the two lists have to be changed together.
   ------------------------------------------------------------------------- */

const IDEMPOTENT_ROUTES: RegExp[] = [
  /^\/orders$/,
  /^\/orders\/[^/]+\/pay$/,
  /^\/orders\/[^/]+\/cancel$/,
  /^\/stock\/[^/]+\/adjust$/,
  /^\/purchases\/[^/]+\/receive$/,
  // A refund is money leaving. A tap repeated on bad Wi-Fi must not take it
  // out of the drawer twice.
  /^\/returns$/,
];

/** Exported for its own test: the route list is where a mistake would hide. */
export function needsIdempotencyKey(method: string | undefined, url: string | undefined): boolean {
  if (method?.toLowerCase() !== "post" || !url) return false;
  const path = url.split("?")[0].replace(/\/+$/, "");
  return IDEMPOTENT_ROUTES.some((r) => r.test(path));
}

/**
 * A fresh key. Pass one to `post()` when a screen needs a *stable* key for one
 * user intent — the same value across a retry, a reconnect and a second tap of
 * the same button — which is the case this whole mechanism exists for.
 *
 * Without an explicit key the interceptor still supplies one, so the request is
 * accepted and a network-level retry is deduplicated; but two separate taps are
 * two separate requests with two separate keys, and the server cannot tell they
 * meant the same thing. Disabling the button is not a substitute for a stable
 * key, it is the other half of it.
 */
export function newIdempotencyKey(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  // Older Safari and any non-secure context. Only needs to be unique, not
  // unguessable — the key names a request, it does not authorise one.
  return `k-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
}

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) {
    config.headers.set("Authorization", `Bearer ${accessToken}`);
  }
  if (needsIdempotencyKey(config.method, config.url)
      && !config.headers.get("Idempotency-Key")) {
    // Set on the config, so the retry after a 401 refresh re-sends this exact
    // value rather than minting a second one and defeating the point.
    config.headers.set("Idempotency-Key", newIdempotencyKey());
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

export async function post<T>(
  url: string,
  body?: unknown,
  idempotencyKey?: string,
): Promise<T> {
  const res = await api.post<ApiResponse<T>>(url, body, {
    headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : undefined,
  });
  return res.data.data;
}

/**
 * Sends a file.
 *
 * <p>The Content-Type is left unset on purpose: the browser sets it, and only
 * the browser knows the multipart boundary it is about to generate. Declaring
 * `multipart/form-data` by hand produces a request with no boundary, which the
 * server cannot parse.
 */
export async function upload<T>(url: string, file: File, field = "file"): Promise<T> {
  const form = new FormData();
  form.append(field, file);
  const res = await api.post<ApiResponse<T>>(url, form, {
    headers: { "Content-Type": undefined },
    // A photograph over a slow connection outlasts the default.
    timeout: 60_000,
  });
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
