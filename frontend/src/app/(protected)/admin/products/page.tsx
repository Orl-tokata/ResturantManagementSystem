"use client";

import { useState } from "react";
import {
  Alert,
  Badge,
  Button,
  ConfirmDialog,
  DataTable,
  Field,
  FieldRow,
  Input,
  Modal,
  Pagination,
  SearchBar,
  Select,
  Textarea,
  Toolbar,
  type Column,
} from "@/components/ui";
import { useAll, useList, useRemove, useSave } from "@/hooks/useCrud";
import { errorMessage } from "@/lib/errors";
import { formatKhr, formatUsd } from "@/lib/format";
import {
  RECORD_STATUS_LABEL,
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
  imageUrl: "",
  description: "",
  status: "ACTIVE",
};

const SIZE = 20;

export default function ProductsPage() {
  const [search, setSearch] = useState("");
  const [categoryId, setCategoryId] = useState<number | "">("");
  const [page, setPage] = useState(0);

  const [editingId, setEditingId] = useState<number | null | undefined>(undefined);
  const [draft, setDraft] = useState<ProductRequest>(EMPTY);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<Product | null>(null);
  const [listError, setListError] = useState<string | null>(null);

  const categories = useAll<Category>("categories");
  const list = useList<Product>("products", {
    search,
    categoryId: categoryId === "" ? undefined : categoryId,
    page,
    size: SIZE,
  });
  const save = useSave<Product, ProductRequest>("products");
  const remove = useRemove("products");

  function openNew() {
    setEditingId(null);
    setDraft({ ...EMPTY, categoryId: categories.data?.[0]?.id ?? 0 });
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
      imageUrl: row.imageUrl ?? "",
      description: row.description ?? "",
      status: row.status,
    });
    setFormError(null);
  }

  async function submit() {
    if (!draft.name.trim()) {
      setFormError("ឈ្មោះផលិតផលត្រូវការ · Product name is required");
      return;
    }
    if (!draft.categoryId) {
      setFormError("សូមជ្រើសរើសប្រភេទ · Please choose a category");
      return;
    }
    if (draft.price < 0) {
      setFormError("តម្លៃមិនអាចអវិជ្ជមាន · Price cannot be negative");
      return;
    }
    try {
      await save.mutateAsync({ id: editingId ?? null, body: draft });
      setEditingId(undefined);
    } catch (e) {
      setFormError(errorMessage(e, "Could not save the product"));
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await remove.mutateAsync(deleting.id);
    } catch (e) {
      setListError(errorMessage(e, "Could not delete the product"));
    } finally {
      setDeleting(null);
    }
  }

  const columns: Column<Product>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * SIZE + i + 1 },
    {
      key: "img",
      header: "រូបភាព",
      width: "64px",
      align: "center",
      render: (r) => <span className="text-xl">{r.imageUrl || "🍽️"}</span>,
    },
    {
      key: "name",
      header: "ឈ្មោះ · Name",
      render: (r) => (
        <>
          <div className="font-medium">{r.name}</div>
          <div className="text-xs text-ink-500">{r.nameEn}</div>
        </>
      ),
    },
    { key: "cat", header: "ប្រភេទ", hideOnMobile: true, render: (r) => r.categoryName },
    { key: "price", header: "តម្លៃ · Price", numeric: true, render: (r) => formatUsd(r.price) },
    { key: "khr", header: "រៀល", numeric: true, hideOnMobile: true, render: (r) => formatKhr(r.price) },
    { key: "cost", header: "ថ្លៃដើម", numeric: true, hideOnMobile: true, render: (r) => formatUsd(r.cost) },
    { key: "stock", header: "ស្តុក", numeric: true, render: (r) => r.stockQty },
    {
      key: "status",
      header: "ស្ថានភាព",
      render: (r) =>
        r.status === "INACTIVE" ? (
          <Badge tone="neutral">{RECORD_STATUS_LABEL.INACTIVE}</Badge>
        ) : r.stockQty <= 0 ? (
          <Badge tone="dead">អស់ស្តុក</Badge>
        ) : r.stockQty < 10 ? (
          <Badge tone="warn">ជិតអស់</Badge>
        ) : (
          <Badge tone="ok">លក់</Badge>
        ),
    },
    {
      key: "actions",
      header: "សកម្មភាព",
      align: "right",
      render: (r) => (
        <div className="flex justify-end gap-1.5">
          <Button size="sm" variant="ghost" onClick={() => openEdit(r)} aria-label={`Edit ${r.name}`}>
            ✏️
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={() => setDeleting(r)}
            aria-label={`Delete ${r.name}`}
          >
            🗑️
          </Button>
        </div>
      ),
    },
  ];

  return (
    <>
      {listError && <Alert tone="error">{listError}</Alert>}
      {list.isError && <Alert tone="error">{errorMessage(list.error)}</Alert>}

      <Toolbar
        left={
          <>
            <Button variant="admin" onClick={openNew}>
              ➕ បន្ថែមថ្មី · Add product
            </Button>
            <Select
              className="w-auto"
              value={categoryId}
              onChange={(e) => {
                setCategoryId(e.target.value === "" ? "" : Number(e.target.value));
                setPage(0);
              }}
            >
              <option value="">ប្រភេទទាំងអស់ · All categories</option>
              {categories.data?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.icon} {c.name} · {c.nameEn}
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
            placeholder="ស្វែងរកផលិតផល · Search"
          />
        }
      />

      <DataTable
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage="គ្មានផលិតផល · No products found"
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

      <Modal
        open={editingId !== undefined}
        onClose={() => setEditingId(undefined)}
        title="ព័ត៌មានផលិតផល · Product Details"
        footer={
          <>
            <Button variant="light" onClick={() => setEditingId(undefined)}>
              បោះបង់ · Cancel
            </Button>
            <Button variant="admin" onClick={submit} loading={save.isPending}>
              រក្សាទុក · Save
            </Button>
          </>
        }
      >
        {formError && <Alert tone="error">{formError}</Alert>}

        <FieldRow>
          <Field label="ឈ្មោះ · Name (Khmer)" htmlFor="p-name" required>
            <Input
              id="p-name"
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              placeholder="បាយឆាគ្រឿងសមុទ្រ"
            />
          </Field>
          <Field label="Name (English)" htmlFor="p-name-en">
            <Input
              id="p-name-en"
              value={draft.nameEn}
              onChange={(e) => setDraft({ ...draft, nameEn: e.target.value })}
              placeholder="Seafood fried rice"
            />
          </Field>
        </FieldRow>

        <Field label="ប្រភេទ · Category" htmlFor="p-cat" required>
          <Select
            id="p-cat"
            value={draft.categoryId || ""}
            onChange={(e) => setDraft({ ...draft, categoryId: Number(e.target.value) })}
          >
            <option value="" disabled>
              — ជ្រើសរើស · Choose —
            </option>
            {categories.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.icon} {c.name} · {c.nameEn}
              </option>
            ))}
          </Select>
        </Field>

        <FieldRow>
          <Field label="តម្លៃលក់ · Price ($)" htmlFor="p-price" required hint={formatKhr(draft.price)}>
            <Input
              id="p-price"
              type="number"
              step="0.25"
              min="0"
              value={draft.price}
              onChange={(e) => setDraft({ ...draft, price: Number(e.target.value) })}
            />
          </Field>
          <Field label="ថ្លៃដើម · Cost ($)" htmlFor="p-cost">
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
          <Field label="ស្តុក · Stock" htmlFor="p-stock">
            <Input
              id="p-stock"
              type="number"
              min="0"
              value={draft.stockQty}
              onChange={(e) => setDraft({ ...draft, stockQty: Number(e.target.value) })}
            />
          </Field>
          <Field label="ស្ថានភាព · Status" htmlFor="p-status">
            <Select
              id="p-status"
              value={draft.status}
              onChange={(e) => setDraft({ ...draft, status: e.target.value as RecordStatus })}
            >
              <option value="ACTIVE">{RECORD_STATUS_LABEL.ACTIVE}</option>
              <option value="INACTIVE">{RECORD_STATUS_LABEL.INACTIVE}</option>
            </Select>
          </Field>
        </FieldRow>

        <Field
          label="រូបភាព · Image"
          htmlFor="p-img"
          hint="Emoji for now — file upload is a later milestone"
        >
          <Input
            id="p-img"
            value={draft.imageUrl}
            onChange={(e) => setDraft({ ...draft, imageUrl: e.target.value })}
            placeholder="🍚"
          />
        </Field>

        <Field label="ការពិពណ៌នា · Description" htmlFor="p-desc">
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
        message={`តើអ្នកប្រាកដជាចង់លុប "${deleting?.name ?? ""}" មែនទេ?`}
        detail="Past receipts keep their own copy of the name and price, so they stay correct."
      />
    </>
  );
}
