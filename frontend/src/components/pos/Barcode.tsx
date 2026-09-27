"use client";

import { useEffect, useRef } from "react";
import JsBarcode from "jsbarcode";

/**
 * A Code 128 barcode, drawn as SVG.
 *
 * <p>Code 128 because an invoice number is alphanumeric — "INV-00179" — which
 * rules out EAN and UPC, and because it packs digit pairs two to a symbol, so
 * the result stays narrow enough for an 80mm roll.
 *
 * <p>SVG rather than canvas. A receipt is printed, and a thermal head renders
 * vector bars at exact widths where a rasterised canvas would be resampled — a
 * bar that lands half a dot narrow is a bar a scanner may refuse.
 *
 * <p>What stood here before was a row of pipe characters that looked like a
 * barcode and encoded nothing. The difference shows up only when someone
 * points a scanner at it, which is the moment it is too late to find out.
 * Barcode.test.tsx therefore decodes the rendered bars rather than counting
 * them.
 */
export function Barcode({
  value,
  /** Bar width in px. 2 keeps a 9-character code inside 80mm paper. */
  width = 2,
  height = 40,
  className,
}: {
  value: string;
  width?: number;
  height?: number;
  className?: string;
}) {
  const ref = useRef<SVGSVGElement | null>(null);

  useEffect(() => {
    if (!ref.current || !value) return;
    try {
      JsBarcode(ref.current, value, {
        format: "CODE128",
        width,
        height,
        // The number is printed beneath in the app's own font; jsbarcode's
        // built-in caption would repeat it in a different one.
        displayValue: false,
        margin: 0,
        background: "transparent",
        lineColor: "#000",
      });
    } catch {
      /*
       * An unencodable value must not take the receipt down with it. The
       * element stays empty and the invoice number is printed as text either
       * way, so the slip remains usable — a bill that will not print because
       * of its barcode is worse than a bill without one.
       *
       * Deliberately no error state: setting one here would re-render from
       * inside the effect for a case that changes nothing a cashier can act on.
       */
    }
  }, [value, width, height]);

  return <svg ref={ref} className={className} aria-label={value} role="img" />;
}
