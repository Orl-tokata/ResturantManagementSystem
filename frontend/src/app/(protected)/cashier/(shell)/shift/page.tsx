"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
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
  FieldRow,
  Input,
  ListPage,
  Modal,
  Select,
  StatGrid,
  StatTile,
  Textarea,
  Toolbar,
  useToast,
} from "@/components/ui";
import { get, post } from "@/lib/api";
import { formatUsd } from "@/lib/format";
import { useApiError } from "@/lib/use-api-error";
import { SHIFT_KEY, useOpenShift } from "@/lib/use-shift";
import {
  MANUAL_MOVEMENTS,
  type CashMovement,
  type CashMovementType,
  type Shift,
  type ShiftDetail,
} from "@/types/shift";

/** What a cashier is most likely to be starting with, as a starting point. */
const DEFAULT_FLOAT = "100.00";

export default function ShiftPage() {
  const t = useTranslations("shift");
  const tc = useTranslations("common");
  const tMv = useTranslations("enum.cashMovement");
  const apiError = useApiError();
  const toast = useToast();
  const qc = useQueryClient();
  const router = useRouter();
  const next = useSearchParams().get("next");

  const { data: shift, isLoading } = useOpenShift();

  const [openingFloat, setOpeningFloat] = useState(DEFAULT_FLOAT);
  const [openError, setOpenError] = useState<string | null>(null);

  const [closing, setClosing] = useState(false);
  const [declared, setDeclared] = useState("");
  const [closeNote, setCloseNote] = useState("");
  const [closeError, setCloseError] = useState<string | null>(null);

  const [movingCash, setMovingCash] = useState(false);
  const [moveType, setMoveType] = useState<CashMovementType>("PAY_OUT");
  const [moveAmount, setMoveAmount] = useState("");
  const [moveReason, setMoveReason] = useState("");
  const [moveError, setMoveError] = useState<string | null>(null);

  /** The last shift this person closed, so the Z-report survives the close. */
  const [closedId, setClosedId] = useState<number | null>(null);

  const detail = useQuery({
    queryKey: ["shift", shift?.id ?? closedId, "detail"],
    queryFn: () => get<ShiftDetail>(`/shifts/${shift?.id ?? closedId}`),
    enabled: shift != null || closedId != null,
  });

  function refresh() {
    void qc.invalidateQueries({ queryKey: SHIFT_KEY });
    void qc.invalidateQueries({ queryKey: ["shift"] });
  }

  const openShift = useMutation({
    mutationFn: () => post<Shift>("/shifts", { openingFloat: Number(openingFloat) }),
    onSuccess: () => {
      setClosedId(null);
      refresh();
      // Straight back to what they were trying to do. The gate sent them here;
      // it should not also make them find their way back.
      if (next) router.replace(next);
    },
    onError: (e) => setOpenError(apiError(e, "openShift")),
  });

  const closeShift = useMutation({
    mutationFn: () =>
      post<Shift>(`/shifts/${shift!.id}/close`, {
        declaredCash: Number(declared),
        note: closeNote.trim() || undefined,
      }),
    onSuccess: (closed) => {
      setClosedId(closed.id);
      setClosing(false);
      setDeclared("");
      setCloseNote("");
      refresh();
    },
    onError: (e) => setCloseError(apiError(e, "closeShift")),
  });

  const addMovement = useMutation({
    mutationFn: () =>
      post<CashMovement>(`/shifts/${shift!.id}/movements`, {
        type: moveType,
        amount: Number(moveAmount),
        reason: moveReason.trim(),
      }),
    onSuccess: () => {
      setMovingCash(false);
      setMoveAmount("");
      setMoveReason("");
      refresh();
      toast.success(t("cashRecorded"));
    },
    onError: (e) => setMoveError(apiError(e, "recordCash")),
  });

  /*
   * Worked out here as well as on the server, and shown while the number is
   * being typed. The variance is the only figure on this screen a cashier
   * actually has to think about, and finding it out after pressing Close is
   * too late to go and look in the drawer again.
   */
  const expected = shift?.expectedCash ?? 0;
  const declaredNumber = declared.trim() === "" ? null : Number(declared);
  const variance =
    declaredNumber === null || Number.isNaN(declaredNumber) ? null : declaredNumber - expected;
  const varianceNeedsNote = variance !== null && Math.abs(variance) >= 0.005;

  const movementColumns: Column<CashMovement>[] = [
    {
      key: "when",
      header: tc("date"),
      render: (m) => new Date(m.createdAt).toLocaleTimeString(),
    },
    {
      key: "type",
      header: tc("type"),
      render: (m) => (
        <Badge tone={m.increase ? "ok" : "info"}>{tMv(m.type)}</Badge>
      ),
    },
    {
      key: "amount",
      header: tc("amount"),
      numeric: true,
      render: (m) => (
        <span className={m.increase ? "text-success-ink" : "text-danger"}>
          {m.increase ? "+" : "−"}
          {formatUsd(m.amount)}
        </span>
      ),
    },
    { key: "why", header: tc("reason"), render: (m) => m.reason ?? "—" },
    { key: "by", header: t("by"), hideOnMobile: true, render: (m) => m.createdBy },
  ];

  /* ---- No shift open ----------------------------------------------------- */

  if (!isLoading && !shift) {
    const closed = closedId != null ? detail.data?.shift : undefined;

    return (
      <ListPage>
        {closed && <ZReport shift={closed} movements={detail.data?.movements ?? []} />}

        <Card title={t("startTitle")} className="mx-auto w-full max-w-md">
          {openError && <Alert tone="error">{openError}</Alert>}

          <p className="mb-4 text-sm text-ink-500">{t("startHelp")}</p>

          <Field label={t("openingFloat")} htmlFor="s-float" required>
            <Input
              id="s-float"
              type="number"
              step="0.01"
              min="0"
              inputMode="decimal"
              className="text-2xl font-[family-name:var(--font-num)]"
              value={openingFloat}
              onChange={(e) => setOpeningFloat(e.target.value)}
              autoFocus
            />
          </Field>

          <Button
            variant="primary"
            className="mt-4 w-full py-3 text-base"
            onClick={() => {
              setOpenError(null);
              openShift.mutate();
            }}
            loading={openShift.isPending}
          >
            {t("start")}
          </Button>
        </Card>
      </ListPage>
    );
  }

  /* ---- A shift is running ------------------------------------------------- */

  return (
    <ListPage>
      <StatGrid>
        <StatTile tone={1} label={t("openingFloat")} value={formatUsd(shift?.openingFloat ?? 0)} />
        <StatTile
          tone={3}
          label={t("cashSales")}
          value={formatUsd(shift?.cashSales ?? 0)}
        />
        <StatTile tone={2} label={t("expected")} value={formatUsd(expected)} />
        <StatTile
          tone={4}
          label={t("openedAt")}
          value={shift ? new Date(shift.openedAt).toLocaleTimeString() : "—"}
        />
      </StatGrid>

      <Toolbar
        left={
          <>
            <Button variant="light" onClick={() => { setMoveError(null); setMovingCash(true); }}>
              💵 {t("recordCash")}
            </Button>
            <Button
              variant="primary"
              onClick={() => {
                setCloseError(null);
                setDeclared("");
                setClosing(true);
              }}
            >
              🔒 {t("close")}
            </Button>
          </>
        }
      />

      <DataTable
        fill
        columns={movementColumns}
        rows={detail.data?.movements ?? []}
        rowKey={(m) => m.id}
        loading={detail.isLoading}
        emptyMessage={t("noMovements")}
      />

      {/* ---- cash in or out ---- */}
      <Modal
        open={movingCash}
        onClose={() => setMovingCash(false)}
        title={t("recordCash")}
        width="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setMovingCash(false)}>
              {tc("cancel")}
            </Button>
            <Button
              variant="primary"
              onClick={() => addMovement.mutate()}
              loading={addMovement.isPending}
              disabled={!moveAmount || !moveReason.trim()}
            >
              {tc("save")}
            </Button>
          </>
        }
      >
        {moveError && <Alert tone="error">{moveError}</Alert>}

        <FieldRow>
          <Field label={tc("type")} htmlFor="m-type" required>
            <Select
              id="m-type"
              value={moveType}
              onChange={(e) => setMoveType(e.target.value as CashMovementType)}
            >
              {MANUAL_MOVEMENTS.map((type) => (
                <option key={type} value={type}>
                  {tMv(type)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={tc("amount")} htmlFor="m-amount" required>
            <Input
              id="m-amount"
              type="number"
              step="0.01"
              min="0.01"
              inputMode="decimal"
              value={moveAmount}
              onChange={(e) => setMoveAmount(e.target.value)}
            />
          </Field>
        </FieldRow>

        {/* Required, because an amount with no reason is the entry nobody can
            explain at the end of the day. */}
        <Field label={tc("reason")} htmlFor="m-why" required hint={t("reasonHint")}>
          <Textarea
            id="m-why"
            value={moveReason}
            onChange={(e) => setMoveReason(e.target.value)}
            placeholder={t("reasonPlaceholder")}
          />
        </Field>
      </Modal>

      {/* ---- closing ---- */}
      <Modal
        open={closing}
        onClose={() => setClosing(false)}
        title={t("closeTitle")}
        width="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setClosing(false)}>
              {tc("cancel")}
            </Button>
            <Button
              variant="primary"
              onClick={() => closeShift.mutate()}
              loading={closeShift.isPending}
              disabled={declaredNumber === null || (varianceNeedsNote && !closeNote.trim())}
            >
              {t("close")}
            </Button>
          </>
        }
      >
        {closeError && <Alert tone="error">{closeError}</Alert>}

        <p className="mb-3 text-sm text-ink-500">{t("closeHelp")}</p>

        <Field label={t("declared")} htmlFor="s-declared" required>
          <Input
            id="s-declared"
            type="number"
            step="0.01"
            min="0"
            inputMode="decimal"
            className="text-2xl font-[family-name:var(--font-num)]"
            value={declared}
            onChange={(e) => setDeclared(e.target.value)}
            autoFocus
          />
        </Field>

        <dl className="mt-4 space-y-1.5 text-sm">
          <Line label={t("expected")} value={formatUsd(expected)} />
          {variance !== null && (
            <Line
              label={variance < 0 ? t("short") : variance > 0 ? t("over") : t("balanced")}
              value={formatUsd(Math.abs(variance))}
              tone={varianceNeedsNote ? (variance < 0 ? "bad" : "warn") : "good"}
            />
          )}
        </dl>

        {varianceNeedsNote && (
          <Field label={tc("reason")} htmlFor="s-note" required hint={t("varianceHint")}>
            <Textarea
              id="s-note"
              value={closeNote}
              onChange={(e) => setCloseNote(e.target.value)}
              placeholder={t("variancePlaceholder")}
            />
          </Field>
        )}
      </Modal>
    </ListPage>
  );
}

/* ---- Pieces --------------------------------------------------------------- */

function Line({
  label,
  value,
  tone = "plain",
}: {
  label: string;
  value: string;
  tone?: "plain" | "good" | "warn" | "bad";
}) {
  const colour =
    tone === "bad" ? "text-danger" : tone === "warn" ? "text-warning-ink" : tone === "good" ? "text-success-ink" : "text-ink-900";
  return (
    <div className="flex justify-between gap-3">
      <dt className="text-ink-500">{label}</dt>
      <dd className={`font-[family-name:var(--font-num)] font-semibold ${colour}`}>{value}</dd>
    </div>
  );
}

/**
 * The Z-report, shown after a shift closes.
 *
 * <p>Printed from the browser rather than built as its own route: it is read
 * once, at the till, by the person who just closed the drawer.
 */
function ZReport({ shift, movements }: { shift: Shift; movements: CashMovement[] }) {
  const t = useTranslations("shift");
  const tc = useTranslations("common");
  const tMv = useTranslations("enum.cashMovement");
  const variance = shift.variance ?? 0;

  return (
    <Card
      title={`${t("zReport")} — ${shift.userName}`}
      className="mb-4"
      action={
        <Button variant="ghost" onClick={() => window.print()}>
          🖨️ {tc("print")}
        </Button>
      }
    >
      <dl className="space-y-1.5 text-sm">
        <Line label={t("openedAt")} value={new Date(shift.openedAt).toLocaleString()} />
        <Line
          label={t("closedAt")}
          value={shift.closedAt ? new Date(shift.closedAt).toLocaleString() : "—"}
        />
        <Line label={t("openingFloat")} value={formatUsd(shift.openingFloat)} />
        {shift.totals.map((total) => (
          <Line key={total.type} label={`${tMv(total.type)} (${total.count})`} value={formatUsd(total.total)} />
        ))}
        <Line label={t("expected")} value={formatUsd(shift.expectedCash)} />
        <Line label={t("declared")} value={formatUsd(shift.declaredCash ?? 0)} />
        <Line
          label={variance < 0 ? t("short") : variance > 0 ? t("over") : t("balanced")}
          value={formatUsd(Math.abs(variance))}
          tone={variance < 0 ? "bad" : variance > 0 ? "warn" : "good"}
        />
      </dl>

      {shift.note && (
        <p className="mt-3 rounded bg-ink-100 px-3 py-2 text-sm text-ink-700">{shift.note}</p>
      )}

      <p className="mt-3 text-xs text-ink-500">
        {t("movementCount", { count: movements.length })}
      </p>
    </Card>
  );
}
