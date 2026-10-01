"use client";

import { useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import {
  Alert,
  Badge,
  Button,
  type Column,
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
  useToast,
} from "@/components/ui";
import { useAll, useList, useRemove, useSave } from "@/hooks/useCrud";
import { useApiError } from "@/lib/use-api-error";
import { pickName } from "@/i18n/name";
import { formatKhr, formatUsd } from "@/lib/format";
import { IconPicker } from "@/components/ui/IconPicker";
import { ProductImage } from "@/components/ui/ProductImage";
import { ImageUpload } from "@/components/ui/ImageUpload";
import { del, upload } from "@/lib/api";
import {
  type Category,
  type Product,
  type ProductRequest,
  type RecordStatus,
} from "@/types/master";

const EMPTY: ProductRequest = {
  name: "",
  nameEn: "",
  categoryId: 0,
  price: 0,
  cost: 0,
  stockQty: 0,
  icon: "",
  description: "",
  status: "ACTIVE",
};

/** Where this screen starts; the reader can change it. */
const INITIAL_SIZE = 20;

export default function ProductsPage() {
  const t = useTranslations("products");
  const tc = useTranslations("common");
  const tStatus = useTranslations("enum.recordStatus");
  const tA11y = useTranslations("a11y");
  const apiError = useApiError();
  const toast = useToast();
  const locale = useLocale();

  const [search, setSearch] = useState("");
  const [categoryId, setCategoryId] = useState<number | "">("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(INITIAL_SIZE);

  const [editingId, setEditingId] = useState<number | null | undefined>(undefined);
  /* The stored photograph of the product open in the form. Separate from the
     draft because it is saved by its own endpoint, not by Save. */
  const [imageFile, setImageFile] = useState<string | null>(null);
  const [imageBusy, setImageBusy] = useState(false);
  const [draft, setDraft] = useState<ProductRequest>(EMPTY);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<Product | null>(null);

  const categories = useAll<Category>("categories");
  const list = useList<Product>("products", {
    search,
    categoryId: categoryId === "" ? undefined : categoryId,
    page,
    size: size,
  });
  const save = useSave<Product, ProductRequest>("products");
  const remove = useRemove("products");

  function openNew() {
    setEditingId(null);
    setDraft({ ...EMPTY, categoryId: categories.data?.[0]?.id ?? 0 });
    setImageFile(null);
    setFormError(null);
  }

  function openEdit(row: Product) {
    setEditingId(row.id);
    setDraft({
      name: row.name,
      nameEn: row.nameEn ?? "",
      categoryId: row.categoryId,
      price: row.price,
      cost: row.cost,
      stockQty: row.stockQty,
      icon: row.icon ?? "",
      description: row.description ?? "",
      status: row.status,
    });
    setImageFile(row.imageFile);
    setFormError(null);
  }

  async function submit() {
    if (!draft.name.trim()) {
      setFormError(t("errName"));
      return;
    }
    if (!draft.categoryId) {
      setFormError(t("errCategory"));
      return;
    }
    if (draft.price < 0) {
      setFormError(t("errPrice"));
      return;
    }
    try {
      await save.mutateAsync({ id: editingId ?? null, body: draft });
      setEditingId(undefined);
    } catch (e) {
      setFormError(apiError(e, "saveProduct"));
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await remove.mutateAsync(deleting.id);
    } catch (e) {
      toast.error(apiError(e, "deleteProduct"));
    } finally {
      setDeleting(null);
    }
  }

  const columns: Column<Product>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * size + i + 1 },
    {
      key: "img",
      header: t("image"),
      width: "64px",
      align: "center",
      render: (r) => (
        <span className="mx-auto grid h-9 w-9 place-items-center overflow-hidden rounded bg-ink-100">
          <ProductImage file={r.imageFile} icon={r.icon} alt={r.name} iconClassName="text-xl" />
        </span>
      ),
    },
    {
      key: "name",
      header: tc("name"),
      render: (r) => (
        <>
          <div className="font-medium">{r.name}</div>
          <div className="text-xs text-ink-500">{r.nameEn}</div>
        </>
      ),
    },
    { key: "cat", header: t("category"), hideOnMobile: true, render: (r) => r.categoryName },
    { key: "price", header: tc("price"), numeric: true, render: (r) => formatUsd(r.price) },
    { key: "khr", header: tc("khr"), numeric: true, hideOnMobile: true, render: (r) => formatKhr(r.price) },
    { key: "cost", header: tc("cost"), numeric: true, hideOnMobile: true, render: (r) => formatUsd(r.cost) },
    { key: "stock", header: t("stock"), numeric: true, render: (r) => r.stockQty },
    {
      key: "status",
      header: tc("status"),
      render: (r) =>
        r.status === "INACTIVE" ? (
          <Badge tone="neutral">{tStatus("INACTIVE")}</Badge>
        ) : r.stockQty <= 0 ? (
          <Badge tone="dead">{t("outOfStock")}</Badge>
        ) : r.stockQty < 10 ? (
          <Badge tone="warn">{t("lowStock")}</Badge>
        ) : (
          <Badge tone="ok">{t("onSale")}</Badge>
        ),
    },
    {
      key: "actions",
      header: tc("actions"),
      align: "right",
      render: (r) => (
        <div className="flex justify-end gap-1.5">
          <Button size="sm" variant="ghost" onClick={() => openEdit(r)} aria-label={tA11y("edit", { name: r.name })}>
            ✏️
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={() => setDeleting(r)}
            aria-label={tA11y("delete", { name: r.name })}
          >
            🗑️
          </Button>
        </div>
      ),
    },
  ];

  return (
    <ListPage>
      {list.isError && <Alert tone="error">{apiError(list.error)}</Alert>}

      <Toolbar
        left={
          <>
            <Button variant="admin" onClick={openNew}>
              ➕ {t("addProduct")}
            </Button>
            <Select
              className="w-auto"
              value={categoryId}
              onChange={(e) => {
                setCategoryId(e.target.value === "" ? "" : Number(e.target.value));
                setPage(0);
              }}
            >
              <option value="">{t("allCategories")}</option>
              {categories.data?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.icon} {pickName(locale, c.name, c.nameEn)}
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
            placeholder={t("searchProduct")}
          />
        }
      />

      <DataTable
        fill
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage={t("noProducts")}
      />

      {/* Always rendered, never conditional: on this layout the table
          claims whatever height the pager does not use, so a pager that
          appears when the data arrives would resize the table under the
          reader. */}
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

      <Modal
        open={editingId !== undefined}
        onClose={() => setEditingId(undefined)}
        title={t("details")}
        footer={
          <>
            <Button variant="light" onClick={() => setEditingId(undefined)}>
              {tc("cancel")}
            </Button>
            <Button variant="admin" onClick={submit} loading={save.isPending}>
              {tc("save")}
            </Button>
          </>
        }
      >
        {formError && <Alert tone="error">{formError}</Alert>}

        <FieldRow>
          <Field label={t("nameKm")} htmlFor="p-name" required>
            <Input
              id="p-name"
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              placeholder={t("namePlaceholder")}
            />
          </Field>
          <Field label={t("nameEn")} htmlFor="p-name-en">
            <Input
              id="p-name-en"
              value={draft.nameEn}
              onChange={(e) => setDraft({ ...draft, nameEn: e.target.value })}
              placeholder="Seafood fried rice"
            />
          </Field>
        </FieldRow>

        <Field label={t("category")} htmlFor="p-cat" required>
          <Select
            id="p-cat"
            value={draft.categoryId || ""}
            onChange={(e) => setDraft({ ...draft, categoryId: Number(e.target.value) })}
          >
            <option value="" disabled>
              — {t("choose")} —
            </option>
            {categories.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.icon} {pickName(locale, c.name, c.nameEn)}
              </option>
            ))}
          </Select>
        </Field>

        <FieldRow>
          <Field label={tc("price")} htmlFor="p-price" required hint={formatKhr(draft.price)}>
            <Input
              id="p-price"
              type="number"
              step="0.25"
              min="0"
              value={draft.price}
              onChange={(e) => setDraft({ ...draft, price: Number(e.target.value) })}
            />
          </Field>
          <Field label={tc("cost")} htmlFor="p-cost">
            <Input
              id="p-cost"
              type="number"
              step="0.25"
              min="0"
              value={draft.cost}
              onChange={(e) => setDraft({ ...draft, cost: Number(e.target.value) })}
            />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label={t("stock")} htmlFor="p-stock">
            <Input
              id="p-stock"
              type="number"
              min="0"
              value={draft.stockQty}
              onChange={(e) => setDraft({ ...draft, stockQty: Number(e.target.value) })}
            />
          </Field>
          <Field label={tc("status")} htmlFor="p-status">
            <Select
              id="p-status"
              value={draft.status}
              onChange={(e) => setDraft({ ...draft, status: e.target.value as RecordStatus })}
            >
              <option value="ACTIVE">{tStatus("ACTIVE")}</option>
              <option value="INACTIVE">{tStatus("INACTIVE")}</option>
            </Select>
          </Field>
        </FieldRow>

        <Field
          label={t("image")}
          htmlFor="p-img"
          hint={t("imageHint")}
        >
          <IconPicker
            id="p-img"
            value={draft.icon}
            onChange={(icon) => setDraft({ ...draft, icon })}
          />
        </Field>

        <Field label={t("photo")}>
          <ImageUpload
            file={imageFile}
            busy={imageBusy}
            // No id to upload against until the product exists, and the form
            // creates it on Save. Saying so beats a button that fails.
            disabledReason={editingId === null ? t("photoAfterSave") : undefined}
            onPick={async (picked) => {
              setImageBusy(true);
              setFormError(null);
              try {
                const saved = await upload<Product>(`/products/${editingId}/image`, picked);
                setImageFile(saved.imageFile);
                void list.refetch();
              } catch (e) {
                setFormError(apiError(e, "uploadImage"));
              } finally {
                setImageBusy(false);
              }
            }}
            onRemove={async () => {
              setImageBusy(true);
              try {
                const saved = await del<Product>(`/products/${editingId}/image`);
                setImageFile(saved.imageFile);
                void list.refetch();
              } catch (e) {
                setFormError(apiError(e, "uploadImage"));
              } finally {
                setImageBusy(false);
              }
            }}
          />
        </Field>

        <Field label={t("description")} htmlFor="p-desc">
          <Textarea
            id="p-desc"
            value={draft.description}
            onChange={(e) => setDraft({ ...draft, description: e.target.value })}
          />
        </Field>
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        busy={remove.isPending}
        onClose={() => setDeleting(null)}
        onConfirm={confirmDelete}
        message={tc("confirmDeleteMessage", { name: deleting?.name ?? "" })}
        detail={t("deleteNote")}
      />
    </ListPage>
  );
}
