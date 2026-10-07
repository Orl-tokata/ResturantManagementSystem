"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  Alert,
  Badge,
  type BadgeTone,
  type Column,
  DataTable,
  Field,
  FieldRow,
  Input,
  ListPage,
  Pagination,
  SearchBar,
  Select,
  Toolbar,
} from "@/components/ui";
import { useList } from "@/hooks/useCrud";
import { useApiError } from "@/lib/use-api-error";
import { formatTimestamp } from "@/lib/format";
import { type AuditEntry, parseFields } from "@/types/audit";

/** Where this screen starts; the reader can change it. */
const INITIAL_SIZE = 20;

/**
 * Only the entities that carry `@Audited` on the server. Listing them rather
 * than offering a free-text box: the value has to match exactly, and a filter
 * that silently returns nothing because of a typo is worse than no filter.
 */
const ENTITIES = [
  "Product",
  "Category",
  "AppSetting",
  "Supplier",
  "DiningTable",
  "StockItem",
  "Staff",
  "UserInfm",
] as const;

const TONE: Record<AuditEntry["action"], BadgeTone> = {
  CREATE: "ok",
  UPDATE: "info",
  DELETE: "dead",
};

export default function AuditPage() {
  const t = useTranslations("audit");
  const apiError = useApiError();

  const [entity, setEntity] = useState("");
  const [userId, setUserId] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(INITIAL_SIZE);

  const list = useList<AuditEntry>("audit", {
    entity,
    userId,
    from,
    to,
    page,
    size: size,
  });

  /** Every filter resets to the first page; staying on page 4 of a narrower
      result set shows an empty table and looks like a bug. */
  function change(set: (v: string) => void) {
    return (e: { target: { value: string } }) => {
      set(e.target.value);
      setPage(0);
    };
  }

  /** SearchBar hands over the value itself, unlike the native-element wrappers. */
  function changeValue(set: (v: string) => void) {
    return (value: string) => {
      set(value);
      setPage(0);
    };
  }

  const columns: Column<AuditEntry>[] = [
    {
      key: "at",
      header: t("when"),
      width: "160px",
      render: (r) => <span className="tabular-nums">{formatTimestamp(r.at)}</span>,
    },
    {
      key: "who",
      header: t("who"),
      width: "120px",
      render: (r) => <span className="font-medium">{r.userId}</span>,
    },
    {
      key: "what",
      header: t("what"),
      render: (r) => (
        <div className="flex items-center gap-2">
          <Badge tone={TONE[r.action]}>{t(`action.${r.action}`)}</Badge>
          <span>
            {r.entity}
            {r.entityId != null && <span className="text-ink-500"> #{r.entityId}</span>}
          </span>
        </div>
      ),
    },
    {
      key: "change",
      header: t("change"),
      render: (r) => <Change entry={r} emptyLabel={t("noDetail")} />,
    },
    {
      key: "ip",
      header: t("ip"),
      width: "120px",
      hideOnMobile: true,
      render: (r) => <span className="text-xs text-ink-500">{r.ip ?? "—"}</span>,
    },
  ];

  return (
    <ListPage>
      {list.isError && <Alert tone="error">{apiError(list.error)}</Alert>}

      {/* A caption, not an Alert: it never changes, so role="status" would
          announce a permanent fact on every render. Said once at the top all
          the same, rather than left to be inferred from the absence of any
          buttons. */}
      <p className="mb-3 text-xs text-ink-500">{t("readOnly")}</p>

      <Toolbar
        left={
          <FieldRow>
            <Field label={t("entity")}>
              <Select value={entity} onChange={change(setEntity)}>
                <option value="">{t("allEntities")}</option>
                {ENTITIES.map((e) => (
                  <option key={e} value={e}>
                    {e}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label={t("from")}>
              <Input type="date" value={from} onChange={change(setFrom)} max={to || undefined} />
            </Field>
            <Field label={t("to")}>
              <Input type="date" value={to} onChange={change(setTo)} min={from || undefined} />
            </Field>
          </FieldRow>
        }
        right={
          <SearchBar
            value={userId}
            onChange={changeValue(setUserId)}
            placeholder={t("searchUser")}
          />
        }
      />

      <DataTable
        fill
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage={t("noEntries")}
      />

      <Pagination
        page={list.data?.page ?? 0}
        totalPages={list.data?.totalPages ?? 0}
        totalElements={list.data?.totalElements ?? 0}
        size={list.data?.size ?? size}
        onPage={setPage}
        onSize={(n) => {
          setSize(n);
          setPage(0);
        }}
      />
    </ListPage>
  );
}

/**
 * One line per changed field, old struck through beside new.
 *
 * <p>The whole point of the log is the pair, so showing only the new value
 * would answer half the question — and the half nobody needed the log for.
 */
function Change({ entry, emptyLabel }: { entry: AuditEntry; emptyLabel: string }) {
  const before = parseFields(entry.before);
  const after = parseFields(entry.after);
  const fields = [...new Set([...Object.keys(before), ...Object.keys(after)])];

  if (fields.length === 0) {
    return <span className="text-xs text-ink-500">{emptyLabel}</span>;
  }

  return (
    <div className="space-y-0.5">
      {fields.map((field) => (
        <div key={field} className="flex flex-wrap items-baseline gap-1.5 text-xs">
          <span className="text-ink-500">{field}</span>
          {field in before && (
            <span className="text-ink-500 line-through">{before[field]}</span>
          )}
          {field in before && field in after && <span className="text-ink-500">→</span>}
          {field in after && <span className="font-medium tabular-nums">{after[field]}</span>}
        </div>
      ))}
    </div>
  );
}
