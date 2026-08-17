import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

// Locale comes from a cookie, so there is no i18n routing to configure —
// only the path to the request config. See src/i18n/request.ts.
const withNextIntl = createNextIntlPlugin("./src/i18n/request.ts");

const nextConfig: NextConfig = {};

export default withNextIntl(nextConfig);
