"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  Alert,
  Badge,
  Button,
  ConfirmDialog,
  DataTable,
  Field,
  FieldRow,
  Input,
  ListPage,
  Modal,
  Pagination,
  SearchBar,
  Select,
  Textarea,
  Toolbar,
  type BadgeTone,
  type Column,
} from "@/components/ui";
import { useList, useRemove, useSave } from "@/hooks/useCrud";
import { post } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import { formatUsd } from "@/lib/format";
import type { AccountRequest, Role } from "@/types/auth";
import {
  type Gender,
  type Shift,
  type Staff,
  type StaffRequest,
  type StaffStatus,
} from "@/types/master";

const EMPTY: StaffRequest = {
  staffCode: "",
  staffName: "",
  role: "CASHIER",
  status: "ACTIVE",
};

const EMPTY_ACCOUNT: AccountRequest = {
  username: "",
  password: "",
  fullName: "",
  role: "CASHIER",
};

const SIZE = 20;

const STATUS_TONE: Record<StaffStatus, BadgeTone> = {
  ACTIVE: "ok",
  ON_LEAVE: "warn",
  RESIGNED: "dead",
};

export default function StaffPage() {
  const t = useTranslations("staff");
  const tc = useTranslations("common");
  const tRole = useTranslations("enum.role");
  const tShift = useTranslations("enum.shift");
  const tGender = useTranslations("enum.gender");
  const tSt = useTranslations("enum.staffStatus");
  const tA11y = useTranslations("a11y");
  const apiError = useApiError();

  const [search, setSearch] = useState("");
  const [role, setRole] = useState<Role | "">("");
  const [page, setPage] = useState(0);

  const [editingId, setEditingId] = useState<number | null | undefined>(undefined);
  const [draft, setDraft] = useState<StaffRequest>(EMPTY);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<Staff | null>(null);
  const [listError, setListError] = useState<string | null>(null);

  /* The login account, which is a different thing from the staff row beside
     it. A staff record is the employment: code, shift, salary. An account is
     the ability to sign in. The two are separate tables and nothing yet joins
     them, so this creates an account *using* someone's details rather than
     one that belongs to their row. */
  const [accountFor, setAccountFor] = useState<Staff | null>(null);
  const [account, setAccount] = useState<AccountRequest>(EMPTY_ACCOUNT);
  const [accountError, setAccountError] = useState<string | null>(null);
  const [accountMade, setAccountMade] = useState<string | null>(null);

  const list = useList<Staff>("staff", {
    search,
    role: role === "" ? undefined : role,
    page,
    size: SIZE,
  });
  const save = useSave<Staff, StaffRequest>("staff");
  const remove = useRemove("staff");

  // Not useSave: this is not a CRUD resource with a list to invalidate, and
  // the accounts it creates are not what this screen is showing.
  const createAccount = useMutation({
    mutationFn: (body: AccountRequest) => post<{ username: string }>("/auth/register", body),
  });

  function openNew() {
    setEditingId(null);
    setDraft(EMPTY);
    setFormError(null);
  }

  function openEdit(row: Staff) {
    setEditingId(row.id);
    setDraft({
      staffCode: row.staffCode,
      staffName: row.staffName,
      gender: row.gender ?? undefined,
      dateOfBirth: row.dateOfBirth ?? undefined,
      phone: row.phone ?? undefined,
      email: row.email ?? undefined,
      role: row.role,
      shift: row.shift ?? undefined,
      salary: row.salary ?? undefined,
      hireDate: row.hireDate ?? undefined,
      address: row.address ?? undefined,
      status: row.status,
    });
    setFormError(null);
  }

  async function submit() {
    if (!draft.staffCode.trim()) {
      setFormError(t("errCode"));
      return;
    }
    if (!draft.staffName.trim()) {
      setFormError(t("errName"));
      return;
    }
    try {
      await save.mutateAsync({ id: editingId ?? null, body: draft });
      setEditingId(undefined);
    } catch (e) {
      setFormError(apiError(e, "saveStaff"));
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await remove.mutateAsync(deleting.id);
    } catch (e) {
      setListError(apiError(e, "deleteStaff"));
    } finally {
      setDeleting(null);
    }
  }

  function set<K extends keyof StaffRequest>(key: K, value: StaffRequest[K]) {
    setDraft((d) => ({ ...d, [key]: value }));
  }

  function openAccount(row: Staff) {
    // Everything the staff row already knows is carried across, so the admin
    // types a username and a password and nothing else. The role defaults to
    // the one they were hired into rather than to the least privileged, which
    // would only be overridden by hand every time.
    setAccountFor(row);
    setAccount({
      username: "",
      password: "",
      fullName: row.staffName,
      email: row.email ?? undefined,
      phone: row.phone ?? undefined,
      role: row.role,
    });
    setAccountError(null);
    setAccountMade(null);
  }

  function setAcc<K extends keyof AccountRequest>(key: K, value: AccountRequest[K]) {
    setAccount((a) => ({ ...a, [key]: value }));
  }

  async function submitAccount() {
    if (account.username.trim().length < 3) {
      setAccountError(t("errUsername"));
      return;
    }
    // Mirrors the server's rule so the admin is told before a round trip; the
    // server enforces it regardless.
    if (!/^(?=.*[A-Z])(?=.*\d).{8,}$/.test(account.password)) {
      setAccountError(t("errPassword"));
      return;
    }
    try {
      await createAccount.mutateAsync({
        ...account,
        username: account.username.trim(),
        email: account.email?.trim() || undefined,
        phone: account.phone?.trim() || undefined,
      });
      setAccountMade(account.username.trim());
      setAccountFor(null);
    } catch (e) {
      setAccountError(apiError(e, "createAccount"));
    }
  }

  const columns: Column<Staff>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * SIZE + i + 1 },
    { key: "code", header: tc("code"), render: (r) => r.staffCode },
    {
      key: "name",
      header: tc("name"),
      render: (r) => (
        <>
          <div className="font-medium">{r.staffName}</div>
          {r.email && <div className="text-xs text-ink-500">{r.email}</div>}
        </>
      ),
    },
    {
      key: "gender",
      header: t("gender"),
      hideOnMobile: true,
      render: (r) => (r.gender ? tGender(r.gender) : "—"),
    },
    { key: "role", header: t("role"), render: (r) => tRole(r.role) },
    { key: "phone", header: tc("phone"), hideOnMobile: true, render: (r) => r.phone ?? "—" },
    {
      key: "shift",
      header: t("shift"),
      hideOnMobile: true,
      render: (r) => (r.shift ? tShift(r.shift) : "—"),
    },
    {
      key: "salary",
      header: t("salary"),
      numeric: true,
      hideOnMobile: true,
      render: (r) => (r.salary != null ? formatUsd(r.salary) : "—"),
    },
    {
      key: "status",
      header: tc("status"),
      render: (r) => <Badge tone={STATUS_TONE[r.status]}>{tSt(r.status)}</Badge>,
    },
    {
      key: "actions",
      header: tc("actions"),
      align: "right",
      render: (r) => (
        <div className="flex justify-end gap-1.5">
          <Button size="sm" variant="ghost" onClick={() => openEdit(r)} aria-label={tA11y("edit", { name: r.staffName })}>
            ✏️
          </Button>
          <Button
            size="sm"
            variant="ghost"
            onClick={() => openAccount(r)}
            aria-label={t("createLoginFor", { name: r.staffName })}
            title={t("createLogin")}
          >
            🔑
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={() => setDeleting(r)}
            aria-label={tA11y("delete", { name: r.staffName })}
          >
            🗑️
          </Button>
        </div>
      ),
    },
  ];

  return (
    <ListPage>
      {listError && <Alert tone="error">{listError}</Alert>}
      {accountMade && <Alert tone="success">{t("accountCreated", { username: accountMade })}</Alert>}
      {list.isError && <Alert tone="error">{apiError(list.error)}</Alert>}

      <Toolbar
        left={
          <>
            <Button variant="admin" onClick={openNew}>
              ➕ {t("addStaff")}
            </Button>
            <Select
              className="w-auto"
              value={role}
              onChange={(e) => {
                setRole(e.target.value as Role | "");
                setPage(0);
              }}
            >
              <option value="">{t("allRoles")}</option>
              {(["ADMIN","CASHIER","WAITER","CHEF"] as Role[]).map((r) => (
                <option key={r} value={r}>
                  {tRole(r)}
                </option>
              ))}
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
            placeholder={t("searchStaff")}
          />
        }
      />

      <DataTable
        fill
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage={t("noStaff")}
      />

      {/* Always rendered, never conditional: on this layout the table
          claims whatever height the pager does not use, so a pager that
          appears when the data arrives would resize the table under the
          reader. */}
      <Pagination
        page={list.data?.page ?? 0}
        totalPages={list.data?.totalPages ?? 0}
        totalElements={list.data?.totalElements ?? 0}
        size={list.data?.size ?? SIZE}
        onPage={setPage}
      />

      <Modal
        open={editingId !== undefined}
        onClose={() => setEditingId(undefined)}
        title={t("details")}
        footer={
          <>
            <Button variant="light" onClick={() => setEditingId(undefined)}>
              {tc("close")}
            </Button>
            <Button variant="admin" onClick={submit} loading={save.isPending}>
              {tc("save")}
            </Button>
          </>
        }
      >
        {formError && <Alert tone="error">{formError}</Alert>}

        <FieldRow>
          <Field label={t("staffCode")} htmlFor="s-code" required>
            <Input
              id="s-code"
              value={draft.staffCode}
              onChange={(e) => set("staffCode", e.target.value)}
              placeholder="EMP-040"
            />
          </Field>
          <Field label={t("staffName")} htmlFor="s-name" required>
            <Input
              id="s-name"
              value={draft.staffName}
              onChange={(e) => set("staffName", e.target.value)}
              placeholder="Sok Dara"
            />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label={t("gender")} htmlFor="s-gender">
            <Select
              id="s-gender"
              value={draft.gender ?? ""}
              onChange={(e) => set("gender", (e.target.value || undefined) as Gender | undefined)}
            >
              <option value="">—</option>
              <option value="MALE">{tGender("MALE")}</option>
              <option value="FEMALE">{tGender("FEMALE")}</option>
            </Select>
          </Field>
          <Field label={t("dob")} htmlFor="s-dob">
            <Input
              id="s-dob"
              type="date"
              value={draft.dateOfBirth ?? ""}
              onChange={(e) => set("dateOfBirth", e.target.value || undefined)}
            />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label={tc("phone")} htmlFor="s-phone">
            <Input
              id="s-phone"
              value={draft.phone ?? ""}
              onChange={(e) => set("phone", e.target.value || undefined)}
              placeholder="012 345 678"
            />
          </Field>
          <Field label={tc("email")} htmlFor="s-email">
            <Input
              id="s-email"
              type="email"
              value={draft.email ?? ""}
              onChange={(e) => set("email", e.target.value || undefined)}
            />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label={t("role")} htmlFor="s-role" required>
            <Select
              id="s-role"
              value={draft.role}
              onChange={(e) => set("role", e.target.value as Role)}
            >
              {(["ADMIN","CASHIER","WAITER","CHEF"] as Role[]).map((r) => (
                <option key={r} value={r}>
                  {tRole(r)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={t("shift")} htmlFor="s-shift">
            <Select
              id="s-shift"
              value={draft.shift ?? ""}
              onChange={(e) => set("shift", (e.target.value || undefined) as Shift | undefined)}
            >
              <option value="">—</option>
              <option value="MORNING">{tShift("MORNING")}</option>
              <option value="EVENING">{tShift("EVENING")}</option>
              <option value="FULL_TIME">{tShift("FULL_TIME")}</option>
            </Select>
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label={t("salary")} htmlFor="s-salary">
            <Input
              id="s-salary"
              type="number"
              min="0"
              step="10"
              value={draft.salary ?? ""}
              onChange={(e) =>
                set("salary", e.target.value === "" ? undefined : Number(e.target.value))
              }
            />
          </Field>
          <Field label={t("hireDate")} htmlFor="s-hire">
            <Input
              id="s-hire"
              type="date"
              value={draft.hireDate ?? ""}
              onChange={(e) => set("hireDate", e.target.value || undefined)}
            />
          </Field>
        </FieldRow>

        <Field label={tc("status")} htmlFor="s-status">
          <Select
            id="s-status"
            value={draft.status}
            onChange={(e) => set("status", e.target.value as StaffStatus)}
          >
            <option value="ACTIVE">{tSt("ACTIVE")}</option>
            <option value="ON_LEAVE">{tSt("ON_LEAVE")}</option>
            <option value="RESIGNED">{tSt("RESIGNED")}</option>
          </Select>
        </Field>

        <Field label={tc("address")} htmlFor="s-addr">
          <Textarea
            id="s-addr"
            value={draft.address ?? ""}
            onChange={(e) => set("address", e.target.value || undefined)}
          />
        </Field>
      </Modal>

      <Modal
        open={accountFor !== null}
        onClose={() => setAccountFor(null)}
        title={t("createLoginFor", { name: accountFor?.staffName ?? "" })}
        footer={
          <>
            <Button variant="light" onClick={() => setAccountFor(null)}>
              {tc("close")}
            </Button>
            <Button variant="admin" onClick={submitAccount} loading={createAccount.isPending}>
              {t("createLogin")}
            </Button>
          </>
        }
      >
        {accountError && <Alert tone="error">{accountError}</Alert>}
        <Alert tone="info">{t("accountHelp")}</Alert>

        <FieldRow>
          <Field label={t("username")} htmlFor="a-user" required>
            <Input
              id="a-user"
              value={account.username}
              onChange={(e) => setAcc("username", e.target.value)}
              autoComplete="off"
              placeholder="sokdara"
            />
          </Field>
          <Field label={t("password")} htmlFor="a-pass" required>
            <Input
              id="a-pass"
              type="password"
              value={account.password}
              onChange={(e) => setAcc("password", e.target.value)}
              autoComplete="new-password"
            />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label={tc("name")} htmlFor="a-name" required>
            <Input
              id="a-name"
              value={account.fullName}
              onChange={(e) => setAcc("fullName", e.target.value)}
            />
          </Field>
          <Field label={t("role")} htmlFor="a-role" required>
            <Select
              id="a-role"
              value={account.role}
              onChange={(e) => setAcc("role", e.target.value as Role)}
            >
              {(["ADMIN","CASHIER","WAITER","CHEF"] as Role[]).map((r) => (
                <option key={r} value={r}>
                  {tRole(r)}
                </option>
              ))}
            </Select>
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label={tc("email")} htmlFor="a-email">
            <Input
              id="a-email"
              type="email"
              value={account.email ?? ""}
              onChange={(e) => setAcc("email", e.target.value || undefined)}
            />
          </Field>
          <Field label={tc("phone")} htmlFor="a-phone">
            <Input
              id="a-phone"
              value={account.phone ?? ""}
              onChange={(e) => setAcc("phone", e.target.value || undefined)}
            />
          </Field>
        </FieldRow>
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        busy={remove.isPending}
        onClose={() => setDeleting(null)}
        onConfirm={confirmDelete}
        message={tc("confirmDeleteMessage", { name: deleting?.staffName ?? "" })}
      />
    </ListPage>
  );
}
