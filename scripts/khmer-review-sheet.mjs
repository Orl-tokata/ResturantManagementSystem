#!/usr/bin/env node
/**
 * Builds a review sheet for the Khmer copy.
 *
 *   node scripts/khmer-review-sheet.mjs            # writes khmer-review.csv
 *   node scripts/khmer-review-sheet.mjs --md       # also writes khmer-review.md
 *
 * About 450 of the Khmer strings in this application have no source in the
 * Figma file — they were written English-first and have never been read by a
 * Khmer speaker. Tests cannot catch stilted or wrong wording, so this exists to
 * put every string in front of someone who can.
 *
 * The sheet is ordered by screen so a reviewer can work through the app rather
 * than through an alphabetical list, and it carries a Correction column to
 * write in. Hand it back filled and the corrections can be applied from it.
 *
 * It also flags four things mechanically, which are bugs rather than matters of
 * taste:
 *
 *   PLACEHOLDER  the {tokens} differ between the two languages. A message
 *                missing its {name} renders the literal word "{name}" or drops
 *                the value entirely.
 *   UNTRANSLATED the Khmer is character-for-character the English.
 *   NO-KHMER     the Khmer field contains no Khmer script at all.
 *   LATIN        the Khmer contains Latin letters. Often legitimate — USD, KHQR,
 *                ABA — but worth a glance.
 */
import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const messages = join(root, "frontend", "messages");

const en = JSON.parse(readFileSync(join(messages, "en.json"), "utf8"));
const km = JSON.parse(readFileSync(join(messages, "km.json"), "utf8"));

/** Where each catalogue section appears, so the sheet follows the app. */
const SCREEN_ORDER = [
  ["auth", "Sign in, forgot password, OTP, reset"],
  ["nav", "Sidebar and navigation"],
  ["common", "Shared words — buttons, labels, table headers"],
  ["cashierHome", "Cashier home"],
  ["pos", "POS / taking an order"],
  ["payment", "Payment screen"],
  ["receipt", "Printed receipt"],
  ["history", "Order history"],
  ["dashboard", "Admin dashboard"],
  ["product", "Products"],
  ["category", "Categories"],
  ["table", "Tables"],
  ["staff", "Staff and login accounts"],
  ["supplier", "Suppliers"],
  ["purchase", "Purchase orders"],
  ["stock", "Stock"],
  ["report", "Reports"],
  ["settings", "Settings"],
  ["profile", "Profile"],
  ["error", "Error messages"],
  ["a11y", "Screen-reader labels (spoken, not seen)"],
  ["enum", "Dropdown values"],
  ["app", "Application metadata"],
];

const PLACEHOLDER = /\{(\w+)\}/g;
const KHMER = /[ក-៿]/;

/**
 * Latin that is Latin on purpose, and not a translation gap.
 *
 * A first run flagged 28 strings and every one was noise: placeholder tokens
 * like {seconds}, and brand names the Khmer would not translate anyway. A flag
 * column full of false positives is a column a reviewer learns to skip, which
 * is worse than having no column at all.
 */
const LATIN_BY_DESIGN = /\b(KHQR|ABA|Bakong|HTTP|USD|KHR|POS|VAT|CSV|INV|PO|QR|ID|OTP)\b/g;

/** Placeholders and brand names removed, so only real Latin words are left. */
function strippedForLatinCheck(value) {
  return String(value).replace(PLACEHOLDER, " ").replace(LATIN_BY_DESIGN, " ");
}

function flatten(obj, prefix = "") {
  const out = {};
  for (const [k, v] of Object.entries(obj ?? {})) {
    const key = prefix ? `${prefix}.${k}` : k;
    if (v && typeof v === "object") Object.assign(out, flatten(v, key));
    else out[key] = String(v);
  }
  return out;
}

function tokens(s) {
  return [...String(s).matchAll(PLACEHOLDER)].map((m) => m[1]).sort().join(",");
}

