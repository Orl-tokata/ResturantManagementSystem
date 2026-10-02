import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NextIntlClientProvider } from "next-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../messages/en.json";
import { BranchBadge } from "@/components/layout/BranchBadge";
import type { BranchSummary, User } from "@/types/auth";

/*
 * The badge is P1's only visible result, and the half of it that matters — the
 * switcher — cannot be reached in a browser yet: nothing creates a second
 * branch until API §6.7 ships `/api/branches`. So it is covered here, where a
 * second shop can be made to exist.
 *
 * What the server does with the switch is tested over real HTTP by
 * BranchIsolationTest. This is only about what the header offers.
 */

const branches = vi.fn<() => Promise<BranchSummary[]>>();
const switchBranch = vi.fn();
const setAccessToken = vi.fn();

vi.mock("@/lib/api", () => ({
  get: () => branches(),
  post: (...args: unknown[]) => switchBranch(...args),
  setAccessToken: (...args: unknown[]) => setAccessToken(...args),
}));

const auth = { user: null as User | null, setUser: vi.fn() };
vi.mock("@/lib/auth-context", () => ({
  useAuth: () => auth,
}));

function user(branchName: string): User {
  return {
    id: 1,
    username: "admin",
    fullName: "Administrator",
    email: null,
    phone: null,
    role: "ADMIN",
    locked: false,
    lastLoginAt: null,
    branchId: 1,
    branchName,
  };
}

function renderBadge() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <NextIntlClientProvider locale="en" messages={messages}>
        <BranchBadge />
      </NextIntlClientProvider>
    </QueryClientProvider>,
  );
}

describe("BranchBadge", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    auth.user = user("Angkor Restaurant");
  });

  it("is a label, not a control, when there is only one shop", async () => {
    branches.mockResolvedValue([
      { id: 1, code: "MAIN", name: "Angkor Restaurant", current: true },
    ]);

    renderBadge();

    expect(await screen.findByText("Angkor Restaurant")).not.toBeNull();
    // A menu with one item in it is a thing to click for no reason, so with
    // one shop the badge renders no button at all.
    expect(screen.queryByRole("button")).toBeNull();
  });

  it("offers the other shops once there are any", async () => {
    branches.mockResolvedValue([
      { id: 1, code: "MAIN", name: "Angkor Restaurant", current: true },
      { id: 2, code: "TWO", name: "Second branch", current: false },
    ]);

    renderBadge();

    const button = await screen.findByRole("button", { name: /Working in/ });
    fireEvent.click(button);

    expect(await screen.findByRole("menu")).not.toBeNull();
    expect((screen.getByRole("menuitem", { name: /Second branch/ }) as HTMLButtonElement).disabled)
      .toBe(false);
    // The one you are already in is not somewhere to go.
    expect((screen.getByRole("menuitem", { name: /Angkor Restaurant/ }) as HTMLButtonElement).disabled)
      .toBe(true);
  });

  it("switching sends the id and installs the token that comes back", async () => {
    branches.mockResolvedValue([
      { id: 1, code: "MAIN", name: "Angkor Restaurant", current: true },
      { id: 2, code: "TWO", name: "Second branch", current: false },
    ]);
    switchBranch.mockResolvedValue({
      accessToken: "a-token-for-the-second-shop",
      user: user("Second branch"),
    });

    renderBadge();

    fireEvent.click(await screen.findByRole("button", { name: /Working in/ }));
    fireEvent.click(screen.getByRole("menuitem", { name: /Second branch/ }));

    await waitFor(() =>
      expect(switchBranch).toHaveBeenCalledWith("/auth/switch-branch", { branchId: 2 }),
    );
    // The new token is what scopes every request after this, so installing it
    // is the whole point of the call.
    expect(setAccessToken).toHaveBeenCalledWith("a-token-for-the-second-shop");
    expect(auth.setUser).toHaveBeenCalled();
  });
});
