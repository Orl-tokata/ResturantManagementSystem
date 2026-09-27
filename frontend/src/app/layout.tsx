import type { Metadata } from "next";
import localFont from "next/font/local";
import { NextIntlClientProvider } from "next-intl";
import { getLocale, getTranslations } from "next-intl/server";
import "./globals.css";
import { Providers } from "./providers";

/**
 * Noto Sans Khmer, served from this repository.
 *
 * <p>The files are committed under app/fonts rather than fetched by
 * next/font/google, which downloads from fonts.googleapis.com at build time.
 * That download failed repeatedly on the development machine and Next did what
 * it says it does on failure: carried on with a fallback font. The app still
 * rendered, so nothing looked broken — every Khmer glyph was simply being drawn
 * in whatever the system had lying around, which for Khmer is often nothing
 * suitable at all.
 *
 * <p>A build that reaches across the network for something it cannot do without
 * is a build that can quietly produce the wrong output. These 328KB remove that.
 *
 * <p>Only the khmer and latin subsets are kept — the app is bilingual Khmer and
 * English, and latin-ext, cyrillic and the rest would be weight for nothing.
 * The unicode-range on each face is what Google served, so a browser still
 * downloads only the subset a page actually needs.
 */
const khmer = localFont({
  variable: "--font-khmer",
  display: "swap",
  src: [
    { path: "./fonts/noto-sans-khmer-khmer-300.woff2", weight: "300", style: "normal" },
    { path: "./fonts/noto-sans-khmer-latin-300.woff2", weight: "300", style: "normal" },
    { path: "./fonts/noto-sans-khmer-khmer-400.woff2", weight: "400", style: "normal" },
    { path: "./fonts/noto-sans-khmer-latin-400.woff2", weight: "400", style: "normal" },
    { path: "./fonts/noto-sans-khmer-khmer-600.woff2", weight: "600", style: "normal" },
    { path: "./fonts/noto-sans-khmer-latin-600.woff2", weight: "600", style: "normal" },
    { path: "./fonts/noto-sans-khmer-khmer-700.woff2", weight: "700", style: "normal" },
    { path: "./fonts/noto-sans-khmer-latin-700.woff2", weight: "700", style: "normal" },
  ],
});

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("app");
  return {
    title: "Restaurant Management System",
    description: t("description"),
  };
}

export default async function RootLayout({ children }: LayoutProps<"/">) {
  // Resolved from the locale cookie by src/i18n/request.ts.
  const locale = await getLocale();

  return (
    <html lang={locale} className={`${khmer.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col">
        <NextIntlClientProvider>
          <Providers>{children}</Providers>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
