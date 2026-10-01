import { API_BASE_URL } from "@/lib/api";

/**
 * Where a stored product photograph is served from.
 *
 * <p>The API returns a filename rather than a URL, so it does not have to know
 * what host it is reached on. This is the one place that turns one into the
 * other.
 */
export function productImageUrl(file: string): string {
  return `${API_BASE_URL}/products/images/${encodeURIComponent(file)}`;
}
