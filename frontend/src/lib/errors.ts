import axios from "axios";

/**
 * Pulls the human-readable message out of the backend's ApiResponse envelope.
 * Falls back to something honest rather than "[object Object]".
 */
export function errorMessage(error: unknown, fallback = "Something went wrong"): string {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message;
    if (typeof message === "string" && message.trim()) return message;

    if (error.code === "ECONNABORTED") return "The server took too long to respond.";
    if (!error.response) {
      return "Cannot reach the server. Is the backend running on port 8081?";
    }
    return `Request failed (HTTP ${error.response.status}).`;
  }

  if (error instanceof Error && error.message) return error.message;
  return fallback;
}
