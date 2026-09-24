import { QueryClient, QueryClientProvider, useQuery } from "@tanstack/react-query";
import { act, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider, useAuth } from "@/lib/auth-context";
import type { AuthResponse, User } from "@/types/auth";

/*
 * One user's data must not survive into the next user's session.
 *
 * The till is shared. A manager signs in, opens Staff, walks away; a cashier
 * signs in on the same tab. React Query keeps what it fetched, keyed by query
 * key and not by who fetched it, so without an explicit clear the cashier's
 * screen renders the manager's rows from cache — for as long as it takes each
 * query to refetch, and indefinitely for anything the cashier is not allowed to
 * refetch, because the failed request leaves the stale data in place.
 *
 * The backend is not the defence here and cannot be: it correctly answered the
 * manager, and it correctly returns 403 to the cashier. The leak is entirely in
 * what the browser chose to keep, which is why this test exists rather than
 * another case in the API suite.
 */

vi.mock("@/lib/api", () => ({
  post: vi.fn(),
  setAccessToken: vi.fn(),
}));

const { post, setAccessToken } = await import("@/lib/api");
const mockPost = vi.mocked(post);
const mockSetAccessToken = vi.mocked(setAccessToken);

function user(overrides: Partial<User>): User {
  return {
    id: 1,
    username: "admin",
    fullName: "Administrator",
    email: null,
    phone: null,
    role: "ADMIN",
    locked: false,
    lastLoginAt: null,
    ...overrides,
  };
}

const ADMIN = user({ id: 1, username: "admin", role: "ADMIN" });
const CASHIER = user({ id: 2, username: "cashier", fullName: "Cashier", role: "CASHIER" });

function authResponse(as: User): AuthResponse {
  return { accessToken: `token-for-${as.username}`, tokenType: "Bearer", expiresInSeconds: 900, user: as };
}

/** The staff list the manager loaded — the rows that must not carry over. */
const STAFF_KEY = ["staff", { page: 0 }];
const STAFF_ROWS = [{ id: 7, fullName: "Sok Dara", role: "WAITER" }];

/**
 * Renders the provider and hands back a handle on the context, so a test can
 * drive login/logout the way a button would.
 */
function mountAuth(client: QueryClient) {
  const handle: { current: ReturnType<typeof useAuth> | null } = { current: null };

  function Probe() {
    handle.current = useAuth();
    return null;
  }

  render(
    <QueryClientProvider client={client}>
      <AuthProvider>
        <Probe />
      </AuthProvider>
    </QueryClientProvider>,
  );

  return handle as { current: ReturnType<typeof useAuth> };
}

function newClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Infinity, staleTime: Infinity } },
  });
}

describe("a session leaves nothing behind for the next one", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // No rms_session cookie, so the provider settles as signed-out on mount
    // instead of attempting a refresh.
    document.cookie = "rms_session=; max-age=0; path=/";
  });

  it("drops the previous user's cached data when someone else signs in", async () => {
    const client = newClient();
    // The manager's session: the staff list is sitting in the cache.
    client.setQueryData(STAFF_KEY, STAFF_ROWS);
    expect(client.getQueryData(STAFF_KEY)).toEqual(STAFF_ROWS);

    const auth = mountAuth(client);
    await waitFor(() => expect(auth.current.status).toBe("unauthenticated"));

    mockPost.mockResolvedValueOnce(authResponse(CASHIER));
    await act(async () => {
      await auth.current.login("cashier", "cashier@2026");
    });

    expect(auth.current.user?.username).toBe("cashier");
    expect(client.getQueryData(STAFF_KEY)).toBeUndefined();
    expect(client.getQueryCache().getAll()).toHaveLength(0);
  });

  it("drops it on the way out too, before the next user arrives", async () => {
    const client = newClient();
    const auth = mountAuth(client);
    await waitFor(() => expect(auth.current.status).toBe("unauthenticated"));

    mockPost.mockResolvedValueOnce(authResponse(ADMIN));
    await act(async () => {
      await auth.current.login("admin", "admin@2026");
    });

    client.setQueryData(STAFF_KEY, STAFF_ROWS);

    mockPost.mockResolvedValueOnce(undefined as never);
    await act(async () => {
      await auth.current.logout();
    });

    expect(auth.current.user).toBeNull();
    expect(client.getQueryData(STAFF_KEY)).toBeUndefined();
    expect(client.getQueryCache().getAll()).toHaveLength(0);
  });

  it("clears even when the logout request fails", async () => {
    // A till on a flaky network still has to forget the person who left.
    const client = newClient();
    const auth = mountAuth(client);
    await waitFor(() => expect(auth.current.status).toBe("unauthenticated"));

    mockPost.mockResolvedValueOnce(authResponse(ADMIN));
    await act(async () => {
      await auth.current.login("admin", "admin@2026");
    });
    client.setQueryData(STAFF_KEY, STAFF_ROWS);

    mockPost.mockRejectedValueOnce(new Error("network down"));
    await act(async () => {
      await expect(auth.current.logout()).rejects.toThrow("network down");
    });

    expect(auth.current.status).toBe("unauthenticated");
    expect(client.getQueryCache().getAll()).toHaveLength(0);
    expect(mockSetAccessToken).toHaveBeenLastCalledWith(null);
  });

  it("shows the next user an empty screen while their own request is in flight", async () => {
    /*
     * The assertions above check the cache. This one checks what reaches the
     * screen, which is what actually went wrong: the cashier signs in, opens a
     * screen, and it paints from cache before any request has answered.
     *
     * The screen is mounted after the switch, which is the real sequence — both
     * login and logout happen with no data screen on the page, because
     * RequireAuth renders nothing once the session ends and the login form is
     * its own route. That unmount is load-bearing and is asserted in
     * RequireAuth.test.tsx.
     *
     * It matters because React Query keeps showing existing data while a
     * refetch is in flight, by design. So clearing the cache is only enough as
     * long as nothing is still subscribed at the moment of the switch: an
     * observer mounted across it would keep rendering its last result until its
     * own request landed. Neither clear() nor resetQueries() changes that.
     */
    const client = newClient();
    client.setQueryData(STAFF_KEY, STAFF_ROWS);

    const auth = mountAuth(client);
    await waitFor(() => expect(auth.current.status).toBe("unauthenticated"));

    mockPost.mockResolvedValueOnce(authResponse(CASHIER));
    await act(async () => {
      await auth.current.login("cashier", "cashier@2026");
    });

    // A request that never answers, standing in for one still on the wire.
    const staffRequest = vi.fn(() => new Promise<typeof STAFF_ROWS>(() => {}));

    function StaffList() {
      const { data } = useQuery({ queryKey: STAFF_KEY, queryFn: staffRequest });
      return <div data-testid="staff">{(data ?? []).map((r) => r.fullName).join(", ")}</div>;
    }

    render(
      <QueryClientProvider client={client}>
        <StaffList />
      </QueryClientProvider>,
    );

    // Empty is correct here. The manager's staff list would not be.
    expect(screen.getByTestId("staff").textContent).toBe("");
    expect(staffRequest).toHaveBeenCalled();
  });
});
