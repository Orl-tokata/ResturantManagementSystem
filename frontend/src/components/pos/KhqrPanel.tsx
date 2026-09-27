"use client";

import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import QRCode from "qrcode";
import { Alert, Button } from "@/components/ui";
import { del, get, post } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import type { Order } from "@/types/order";

interface KhqrResponse {
  payload: string;
  amount: string;
  currency: string;
  expiresAt: string;
  /** False when no Bakong token is configured: the code scans, but nothing here can confirm it was paid. */
  verifiable: boolean;
}

interface KhqrStatusResponse {
  state: "PAID" | "NOT_PAID" | "UNKNOWN" | "UNVERIFIABLE" | "EXPIRED";
  detail: string | null;
  order: Order;
}

/**
 * The scan-to-pay panel.
 *
 * <p>The bill is frozen the moment the code appears — the server moves it to
 * AWAITING_PAYMENT — so the two things this screen must never do are claim a
 * payment arrived when it has not, and strand a cashier with a bill they cannot
 * settle. Hence the abandon button: it is the way back to cash.
 */
export function KhqrPanel({
  orderId,
  onPaid,
}: {
  orderId: number;
  onPaid: () => void;
}) {
  const t = useTranslations("payment");
  const apiError = useApiError();

  const [error, setError] = useState<string | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  const start = useMutation({
    mutationFn: () => post<KhqrResponse>(`/orders/${orderId}/khqr`, {}),
    onError: (e) => setError(apiError(e, "khqrFailed")),
  });

  const abandon = useMutation({
    mutationFn: () => del<Order>(`/orders/${orderId}/khqr`),
    onError: (e) => setError(apiError(e, "khqrFailed")),
  });

  const qr = start.data;

  /*
   * Ask the bank on a timer, but only while a code is live and only when it
   * can actually answer. Polling with no token would spin forever against a
   * question nothing is going to resolve.
   */
  const status = useQuery({
    queryKey: ["khqr", orderId],
    queryFn: () => get<KhqrStatusResponse>(`/orders/${orderId}/khqr`),
    enabled: Boolean(qr?.verifiable) && !abandon.isSuccess,
    refetchInterval: (query) =>
      query.state.data?.state === "PAID" ? false : 3000,
    retry: false,
  });

  // Draw the code. The payload is a string; turning it into pixels happens
  // here rather than shipping an image over the wire.
  useEffect(() => {
    if (!qr?.payload || !canvasRef.current) return;
    QRCode.toCanvas(canvasRef.current, qr.payload, {
      width: 232,
      margin: 1,
      errorCorrectionLevel: "M",
    }).catch(() => setError(t("khqrDrawFailed")));
  }, [qr?.payload, t]);

  useEffect(() => {
    if (status.data?.state === "PAID") onPaid();
  }, [status.data?.state, onPaid]);

  if (!qr) {
    return (
      <div className="flex flex-col gap-3">
        {error && <Alert tone="error">{error}</Alert>}
        <Button variant="admin" size="lg" block loading={start.isPending} onClick={() => start.mutate()}>
          {t("showKhqr")}
        </Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center gap-3">
      {error && <Alert tone="error">{error}</Alert>}

      <div className="rounded-lg border border-ink-200 bg-white p-3">
        <canvas ref={canvasRef} aria-label={t("khqrAlt", { amount: qr.amount })} />
      </div>

      <div className="text-center">
        <div className="text-lg font-bold">
          {qr.amount} {qr.currency}
        </div>
        <p className="text-xs text-ink-500">{t("khqrScan")}</p>
      </div>

      {qr.verifiable ? (
        <p className="flex items-center gap-2 text-sm text-ink-600">
          <span className="h-2 w-2 animate-pulse rounded-full bg-teal-600" />
          {status.data?.state === "EXPIRED" ? t("khqrExpired") : t("khqrWaiting")}
        </p>
      ) : (
        /*
         * No token, so the bank is never asked. Saying this plainly matters:
         * the alternative is a spinner that never resolves, and a cashier who
         * eventually assumes the payment failed.
         */
        <Alert tone="info">{t("khqrUnverified")}</Alert>
      )}

      <Button variant="light" block loading={abandon.isPending} onClick={() => abandon.mutate()}>
        {t("khqrAbandon")}
      </Button>
    </div>
  );
}
