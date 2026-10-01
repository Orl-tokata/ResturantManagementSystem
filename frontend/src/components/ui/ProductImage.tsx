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
    /* eslint-disable-next-line @next/next/no-img-element --
       Not next/image: these are served by the API on another origin in
       development, so the loader would need a remote pattern configured per
       deployment — to optimise an image the server already scaled to 600px on
       the way in. */
    <img
      src={src}
      alt={alt}
      loading="lazy"
      onError={() => setFailed(true)}
      className={`h-full w-full object-cover ${className}`}
    />
  );
}
