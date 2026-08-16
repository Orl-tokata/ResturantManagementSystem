"use client";

import { useQuery } from "@tanstack/react-query";
import { get } from "@/lib/api";
import type { ReactNode } from "react";

type Health = {
  service: string;
  status: string;
  time: string;
  database: string;
  databaseError?: string;
};

/**
 * Milestone 1 landing page: proves the Next app and the Spring Boot API are
 * wired together. Replaced by the real dashboard in milestone 5.
 */
export default function Home() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["health"],
    queryFn: () => get<Health>("/health"),
    retry: false,
  });

  const apiBase =
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8081/api";

  return (
    <main className="mx-auto w-full max-w-2xl px-6 py-16">
      <header className="mb-10 flex items-center gap-4">
        <div className="grid h-14 w-14 place-items-center rounded-md bg-teal-800 text-3xl">
          🍽️
        </div>
        <div>
          <h1 className="text-2xl font-bold">Restaurant Management System</h1>
          <p className="text-sm text-ink-500">
            Spring Boot · Next.js — milestone 1 scaffold
          </p>
        </div>
      </header>

      <section className="rounded-md border border-ink-200 bg-white shadow-sm">
        <div className="border-b border-ink-200 px-5 py-3 font-semibold">
          API connection
        </div>

        <div className="space-y-3 px-5 py-5 text-sm">
          <Row label="Frontend">
            <Badge tone="ok">UP</Badge>
            <span className="ml-2 text-ink-500">Next.js on :3000</span>
          </Row>

          {isLoading && (
            <Row label="Backend">
              <Badge tone="warn">checking…</Badge>
            </Row>
          )}

          {isError && (
            <>
              <Row label="Backend">
                <Badge tone="down">DOWN</Badge>
              </Row>
              <p className="rounded-sm bg-ink-100 p-3 text-xs leading-relaxed text-ink-700">
                Could not reach <code>{apiBase}/health</code>.
                <br />
                Start it with{" "}
                <code>
                  ./gradlew bootRun --args=&apos;--spring.profiles.active=dev&apos;
                </code>
                <br />
                <span className="text-ink-500">{(error as Error)?.message}</span>
              </p>
            </>
          )}

          {data && (
            <>
              <Row label="Backend">
                <Badge tone={data.status === "UP" ? "ok" : "down"}>
                  {data.status}
                </Badge>
                <span className="ml-2 text-ink-500">{data.service}</span>
              </Row>
              <Row label="Database">
                <Badge tone={data.database === "UP" ? "ok" : "down"}>
                  {data.database}
                </Badge>
                {data.databaseError && (
                  <span className="ml-2 text-xs text-danger">
                    {data.databaseError}
                  </span>
                )}
              </Row>
              <Row label="Server time">
                <span className="text-ink-700">{data.time}</span>
              </Row>
            </>
          )}
        </div>
      </section>

      <section className="mt-8 rounded-md border-l-4 border-warning bg-[#fdf8ec] px-5 py-4 text-sm leading-relaxed">
        <b>Next up — milestone 2 (Schema).</b> Flyway <code>V2__init.sql</code>,
        the 14 entities and their repositories. See <code>PROJECT-SPEC.md</code>{" "}
        §4 and §10.
      </section>
    </main>
  );
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-center">
      <span className="w-32 shrink-0 text-ink-500">{label}</span>
      <span className="flex items-center">{children}</span>
    </div>
  );
}

function Badge({
  tone,
  children,
}: {
  tone: "ok" | "warn" | "down";
  children: ReactNode;
}) {
  const tones = {
    ok: "bg-[#dcf3e6] text-[#1c6b3d]",
    warn: "bg-[#fdf0d2] text-[#8a6100]",
    down: "bg-[#fbdcdc] text-[#8f1414]",
  } as const;

  return (
    <span
      className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${tones[tone]}`}
    >
      {children}
    </span>
  );
}
