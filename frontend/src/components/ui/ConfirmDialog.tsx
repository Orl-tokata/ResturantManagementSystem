"use client";

import { useTranslations } from "next-intl";
import type { ReactNode } from "react";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";

/**
 * The delete-confirmation pattern repeated across six prototype screens.
 * Destructive by default, because that is what it is nearly always used for.
 */
export function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title,
  message,
  detail,
  confirmLabel,
  cancelLabel,
  destructive = true,
  busy = false,
}: {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title?: ReactNode;
  message?: ReactNode;
  detail?: ReactNode;
  confirmLabel?: ReactNode;
  cancelLabel?: ReactNode;
  destructive?: boolean;
  busy?: boolean;
}) {
  const t = useTranslations("common");

  // Every label falls back to the translated default, so a caller only passes
  // the ones it wants to override.
  const heading = title ?? t("confirmDelete");
  const body = message ?? t("confirmDeleteMessage", { name: "" });
  // Only a destructive action gets the irreversibility warning by default. It
  // used to be unconditional, so the first non-destructive dialog — unlocking
  // an account, which can simply be done again — told the reader it could not
  // be undone. A caller wanting a hint on one of those passes `detail`.
  const hint = detail ?? (destructive ? t("cannotUndo") : undefined);
  const confirmText = confirmLabel ?? t("yesDelete");
  const cancelText = cancelLabel ?? t("no");

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={heading}
      chrome={destructive ? "danger" : "admin"}
      width="sm"
      footer={
        <>
          <Button variant="light" onClick={onClose} disabled={busy}>
            {cancelText}
          </Button>
          <Button
            variant={destructive ? "danger" : "admin"}
            onClick={onConfirm}
            loading={busy}
          >
            {confirmText}
          </Button>
        </>
      }
    >
      <div className="text-center">
        <div className="mb-2 text-4xl">{destructive ? "⚠️" : "❓"}</div>
        <p className="text-sm">
          {body}
          {hint && (
            <>
              <br />
              <span className="text-ink-500">{hint}</span>
            </>
          )}
        </p>
      </div>
    </Modal>
  );
}
