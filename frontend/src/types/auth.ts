// MANAGER arrived with V17; the role CHECK refused it until then, so it
// existed in @PreAuthorize and nowhere else.
export type Role = "ADMIN" | "MANAGER" | "CASHIER" | "WAITER" | "CHEF";

export interface User {
  id: number;
  username: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  role: Role;
  locked: boolean;
  lastLoginAt: string | null;
  /** The shop this session is working in. Comes from the signed token. */
  branchId?: number;
  branchName?: string;
}

/** One shop a user may work in, for the badge's menu. */
export interface BranchSummary {
  id: number;
  code: string;
  name: string;
  nameEn?: string;
  current: boolean;
}

/**
 * What an administrator supplies to create someone's login.
 *
 * <p>Note the role: it is chosen by the caller, which is only safe because the
 * endpoint refuses anyone who is not already an administrator. This payload
 * used to be sent by a public signup form, where it let anyone make themselves
 * an admin.
 */
export interface AccountRequest {
  username: string;
  password: string;
  fullName: string;
  email?: string;
  phone?: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: User;
}

export interface VerifyOtpResponse {
  resetToken: string;
  expiresAt: string;
}

/** Where each role lands after signing in. */
export const HOME_BY_ROLE: Record<Role, string> = {
  ADMIN: "/admin",
  // A manager runs a shop rather than a till, so they land where the figures
  // are, like an admin.
  MANAGER: "/admin",
  CASHIER: "/cashier/order",
  WAITER: "/cashier/order",
  CHEF: "/cashier/order",
};
