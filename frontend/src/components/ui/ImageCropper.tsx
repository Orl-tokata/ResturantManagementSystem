"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";

/**
 * Choosing which part of a photograph becomes the product's picture.
 *
 * <p>A tile is 4:3 and a photograph rarely is, so something has to give: fit
 * the whole picture and leave bars, or fill the tile and lose the edges. Both
 * are worse than letting the person uploading decide, which is what this does
 * — the window is the tile, and what is inside it is what gets stored.
 *
 * <p>Pan and zoom rather than drag-handles. There is one shape to produce, so
 * resizing a rectangle is a degree of freedom that only creates the chance of
 * getting it wrong; moving the picture behind a fixed window cannot.
 */

/** The shape of a POS tile. The window matches it; its size on screen does not matter. */
const RATIO = 4 / 3;

/** What is written out. Larger than the window so the stored file is not soft. */
const OUT_W = 960;
const OUT_H = Math.round(OUT_W / RATIO);

const MAX_ZOOM = 4;

export function ImageCropper({
  file,
  onCancel,
  onDone,
}: {
  file: File;
  onCancel: () => void;
  onDone: (cropped: File) => void;
}) {
  const t = useTranslations("products");
  const tc = useTranslations("common");

  const [image, setImage] = useState<HTMLImageElement | null>(null);
  const [zoom, setZoom] = useState(1);
  /*
   * Geometry is kept as fractions of the window, never pixels.
   *
   * The window is as wide as the dialog allows and takes its height from an
   * aspect ratio, so its pixel size depends on the screen. Measuring it would
   * mean tracking a resize; expressing everything as a fraction means the same
   * numbers describe a 320px preview and a 960px canvas, and what was framed is
   * what gets written whatever the screen.
   */
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const boxRef = useRef<HTMLDivElement>(null);
  const drag = useRef<{ x: number; y: number; ox: number; oy: number } | null>(null);

  useEffect(() => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      setImage(img);
      setZoom(1);
      setOffset({ x: 0, y: 0 });
    };
    img.src = url;
    return () => URL.revokeObjectURL(url);
  }, [file]);

  /* ---- Size, as multiples of the window ----------------------------------- */

  const imageRatio = image ? image.width / image.height : RATIO;
  // Cover: whichever dimension would fall short is the one pinned to 1.
  const coverW = imageRatio >= RATIO ? imageRatio / RATIO : 1;
  const coverH = imageRatio >= RATIO ? 1 : RATIO / imageRatio;
  const w = coverW * zoom;
  const h = coverH * zoom;

  /**
   * How far out the slider goes: far enough to see the whole photograph.
   *
   * <p>It used to stop at 1, which is the point where the picture just covers
   * the window — so a square photograph in a 4:3 window could never show its
   * own top and bottom, however far the slider was dragged. Filling the tile is
   * the usual answer, which is why zoom still *starts* at 1, but "show all of
   * it and let the sides be white" is a legitimate choice and was unreachable.
   */
  const minZoom = 1 / Math.max(coverW, coverH);

  /**
   * Keeps the window covered while the picture is larger than it, and centres
   * the picture once it is smaller.
   *
   * <p>Below a zoom of 1 there is nothing to pan — any offset would only put
   * the gap on one side instead of splitting it — so the axis is pinned to the
   * middle rather than left draggable to no purpose.
   */
  function clamp(next: { x: number; y: number }) {
    return {
      x: w >= 1 ? Math.min(0, Math.max(1 - w, next.x)) : (1 - w) / 2,
      y: h >= 1 ? Math.min(0, Math.max(1 - h, next.y)) : (1 - h) / 2,
    };
  }

  /*
   * Clamped here rather than corrected in an effect after a zoom. Zooming out
   * can leave a stored offset that no longer covers the window; fixing that
   * from an effect means one render showing the gap and a second without it.
   */
  const view = clamp(offset);

  /* ---- Dragging ----------------------------------------------------------- */

  function onPointerDown(e: React.PointerEvent) {
    (e.target as Element).setPointerCapture(e.pointerId);
    drag.current = { x: e.clientX, y: e.clientY, ox: view.x, oy: view.y };
  }

  function onPointerMove(e: React.PointerEvent) {
    const d = drag.current;
    const box = boxRef.current;
    if (!d || !box) return;
    // Pointer movement is pixels; the geometry is fractions, so it is divided
    // by the window's current size rather than by a constant.
    setOffset(
      clamp({
        x: d.ox + (e.clientX - d.x) / box.clientWidth,
        y: d.oy + (e.clientY - d.y) / box.clientHeight,
      }),
    );
  }

  function onPointerUp(e: React.PointerEvent) {
    (e.target as Element).releasePointerCapture(e.pointerId);
    drag.current = null;
  }

  /* ---- Writing it out ------------------------------------------------------ */

  async function confirm() {
    if (!image) return;

    const canvas = document.createElement("canvas");
    canvas.width = OUT_W;
    canvas.height = OUT_H;
    const g = canvas.getContext("2d");
    if (!g) return;

    // White underneath: a transparent PNG would otherwise come out on black
    // once it is encoded as the JPEG the server stores.
    g.fillStyle = "#ffffff";
    g.fillRect(0, 0, OUT_W, OUT_H);
    g.imageSmoothingQuality = "high";

    // The same fractions that positioned it on screen, against the canvas.
    g.drawImage(image, view.x * OUT_W, view.y * OUT_H, w * OUT_W, h * OUT_H);

    const blob = await new Promise<Blob | null>((r) => canvas.toBlob(r, "image/jpeg", 0.9));
    if (!blob) return;

    // Named .jpg because that is what it now is, whatever was picked.
    onDone(new File([blob], "photo.jpg", { type: "image/jpeg" }));
  }

  const pct = (n: number) => `${n * 100}%`;

  return (
    <Modal
      open
      onClose={onCancel}
      title={t("cropTitle")}
      width="sm"
      footer={
        <>
          <Button variant="light" onClick={onCancel}>
            {tc("cancel")}
          </Button>
          <Button variant="admin" onClick={confirm} disabled={!image}>
            {t("cropUse")}
          </Button>
        </>
      }
    >
      <p className="mb-2 text-xs text-ink-500">{t("cropHelp")}</p>

      <div
        ref={boxRef}
        className="relative aspect-[4/3] w-full touch-none overflow-hidden rounded border border-ink-300 bg-ink-200"
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerCancel={onPointerUp}
      >
        {image && (
          // eslint-disable-next-line @next/next/no-img-element -- a local object URL positioned by hand
          <img
            src={image.src}
            alt=""
            draggable={false}
            className="absolute max-w-none cursor-grab select-none"
            style={{ left: pct(view.x), top: pct(view.y), width: pct(w), height: pct(h) }}
          />
        )}
      </div>

      <label className="mt-3 flex items-center gap-2 text-sm">
        {t("cropZoom")}
        <input
          type="range"
          min={minZoom}
          max={MAX_ZOOM}
          step={0.01}
          value={zoom}
          onChange={(e) => setZoom(Number(e.target.value))}
          className="flex-1"
        />
      </label>
    </Modal>
  );
}
