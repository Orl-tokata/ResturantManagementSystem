"use client";

import { useLocale } from "next-intl";
import en from "../../messages/en.json";
import km from "../../messages/km.json";

/**
 * Both languages at once, for the printed receipt.
 *
 * <p>Everywhere else in the application one language is right: the person
 * reading the screen chose it. A receipt is different — it is handed to a
 * customer whose language nobody asked, and it is the document someone may
 * later take to a bank or an accountant. Printing it in whichever language the
 * cashier happened to have selected makes it unreadable to half the people who
 * will hold it.
 *
 * <p>The reader's own language leads and the other follows, so a cashier
 * working in Khmer sees Khmer first. The default locale is Khmer, so that is
 * what an unconfigured till prints.
 *
 * <p>Both catalogues are imported here rather than in the page, so the awkward
 * relative path exists once. It costs the receipt route both message files,
 * which is the price of the slip being readable by everyone who holds it.
 */
export function useBilingual() {
  const locale = useLocale();
  const [first, second] = locale === "en" ? [en, km] : [km, en];

  return (path: string): string => {
    const a = lookup(first, path);
    const b = lookup(second, path);

    // "ABA KHQR" is the same in both; printing it twice would just be noise.
    if (!b || a === b) return a;
    if (!a) return b;
    return `${a} / ${b}`;
  };
}

/** Both halves separately, for places that want them on their own lines. */
export function useBilingualPair() {
  const locale = useLocale();
  const [first, second] = locale === "en" ? [en, km] : [km, en];

  return (path: string): [string, string | null] => {
    const a = lookup(first, path);
    const b = lookup(second, path);
    return [a, !b || a === b ? null : b];
  };
}

function lookup(messages: unknown, path: string): string {
  let node: unknown = messages;
  for (const part of path.split(".")) {
    if (typeof node !== "object" || node === null) return "";
    node = (node as Record<string, unknown>)[part];
  }
  return typeof node === "string" ? node : "";
}
