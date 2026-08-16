import type { ReactNode } from "react";

export function EmptyState({
  icon = "📭",
  title,
  description,
  action,
}: {
  icon?: ReactNode;
  title: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className="grid place-items-center px-6 py-12 text-center">
      <div className="mb-2 text-4xl">{icon}</div>
      <h3 className="mb-1 text-base font-semibold">{title}</h3>
      {description && (
        <p className="max-w-sm text-sm text-ink-500">{description}</p>
      )}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
