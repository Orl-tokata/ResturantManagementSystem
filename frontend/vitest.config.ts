import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

/**
 * Test setup for the browser half of the app.
 *
 * <p>There is no Next runtime here: these are component and unit tests, run by
 * esbuild straight from source. Anything that needs a real server — routing,
 * data fetching, authorisation — is covered by the backend suite and by
 * scripts/smoke-api.mjs. What lives here is the logic the server cannot see,
 * such as what the client keeps in memory between two users.
 */
export default defineConfig({
  // Next compiles JSX itself, so tsconfig says "preserve"; esbuild needs telling.
  esbuild: { jsx: "automatic" },
  resolve: {
    alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
  },
});
