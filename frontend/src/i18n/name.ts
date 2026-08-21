import type { Locale } from "@/i18n/config";

/**
 * Picks the right side of a bilingual database name.
 *
 * <p>Categories and products carry both `name` (Khmer) and `nameEn`. Chrome is
 * translated from the message catalogue, but this is *data* — it has to be
 * chosen at read time from whichever column matches the active locale.
 *
 * <p>Falls back to the Khmer name: it is the required column, so it is always
 * present, whereas `nameEn` is optional.
 */
export function pickName(locale: string, name: string, nameEn?: string | null): string {
  if ((locale as Locale) === "en" && nameEn && nameEn.trim()) return nameEn;
  return name;
}
