export type Role = "ADMIN" | "CASHIER" | "WAITER" | "CHEF";

export interface User {
  id: number;
  username: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  role: Role;
  locked: boolean;
  lastLoginAt: string | null;
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
  CASHIER: "/cashier/order",
  WAITER: "/cashier/order",
  CHEF: "/cashier/order",
};
