"use client";

import Link from "next/link";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  Alert,
  Badge,
  Button,
  DataTable,
  Input,
  Pagination,
  SearchBar,
  Select,
  StatGrid,
  StatTile,
  Toolbar,
  toneForOrderStatus,
  type Column,
} from "@/components/ui";
import { get, type PageResponse } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import { formatUsd } from "@/lib/format";
import {
  type Order,
  type OrderStatus,
} from "@/types/order";

const SIZE = 20;

function todayIso() {
  // Local date, not toISOString() — that shifts to UTC and can report yesterday.
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
    d.getDate(),
  ).padStart(2, "0")}`;
}

interface Summary {
  totalSales: number;
  paidCount: number;
  averageSale: number;
  cancelledCount: number;
  totalCount: number;
}

export default function CashierHistoryPage() {
  const t = useTranslations("history");
  const tc = useTranslations("common");
  const tPos = useTranslations("pos");
  const tOs = useTranslations("enum.orderStatus");
  const tPay = useTranslations("enum.paymentMethod");
  const tCh = useTranslations("cashierHome");
  const tA11y = useTranslations("a11y");
  const apiError = useApiError();

  const today = todayIso();

  const [from, setFrom] = useState(today);
  const [to, setTo] = useState(today);
  const [status, setStatus] = useState<OrderStatus | "">("");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);

  const params = {
    ...(from ? { from } : {}),
    ...(to ? { to } : {}),
    ...(status ? { status } : {}),
    ...(search ? { search } : {}),
  };

  const list = useQuery({
    queryKey: ["orders", { ...params, page, size: SIZE }],
    queryFn: () =>
      get<PageResponse<Order>>("/orders", { ...params, page, size: SIZE }),
    placeholderData: (prev) => prev,
  });

  const summary = useQuery({
    queryKey: ["orders", "summary", { from, to }],
    queryFn: () => get<Summary>("/orders/summary", { from, to }),
  });

  function resetRange(days: number) {
    const start = new Date();
    start.setDate(start.getDate() - days);
    const iso = `${start.getFullYear()}-${String(start.getMonth() + 1).padStart(2, "0")}-${String(
      start.getDate(),
    ).padStart(2, "0")}`;
    setFrom(iso);
    setTo(today);
    setPage(0);
  }

  const columns: Column<Order>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * SIZE + i + 1 },
    {
      key: "invoice",
      header: t("invoice"),
      render: (r) => <span className="font-medium">{r.invoiceNo}</span>,
    },
    { key: "table", header: tPos("table"), render: (r) => r.tableName ?? "—" },
    {
      key: "cashier",
      header: tPos("cashier"),
      hideOnMobile: true,
      render: (r) => r.cashierName ?? "—",
    },
    { key: "items", header: t("dishes"), numeric: true, render: (r) => r.items.length },
    { key: "total", header: tc("total"), numeric: true, render: (r) => formatUsd(r.total) },
    {
      key: "method",
      header: t("paymentMethod"),
      hideOnMobile: true,
      render: (r) => (r.paymentMethod ? tPay(r.paymentMethod) : "—"),
    },
    {
      key: "status",
      header: tc("status"),
      render: (r) => (
        <Badge tone={toneForOrderStatus(r.status)}>{tOs(r.status)}</Badge>
      ),
    },
    {
      key: "time",
      header: tc("time"),
      hideOnMobile: true,
      render: (r) =>
        r.createdAt ? new Date(r.createdAt).toLocaleTimeString(undefined, {
          hour: "numeric",
          minute: "2-digit",
        }) : "—",
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (r) => (
        <div className="flex justify-end gap-1.5">
          <Link href={`/cashier/receipt/${r.id}`}>
            <Button size="sm" variant="ghost">
              🧾 {tc("view")}
            </Button>
          </Link>
          {r.status === "OPEN" && (
            <Link href={`/cashier/order?tableId=${r.tableId ?? ""}`}>
              <Button size="sm" variant="accent">
                {tCh("continue")}
              </Button>
            </Link>
          )}
        </div>
      ),
    },
  ];

  return (
    <>
      {list.isError && <Alert tone="error">{apiError(list.error)}</Alert>}

      <StatGrid>
        <StatTile
          tone={1}
          label={t("totalSales")}
          value={formatUsd(summary.data?.totalSales ?? 0)}
        />
        <StatTile tone={3} label={t("paid")} value={summary.data?.paidCount ?? "—"} />
        <StatTile
          tone={2}
          label={t("average")}
          value={formatUsd(summary.data?.averageSale ?? 0)}
        />
        <StatTile
          tone={4}
          label={t("cancelled")}
          value={summary.data?.cancelledCount ?? "—"}
        />
      </StatGrid>

      <Toolbar
        left={
          <>
            <Input
              type="date"
              value={from}
              onChange={(e) => {
                setFrom(e.target.value);
                setPage(0);
              }}
              className="w-auto"
              aria-label={tA11y("fromDate")}
            />
            <span className="text-ink-500">→</span>
            <Input
              type="date"
              value={to}
              onChange={(e) => {
                setTo(e.target.value);
                setPage(0);
              }}
              className="w-auto"
              aria-label={tA11y("toDate")}
            />
            <Select
              className="w-auto"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value as OrderStatus | "");
                setPage(0);
              }}
              aria-label={tc("status")}
            >
              <option value="">{t("allStatus")}</option>
              <option value="PAID">{tOs("PAID")}</option>
              <option value="OPEN">{tOs("OPEN")}</option>
              <option value="CANCELLED">{tOs("CANCELLED")}</option>
            </Select>
          </>
        }
        right={
          <SearchBar
            value={search}
            onChange={(v) => {
              setSearch(v);
              setPage(0);
            }}
            placeholder={t("invoiceNo")}
          />
        }
      />

      <div className="mb-3.5 flex flex-wrap gap-1.5">
        <Button size="sm" variant="light" onClick={() => resetRange(0)}>
          {tc("today")}
        </Button>
        <Button size="sm" variant="light" onClick={() => resetRange(6)}>
          {tc("sevenDays")}
        </Button>
        <Button size="sm" variant="light" onClick={() => resetRange(29)}>
          {tc("thirtyDays")}
        </Button>
        <Button
          size="sm"
          variant="light"
          onClick={() => {
            setFrom("");
            setTo("");
            setPage(0);
          }}
        >
          {tc("allTime")}
        </Button>
      </div>

      <DataTable
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage={t("noOrders")}
      />

      {list.data && (
        <Pagination
          page={list.data.page}
          totalPages={list.data.totalPages}
          totalElements={list.data.totalElements}
          size={list.data.size}
          onPage={setPage}
        />
      )}
    </>
  );
}
