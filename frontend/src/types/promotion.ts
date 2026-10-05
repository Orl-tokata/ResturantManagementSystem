/**
 * A rule that takes money off by itself.
 *
 * <p>Distinct from the discount a manager types into the payment panel: that
 * is one decision about one bill, this applies the same way to everybody who
 * qualifies. The bill records the two separately so a report can say whether
 * the rule was worth running.
 */
export type PromotionType = "PERCENT" | "AMOUNT" | "BUY_X_GET_Y";
export type PromotionScope = "ITEM" | "CATEGORY" | "ORDER";

/** SCREENS §3.5 holds buy-X-get-Y back; the server refuses it. */
export const PROMOTION_TYPES: PromotionType[] = ["PERCENT", "AMOUNT"];
export const PROMOTION_SCOPES: PromotionScope[] = ["ITEM", "CATEGORY", "ORDER"];

export interface Promotion {
  id: number;
  name: string;
  type: PromotionType;
  /** A percentage for PERCENT, money for AMOUNT. */
  value: number;
  scope: PromotionScope;
  scopeId?: number;
  /** What the scope names, so a list is not a column of ids. */
  scopeName?: string;
  minAmount?: number;
  startsAt: string;
  endsAt: string;
  timeFrom?: string;
  timeTo?: string;
  active: boolean;
  /** Dates, switch and clock together — whether it would fire right now. */
  liveNow: boolean;
}

export interface PromotionRequest {
  name: string;
  type: PromotionType;
  value: number;
  scope: PromotionScope;
  scopeId?: number;
  minAmount?: number;
  startsAt: string;
  endsAt: string;
  timeFrom?: string;
  timeTo?: string;
  active?: boolean;
}

/** One rule that actually came off a bill. */
export interface AppliedPromotion {
  promotionId: number;
  name: string;
  scope: PromotionScope;
  /** The dish it applied to, or absent for a whole-bill rule. */
  productName?: string;
  discount: number;
}
