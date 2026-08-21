"use client";

import { useCallback } from "react";
import { useTranslations } from "next-intl";
import { describeError } from "./errors";

/**
 * Turns any thrown value into a sentence the reader can understand.
 *
 * <p>Usage: `const apiError = useApiError()` then
 * `apiError(e, "saveCategory")`, where the second argument is a key in the
 * `error` namespace naming the action that failed. It is only reached when
 * nothing better is available — a message from the backend always wins, because
 * the server knows *why* it refused and this side only knows *what* was being
 * attempted.
 */
export function useApiError() {
  const t = useTranslations("error");

  return useCallback(
    (error: unknown, fallbackKey?: string): string => {
      const info = describeError(error);

      switch (info.kind) {
        case "server":
          // Already localized by the API. Translating it again here would mean
          // keeping a copy of every backend message in this catalogue, and the
          // two would drift the first time one side was edited alone.
          return info.message;
        case "timeout":
          return t("timeout");
        case "unreachable":
          return t("unreachable");
        case "http":
          return t("requestFailed", { status: info.status });
        case "js":
          // A bug in our own code, not a server response. The message is a
          // developer string, so prefer the caller's description of what
          // failed and keep the detail for the console.
          if (fallbackKey) return t(fallbackKey);
          return info.message;
        case "unknown":
        default:
          return fallbackKey ? t(fallbackKey) : t("generic");
      }
    },
    [t],
  );
}
