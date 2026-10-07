// MANAGER arrived with V17; the role CHECK refused it until then, so it
// existed in @PreAuthorize and nowhere else.
export type Role = "ADMIN" | "MANAGER" | "CASHIER" | "WAITER" | "CHEF";

/**
 * The roles that may hold a login, which is not all of them.
 *
 * <p>{@link Role} is a job title on an HR record — a chef has a row in staff,
 * not a password. Both used to be the same type, so the account form offered
 * every title and a CHEF login was one dropdown away from the till: nothing
 * below ADMIN was told apart, and orders, payments, cash shifts and refunds
 * ask only for a signed-in user. Role.canSignIn() is the server's half of
 * this; keeping the two types apart is the client's.
 *
 * <p>MANAGER is absent on purpose. The backend has granted it admin work
 * since P3 and V17 widened the role CHECK, but no screen creates one and the
 * admin shell turns it away — so it is unfinished, not available.
 */
export type LoginRole = Extract<Role, "ADMIN" | "CASHIER">;

/** For a dropdown that issues a login. */
export const LOGIN_ROLES: LoginRole[] = ["ADMIN", "CASHIER"];

export interface User {
  id: number;
  username: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  role: LoginRole;
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
  role: LoginRole;
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
export const HOME_BY_ROLE: Record<LoginRole, string> = {
  ADMIN: "/admin",
  CASHIER: "/cashier/order",
};
