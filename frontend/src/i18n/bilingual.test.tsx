import { renderHook } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import en from "../../messages/en.json";
import km from "../../messages/km.json";
import { useBilingual, useBilingualPair } from "./bilingual";

/*
 * The receipt prints both languages because it is handed to a customer whose
 * language nobody asked. Which one leads follows the cashier's own setting, and
 * getting that backwards is the sort of thing nobody notices until a slip is in
 * someone's hand.
 */

function wrapper(locale: "km" | "en") {
  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <NextIntlClientProvider locale={locale} messages={locale === "en" ? en : km}>
        {children}
      </NextIntlClientProvider>
    );
  };
}

describe("bilingual receipt labels", () => {
  it("leads with Khmer when the till is in Khmer", () => {
    const { result } = renderHook(() => useBilingual(), { wrapper: wrapper("km") });
    expect(result.current("receipt.invoice")).toBe(`${km.receipt.invoice} / ${en.receipt.invoice}`);
  });

  it("leads with English when the till is in English", () => {
    const { result } = renderHook(() => useBilingual(), { wrapper: wrapper("en") });
    expect(result.current("receipt.invoice")).toBe(`${en.receipt.invoice} / ${km.receipt.invoice}`);
  });

  it("prints a shared term once rather than twice", () => {
    // "ABA KHQR" is identical in both catalogues. "ABA KHQR / ABA KHQR" on a
    // receipt is noise, and on an 80mm roll it is noise that costs width.
    const { result } = renderHook(() => useBilingual(), { wrapper: wrapper("km") });
    expect(result.current("enum.paymentMethod.KHQR")).toBe("ABA KHQR");
  });

  it("reaches nested keys", () => {
    const { result } = renderHook(() => useBilingual(), { wrapper: wrapper("km") });
    expect(result.current("enum.paymentMethod.CASH")).toContain(km.enum.paymentMethod.CASH);
    expect(result.current("enum.paymentMethod.CASH")).toContain(en.enum.paymentMethod.CASH);
  });

  it("returns empty for a key that does not exist rather than throwing", () => {
    // A missing key must not take the receipt down. The slip loses a label; it
    // still prints, and the customer still gets their total.
    const { result } = renderHook(() => useBilingual(), { wrapper: wrapper("km") });
    expect(result.current("nope.not.here")).toBe("");
  });

  it("gives sentences back as separate lines", () => {
    // "Thank you / សូមអរគុណ" joined by a slash reads as one broken sentence.
    const { result } = renderHook(() => useBilingualPair(), { wrapper: wrapper("km") });
    const [first, second] = result.current("receipt.thanks");

    expect(first).toBe(km.receipt.thanks);
    expect(second).toBe(en.receipt.thanks);
  });

  it("gives a shared sentence only once", () => {
    const { result } = renderHook(() => useBilingualPair(), { wrapper: wrapper("km") });
    const [, second] = result.current("enum.paymentMethod.KHQR");

    expect(second).toBeNull();
  });
});
