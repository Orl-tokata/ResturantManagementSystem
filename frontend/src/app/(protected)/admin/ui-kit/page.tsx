"use client";

import { useMemo, useState } from "react";
import {
  Badge,
  Button,
  Card,
  Checkbox,
  ConfirmDialog,
  DataTable,
  EmptyState,
  Field,
  FieldRow,
  Input,
  Meter,
  Modal,
  Pagination,
  PageTitle,
  SearchBar,
  Select,
  StatGrid,
  StatTile,
  Tabs,
  Textarea,
  Toolbar,
  type Column,
} from "@/components/ui";
import { formatKhr, formatUsd } from "@/lib/format";

/**
 * Component gallery — a development aid, not a product screen. It is
 * deliberately absent from the sidebar; reach it at /admin/ui-kit.
 *
 * Every component in PROJECT-SPEC.md §7.3 is rendered here, so the kit can be
 * reviewed in one place before the real screens consume it.
 */

interface Dish {
  id: number;
  name: string;
  category: string;
  price: number;
  stock: number;
  min: number;
  status: "ACTIVE" | "INACTIVE";
}

const DISHES: Dish[] = [
  { id: 1, name: "បាយឆាគ្រឿងសមុទ្រ", category: "បាយ · Rice", price: 4.5, stock: 42, min: 10, status: "ACTIVE" },
  { id: 2, name: "គុយទាវសាច់គោ", category: "មី · Noodle", price: 3.5, stock: 38, min: 10, status: "ACTIVE" },
  { id: 3, name: "មាន់អាំងឃ្មុំ", category: "អាំង · Grill", price: 7.5, stock: 14, min: 10, status: "ACTIVE" },
  { id: 4, name: "បង្គាឆាម្ទេស", category: "ឆា · Fried", price: 9.0, stock: 3, min: 8, status: "ACTIVE" },
  { id: 5, name: "បៀរអង្គរ", category: "ភេសជ្ជៈ · Drink", price: 1.5, stock: 4, min: 48, status: "INACTIVE" },
];

