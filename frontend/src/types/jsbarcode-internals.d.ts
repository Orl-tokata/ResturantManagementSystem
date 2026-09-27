/**
 * The Code 128 symbol table, reached by deep import.
 *
 * <p>jsbarcode ships no types for its internals, and Barcode.test.tsx decodes
 * the rendered bars against this table rather than a copy typed out by hand —
 * a first attempt did hand-copy it and got several symbols wrong, which is the
 * same class of mistake as a barcode that looks right and does not scan.
 *
 * <p>BARS holds numbers, not strings: 11010010000 rather than "11010010000".
 * Every pattern starts with a bar, so no leading zero is lost when converting.
 */
declare module "jsbarcode/src/barcodes/CODE128/constants" {
  export const BARS: number[];
  export const START_A: number;
  export const START_B: number;
  export const START_C: number;
  export const STOP: number;
  export const MODULO: number;
}
