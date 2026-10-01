"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/Button";
import { ImageCropper } from "@/components/ui/ImageCropper";
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
  pending,
  busy,
  onPick,
  onRemove,
  hint,
}: {
  /** A stored photograph's filename. */
  file?: string | null;
  /**
   * A file chosen but not uploaded yet — the new-product case, where there is
   * no id to upload against until Save has created one.
   */
  pending?: File | null;
  busy?: boolean;
  onPick: (file: File) => void;
  onRemove: () => void;
  /** Replaces the standing hint, e.g. to say the photo uploads on save. */
  hint?: string;
}) {
  const t = useTranslations("products");
  const input = useRef<HTMLInputElement>(null);
  const [tooBig, setTooBig] = useState(false);
  /* Picked, but not yet framed. The cropper decides what is actually sent. */
  const [cropping, setCropping] = useState<File | null>(null);

  /*
   * A local preview of a file that has not been uploaded.
   *
   * Derived rather than held in state: an effect that sets state runs a second
   * render for a value already known during the first. The effect is only here
   * to revoke — object URLs hold the file in memory until they are, so picking
   * three photographs before saving would otherwise leak all three.
   */
  const preview = useMemo(
    () => (pending ? URL.createObjectURL(pending) : null),
    [pending],
  );
  useEffect(() => {
    if (!preview) return;
    return () => URL.revokeObjectURL(preview);
  }, [preview]);

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
    // Straight to the cropper rather than to the caller: a tile is 4:3 and a
    // photograph rarely is, so someone has to choose what is kept. Better the
    // person who can see the dish than a rule that always takes the middle.
    setCropping(picked);
  }

  const shown = preview ?? (file ? productImageUrl(file) : null);
  const has = Boolean(shown);

  return (
    <div>
      {cropping && (
        <ImageCropper
          file={cropping}
          onCancel={() => setCropping(null)}
          onDone={(cropped) => {
            setCropping(null);
            onPick(cropped);
          }}
        />
      )}

      <div className="flex items-center gap-3">
        <div className="grid h-20 w-20 shrink-0 place-items-center overflow-hidden rounded border border-ink-300 bg-ink-50">
          {shown ? (
            /* eslint-disable-next-line @next/next/no-img-element --
               Same reasoning as ProductImage: already scaled server-side. */
            <img src={shown} alt="" className="h-full w-full object-cover" />
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
            📷 {has ? t("replacePhoto") : t("addPhoto")}
          </Button>
          {has && (
            <Button size="sm" variant="ghost" onClick={onRemove} disabled={busy}>
              {t("removePhoto")}
            </Button>
          )}
        </div>
      </div>

      <p className="mt-1.5 text-xs text-ink-500">{hint ?? t("photoHint")}</p>
      {tooBig && (
        <p role="alert" className="mt-1 text-xs font-semibold text-danger">
          {t("photoTooBig")}
        </p>
      )}
    </div>
  );
}
