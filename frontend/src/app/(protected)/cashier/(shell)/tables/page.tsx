"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import {
  Alert,
  Badge,
  SearchBar,
  StatGrid,
  StatTile,
  Toolbar,
} from "@/components/ui";
import { get, type PageResponse } from "@/lib/api";
import { errorMessage } from "@/lib/errors";
import {
  type DiningTable,
  type TableSummary,
} from "@/types/master";

const CARD_TONE: Record<DiningTable["status"], string> = {
  FREE: "bg-success",
  OCCUPIED: "bg-danger-soft",
  RESERVED: "bg-warning",
};

export default function CashierTablesPage() {
  const t = useTranslations("tables");
  const tc = useTranslations("common");
  const tZone = useTranslations("enum.zone");
  const tStatus = useTranslations("enum.tableStatus");

  const router = useRouter();
  const [search, setSearch] = useState("");

  const tables = useQuery({
    queryKey: ["tables", { search, size: 100 }],
    queryFn: () => get<PageResponse<DiningTable>>("/tables", { search, size: 100 }),
  });

  const summary = useQuery({
    queryKey: ["tables", "summary"],
    queryFn: () => get<TableSummary>("/tables/summary"),
  });

  return (
    <>
      {tables.isError && <Alert tone="error">{errorMessage(tables.error)}</Alert>}

      <StatGrid>
        <StatTile tone={1} label={tStatus("FREE")} value={summary.data?.free ?? "—"} />
        <StatTile tone={4} label={tStatus("OCCUPIED")} value={summary.data?.occupied ?? "—"} />
        <StatTile tone={2} label={tStatus("RESERVED")} value={summary.data?.reserved ?? "—"} />
        <StatTile tone={3} label={tc("total")} value={summary.data?.total ?? "—"} />
      </StatGrid>

      <Toolbar
        left={
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <Badge tone="ok">{tStatus("FREE")}</Badge>
            <Badge tone="dead">{tStatus("OCCUPIED")}</Badge>
            <Badge tone="warn">{tStatus("RESERVED")}</Badge>
          </div>
        }
        right={
          <SearchBar value={search} onChange={setSearch} placeholder={t("searchTable")} />
        }
      />

      {tables.isLoading && <p className="py-10 text-center text-sm text-ink-500">{tc("loading")}</p>}

      <div className="grid grid-cols-[repeat(auto-fill,minmax(160px,1fr))] gap-3.5">
        {tables.data?.content.map((tbl) => (
          <button
            key={tbl.id}
            type="button"
            onClick={() => router.push(`/cashier/order?tableId=${tbl.id}`)}
            className="overflow-hidden rounded-md border border-ink-300 bg-white text-left transition hover:-translate-y-0.5 hover:shadow-md"
          >
            <div className={`px-3 py-2 text-sm font-semibold text-white ${CARD_TONE[tbl.status]}`}>
              {tbl.name}
            </div>
            <div className="px-3 py-2.5 text-xs leading-6 text-ink-700">
              <div>{t("seats")}: {tbl.seats}</div>
              <div>{tZone(tbl.zone)}</div>
              <div className="text-ink-500">{tStatus(tbl.status)}</div>
            </div>
          </button>
        ))}
      </div>

      {tables.data?.content.length === 0 && (
        <p className="py-10 text-center text-sm text-ink-500">{t("noTables")}</p>
      )}
    </>
  );
}
