import axios from "axios";

/**
 * What went wrong, as data rather than as a sentence.
 *
 * <p>Splitting the classification from the wording is what makes error text
 * translatable. This half is pure and runs anywhere; `useApiError` turns it
 * into words with the reader's locale in hand. A function that returned a
 * finished English string could only be localized by being called from inside a
 * component, and classifying an axios error is not a component's job.
 */
export type ErrorInfo =
  /**
   * The backend explained itself, and the message is already in the reader's
   * language — the API localizes its own errors from the Accept-Language header
   * our axios client sends. So this one is passed through, not translated here.
   */
  | { kind: "server"; message: string }
  | { kind: "timeout" }
  | { kind: "unreachable" }
  /** A response with no usable message; only the status is worth reporting. */
  | { kind: "http"; status: number }
  | { kind: "js"; message: string }
  | { kind: "unknown" };

export function describeError(error: unknown): ErrorInfo {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message;
    if (typeof message === "string" && message.trim()) {
      return { kind: "server", message };
    }
    if (error.code === "ECONNABORTED") return { kind: "timeout" };
    if (!error.response) return { kind: "unreachable" };
    return { kind: "http", status: error.response.status };
  }

  if (error instanceof Error && error.message) {
    return { kind: "js", message: error.message };
  }
  return { kind: "unknown" };
}
