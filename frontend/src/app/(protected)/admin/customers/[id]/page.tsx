"use client";

import { useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  Alert,
  Badge,
  Button,
  Card,
  type Column,
  DataTable,
  Field,
  Input,
  ListPage,
  Modal,
  StatGrid,
  StatTile,
  Textarea,
  Toolbar,
  useToast,
} from "@/components/ui";
import { get, post, type PageResponse } from "@/lib/api";
import { formatUsd } from "@/lib/format";
import { useApiError } from "@/lib/use-api-error";
import type { Customer, LoyaltyEntry, LoyaltyType } from "@/types/customer";
import type { Order } from "@/types/order";

/** Points arriving, points leaving, points corrected. */
const TONE: Record<LoyaltyType, "ok" | "info" | "warn" | "dead"> = {
  EARN: "ok",
  REDEEM: "info",
  ADJUST: "warn",
  EXPIRE: "dead",
  REVERSE: "dead",
};

export default function CustomerDetailPage() {
  const id = Number(useParams().id);
  const t = useTranslations("customers");
  const tc = useTranslations("common");
  const tH = useTranslations("history");
  const tNav = useTranslations("nav");
  const tL = useTranslations("enum.loyaltyType");
  const apiError = useApiError();
  const toast = useToast();
  const qc = useQueryClient();

  const [adjusting, setAdjusting] = useState(false);
  const [points, setPoints] = useState("");
  const [note, setNote] = useState("");
  const [adjustError, setAdjustError] = useState<string | null>(null);

  const customer = useQuery({
    queryKey: ["customers", id],
    queryFn: () => get<Customer>(`/customers/${id}`),
  });

  const ledger = useQuery({
    queryKey: ["customers", id, "loyalty"],
    queryFn: () => get<PageResponse<LoyaltyEntry>>(`/customers/${id}/loyalty`, { size: 50 }),
  });

  const history = useQuery({
    queryKey: ["customers", id, "orders"],
    queryFn: () => get<PageResponse<Order>>("/orders", { customerId: id, size: 20 }),
  });

  const adjust = useMutation({
    mutationFn: () => post<LoyaltyEntry>(`/customers/${id}/loyalty`, {
      points: Number(points),
      note: note.trim(),
    }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["customers", id] });
      setAdjusting(false);
      setPoints("");
      setNote("");
      toast.success(t("adjusted"));
    },
    onError: (e) => setAdjustError(apiError(e, "adjustLoyalty")),
  });

  const c = customer.data;

  const ledgerColumns: Column<LoyaltyEntry>[] = [
    { key: "when", header: tc("date"), render: (r) => new Date(r.createdAt).toLocaleString() },
    { key: "type", header: tc("type"), render: (r) => <Badge tone={TONE[r.type]}>{tL(r.type)}</Badge> },
    {
      key: "points",
      header: t("points"),
      numeric: true,
      // Signed as stored: these are the numbers that add up to the balance,
      // so showing them any other way would stop them adding up.
      render: (r) => (
        <span className={r.points < 0 ? "text-danger" : "text-success"}>
          {r.points > 0 ? "+" : ""}
          {r.points}
        </span>
      ),
    },
    {
      key: "why",
      header: tc("reason"),
      render: (r) =>
        r.invoiceNo ? (
          <Link href={`/cashier/receipt/${r.orderId}`} className="text-brand-600 hover:underline">
            {r.invoiceNo}
          </Link>
        ) : (
          (r.note ?? "—")
        ),
    },
    { key: "by", header: t("by"), hideOnMobile: true, render: (r) => r.createdBy ?? "—" },
  ];

  const orderColumns: Column<Order>[] = [
    { key: "inv", header: tH("invoice"), render: (o) => o.invoiceNo },
    {
      key: "when",
      header: tc("date"),
      render: (o) => (o.paidAt ? new Date(o.paidAt).toLocaleString() : "—"),
    },
    { key: "items", header: tH("dishes"), numeric: true, render: (o) => o.items.length },
    { key: "total", header: tc("total"), numeric: true, render: (o) => formatUsd(o.total) },
    {
      key: "go",
      header: "",
      align: "right",
      render: (o) => (
        <Link href={`/cashier/receipt/${o.id}`} className="text-sm text-brand-600 hover:underline">
          {tNav("receipt")}
        </Link>
      ),
    },
  ];

  return (
    <ListPage>
      {customer.isError && <Alert tone="error">{apiError(customer.error)}</Alert>}

      <StatGrid>
        <StatTile tone={1} label={t("points")} value={c?.points ?? "—"} />
        <StatTile tone={3} label={t("totalSpent")} value={formatUsd(c?.totalSpent ?? 0)} />
        <StatTile tone={2} label={t("visits")} value={c?.visitCount ?? "—"} />
        <StatTile
          tone={4}
          label={t("lastVisit")}
          value={c?.lastVisit ? new Date(c.lastVisit).toLocaleDateString() : "—"}
        />
      </StatGrid>

      <Toolbar
        left={
          <>
            <Link href="/admin/customers">
              <Button variant="light">← {t("backToList")}</Button>
            </Link>
            <Button variant="admin" onClick={() => { setAdjustError(null); setAdjusting(true); }}>
              ⚖️ {t("adjust")}
            </Button>
          </>
        }
      />

      <Card title={`${c?.name ?? ""} · ${c?.code ?? ""}`} className="mb-4">
        <dl className="grid gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
          <Pair label={t("phone")} value={c?.phone ?? "—"} />
          <Pair label={tc("email")} value={c?.email ?? "—"} />
          <Pair
            label={t("birthDate")}
            value={c?.birthDate ? new Date(c.birthDate).toLocaleDateString() : "—"}
          />
          <Pair label={tc("note")} value={c?.note ?? "—"} />
        </dl>
      </Card>

      <Card title={t("ledger")} className="mb-4">
        <DataTable
          columns={ledgerColumns}
          height="34vh"
          rows={ledger.data?.content ?? []}
          rowKey={(r) => r.id}
          loading={ledger.isLoading}
          emptyMessage={t("noPoints")}
        />
      </Card>

      <Card title={t("purchases")}>
        <DataTable
          columns={orderColumns}
          height="34vh"
          rows={history.data?.content ?? []}
          rowKey={(o) => o.id}
          loading={history.isLoading}
          emptyMessage={t("noPurchases")}
        />
      </Card>

      <Modal
        open={adjusting}
        onClose={() => setAdjusting(false)}
        title={t("adjustTitle")}
        width="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setAdjusting(false)}>
              {tc("cancel")}
            </Button>
            <Button
              variant="admin"
              onClick={() => adjust.mutate()}
              loading={adjust.isPending}
              disabled={!points || Number(points) === 0 || !note.trim()}
            >
              {tc("save")}
            </Button>
          </>
        }
      >
        {adjustError && <Alert tone="error">{adjustError}</Alert>}

        <p className="mb-3 text-sm text-ink-500">{t("adjustHelp")}</p>

        <Field label={t("points")} htmlFor="l-points" required hint={t("adjustHint")}>
          <Input
            id="l-points"
            type="number"
            step="0.01"
            inputMode="decimal"
            value={points}
            onChange={(e) => setPoints(e.target.value)}
            placeholder="-50"
          />
        </Field>

        {/* Required: an unexplained correction to someone's points is exactly
            what a ledger exists to prevent. */}
        <Field label={tc("reason")} htmlFor="l-note" required>
          <Textarea id="l-note" value={note} onChange={(e) => setNote(e.target.value)} />
        </Field>
      </Modal>
    </ListPage>
  );
}

function Pair({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-3 border-b border-ink-100 py-1 last:border-0">
      <dt className="text-ink-500">{label}</dt>
      <dd className="text-right font-medium">{value}</dd>
    </div>
  );
}
