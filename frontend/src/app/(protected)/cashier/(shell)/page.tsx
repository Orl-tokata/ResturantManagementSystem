"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  Alert,
  Badge,
  Button,
  Card,
  DataTable,
  StatGrid,
  StatTile,
  toneForOrderStatus,
  type Column,
} from "@/components/ui";
import { get, type PageResponse } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import { formatUsd } from "@/lib/format";
import type { Order } from "@/types/order";
import type { CashierSummary } from "@/types/report";

export default function CashierHomePage() {
  const t = useTranslations("cashierHome");
  const tc = useTranslations("common");
  const tH = useTranslations("history");
  const tOs = useTranslations("enum.orderStatus");
  const tPos = useTranslations("pos");
  const apiError = useApiError();

  const summary = useQuery({
    queryKey: ["dashboard", "cashier"],
    queryFn: () => get<CashierSummary>("/dashboard/cashier"),
  });

  const recent = useQuery({
    queryKey: ["orders", { size: 5, page: 0 }],
    queryFn: () => get<PageResponse<Order>>("/orders", { size: 5, page: 0 }),
  });

  const s = summary.data;

  const columns: Column<Order>[] = [
    { key: "inv", header: tH("invoice"), render: (r) => r.invoiceNo },
    { key: "table", header: tPos("table"), render: (r) => r.tableName ?? "—" },
    { key: "items", header: tH("dishes"), numeric: true, render: (r) => r.items.length },
    { key: "total", header: tc("total"), numeric: true, render: (r) => formatUsd(r.total) },
    {
      key: "status",
      header: tc("status"),
      render: (r) => (
        <Badge tone={toneForOrderStatus(r.status)}>{tOs(r.status)}</Badge>
      ),
    },
    {
      key: "go",
      header: "",
      align: "right",
      render: (r) =>
        r.status === "OPEN" ? (
          <Link href={`/cashier/order?tableId=${r.tableId ?? ""}`}>
            <Button size="sm" variant="accent">
              {t("continue")}
            </Button>
          </Link>
        ) : (
          <Link href={`/cashier/receipt/${r.id}`}>
            <Button size="sm" variant="ghost">
              🧾
            </Button>
          </Link>
        ),
    },
  ];

  return (
    <>
      {summary.isError && <Alert tone="error">{apiError(summary.error)}</Alert>}

      <StatGrid>
        <StatTile tone={1} label={t("todaySales")} value={formatUsd(s?.todaySales ?? 0)} />
        <StatTile tone={3} label={t("todayInvoices")} value={s?.todayInvoices ?? "—"} />
        <StatTile
          tone={2}
          label={t("occupied")}
          value={s ? `${s.tablesOccupied} / ${s.tablesTotal}` : "—"}
        />
        <StatTile tone={4} label={t("openBills")} value={s?.openBills ?? "—"} />
      </StatGrid>

      <h2 className="mb-3 text-xl font-semibold">{t("quickActions")}</h2>

      <div className="mb-4 flex flex-wrap gap-2">
        <Link href="/cashier/tables">
          <Button variant="accent" size="lg">
            🪑 {t("selectTable")}
          </Button>
        </Link>
        <Link href="/cashier/order">
          <Button variant="primary" size="lg">
            🛒 {t("newOrder")}
          </Button>
        </Link>
        <Link href="/cashier/history">
          <Button variant="ghost" size="lg">
            🕘 {tH("title")}
          </Button>
        </Link>
      </div>

      <Card
        title={t("recentOrders")}
        action={
          <Link href="/cashier/history">
            <Button size="sm" variant="ghost">
              {tc("viewAll")}
            </Button>
          </Link>
        }
        padded={false}
      >
        <DataTable
          columns={columns}
          rows={recent.data?.content ?? []}
          rowKey={(r) => r.id}
          loading={recent.isLoading}
          emptyMessage={t("noOrders")}
        />
      </Card>
    </>
  );
}
