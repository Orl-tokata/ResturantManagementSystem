"use server";

import { cookies } from "next/headers";
import { revalidatePath } from "next/cache";
import { LOCALE_COOKIE, isLocale } from "@/i18n/config";

/**
 * Switches language by writing the locale cookie.
 *
 * <p>A server action rather than a client-side cookie write, because the
 * messages are resolved on the server — the new locale must be readable on the
 * next render pass, which `revalidatePath` forces.
 */
export async function setLocale(locale: string) {
  if (!isLocale(locale)) return;

  const store = await cookies();
  store.set(LOCALE_COOKIE, locale, {
    path: "/",
    maxAge: 60 * 60 * 24 * 365,
    sameSite: "lax",
    // Readable by the server on every request; nothing secret in it.
    httpOnly: false,
  });

  revalidatePath("/", "layout");
}
