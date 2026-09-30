import { describe, expect, it } from "vitest";

import { parseTendered, tenderState } from "./payment";
import type { OrderStatus, PaymentMethod } from "@/types/order";

const base = {
  total: 10,
  tendered: "",
  method: "CASH" as PaymentMethod,
  status: "OPEN" as OrderStatus | undefined,
  itemCount: 2,
};

const state = (over: Partial<typeof base> = {}) => tenderState({ ...base, ...over });

describe("parseTendered", () => {
  it("reads a normal amount", () => {
    expect(parseTendered("12.50")).toBe(12.5);
  });

  it("treats an empty box as nothing", () => {
    expect(parseTendered("")).toBe(0);
  });

  it("treats a lone decimal point as nothing, not NaN", () => {
    // The keypad allows "." as the first press, and NaN would poison the
    // change, the comparison and the message all at once.
    expect(parseTendered(".")).toBe(0);
  });

  it("accepts a trailing point mid-typing", () => {
    expect(parseTendered("12.")).toBe(12);
  });
});

describe("tenderState — cash", () => {
  it("will not settle before anything is typed", () => {
    const s = state();
    expect(s.canPay).toBe(false);
    expect(s.blocker).toBe("enterTendered");
  });

  it("works out the change and allows the settle", () => {
    const s = state({ tendered: "20" });
    expect(s.change).toBe(10);
    expect(s.canPay).toBe(true);
    expect(s.blocker).toBeNull();
  });

  it("refuses a tender under the total and says which", () => {
    const s = state({ tendered: "5" });
    expect(s.shortfall).toBe(true);
    expect(s.canPay).toBe(false);
    expect(s.blocker).toBe("shortfall");
  });

  it("allows the exact amount", () => {
    const s = state({ tendered: "10" });
    expect(s.change).toBe(0);
    expect(s.canPay).toBe(true);
  });

  it("does not call a lone decimal point a shortfall", () => {
    // Mid-typing, not an error. Saying "shortfall" here would scold a cashier
    // for pressing one key.
    const s = state({ tendered: "." });
    expect(s.shortfall).toBe(false);
    expect(s.blocker).toBe("enterTendered");
  });
});

describe("tenderState — the other methods", () => {
  it.each<PaymentMethod>(["CARD", "TRANSFER"])("%s settles the exact total with no tender", (method) => {
    const s = state({ method, tendered: "" });
    expect(s.canPay).toBe(true);
    expect(s.blocker).toBeNull();
  });

  it("never reports a shortfall for a method that carries no tender", () => {
    expect(state({ method: "CARD", tendered: "1" }).shortfall).toBe(false);
  });
});

describe("tenderState — the bill's own state", () => {
  it("blocks an empty bill", () => {
    const s = state({ itemCount: 0, tendered: "20" });
    expect(s.canPay).toBe(false);
    expect(s.blocker).toBe("noItems");
  });

  /**
   * The bug this replaces: AWAITING_PAYMENT was not in the frontend's
   * OrderStatus at all, so it fell through to the already-cancelled branch —
   * and a cashier watching a customer scan a live QR code was told the bill had
   * been cancelled.
   */
  it("says a KHQR code is live, not that the bill was cancelled", () => {
    const s = state({ status: "AWAITING_PAYMENT", tendered: "20" });
    expect(s.canPay).toBe(false);
    expect(s.blocker).toBe("awaitingKhqr");
    expect(s.blocker).not.toBe("alreadyCancelled");
  });

  it.each<[OrderStatus, string]>([
    ["PAID", "alreadyPaid"],
    ["CANCELLED", "alreadyCancelled"],
  ])("blocks a %s bill", (status, blocker) => {
    expect(state({ status, tendered: "20" }).blocker).toBe(blocker);
  });

  it("reports the bill's state ahead of the tender", () => {
    // A cashier on a paid bill needs to hear "already paid", not "enter an
    // amount" — the amount would not help.
    expect(state({ status: "PAID", tendered: "" }).blocker).toBe("alreadyPaid");
  });

  it("blocks silently while the order is still loading", () => {
    const s = state({ status: undefined, tendered: "20" });
    expect(s.canPay).toBe(false);
    expect(s.blocker).toBeNull();
  });
});

describe("tenderState — canPay and blocker never disagree", () => {
  it.each<Partial<typeof base>>([
    {},
    { tendered: "20" },
    { tendered: "5" },
    { tendered: "." },
    { itemCount: 0 },
    { status: "PAID" },
    { status: "CANCELLED" },
    { status: "AWAITING_PAYMENT" },
    { status: undefined },
    { method: "CARD" },
    { method: "KHQR" },
  ])("%o", (over) => {
    const s = state(over);
    // The whole reason they are derived together: a disabled button with no
    // reason, or a reason beside an enabled one, is how the old pair drifted.
    if (s.canPay) expect(s.blocker).toBeNull();
    if (s.blocker !== null) expect(s.canPay).toBe(false);
  });
});
