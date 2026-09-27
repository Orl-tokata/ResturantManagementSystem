import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
// The 107-symbol table, taken from the encoder rather than typed out here. A
// first version of this test hand-copied it and got several symbols wrong,
// which is the same mistake in miniature as the thing being fixed: a table
// that looks plausible and is not.
import { BARS, START_B, START_C, STOP } from "jsbarcode/src/barcodes/CODE128/constants";
import { Barcode } from "./Barcode";

/*
 * A barcode is the one thing on a receipt whose correctness cannot be judged by
 * looking at it. What stood here before was a row of pipe characters that
 * looked entirely convincing and encoded nothing.
 *
 * So this does what a scanner does: reads the bar geometry out of the rendered
 * SVG, turns it back into modules, and decodes it.
 *
 * What that proves, and what it does not. It proves the SVG geometry is right,
 * that module widths are uniform and whole, that the framing is a valid
 * start/stop pair, that the checksum arithmetic is correct — computed here
 * independently — and that the symbols spell the invoice number, including
 * across a Code B to Code C switch. It does not independently prove the symbol
 * table itself, which comes from the encoder. Verifying that would need a
 * scanner, and there is none here; it is stated plainly rather than implied.
 */

const MODULO = 103;

/*
 * BARS holds numbers, not strings — 11010010000 rather than "11010010000" — so
 * comparing module strings against it directly finds nothing. Every pattern
 * begins with a bar, so no leading zero is lost in the conversion.
 */
const PATTERN_TO_CODE = new Map<string, number>(
  (BARS as unknown as number[]).map((pattern, code) => [String(pattern), code]),
);

/** Turns the rendered SVG back into a string of 1s and 0s, one per module. */
function modulesFrom(svg: SVGElement): string {
  const bars = [...svg.querySelectorAll("rect")]
    // The encoder draws a full-width background rect first. Including it makes
    // every bar look like it overlaps the one before.
    .filter((r) => r.getAttribute("fill") !== "transparent")
    .map((r) => ({ x: Number(r.getAttribute("x")), w: Number(r.getAttribute("width")) }))
    .filter((b) => Number.isFinite(b.x) && b.w > 0)
    .sort((a, b) => a.x - b.x);

  if (bars.length === 0) throw new Error("no bars were drawn");

  // One module is the narrowest bar the encoder drew.
  const unit = Math.min(...bars.map((b) => b.w));

  let out = "";
  let cursor = bars[0].x;
  for (const bar of bars) {
    const gap = Math.round((bar.x - cursor) / unit);
    if (gap < 0) throw new Error("bars overlap");
    out += "0".repeat(gap) + "1".repeat(Math.round(bar.w / unit));
    cursor = bar.x + bar.w;
  }
  return out;
}

/** Reads Code 128 symbols and returns the text they carry. */
function decode(svg: SVGElement): string {
  const modules = modulesFrom(svg);

  const symbols: number[] = [];
  for (let i = 0; i < modules.length; ) {
    // Every symbol is 11 modules; the stop pattern is 13.
    const slice = modules.slice(i, i + 11);
    const code = PATTERN_TO_CODE.get(slice);
    if (code === undefined) {
      const stopSlice = modules.slice(i, i + 13);
      const stopCode = PATTERN_TO_CODE.get(stopSlice);
      if (stopCode !== STOP) throw new Error(`unknown pattern "${slice}" at module ${i}`);
      symbols.push(stopCode);
      break;
    }
    symbols.push(code);
    i += 11;
  }

  const stopAt = symbols.indexOf(STOP);
  if (stopAt === -1) throw new Error("no stop symbol");

  const start = symbols[0];
  if (start !== START_B && start !== START_C) {
    throw new Error(`expected a Start B or Start C, got ${start}`);
  }

  const payload = symbols.slice(1, stopAt - 1);
  const checksum = symbols[stopAt - 1];

  // Computed here rather than taken from the encoder: a scanner rejects a code
  // whose sum disagrees, so this is what separates a readable print from a
  // corrupted one.
  const expected =
    (start + payload.reduce((sum, code, i) => sum + code * (i + 1), 0)) % MODULO;
  if (checksum !== expected) {
    throw new Error(`checksum ${checksum} does not match computed ${expected}`);
  }

  // Walk the payload, honouring switches between character sets.
  let set: "A" | "B" | "C" = start === START_C ? "C" : "B";
  let text = "";
  for (const code of payload) {
    /*
     * A switch code only means "switch" in a set other than the one it
     * selects. 99 changes to Code C, but read while already in C it is the
     * digit pair "99"; 101 changes to Code A, which is where this encoder puts
     * a hyphen between two digit runs.
     *
     * Getting this wrong produced plausible nonsense rather than an error:
     * "INV-99999" came back as "INV-9", and "PO-2026-0042" as
     * "PO-202610113990042". A decoder that quietly returns the wrong string is
     * the same failure as a barcode that quietly scans as the wrong number.
     */
    if (set !== "C" && code === 99) { set = "C"; continue; }
    if (set !== "B" && code === 100) { set = "B"; continue; }
    if (set !== "A" && code === 101) { set = "A"; continue; }
    text += set === "C" ? String(code).padStart(2, "0") : String.fromCharCode(code + 32);
  }
  return text;
}

async function renderAndDecode(value: string) {
  render(<Barcode value={value} />);
  const svg = await screen.findByRole("img", { name: value });
  await waitFor(() => expect(svg.querySelectorAll("rect").length).toBeGreaterThan(5));
  return decode(svg as unknown as SVGElement);
}

describe("the receipt barcode", () => {
  it("encodes the invoice number so a scanner reads it back", async () => {
    expect(await renderAndDecode("INV-00179")).toBe("INV-00179");
  });

  it("reads back across a Code B to Code C switch", async () => {
    // An invoice number is letters then a run of digits, and the encoder packs
    // digit pairs two to a symbol. A decoder that ignored the switch would
    // return plausible nonsense, which is the failure worth catching.
    expect(await renderAndDecode("INV-00042")).toBe("INV-00042");
  });

  it("handles a longer document number", async () => {
    expect(await renderAndDecode("PO-2026-0042")).toBe("PO-2026-0042");
  });

  it("carries a checksum that agrees with the payload", async () => {
    // decode() throws when it does not, so arriving here is the assertion.
    await expect(renderAndDecode("INV-99999")).resolves.toBe("INV-99999");
  });

  it("renders nothing rather than breaking the receipt when it cannot encode", () => {
    // A slip that fails to print because of its barcode is worse than a slip
    // without one. The number is printed as text either way.
    const { container } = render(<Barcode value="" />);
    expect(container.querySelector("rect")).toBeNull();
  });

  it("is announced to a screen reader as the number it carries", async () => {
    render(<Barcode value="INV-00179" />);
    expect(await screen.findByRole("img", { name: "INV-00179" })).toBeTruthy();
  });
});