export default function UiKitPage() {
  const [query, setQuery] = useState("");
  const [tab, setTab] = useState("all");
  const [page, setPage] = useState(0);
  const [modalOpen, setModalOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirmBusy, setConfirmBusy] = useState(false);
  const [showEmpty, setShowEmpty] = useState(false);
  const [loading, setLoading] = useState(false);

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    return DISHES.filter((d) => {
      if (tab === "low" && d.stock >= d.min) return false;
      if (tab === "inactive" && d.status !== "INACTIVE") return false;
      if (!q) return true;
      return d.name.toLowerCase().includes(q) || d.category.toLowerCase().includes(q);
    });
  }, [query, tab]);

  const columns: Column<Dish>[] = [
    { key: "id", header: "#", width: "56px", render: (_r, i) => i + 1 },
    { key: "name", header: "ឈ្មោះ · Name", render: (r) => r.name },
    { key: "category", header: "ប្រភេទ", hideOnMobile: true, render: (r) => r.category },
    { key: "price", header: "តម្លៃ", numeric: true, render: (r) => formatUsd(r.price) },
    { key: "khr", header: "រៀល", numeric: true, hideOnMobile: true, render: (r) => formatKhr(r.price) },
    {
      key: "level",
      header: "កម្រិត · Level",
      width: "130px",
      render: (r) => <Meter value={r.stock} max={r.min} />,
    },
    {
      key: "status",
      header: "ស្ថានភាព",
      render: (r) =>
        r.stock <= 0 ? (
          <Badge tone="dead">អស់ស្តុក</Badge>
        ) : r.stock < r.min ? (
          <Badge tone="warn">ជិតអស់</Badge>
        ) : (
          <Badge tone="ok">គ្រប់គ្រាន់</Badge>
        ),
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: () => (
        <div className="flex justify-end gap-1.5">
          <Button size="sm" variant="ghost" onClick={() => setModalOpen(true)}>
            ✏️
          </Button>
          <Button size="sm" variant="danger" onClick={() => setConfirmOpen(true)}>
            🗑️
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <PageTitle>UI kit · មិនមែនអេក្រង់ផលិតកម្ម</PageTitle>
        <p className="-mt-2 mb-4 text-sm text-ink-500">
          Development gallery for the shared components. Not linked from the sidebar.
        </p>
      </div>

      {/* ---- stat tiles ---- */}
      <Card title="StatTile">
        <StatGrid>
          <StatTile tone={1} label="ការលក់ថ្ងៃនេះ · Today" value={formatUsd(486.5)} />
          <StatTile tone={2} label="វិក្កយបត្រ · Invoices" value="37" />
          <StatTile tone={3} label="តុកំពុងប្រើ · Occupied" value="6 / 12" />
          <StatTile tone={4} label="ស្តុកជិតអស់ · Low stock" value="7" />
        </StatGrid>
      </Card>

      {/* ---- buttons ---- */}
      <Card title="Button">
        <div className="flex flex-wrap items-center gap-2">
          <Button variant="primary">Primary</Button>
          <Button variant="admin">Admin</Button>
          <Button variant="accent">Accent</Button>
          <Button variant="danger">Danger</Button>
          <Button variant="ghost">Ghost</Button>
          <Button variant="light">Light</Button>
          <Button loading>Loading</Button>
          <Button disabled>Disabled</Button>
          <Button size="sm">Small</Button>
          <Button size="lg">Large</Button>
        </div>
      </Card>

      {/* ---- badges ---- */}
      <Card title="Badge">
        <div className="flex flex-wrap gap-2">
          <Badge tone="ok">បានបង់ · Paid</Badge>
          <Badge tone="warn">មិនទាន់បង់ · Unpaid</Badge>
          <Badge tone="dead">បានលុប · Cancelled</Badge>
          <Badge tone="info">ព័ត៌មាន · Info</Badge>
          <Badge tone="neutral">Neutral</Badge>
        </div>
      </Card>

      {/* ---- form ---- */}
      <Card title="Field · Input · Select · Textarea · Checkbox">
        <FieldRow>
          <Field label="ឈ្មោះផលិតផល · Name" htmlFor="k-name" required>
            <Input id="k-name" placeholder="បាយឆាគ្រឿងសមុទ្រ" />
          </Field>
          <Field label="តម្លៃ · Price" htmlFor="k-price" hint="USD, two decimals">
            <Input id="k-price" type="number" step="0.25" placeholder="4.50" />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label="ប្រភេទ · Category" htmlFor="k-cat">
            <Select id="k-cat">
              <option>បាយ · Rice</option>
              <option>មី · Noodle</option>
              <option>ភេសជ្ជៈ · Drink</option>
            </Select>
          </Field>
          <Field label="With an error" htmlFor="k-err" error="តម្លៃមិនត្រឹមត្រូវ · Invalid value">
            <Input id="k-err" defaultValue="-3" />
          </Field>
        </FieldRow>

        <Field label="ការពិពណ៌នា · Description" htmlFor="k-desc">
          <Textarea id="k-desc" placeholder="…" />
        </Field>

        <Checkbox label="សកម្ម · Active" defaultChecked />
      </Card>

      {/* ---- tabs + toolbar + table + pagination ---- */}
      <Card title="Tabs · Toolbar · SearchBar · DataTable · Meter · Pagination" padded>
        <Tabs
          active={tab}
          onChange={(id) => {
            setTab(id);
            setPage(0);
          }}
          items={[
            { id: "all", label: "ទាំងអស់ · All" },
            { id: "low", label: "ជិតអស់ · Low stock" },
            { id: "inactive", label: "មិនសកម្ម · Inactive" },
          ]}
        />

        <Toolbar
          left={
            <>
              <Button variant="admin" onClick={() => setModalOpen(true)}>
                ➕ បន្ថែមថ្មី
              </Button>
              <Button variant="light" onClick={() => setLoading((v) => !v)}>
                {loading ? "Stop loading" : "Show loading"}
              </Button>
              <Button variant="light" onClick={() => setShowEmpty((v) => !v)}>
                {showEmpty ? "Show rows" : "Show empty"}
              </Button>
            </>
          }
          right={<SearchBar value={query} onChange={setQuery} />}
        />

        <DataTable
          columns={columns}
          rows={showEmpty ? [] : rows}
          rowKey={(r) => r.id}
          loading={loading}
        />

        <Pagination
          page={page}
          totalPages={5}
          totalElements={48}
          size={10}
          onPage={setPage}
        />
      </Card>

      {/* ---- empty state ---- */}
      <Card title="EmptyState">
        <EmptyState
          icon="🍽️"
          title="គ្មានផលិតផល · No products yet"
          description="បន្ថែមផលិតផលដំបូងរបស់អ្នកដើម្បីចាប់ផ្តើម។ Add your first product to get started."
          action={<Button variant="admin">➕ បន្ថែមផលិតផល</Button>}
        />
      </Card>

      {/* ---- dialogs ---- */}
      <Card title="Modal · ConfirmDialog">
        <div className="flex flex-wrap gap-2">
          <Button variant="admin" onClick={() => setModalOpen(true)}>
            Open modal
          </Button>
          <Button variant="danger" onClick={() => setConfirmOpen(true)}>
            Open confirm
          </Button>
        </div>
        <p className="mt-3 text-xs text-ink-500">
          Escape closes, focus is trapped while open and restored to the trigger on close,
          and the page behind does not scroll.
        </p>
      </Card>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title="ព័ត៌មានផលិតផល · Product Details"
        footer={
          <>
            <Button variant="light" onClick={() => setModalOpen(false)}>
              បោះបង់ · Cancel
            </Button>
            <Button variant="admin" onClick={() => setModalOpen(false)}>
              រក្សាទុក · Save
            </Button>
          </>
        }
      >
        <Field label="ឈ្មោះផលិតផល · Name" htmlFor="m-name" required>
          <Input id="m-name" defaultValue="បាយឆាគ្រឿងសមុទ្រ" />
        </Field>
        <FieldRow>
          <Field label="តម្លៃ · Price" htmlFor="m-price">
            <Input id="m-price" type="number" defaultValue="4.50" />
          </Field>
          <Field label="ស្តុក · Stock" htmlFor="m-stock">
            <Input id="m-stock" type="number" defaultValue="42" />
          </Field>
        </FieldRow>
      </Modal>

      <ConfirmDialog
        open={confirmOpen}
        busy={confirmBusy}
        onClose={() => setConfirmOpen(false)}
        onConfirm={() => {
          setConfirmBusy(true);
          setTimeout(() => {
            setConfirmBusy(false);
            setConfirmOpen(false);
          }, 900);
        }}
        message="តើអ្នកប្រាកដជាចង់លុបផលិតផលនេះមែនទេ?"
      />
    </div>
  );
}
