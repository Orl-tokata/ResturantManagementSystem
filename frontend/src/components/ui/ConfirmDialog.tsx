"use client";

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
  title = "បញ្ជាក់ការលុប · Confirm delete",
  message = "តើអ្នកប្រាកដជាចង់លុបមែនទេ?",
  detail = "This action cannot be undone.",
  confirmLabel = "បាទ/ចាស លុប · Yes, delete",
  cancelLabel = "ទេ · No",
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
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      chrome={destructive ? "danger" : "admin"}
      width="sm"
      footer={
        <>
          <Button variant="light" onClick={onClose} disabled={busy}>
            {cancelLabel}
          </Button>
          <Button
            variant={destructive ? "danger" : "admin"}
            onClick={onConfirm}
            loading={busy}
          >
            {confirmLabel}
          </Button>
        </>
      }
    >
      <div className="text-center">
        <div className="mb-2 text-4xl">{destructive ? "⚠️" : "❓"}</div>
        <p className="text-sm">
          {message}
          {detail && (
            <>
              <br />
              <span className="text-ink-500">{detail}</span>
            </>
          )}
        </p>
      </div>
    </Modal>
  );
}
