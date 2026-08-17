export const LOCALES = ["km", "en"] as const;
export type Locale = (typeof LOCALES)[number];

/** Khmer is the default: the staff using this till read Khmer. */
export const DEFAULT_LOCALE: Locale = "km";

export const LOCALE_COOKIE = "rms_locale";

export const LOCALE_LABEL: Record<Locale, string> = {
  km: "ខ្មែរ",
  en: "English",
};

export function isLocale(value: string | undefined): value is Locale {
  return value != null && (LOCALES as readonly string[]).includes(value);
}
