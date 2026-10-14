"use client";

import { ArrowUpIcon } from "lucide-react";
import { useEffect, useState } from "react";

import { cx } from "@/lib/cx";

const THRESHOLD_PX = 600;

/** Bouton « revenir en haut », affiché après un défilement d'environ un écran. */
export function BackToTop({ label }: { label: string }) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const onScroll = () => setVisible(window.scrollY > THRESHOLD_PX);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      tabIndex={visible ? 0 : -1}
      onClick={() => window.scrollTo({ top: 0, behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth" })}
      className={cx(
        "fixed right-5 bottom-5 z-40 inline-flex size-12 print:hidden items-center justify-center rounded-full bg-primary text-primary-foreground shadow-lg transition duration-300 hover:scale-110 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary",
        visible ? "visible translate-y-0 opacity-100" : "pointer-events-none invisible translate-y-3 opacity-0",
      )}
    >
      <ArrowUpIcon className="size-5" aria-hidden="true" />
    </button>
  );
}
