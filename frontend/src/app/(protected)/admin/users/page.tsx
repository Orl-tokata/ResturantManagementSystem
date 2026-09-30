"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  Badge,
  Button,
  type Column,
  ConfirmDialog,
  DataTable,
  ListPage,
  Pagination,
  useToast,
} from "@/components/ui";
import { useList } from "@/hooks/useCrud";
import { post } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import { formatTimestamp } from "@/lib/format";
import type { UserAccount } from "@/types/account";

/** Where this screen starts; the reader can change it. */
const INITIAL_SIZE = 20;

export default function UsersPage() {
  const t = useTranslations("users");
  const tc = useTranslations("common");
  const tRole = useTranslations("enum.role");
  const apiError = useApiError();
  const toast = useToast();
  const qc = useQueryClient();

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(INITIAL_SIZE);
  const [unlocking, setUnlocking] = useState<UserAccount | null>(null);

  const list = useList<UserAccount>("users", { page, size });

  const unlock = useMutation({
    mutationFn: (id: number) => post<UserAccount>(`/users/${id}/unlock`),
    onSuccess: (account) => {
      void qc.invalidateQueries({ queryKey: ["users"] });
      toast.success(t("unlocked", { name: account.username }));
    },
    onError: (e) => toast.error(apiError(e, "unlock")),
    onSettled: () => setUnlocking(null),
  });

  const columns: Column<UserAccount>[] = [
    {
      key: "username",
      header: t("username"),
      render: (r) => (
        <>
          <div className="font-medium">{r.username}</div>
          <div className="text-xs text-ink-500">{r.fullName}</div>
        </>
      ),
    },
    {
      key: "role",
      header: tc("role"),
      render: (r) => <Badge tone="neutral">{tRole(r.role)}</Badge>,
    },
    {
      key: "state",
      header: tc("status"),
      render: (r) => <State account={r} t={t} />,
    },
    {
      key: "failed",
      header: t("failedAttempts"),
      numeric: true,
      hideOnMobile: true,
      // Zero is the normal case and says nothing; a dash reads faster than a
      // column of noughts, and makes a non-zero count stand out.
      render: (r) =>
        r.failedAttempts > 0 ? (
          <b className="text-danger">{r.failedAttempts}</b>
        ) : (
          <span className="text-ink-400">—</span>
        ),
    },
    {
      key: "lastLogin",
      header: t("lastLogin"),
      hideOnMobile: true,
      render: (r) => (
        <span className="text-xs tabular-nums text-ink-500">
          {r.lastLoginAt ? formatTimestamp(r.lastLoginAt) : t("neverSignedIn")}
        </span>
      ),
    },
    {
      key: "actions",
      header: tc("actions"),
      align: "right",
      render: (r) => (
        <Button
          size="sm"
          variant={r.locked ? "admin" : "ghost"}
          disabled={!r.locked}
          onClick={() => setUnlocking(r)}
        >
          🔓 {t("unlock")}
        </Button>
      ),
    },
  ];

  return (
    <ListPage>
      {list.isError && <Alert tone="error">{apiError(list.error)}</Alert>}

      <p className="mb-3 text-xs text-ink-500">{t("help")}</p>

      <DataTable
        fill
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage={t("noAccounts")}
      />

      <Pagination
        page={list.data?.page ?? 0}
        totalPages={list.data?.totalPages ?? 0}
        totalElements={list.data?.totalElements ?? 0}
        size={list.data?.size ?? size}
        onPage={setPage}
        onSize={(n) => {
          setSize(n);
          setPage(0);
        }}
      />

      <ConfirmDialog
        open={unlocking !== null}
        title={t("unlockTitle")}
        message={t("unlockConfirm", { name: unlocking?.username ?? "" })}
        confirmLabel={t("unlock")}
        // Not destructive: this restores access rather than removing anything,
        // and a red button would say the opposite of what the action does.
        destructive={false}
        busy={unlock.isPending}
        onClose={() => setUnlocking(null)}
        onConfirm={() => unlocking && unlock.mutate(unlocking.id)}
      />
    </ListPage>
  );
}

/**
 * Locked, and for how much longer.
 *
 * <p>An automatic lock says when it lifts, so an administrator can decide
 * whether to intervene at all — usually the answer is to wait. A lock with no
 * deadline was set by a person and will not lift on its own, which is worth
 * saying differently.
 */
function State({
  account,
  t,
}: {
  account: UserAccount;
  t: ReturnType<typeof useTranslations<"users">>;
}) {
  if (!account.active) return <Badge tone="dead">{t("disabled")}</Badge>;
  if (!account.locked) return <Badge tone="ok">{t("ok")}</Badge>;

  return (
    <div className="flex flex-col gap-0.5">
      <Badge tone="warn">{t("locked")}</Badge>
      <span className="text-[11px] text-ink-500">
        {account.lockedUntil
          ? t("until", { time: formatTimestamp(account.lockedUntil) })
          : t("lockedByAdmin")}
      </span>
    </div>
  );
}
