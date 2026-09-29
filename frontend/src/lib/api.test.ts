import { describe, expect, it } from "vitest";

import { needsIdempotencyKey, newIdempotencyKey } from "./api";

/**
 * The route list mirrors IdempotencyFilter.REQUIRED on the backend. If the two
 * drift, the symptom is a 400 the user cannot do anything about — so the cases
 * below are the exact five, stated once more in a place that fails loudly.
 */
describe("needsIdempotencyKey", () => {
  it.each([
    "/orders",
    "/orders/42/pay",
    "/orders/42/cancel",
    "/stock/7/adjust",
    "/purchases/3/receive",
  ])("requires a key for POST %s", (url) => {
    expect(needsIdempotencyKey("post", url)).toBe(true);
  });

  it("matches regardless of how the method is cased", () => {
    expect(needsIdempotencyKey("POST", "/orders")).toBe(true);
  });

  it("ignores a query string", () => {
    expect(needsIdempotencyKey("post", "/orders?table=4")).toBe(true);
  });

  it.each([
    ["get", "/orders"],
    ["put", "/orders/42/items"],
    ["delete", "/orders/42/khqr"],
  ])("leaves %s %s alone", (method, url) => {
    expect(needsIdempotencyKey(method, url)).toBe(false);
  });

  it.each([
    "/categories",
    "/products",
    "/orders/42/khqr",
    "/purchases/3/cancel",
    "/orders/42/pay/extra",
  ])("does not require a key for POST %s", (url) => {
    // The last two matter most: /purchases/*/cancel is deliberately not on the
    // list, and a deeper path must not match a prefix.
    expect(needsIdempotencyKey("post", url)).toBe(false);
  });

  it("copes with a missing url or method", () => {
    expect(needsIdempotencyKey(undefined, "/orders")).toBe(false);
    expect(needsIdempotencyKey("post", undefined)).toBe(false);
  });
});

describe("newIdempotencyKey", () => {
  it("fits the column the server stores it in, and is not repeated", () => {
    const keys = new Set(Array.from({ length: 200 }, () => newIdempotencyKey()));
    expect(keys.size).toBe(200);
    for (const k of keys) expect(k.length).toBeLessThanOrEqual(80);
  });
});
