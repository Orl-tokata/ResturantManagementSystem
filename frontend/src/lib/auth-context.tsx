"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { post, setAccessToken } from "@/lib/api";
import type { AuthResponse, RegisterPayload, User } from "@/types/auth";

type Status = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  user: User | null;
  status: Status;
  login: (username: string, password: string) => Promise<User>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * Holds the access token in memory only — never localStorage, which an XSS
 * payload could read. Durability comes from the httpOnly refresh cookie the
 * backend sets, which JavaScript cannot touch.
 *
 * On mount we attempt a silent refresh so a page reload does not log the
 * user out.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [status, setStatus] = useState<Status>("loading");

  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        const data = await post<AuthResponse>("/auth/refresh");
        if (cancelled) return;
        setAccessToken(data.accessToken);
        setUser(data.user);
        setStatus("authenticated");
      } catch {
        if (cancelled) return;
        // No cookie, or it expired — a normal first visit.
        setAccessToken(null);
        setUser(null);
        setStatus("unauthenticated");
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const data = await post<AuthResponse>("/auth/login", { username, password });
    setAccessToken(data.accessToken);
    setUser(data.user);
    setStatus("authenticated");
    return data.user;
  }, []);

  const register = useCallback(async (payload: RegisterPayload) => {
    await post("/auth/register", payload);
  }, []);

  const logout = useCallback(async () => {
    try {
      await post("/auth/logout");
    } finally {
      // Clear locally even if the network call fails — the user asked to leave.
      setAccessToken(null);
      setUser(null);
      setStatus("unauthenticated");
    }
  }, []);

  const value = useMemo(
    () => ({ user, status, login, register, logout }),
    [user, status, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used inside <AuthProvider>");
  }
  return ctx;
}
