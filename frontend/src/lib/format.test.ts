import { describe, expect, it } from "vitest";
import { formatReceiptDateTime, formatTimestamp } from "./format";

/*
 * A receipt timestamp is read by whoever holds the slip, which may be an
 * accountant months later. It was printing "9/11/2026, 8:16:00 PM": the
 * browser's locale rather than the till's, and a date that means two different
 * days depending on who reads it.
 */
describe("formatReceiptDateTime", () => {
  it("is year-month-day, which reads the same in every locale", () => {
    expect(formatReceiptDateTime("2026-11-09T20:16:42")).toBe("2026-11-09 20:16");
  });

  it("pads, so the column lines up down the slip", () => {
    expect(formatReceiptDateTime("2026-01-05T08:07:00")).toBe("2026-01-05 08:07");
  });

  it("drops seconds, which nobody reconciles to", () => {
    expect(formatReceiptDateTime("2026-11-09T20:16:59")).not.toContain("59");
  });

  it("prints a dash rather than 'Invalid Date' when there is no timestamp", () => {
    // An unpaid bill has no paidAt, and the slip still has to print.
    expect(formatReceiptDateTime(null)).toBe("—");
    expect(formatReceiptDateTime(undefined)).toBe("—");
    expect(formatReceiptDateTime("not a date")).toBe("—");
  });

  it("accepts a Date as well as a string", () => {
    expect(formatReceiptDateTime(new Date(2026, 10, 9, 20, 16))).toBe("2026-11-09 20:16");
  });
});

describe("formatTimestamp", () => {
  it("keeps the seconds a receipt drops", () => {
    expect(formatTimestamp("2026-11-09T20:16:42")).toBe("2026-11-09 20:16:42");
  });

  it("pads every part", () => {
    expect(formatTimestamp("2026-01-05T08:07:09")).toBe("2026-01-05 08:07:09");
  });

  it("distinguishes two changes in the same minute", () => {
    // The whole reason this exists beside formatReceiptDateTime.
    expect(formatTimestamp("2026-11-09T20:16:01"))
      .not.toBe(formatTimestamp("2026-11-09T20:16:59"));
  });

  it("says nothing rather than Invalid Date", () => {
    expect(formatTimestamp(null)).toBe("—");
    expect(formatTimestamp("not a date")).toBe("—");
  });
});
