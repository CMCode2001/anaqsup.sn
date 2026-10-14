"use client";

import { ArrowRightIcon, ChevronDownIcon } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { cx } from "@/lib/cx";

export type ResolvedLink = { label: string; href: string };
export type ResolvedGroup = { id: string; label: string; href?: string; children?: ResolvedLink[]; section: string[] };

const CLOSE_DELAY_MS = 150;

const within = (pathname: string, href: string) => pathname === href || pathname.startsWith(`${href}/`);

/**
 * Menu principal (écran large) : sous-menus en carte, ouverts au survol de la souris, au clic, au
 * toucher ou au clavier (Entrée/Espace), fermés par Échap, un clic à l'extérieur ou en quittant la rubrique.
 * Schéma « disclosure » du W3C : bouton + aria-expanded, liens ordinaires dans le panneau.
 * Libellés et adresses arrivent déjà traduits du serveur : aucune bibliothèque i18n côté navigateur.
 */
export function DesktopNav({ label, groups }: { label: string; groups: ResolvedGroup[] }) {
  const pathname = usePathname();
  // Mémorise la page où le menu a été ouvert : changer de page le referme, sans effet de bord.
  const [state, setState] = useState<{ id: string; pathname: string } | null>(null);
  const open = state?.pathname === pathname ? state.id : null;
  const setOpen = (id: string | null) => setState(id ? { id, pathname } : null);

  const navRef = useRef<HTMLElement>(null);
  const closeTimer = useRef<number | undefined>(undefined);

  useEffect(() => {
    if (!open) return;
    const onPointerDown = (event: PointerEvent) => {
      if (!navRef.current?.contains(event.target as Node)) setState(null);
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      setState(null);
      document.getElementById(`${open}-trigger`)?.focus();
    };
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  return (
    <nav ref={navRef} aria-label={label} className="hidden xl:block">
      <ul className="flex items-center gap-0.5">
        {groups.map((group, index) => {
          const active = [group.href, ...(group.children ?? []).map((c) => c.href), ...group.section]
            .some((href) => href && within(pathname, href));
          const itemClass = cx(
            "inline-flex h-9 items-center gap-1 rounded-full px-3.5 text-[13px] font-medium transition-colors hover:bg-muted hover:text-brand-ink",
            active && "font-semibold text-brand-ink",
          );

          if (!group.children) {
            return (
              <li key={group.id}>
                <Link href={group.href!} aria-current={pathname === group.href ? "page" : undefined} className={cx(itemClass, active && "bg-muted")}>
                  {group.label}
                </Link>
              </li>
            );
          }

          const isOpen = open === group.id;
          const alignRight = index >= groups.length / 2;
          return (
            <li
              key={group.id}
              data-nav-group
              className="relative"
              onPointerEnter={(event) => {
                if (event.pointerType !== "mouse") return;
                window.clearTimeout(closeTimer.current);
                setOpen(group.id);
              }}
              onPointerLeave={(event) => {
                if (event.pointerType !== "mouse") return;
                closeTimer.current = window.setTimeout(() => setState(null), CLOSE_DELAY_MS);
              }}
              onBlur={(event) => {
                if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
                  setState((current) => (current?.id === group.id ? null : current));
                }
              }}
            >
              <button
                id={`${group.id}-trigger`}
                type="button"
                aria-expanded={isOpen}
                aria-controls={`${group.id}-panel`}
                onClick={() => setOpen(isOpen ? null : group.id)}
                className={cx(itemClass, (isOpen || active) && "bg-muted")}
              >
                {group.label}
                <ChevronDownIcon className={cx("size-4 transition-transform duration-200", isOpen && "rotate-180")} aria-hidden="true" />
              </button>

              <div
                id={`${group.id}-panel`}
                data-nav-panel
                className={cx(
                  "absolute top-full z-50 w-[36rem] pt-3 transition duration-200 ease-out",
                  alignRight ? "right-0" : "left-0",
                  isOpen ? "visible translate-y-0 opacity-100" : "pointer-events-none invisible -translate-y-1 opacity-0",
                )}
              >
                {/* Pointe de la carte, sous la rubrique. */}
                <span
                  aria-hidden="true"
                  className={cx("absolute top-1.5 size-3 rotate-45 rounded-[3px] border-t border-l bg-background", alignRight ? "right-8" : "left-8")}
                />
                <ul className="grid grid-cols-2 gap-1 rounded-3xl border bg-background p-3 shadow-xl">
                  {group.children.map((child) => (
                    <li key={child.href}>
                      <Link
                        href={child.href}
                        aria-current={pathname === child.href ? "page" : undefined}
                        onClick={() => setState(null)}
                        className="group/item flex items-center justify-between gap-3 rounded-2xl px-4 py-3.5 text-sm font-medium transition-colors hover:bg-muted hover:text-brand-ink aria-[current=page]:bg-muted aria-[current=page]:font-semibold aria-[current=page]:text-brand-ink"
                      >
                        {child.label}
                        <ArrowRightIcon
                          className="size-4 shrink-0 -translate-x-1 text-primary opacity-0 transition duration-200 group-hover/item:translate-x-0 group-hover/item:opacity-100"
                          aria-hidden="true"
                        />
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