function flagsFor(english, khmer) {
  const flags = [];
  if (tokens(english) !== tokens(khmer)) flags.push("PLACEHOLDER");
  if (english.trim() === khmer.trim()) flags.push("UNTRANSLATED");
  else if (!KHMER.test(khmer)) flags.push("NO-KHMER");
  else if (/[A-Za-z]{2,}/.test(strippedForLatinCheck(khmer))) flags.push("LATIN");
  return flags;
}

const enFlat = flatten(en);
const kmFlat = flatten(km);

const sections = new Map();
for (const key of Object.keys(enFlat)) {
  const section = key.split(".")[0];
  if (!sections.has(section)) sections.set(section, []);
  sections.get(section).push(key);
}

// Known sections first, in screen order; anything unlisted goes after.
const ordered = [
  ...SCREEN_ORDER.filter(([s]) => sections.has(s)),
  ...[...sections.keys()]
    .filter((s) => !SCREEN_ORDER.some(([known]) => known === s))
    .sort()
    .map((s) => [s, ""]),
];

const csvCell = (v) => `"${String(v ?? "").replace(/"/g, '""')}"`;

const rows = [
  ["Screen", "Key", "English", "Khmer (current)", "Correction", "Flags"].map(csvCell).join(","),
];
const md = [
  "# Khmer copy review",
  "",
  "Every Khmer string in the application, in the order the screens appear.",
  "",
  "About 450 of these have no source in the Figma design — they were written",
  "English-first and have never been read by a Khmer speaker. Nothing in the test",
  "suite can tell whether they read naturally; only a person can.",
  "",
  "Write corrections in the CSV's **Correction** column and hand it back.",
  "",
  "Flags are mechanical, and the first three are bugs rather than matters of taste:",
  "",
  "| Flag | Meaning |",
  "|---|---|",
  "| `PLACEHOLDER` | the `{tokens}` differ between languages — the message will render wrongly |",
  "| `UNTRANSLATED` | the Khmer is identical to the English |",
  "| `NO-KHMER` | no Khmer script in the Khmer field at all |",
  "| `LATIN` | contains Latin letters; often fine (USD, KHQR, ABA) but worth a glance |",
  "",
];

let total = 0;
const flagged = [];

for (const [section, label] of ordered) {
  const keys = sections.get(section).sort();
  md.push("", `## ${section}${label ? ` — ${label}` : ""}`, "", "| Key | English | Khmer | Flags |", "|---|---|---|---|");

  for (const key of keys) {
    const english = enFlat[key] ?? "";
    const khmer = kmFlat[key] ?? "";
    const flags = flagsFor(english, khmer);
    total++;
    if (flags.length) flagged.push({ key, english, khmer, flags });

    rows.push([section, key, english, khmer, "", flags.join(" ")].map(csvCell).join(","));
    md.push(
      `| \`${key.replace(`${section}.`, "")}\` | ${english.replace(/\|/g, "\\|")} | ${khmer.replace(/\|/g, "\\|")} | ${flags.join(" ")} |`,
    );
  }
}

// BOM so Excel opens the Khmer correctly instead of as mojibake.
writeFileSync(join(root, "khmer-review.csv"), "﻿" + rows.join("\r\n") + "\r\n", "utf8");

if (process.argv.includes("--md")) {
  writeFileSync(join(root, "khmer-review.md"), md.join("\n") + "\n", "utf8");
}

console.log(`${total} strings across ${ordered.length} sections -> khmer-review.csv`);

if (flagged.length === 0) {
  console.log("no mechanical flags");
} else {
  console.log(`\n${flagged.length} flagged mechanically:\n`);
  for (const f of flagged) {
    console.log(`  [${f.flags.join(" ")}] ${f.key}`);
    console.log(`      en: ${f.english}`);
    console.log(`      km: ${f.khmer}`);
  }
}
