"use client";

import Image from "next/image";
import { useState } from "react";
import { isPlaceholderImage } from "@/lib/images";

type ProductGalleryProps = {
  /** Image URLs in display order; the first one is the cover. */
  images: string[];
  productName: string;
};

/** Large main image plus clickable thumbnails (shown only with 2+ images). */
export function ProductGallery({ images, productName }: ProductGalleryProps) {
  const [selectedIndex, setSelectedIndex] = useState(0);
  const selected = images[selectedIndex];

  return (
    <div className="flex flex-col gap-3">
      <div className="relative aspect-4/5 overflow-hidden rounded-soft bg-peach/30">
        {selected ? (
          <Image
            key={selected}
            src={selected}
            alt={
              images.length > 1
                ? `${productName}, foto ${selectedIndex + 1} de ${images.length}`
                : productName
            }
            fill
            preload={selectedIndex === 0}
            sizes="(min-width: 768px) 50vw, 100vw"
            unoptimized={isPlaceholderImage(selected)}
            className="object-cover"
          />
        ) : (
          <div className="flex h-full items-center justify-center font-script text-5xl text-charcoal/40">
            Vitta
          </div>
        )}
      </div>

      {images.length > 1 && (
        <ul className="grid grid-cols-4 gap-2 sm:gap-3">
          {images.map((url, index) => {
            const isSelected = index === selectedIndex;
            return (
              <li key={url}>
                <button
                  type="button"
                  onClick={() => setSelectedIndex(index)}
                  aria-label={`Ver foto ${index + 1} de ${images.length}`}
                  aria-current={isSelected ? "true" : undefined}
                  className={`relative block aspect-4/5 w-full overflow-hidden rounded-soft bg-peach/30 transition-opacity ${
                    isSelected
                      ? "ring-1 ring-charcoal ring-offset-2 ring-offset-cream"
                      : "opacity-60 hover:opacity-100"
                  }`}
                >
                  <Image
                    src={url}
                    alt=""
                    fill
                    sizes="(min-width: 768px) 12vw, 25vw"
                    unoptimized={isPlaceholderImage(url)}
                    className="object-cover"
                  />
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
