"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
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
  TABLE_STATUS_LABEL,
  ZONE_LABEL,
  type DiningTable,
  type TableSummary,
} from "@/types/master";

const CARD_TONE: Record<DiningTable["status"], string> = {
  FREE: "bg-success",
  OCCUPIED: "bg-danger-soft",
  RESERVED: "bg-warning",
};

export default function CashierTablesPage() {
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
        <StatTile tone={1} label="ទំនេរ · Free" value={summary.data?.free ?? "—"} />
        <StatTile tone={4} label="កំពុងប្រើ · Occupied" value={summary.data?.occupied ?? "—"} />
        <StatTile tone={2} label="កក់ទុក · Reserved" value={summary.data?.reserved ?? "—"} />
        <StatTile tone={3} label="សរុប · Total" value={summary.data?.total ?? "—"} />
      </StatGrid>

      <Toolbar
        left={
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <Badge tone="ok">{TABLE_STATUS_LABEL.FREE}</Badge>
            <Badge tone="dead">{TABLE_STATUS_LABEL.OCCUPIED}</Badge>
            <Badge tone="warn">{TABLE_STATUS_LABEL.RESERVED}</Badge>
          </div>
        }
        right={
          <SearchBar value={search} onChange={setSearch} placeholder="ស្វែងរកតុ · Search table" />
        }
      />

      {tables.isLoading && <p className="py-10 text-center text-sm text-ink-500">កំពុងផ្ទុក…</p>}

      <div className="grid grid-cols-[repeat(auto-fill,minmax(160px,1fr))] gap-3.5">
        {tables.data?.content.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => router.push(`/cashier/order?tableId=${t.id}`)}
            className="overflow-hidden rounded-md border border-ink-300 bg-white text-left transition hover:-translate-y-0.5 hover:shadow-md"
          >
            <div className={`px-3 py-2 text-sm font-semibold text-white ${CARD_TONE[t.status]}`}>
              {t.name}
            </div>
            <div className="px-3 py-2.5 text-xs leading-6 text-ink-700">
              <div>អាសនៈ · {t.seats} seats</div>
              <div>{ZONE_LABEL[t.zone]}</div>
              <div className="text-ink-500">{TABLE_STATUS_LABEL[t.status]}</div>
            </div>
          </button>
        ))}
      </div>

      {tables.data?.content.length === 0 && (
        <p className="py-10 text-center text-sm text-ink-500">គ្មានតុ · No tables found</p>
      )}
    </>
  );
}
