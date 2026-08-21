"use client";

import { Languages } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useTransition } from "react";
import { setLocale } from "@/app/actions/locale";
import { LOCALES, LOCALE_LABEL, type Locale } from "@/i18n/config";

/**
 * Two-state language toggle for the topbar.
 *
 * <p>Calls a server action because the messages are resolved server-side; the
 * cookie has to be set and the tree re-rendered, which a client-side cookie
 * write alone would not trigger.
 */
export function LanguageSwitcher() {
  const tA11y = useTranslations("a11y");
  const active = useLocale() as Locale;
  const [pending, startTransition] = useTransition();

  return (
    <div
      className="flex items-center gap-1 rounded bg-white/15 p-0.5"
      role="group"
      aria-label={tA11y("language")}
    >
      <Languages size={14} className="ml-1 shrink-0 opacity-80" aria-hidden />
      {LOCALES.map((code) => (
        <button
          key={code}
          type="button"
          disabled={pending || code === active}
          aria-pressed={code === active}
          onClick={() => startTransition(() => setLocale(code))}
          className={`rounded px-2 py-0.5 text-xs font-semibold transition disabled:cursor-default ${
            code === active ? "bg-white text-ink-900" : "text-white/85 hover:bg-white/20"
          }`}
        >
          {LOCALE_LABEL[code]}
        </button>
      ))}
    </div>
  );
}
