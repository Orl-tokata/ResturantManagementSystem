import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import messages from "../../../messages/en.json";
import { RequireAuth } from "@/components/auth/RequireAuth";
import type { Role, User } from "@/types/auth";

/*
 * The client-side half of authorisation.
 *
 * The backend is what actually enforces this and always will be — every admin
 * endpoint is @PreAuthorize'd and answers a cashier with 403. What this guard
 * decides is whether the browser offers a screen the server is going to refuse,
 * and whether the previous occupant's screen stays on the glass after they go.
 *
 * Both of those went wrong at once: the admin section was wrapped in a guard
 * that only asked for *a* signed-in user, so a cashier got the admin navigation
 * and a set of empty tables.
 */

const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, push: vi.fn(), back: vi.fn(), refresh: vi.fn() }),
  usePathname: () => "/admin/staff",
}));

const auth = { user: null as User | null, status: "loading" as string };
vi.mock("@/lib/auth-context", () => ({
  useAuth: () => auth,
}));

function signedIn(role: Role): User {
  return {
    id: 1,
    username: role.toLowerCase(),
    fullName: "Someone",
    email: null,
    phone: null,
    role,
    locked: false,
    lastLoginAt: null,
  };
}

function show(node: ReactNode) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {node}
    </NextIntlClientProvider>,
  );
}

const SECRET = <div data-testid="secret">staff list</div>;

describe("RequireAuth", () => {
  beforeEach(() => {
    replace.mockClear();
    auth.user = null;
    auth.status = "loading";
  });

  it("shows nothing but a spinner while the session is still being restored", () => {
    // A page reload asks the backend to refresh before it knows who this is.
    // Rendering children first would flash admin screens at everybody.
    show(<RequireAuth roles={["ADMIN"]}>{SECRET}</RequireAuth>);

    expect(screen.queryByTestId("secret")).toBeNull();
    expect(replace).not.toHaveBeenCalled();
  });

  it("renders nothing once signed out, and asks for a sign-in", () => {
    /*
     * The unmount that the cache test depends on. When a session ends, every
     * screen under the guard leaves the tree, so nothing is left subscribed to
     * a query holding the previous user's data.
     */
    auth.status = "unauthenticated";
    const { container } = show(<RequireAuth>{SECRET}</RequireAuth>);

    expect(screen.queryByTestId("secret")).toBeNull();
    expect(container.textContent).toBe("");
    expect(replace).toHaveBeenCalledWith("/login?next=%2Fadmin%2Fstaff");
  });

  it("refuses a signed-in user who does not hold the role", () => {
    auth.status = "authenticated";
    auth.user = signedIn("CASHIER");
    show(<RequireAuth roles={["ADMIN"]}>{SECRET}</RequireAuth>);

    expect(screen.queryByTestId("secret")).toBeNull();
    expect(screen.getByText(messages.common.notPermitted)).toBeTruthy();
  });

  it("lets the role through", () => {
    auth.status = "authenticated";
    auth.user = signedIn("ADMIN");
    show(<RequireAuth roles={["ADMIN"]}>{SECRET}</RequireAuth>);

    expect(screen.getByTestId("secret")).toBeTruthy();
  });

  it("asks only for a session when no role is named", () => {
    // The shared screens — the POS, order history — are open to every role.
    auth.status = "authenticated";
    auth.user = signedIn("CHEF");
    show(<RequireAuth>{SECRET}</RequireAuth>);

    expect(screen.getByTestId("secret")).toBeTruthy();
  });
});
