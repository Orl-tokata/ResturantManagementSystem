import { HOME_BY_ROLE, type Role } from "@/types/auth";

/**
 * The one part of the site not open to every signed-in user.
 *
 * <p>Kept here rather than read off the route tree because the login screen has
 * to answer the question before the route is ever mounted. It must agree with
 * the roles passed to RequireAuth in app/(protected)/admin/layout.tsx, which
 * AdminLayout's own test pins.
 */
export const ADMIN_PREFIX = "/admin";

/**
 * Where to send someone once they have signed in.
 *
 * <p>{@code next} is whatever arrived in the query string, so it is attacker-
 * controlled and gets two checks rather than one:
 *
 * <ul>
 *   <li>it must be a local path — an absolute URL would make the login screen an
 *       open redirect, and {@code //host} is absolute despite the leading slash;
 *   <li>the signed-in user must actually be allowed to open it. RequireAuth
 *       appends {@code ?next=} to the URL it bounced someone off, so a cashier
 *       who wandered into /admin/staff carries that path to the login screen and
 *       would be sent straight back to a page they cannot use.
 * </ul>
 *
 * Anything that fails either check falls back to the role's own home, which is
 * the same place a plain visit to /login ends up.
 */
export function landingPath(next: string | null, role: Role): string {
  const home = HOME_BY_ROLE[role];
  if (next === null) return home;

  const local = next.startsWith("/") && !next.startsWith("//");
  if (!local) return home;

  const adminOnly = next === ADMIN_PREFIX || next.startsWith(`${ADMIN_PREFIX}/`);
  if (adminOnly && role !== "ADMIN") return home;

  return next;
}
