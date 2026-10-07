import type { LoginRole } from "@/types/auth";

/** A sign-in account, as `GET /api/users` returns it. */
export interface UserAccount {
  id: number;
  username: string;
  fullName: string;
  email?: string;
  phone?: string;
  role: LoginRole;
  active: boolean;
  locked: boolean;
  /**
   * When an automatic lock lifts. Absent on an account an administrator locked
   * by hand, which stays locked until one lifts it — and absent, of course, on
   * an account that is not locked at all.
   */
  lockedUntil?: string;
  failedAttempts: number;
  lastLoginAt?: string;
}
