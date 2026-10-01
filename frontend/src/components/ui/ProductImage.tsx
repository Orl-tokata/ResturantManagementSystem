"use client";

import { useState } from "react";
import { productImageUrl } from "@/lib/images";

/**
 * A product's photograph, falling back to its emoji.
 *
 * <p>Three things can leave a tile without a picture: the product never had
 * one, the file is missing after a database restore that did not bring the
 * uploads directory, or the network dropped mid-load. All three land here, and
 * all three show the icon — which is what the screen did before photographs
 * existed, so the worst case is the old behaviour rather than an empty box.
 */
export function ProductImage({
  file,
  icon,
  alt,
  className = "",
  iconClassName = "text-3xl",
}: {
  file?: string | null;
  icon?: string | null;
  alt: string;
  className?: string;
  iconClassName?: string;
}) {
  const [failed, setFailed] = useState(false);
  const src = file && !failed ? productImageUrl(file) : null;

  if (!src) {
    return (
      <span aria-label={alt} role="img" className={iconClassName}>
        {icon || "🍽️"}
      </span>
    );
  }

  return (
    /*
     * A positioned wrapper, so the image has a box to fill.
     *
     * Two earlier attempts failed in opposite directions. `h-full w-full`
     * inside a parent sized by aspect-ratio let the percentage resolve to auto,
     * so a 348x346 photograph rendered 127x126 in a 127x95 tile and was clipped
     * by overflow-hidden — object-contain undone by the element's own size.
     * Switching to `max-h-full max-w-full` removed the size entirely: the image
     * measured 0x0, and `loading="lazy"` on a zero-size element never enters
     * the viewport, so it never began loading at all.
     *
     * inset-0 against a relative wrapper is definite whatever the parent is
     * sized by, and object-contain then fits the whole dish inside it.
     */
    <span className="relative block h-full w-full">
      {/* eslint-disable-next-line @next/next/no-img-element --
          Not next/image: these are served by the API on another origin in
          development, so the loader would need a remote pattern configured per
          deployment — to optimise an image the server already scaled to 600px
          on the way in. */}
      <img
        src={src}
        alt={alt}
        loading="lazy"
        onError={() => setFailed(true)}
        className={`absolute inset-0 h-full w-full object-contain ${className}`}
      />
    </span>
  );
}
