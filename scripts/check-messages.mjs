#!/usr/bin/env node
/**
 * Checks the frontend message catalogues against each other and against the
 * code that uses them.
 *
 *   node scripts/check-messages.mjs
 *
 * Exits 0 when everything lines up, 1 otherwise.
 *
 * This exists because a message key is a string: `t("saveProduct")` compiles,
 * lints and builds whether or not that key was ever written. A key added to
 * English and forgotten in Khmer reaches a screen before anyone notices. Three
 * things are checked:
 *
 *   1. Both catalogues define the same keys.
 *   2. Every key the code asks for exists.
 *   3. No value still carries a zero-width space, which is invisible in an
 *      editor and quietly breaks any later comparison on the string.
 *
 * The backend half of this is MessageBundleTest, which does the same for
 * messages_{km,en}.properties and additionally renders every pattern.
 */

import { readFileSync, readdirSync } from "node:fs";
import { join, sep } from "node:path";

const ROOT = "frontend";
const problems = [];

/* ---- 1. the two catalogues must agree -------------------------------- */

function leaves(obj, prefix = "", out = []) {
  for (const [k, v] of Object.entries(obj)) {
    const key = prefix ? `${prefix}.${k}` : k;
    if (v && typeof v === "object") leaves(v, key, out);
    else out.push(key);
  }
  return out;
}

const load = (loc) => JSON.parse(readFileSync(join(ROOT, "messages", `${loc}.json`), "utf8"));
const km = load("km");
const en = load("en");
const kmKeys = leaves(km), enKeys = leaves(en);
const kmSet = new Set(kmKeys), enSet = new Set(enKeys);

const missingInKm = enKeys.filter((k) => !kmSet.has(k));
const missingInEn = kmKeys.filter((k) => !enSet.has(k));
for (const k of missingInKm) problems.push(`defined in en but not km: ${k}`);
for (const k of missingInEn) problems.push(`defined in km but not en: ${k}`);

/* ---- 2. every key the code asks for must exist ------------------------ */

const files = [];
(function walk(dir) {
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    const p = join(dir, e.name);
    if (e.isDirectory()) walk(p);
    else if (e.name.endsWith(".ts") || e.name.endsWith(".tsx")) files.push(p);
  }
})(join(ROOT, "src"));

let asked = 0;
for (const file of files) {
  const rel = file.split(sep).join("/");
  // The UI-kit gallery is a development aid with deliberately fake content.
  if (rel.includes("ui-kit")) continue;
  const src = readFileSync(file, "utf8");

  // Map each local translator to the namespace it was opened with, so
  // tc("save") can be resolved to common.save.
  const namespaces = {};
  const decl = /const ([A-Za-z0-9_]+) = (?:await )?(?:useTranslations|getTranslations)\("([^"]+)"\)/g;
  for (const m of src.matchAll(decl)) namespaces[m[1]] = m[2];

  for (const [fn, ns] of Object.entries(namespaces)) {
    const re = new RegExp(`(?:^|[^A-Za-z0-9_.])${fn}\\("([^"]+)"`, "g");
    for (const m of src.matchAll(re)) {
      asked++;
      const key = `${ns}.${m[1]}`;
      if (!kmSet.has(key)) problems.push(`${rel}: ${fn}("${m[1]}") -> ${key} is not defined`);
    }
  }

  // apiError(e, "saveProduct") names a key in the error namespace. Without
  // this the action keys look unreferenced and a typo in one sails through.
  for (const m of src.matchAll(/apiError\([^,)]+,\s*"([^"]+)"\s*\)/g)) {
    asked++;
    const key = `error.${m[1]}`;
    if (!kmSet.has(key)) problems.push(`${rel}: apiError(…, "${m[1]}") -> ${key} is not defined`);
  }
}

/* ---- 3. no invisible characters -------------------------------------- */

for (const loc of ["km", "en"]) {
  const raw = readFileSync(join(ROOT, "messages", `${loc}.json`), "utf8");
  const count = (raw.match(/​/g) || []).length;
  if (count) problems.push(`${loc}.json contains ${count} zero-width space(s) — invisible, and they break string comparison`);
}

/* ---- report ----------------------------------------------------------- */

console.log(`km: ${kmKeys.length} keys · en: ${enKeys.length} keys · ${asked} lookups found in code`);

if (problems.length === 0) {
  console.log("catalogues agree, every key resolves, no invisible characters");
  process.exit(0);
}

console.error(`\n${problems.length} problem(s):`);
for (const p of problems) console.error("  " + p);
process.exit(1);
