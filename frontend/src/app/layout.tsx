import type { Metadata } from "next";
import { Noto_Sans_Khmer } from "next/font/google";
import { NextIntlClientProvider } from "next-intl";
import { getLocale } from "next-intl/server";
import "./globals.css";
import { Providers } from "./providers";

/**
 * Self-hosted by next/font at build time, so the UI does not depend on the
 * user having "Khmer OS" installed — which is what the HTML prototype relied on.
 */
const khmer = Noto_Sans_Khmer({
  variable: "--font-khmer",
  subsets: ["khmer", "latin"],
  weight: ["300", "400", "600", "700"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "Restaurant Management System",
  description: "ប្រព័ន្ធគ្រប់គ្រងភោជនីយដ្ឋាន · Restaurant Management System",
};

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
