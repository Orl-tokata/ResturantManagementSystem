"use client";

import Link from "next/link";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
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
import { errorMessage } from "@/lib/errors";
import { formatUsd } from "@/lib/format";
import {
  ORDER_STATUS_LABEL,
  PAYMENT_LABEL,
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
      header: "វិក្កយបត្រ · Invoice",
      render: (r) => <span className="font-medium">{r.invoiceNo}</span>,
    },
    { key: "table", header: "តុ", render: (r) => r.tableName ?? "—" },
    {
      key: "cashier",
      header: "អ្នកគិតលុយ",
      hideOnMobile: true,
      render: (r) => r.cashierName ?? "—",
    },
    { key: "items", header: "មុខម្ហូប", numeric: true, render: (r) => r.items.length },
    { key: "total", header: "សរុប · Total", numeric: true, render: (r) => formatUsd(r.total) },
    {
      key: "method",
      header: "វិធីបង់",
      hideOnMobile: true,
      render: (r) => (r.paymentMethod ? PAYMENT_LABEL[r.paymentMethod] : "—"),
    },
    {
      key: "status",
      header: "ស្ថានភាព",
      render: (r) => (
        <Badge tone={toneForOrderStatus(r.status)}>{ORDER_STATUS_LABEL[r.status]}</Badge>
      ),
    },
    {
      key: "time",
      header: "ម៉ោង",
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
              🧾 មើល
            </Button>
          </Link>
          {r.status === "OPEN" && (
            <Link href={`/cashier/order?tableId=${r.tableId ?? ""}`}>
              <Button size="sm" variant="accent">
                បន្ត
              </Button>
            </Link>
          )}
        </div>
      ),
    },
  ];

  return (
    <>
      {list.isError && <Alert tone="error">{errorMessage(list.error)}</Alert>}

      <StatGrid>
        <StatTile
          tone={1}
          label="សរុបការលក់ · Total sales"
          value={formatUsd(summary.data?.totalSales ?? 0)}
        />
        <StatTile tone={3} label="បានបង់ · Paid" value={summary.data?.paidCount ?? "—"} />
        <StatTile
          tone={2}
          label="មធ្យម · Average"
          value={formatUsd(summary.data?.averageSale ?? 0)}
        />
        <StatTile
          tone={4}
          label="បានលុប · Cancelled"
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
              aria-label="From date"
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
              aria-label="To date"
            />
            <Select
              className="w-auto"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value as OrderStatus | "");
                setPage(0);
              }}
              aria-label="Status"
            >
              <option value="">ស្ថានភាពទាំងអស់ · All status</option>
              <option value="PAID">{ORDER_STATUS_LABEL.PAID}</option>
              <option value="OPEN">{ORDER_STATUS_LABEL.OPEN}</option>
              <option value="CANCELLED">{ORDER_STATUS_LABEL.CANCELLED}</option>
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
            placeholder="លេខវិក្កយបត្រ · Invoice no."
          />
        }
      />

      <div className="mb-3.5 flex flex-wrap gap-1.5">
        <Button size="sm" variant="light" onClick={() => resetRange(0)}>
          ថ្ងៃនេះ · Today
        </Button>
        <Button size="sm" variant="light" onClick={() => resetRange(6)}>
          ៧ ថ្ងៃ · 7 days
        </Button>
        <Button size="sm" variant="light" onClick={() => resetRange(29)}>
          ៣០ ថ្ងៃ · 30 days
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
          ទាំងអស់ · All time
        </Button>
      </div>

      <DataTable
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage="គ្មានវិក្កយបត្រក្នុងចន្លោះពេលនេះ · No orders in this range"
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
