import type { ReactNode } from "react";

/**
 * Auth screens have no app chrome: a centred card on the teal ground, matching
 * the prototype's `.auth` wrapper.
 */
export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-teal-800 p-6">
      {children}
    </div>
  );
}
