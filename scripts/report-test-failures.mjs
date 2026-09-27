#!/usr/bin/env node
/**
 * Turns JUnit XML into GitHub annotations.
 *
 *   node scripts/report-test-failures.mjs backend/build/test-results/test
 *
 * Actions logs need a sign-in to read, even on a public repository, but
 * annotations show on the run page to anyone. Without this a failing suite
 * reports "Process completed with exit code 1" and nothing else, and finding
 * out which test failed means having push access and a browser.
 *
 * Exits 0 always: this reports, it does not judge. The test task's own exit
 * code is what fails the job.
 */
import { readdirSync, readFileSync, existsSync } from "node:fs";
import { join } from "node:path";

const dir = process.argv[2];

if (!dir || !existsSync(dir)) {
  console.log(`No test results at ${dir ?? "(no path given)"} — nothing to report.`);
  process.exit(0);
}

/** `<` in a message would otherwise truncate the annotation. */
const clean = (s) =>
  s.replace(/\s+/g, " ").replace(/%/g, "%25").replace(/\r/g, "%0D").replace(/\n/g, "%0A").trim();

let failures = 0;

for (const file of readdirSync(dir).filter((f) => f.endsWith(".xml"))) {
  const xml = readFileSync(join(dir, file), "utf8");

  // A passing test is written self-closing. Drop those first: otherwise the
  // pair-matching regex below starts at a passing testcase, runs on to the
  // NEXT closing tag, and reports someone else's failure under its name —
  // which is worse than no report at all, because it sends you to the wrong
  // test. This exact bug was in the first version of this script.
  const withBodies = xml.replace(/<testcase\b[^>]*\/>/g, "");

  for (const [, attrs, body] of withBodies.matchAll(
    /<testcase\b([^>]*)>([\s\S]*?)<\/testcase>/g,
  )) {
    if (!/<(failure|error)\b/.test(body)) continue;

    const name = (attrs.match(/name="([^"]*)"/) || [])[1] ?? "unknown test";
    const cls = (attrs.match(/classname="([^"]*)"/) || [])[1] ?? "";
    const message =
      (body.match(/<(?:failure|error)[^>]*message="([^"]*)"/) || [])[1] ?? "";

    failures++;
    console.log(`::error title=${clean(cls.split(".").pop() + " · " + name)}::${clean(message) || "see the uploaded report"}`);
  }
}

// Self-closing testcases have no body and cannot have failed, so a count of
// zero here with a red job means the failure was outside the tests — a
// compile error, or the task never reaching them.
console.log(
  failures === 0
    ? "No failing test cases found. If the job is red, it failed before or after the tests ran."
    : `${failures} failing test case${failures === 1 ? "" : "s"} reported above.`,
);
