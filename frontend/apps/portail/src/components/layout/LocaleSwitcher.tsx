"use client";

import { CheckIcon, ChevronDownIcon, GlobeIcon } from "lucide-react";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { alternatePath } from "@/i18n/alternate";
import { LOCALE_NAMES, type Locale, routing } from "@/i18n/routing";
import { cx } from "@/lib/cx";

/**
 * Sélecteur de langue du bandeau supérieur, après les réseaux sociaux.
 * Chaque langue est un vrai lien (indexable, utilisable sans JavaScript) vers la même page traduite.
 */
export function LocaleSwitcher({ locale, label }: { locale: Locale; label: string }) {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onPointerDown = (event: PointerEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false);
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  return (
    <div ref={rootRef} data-nav-group className="relative">
      <button
        type="button"
        aria-expanded={open}
        aria-controls="langues"
        aria-label={label}
        onClick={() => setOpen(!open)}
        className="inline-flex h-7 items-center gap-1.5 rounded-full px-2.5 font-medium text-white transition-colors hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
      >
        <GlobeIcon className="size-4 text-primary" aria-hidden="true" />
        <span className="hidden sm:inline">{LOCALE_NAMES[locale]}</span>
        <span className="uppercase sm:hidden">{locale}</span>
        <ChevronDownIcon className={cx("size-3.5 transition-transform duration-200", open && "rotate-180")} aria-hidden="true" />
      </button>
      <ul
        id="langues"
        data-nav-panel
        className={cx(
          "absolute top-full right-0 z-50 mt-2 w-40 rounded-2xl border bg-background p-1.5 text-foreground shadow-xl transition duration-200 ease-out",
          open ? "visible translate-y-0 opacity-100" : "pointer-events-none invisible -translate-y-1 opacity-0",
        )}
      >
        {routing.locales.map((target) => (
          <li key={target}>
            <a
              href={alternatePath(pathname, target)}
              hrefLang={target}
              lang={target}
              aria-current={target === locale ? "true" : undefined}
              className="flex items-center justify-between rounded-xl px-3 py-2 text-sm font-medium hover:bg-muted aria-[current=true]:text-brand-ink"
            >
              {LOCALE_NAMES[target]}
              {target === locale && <CheckIcon className="size-4 text-primary" aria-hidden="true" />}
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}
