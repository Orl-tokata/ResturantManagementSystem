"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/Field";

/**
 * Choosing the emoji that stands in for a dish or a category.
 *
 * <p>This used to be a text box you pasted into, which meant leaving the form,
 * finding an emoji somewhere else, and copying it back — for every product on
 * the menu. The characters are a short, fixed set, so they belong on screen.
 *
 * <p>Not an image upload. A photograph is a better picture of a dish and a
 * worse thing to run: storage, thumbnails, backups, and someone to take
 * eighteen decent pictures. An emoji renders instantly on a till, survives a
 * pg_dump with everything else, and tells a cashier which tile is the coffee.
 * V9 has the reasoning, and leaves room to add photographs beside these rather
 * than instead of them.
 */

/**
 * Grouped so the list can be skimmed, and searchable so it need not be.
 *
 * <p>Keywords are English only on purpose. They are what someone types to find
 * a picture, not text anyone reads — and a Khmer cashier looking for coffee is
 * as likely to find it by its position in the Drinks row as by typing.
 */
const GROUPS: { key: string; emoji: [string, string][] }[] = [
  {
    key: "rice",
    emoji: [["🍚", "rice"], ["🍛", "curry"], ["🍜", "noodle"], ["🍝", "pasta"],
            ["🍲", "soup stew"], ["🥘", "pan"], ["🍥", "fish cake"], ["🍘", "cracker"]],
  },
  {
    key: "meat",
    emoji: [["🍗", "chicken"], ["🍖", "meat"], ["🥩", "beef steak"], ["🥓", "pork"],
            ["🐟", "fish"], ["🦐", "prawn shrimp"], ["🦀", "crab"], ["🍤", "fried prawn"]],
  },
  {
    key: "veg",
    emoji: [["🥗", "salad"], ["🥬", "greens"], ["🥦", "broccoli"], ["🍅", "tomato"],
            ["🌶️", "chilli spicy"], ["🧄", "garlic"], ["🥕", "carrot"], ["🍄", "mushroom"]],
  },
  {
    key: "drink",
    emoji: [["☕", "coffee"], ["🍵", "tea"], ["🥤", "soft drink"], ["🧃", "juice"],
            ["🧊", "ice"], ["🍺", "beer"], ["🍷", "wine"], ["🥛", "milk"]],
  },
  {
    key: "sweet",
    emoji: [["🍰", "cake"], ["🍧", "shaved ice"], ["🍨", "ice cream"], ["🍩", "donut"],
            ["🍌", "banana"], ["🍉", "watermelon"], ["🥭", "mango"], ["🍍", "pineapple"]],
  },
  {
    key: "other",
    emoji: [["🍽️", "plate meal"], ["🥡", "takeaway box"], ["🍳", "fried egg"],
            ["🥚", "egg"], ["🍞", "bread"], ["🧂", "salt"], ["🥢", "chopsticks"],
            ["🔥", "hot grilled"]],
  },
];

export function IconPicker({
  value = "",
  onChange,
  id,
}: {
  /** Optional: a draft legitimately has no icon until one is picked. */
  value?: string;
  onChange: (icon: string) => void;
  id?: string;
}) {
  const t = useTranslations("icons");
  const [query, setQuery] = useState("");

  const groups = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return GROUPS;
    return GROUPS.map((g) => ({
      ...g,
      emoji: g.emoji.filter(([char, words]) => words.includes(q) || char === q),
    })).filter((g) => g.emoji.length > 0);
  }, [query]);

  return (
    <div>
      <div className="mb-2 flex items-center gap-2">
        {/* What is chosen, at the size it will actually be shown. */}
        <span
          aria-hidden
          className="grid h-10 w-10 shrink-0 place-items-center rounded border border-ink-300 bg-ink-50 text-2xl"
        >
          {value || "🍽️"}
        </span>
        <Input
          id={id}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder={t("search")}
          className="py-1.5 text-sm"
        />
        {value && (
          <button
            type="button"
            onClick={() => onChange("")}
            className="shrink-0 text-xs text-ink-500 underline hover:text-ink-700"
          >
            {t("clear")}
          </button>
        )}
      </div>

      <div className="max-h-44 overflow-y-auto rounded border border-ink-200 bg-white p-1.5">
        {groups.length === 0 && (
          <p className="py-4 text-center text-xs text-ink-500">{t("noMatch")}</p>
        )}

        {groups.map((group) => (
          <div key={group.key} className="mb-1.5 last:mb-0">
            <div className="px-0.5 pb-0.5 text-[10px] font-semibold uppercase tracking-wide text-ink-500">
              {t(group.key)}
            </div>
            <div className="flex flex-wrap gap-1">
              {group.emoji.map(([char, words]) => (
                <button
                  key={char}
                  type="button"
                  onClick={() => onChange(char)}
                  aria-label={words}
                  aria-pressed={value === char}
                  className={`grid h-8 w-8 place-items-center rounded text-xl transition ${
                    value === char
                      ? "bg-[#e8f5f5] ring-2 ring-teal-600"
                      : "hover:bg-ink-100"
                  }`}
                >
                  {char}
                </button>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
