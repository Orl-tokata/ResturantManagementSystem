import type { ReactNode } from "react";

/**
 * Stand-in for a screen that has its route and navigation wired but not yet its
 * content. Says plainly which milestone builds it, so nothing looks finished
 * when it is not.
 */
export function Placeholder({
  title,
  prototype,
  milestone,
  children,
}: {
  title: string;
  prototype: string;
  milestone: string;
  children?: ReactNode;
}) {
  return (
    <div className="mx-auto max-w-2xl">
      <div className="rounded-md border border-ink-200 bg-white shadow-sm">
        <div className="border-b border-ink-200 px-5 py-3 font-semibold">{title}</div>
        <div className="px-5 py-6 text-sm">
          <p className="mb-4 text-ink-500">
            Route and navigation are wired. The screen itself lands in{" "}
            <b className="text-ink-900">{milestone}</b>.
          </p>

          <dl className="space-y-1.5 text-xs">
            <div className="flex">
              <dt className="w-40 shrink-0 text-ink-500">Prototype reference</dt>
              <dd>
                <code className="rounded bg-ink-100 px-1.5 py-0.5">{prototype}</code>
              </dd>
            </div>
            <div className="flex">
              <dt className="w-40 shrink-0 text-ink-500">Spec</dt>
              <dd>
                <code className="rounded bg-ink-100 px-1.5 py-0.5">PROJECT-SPEC.md §3</code>
              </dd>
            </div>
          </dl>

          {children}
        </div>
      </div>
    </div>
  );
}
