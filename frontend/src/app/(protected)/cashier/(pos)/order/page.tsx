"use client";

import Link from "next/link";
import { X } from "lucide-react";
import { Clock } from "@/components/layout/Clock";
import { useAuth } from "@/lib/auth-context";

/**
 * The POS deliberately sits in the (pos) route group rather than (shell): it
 * takes the whole viewport, with navy chrome instead of the teal sidebar.
 * Milestone 8 fills in the category rail, product grid and order panel.
 */
export default function CashierOrderPage() {
  const { user } = useAuth();

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-ink-200">
      {/* top action strip */}
      <div className="flex shrink-0 items-center gap-2.5 bg-navy-800 px-3 py-2 text-white">
        <Link
          href="/cashier"
          aria-label="Close POS"
          className="grid h-8 w-8 place-items-center rounded text-orange-500 hover:bg-white/15"
        >
          <X size={18} />
        </Link>
        <div className="flex-1 text-sm font-semibold">អេក្រង់បញ្ជាទិញ · Order Screen</div>
        <div className="text-right text-xs leading-tight">
          <b className="block text-sm">តុ · Table —</b>
          <span className="text-white/70">មិនទាន់ជ្រើសរើស · none selected</span>
        </div>
      </div>

      {/* body */}
      <div className="grid min-h-0 flex-1 grid-cols-1 md:grid-cols-[92px_1fr_330px]">
        <div className="hidden bg-teal-800 p-1.5 md:block">
          <div className="rounded bg-white/10 p-2 text-center text-xs text-white/70">
            ប្រភេទ
            <br />
            Categories
          </div>
        </div>

        <div className="grid place-items-center overflow-auto bg-ink-100 p-6">
          <div className="max-w-md rounded-md border border-ink-200 bg-white p-6 text-center shadow-sm">
            <div className="mb-2 text-4xl">🛒</div>
            <h1 className="mb-1 text-lg font-bold">អេក្រង់បញ្ជាទិញ · Order Screen</h1>
            <p className="text-sm text-ink-500">
              Full-screen POS chrome is in place and this route is guarded. The category
              rail, product grid and order panel arrive in{" "}
              <b className="text-ink-900">milestone 8 (POS)</b>.
            </p>
            <p className="mt-3 text-xs text-ink-500">
              Prototype reference{" "}
              <code className="rounded bg-ink-100 px-1.5 py-0.5">cashier-order.html</code>
            </p>
            <Link
              href="/cashier"
              className="mt-4 inline-block rounded bg-teal-600 px-4 py-2 text-sm font-semibold text-white"
            >
              ត្រឡប់ក្រោយ · Back to cashier home
            </Link>
          </div>
        </div>

        <div className="hidden flex-col border-l border-ink-300 bg-cream-100 md:flex">
          <div className="bg-navy-800 px-3 py-2 text-sm font-semibold text-white">
            បញ្ជីបញ្ជាទិញ · Order
          </div>
          <div className="grid flex-1 place-items-center p-4 text-center text-xs text-ink-500">
            មិនទាន់មានទំនិញ
            <br />
            No items yet
          </div>
        </div>
      </div>

      {/* bottom bar */}
      <div className="flex shrink-0 items-center justify-between bg-navy-800 px-3.5 py-1.5 text-xs text-white/85">
        <span>អ្នកគិតលុយ · Cashier: {user?.fullName ?? "—"}</span>
        <Clock mode="full" />
      </div>
    </div>
  );
}
