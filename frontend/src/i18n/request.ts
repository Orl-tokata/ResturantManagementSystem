import { cookies } from "next/headers";
import { getRequestConfig } from "next-intl/server";
import { DEFAULT_LOCALE, LOCALE_COOKIE, isLocale } from "@/i18n/config";

/**
 * Locale comes from a cookie, not from the URL.
 *
 * <p>The whole app sits behind a login, so there is no SEO reason to put the
 * locale in the path — and doing so would mean moving all 28 route files under
 * a `[locale]` segment for no user-visible benefit.
 */
export default getRequestConfig(async () => {
  const store = await cookies();
  const requested = store.get(LOCALE_COOKIE)?.value;
  const locale = isLocale(requested) ? requested : DEFAULT_LOCALE;

  return {
    locale,
    messages: (await import(`../../messages/${locale}.json`)).default,
  };
});
