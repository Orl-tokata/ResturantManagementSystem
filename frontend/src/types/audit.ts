/** One recorded change, as `GET /api/audit` returns it. */
export interface AuditEntry {
  id: number;
  userId: string;
  action: "CREATE" | "UPDATE" | "DELETE";
  entity: string;
  entityId: number | null;
  /**
   * JSON objects of the fields that changed, or absent. A create has no
   * `before` and a delete has no `after`; the server omits null fields rather
   * than sending them, so both are optional here rather than nullable.
   */
  before?: string;
  after?: string;
  ip?: string;
  at: string;
}

/** Parses one side of a change, tolerating anything unexpected. */
export function parseFields(json: string | undefined): Record<string, string> {
  if (!json) return {};
  try {
    const parsed: unknown = JSON.parse(json);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) return {};
    return Object.fromEntries(
      Object.entries(parsed as Record<string, unknown>).map(([k, v]) => [k, String(v)]),
    );
  } catch {
    // The column is written by the server and should always hold valid JSON.
    // If it ever does not, showing the rest of the log matters more than
    // being right about this one row.
    return {};
  }
}
