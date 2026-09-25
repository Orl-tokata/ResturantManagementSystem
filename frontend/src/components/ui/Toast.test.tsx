import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ToastProvider, useToast } from "./Toast";

/*
 * Toasts disappear on a timer, which is the whole reason to test them rather
 * than to click around and trust what was on screen a moment ago. A toast that
 * never leaves becomes furniture; one that leaves too early is a message the
 * reader never got.
 */

function harness() {
  const api: { current: ReturnType<typeof useToast> | null } = { current: null };

  function Probe() {
    api.current = useToast();
    return null;
  }

  render(
    <ToastProvider>
      <Probe />
    </ToastProvider>,
  );

  return api as { current: ReturnType<typeof useToast> };
}

describe("toasts", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("shows nothing until something is pushed", () => {
    harness();
    expect(screen.queryByRole("status")).toBeNull();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("announces a success politely, then takes it away", () => {
    const toast = harness();

    act(() => toast.current.success("Settings saved"));
    const shown = screen.getByRole("status");
    expect(shown.textContent).toContain("Settings saved");

    // Long enough to read, short enough not to linger.
    act(() => void vi.advanceTimersByTime(3999));
    expect(screen.queryByRole("status")).not.toBeNull();

    act(() => void vi.advanceTimersByTime(1));
    expect(screen.queryByRole("status")).toBeNull();
  });

  it("gives a failure the louder role and more time", () => {
    const toast = harness();

    act(() => toast.current.error("Could not delete the supplier"));
    // role=alert, because a failure is worth interrupting for; a confirmation
    // is not.
    expect(screen.getByRole("alert").textContent).toContain("Could not delete the supplier");

    act(() => void vi.advanceTimersByTime(4000));
    expect(screen.queryByRole("alert")).not.toBeNull();

    act(() => void vi.advanceTimersByTime(4000));
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("keeps the newest three and drops the rest", () => {
    // A burst should not bury the screen, and the newest are the ones
    // describing what just happened.
    const toast = harness();

    act(() => {
      toast.current.info("one");
      toast.current.info("two");
      toast.current.info("three");
      toast.current.info("four");
    });

    const shown = screen.getAllByRole("status").map((n) => n.textContent);
    expect(shown).toHaveLength(3);
    expect(shown.join(" ")).not.toContain("one");
    expect(shown.join(" ")).toContain("four");
  });

  it("can be dismissed by hand before it expires", () => {
    const toast = harness();
    act(() => toast.current.success("Login created"));

    act(() => screen.getByRole("button", { name: "Dismiss" }).click());
    expect(screen.queryByRole("status")).toBeNull();
  });

  it("refuses to be used outside the provider", () => {
    // Otherwise a page renders fine until the first failure, and then the
    // error handler is what crashes.
    function Orphan() {
      useToast();
      return null;
    }
    const quiet = vi.spyOn(console, "error").mockImplementation(() => {});
    expect(() => render(<Orphan />)).toThrow(/ToastProvider/);
    quiet.mockRestore();
  });
});
