"use client";

import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/Button";
import { productImageUrl } from "@/lib/images";

/**
 * Choosing, replacing or removing a product's photograph.
 *
 * <p>Uploads immediately rather than on save. The file goes to its own
 * endpoint, so holding it until the form is submitted would mean either a
 * second request the user does not know happened, or carrying bytes through a
 * JSON payload. Immediate also means the preview is the stored, scaled image
 * rather than a local object URL that looks different from the result.
 *
 * <p>Only offered for a product that already exists: there is no id to upload
 * against until it has been created once.
 */
export function ImageUpload({
  file,
  busy,
  onPick,
  onRemove,
  disabledReason,
}: {
  file?: string | null;
  busy?: boolean;
  onPick: (file: File) => void;
  onRemove: () => void;
  /** Shown instead of the controls when uploading is not possible yet. */
  disabledReason?: string;
}) {
  const t = useTranslations("products");
  const input = useRef<HTMLInputElement>(null);
  const [tooBig, setTooBig] = useState(false);

  /** Matches app.uploads.max-image-size, so the refusal is instant. */
  const MAX_BYTES = 5 * 1024 * 1024;

  function choose(picked: File | undefined) {
    if (!picked) return;
    if (picked.size > MAX_BYTES) {
      // Caught here as well as on the server — a phone photograph over the
      // limit should not have to be uploaded before being refused.
      setTooBig(true);
      return;
    }
    setTooBig(false);
    onPick(picked);
  }

  if (disabledReason) {
    return <p className="text-xs text-ink-500">{disabledReason}</p>;
  }

  return (
    <div>
      <div className="flex items-center gap-3">
        <div className="grid h-20 w-20 shrink-0 place-items-center overflow-hidden rounded border border-ink-300 bg-ink-50">
          {file ? (
            /* eslint-disable-next-line @next/next/no-img-element --
               Same reasoning as ProductImage: already scaled server-side. */
            <img
              src={productImageUrl(file)}
              alt=""
              className="h-full w-full object-cover"
            />
          ) : (
            <span className="text-xs text-ink-500">{t("noPhoto")}</span>
          )}
        </div>

        <div className="flex flex-wrap gap-1.5">
          <input
            ref={input}
            type="file"
            accept="image/jpeg,image/png"
            className="hidden"
            onChange={(e) => {
              choose(e.target.files?.[0]);
              // Cleared so picking the same file twice still fires a change.
              e.target.value = "";
            }}
          />
          <Button
            size="sm"
            variant="light"
            loading={busy}
            onClick={() => input.current?.click()}
          >
            📷 {file ? t("replacePhoto") : t("addPhoto")}
          </Button>
          {file && (
            <Button size="sm" variant="ghost" onClick={onRemove} disabled={busy}>
              {t("removePhoto")}
            </Button>
          )}
        </div>
      </div>

      <p className="mt-1.5 text-xs text-ink-500">{t("photoHint")}</p>
      {tooBig && (
        <p role="alert" className="mt-1 text-xs font-semibold text-danger">
          {t("photoTooBig")}
        </p>
      )}
    </div>
  );
}
