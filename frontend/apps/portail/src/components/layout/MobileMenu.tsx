"use client";

import { ChevronDownIcon, MailIcon, MenuIcon, PhoneIcon, XIcon } from "lucide-react";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import type { ResolvedGroup } from "@/components/layout/DesktopNav";
import { cx } from "@/lib/cx";

const CLOSE_MS = 250;

const within = (pathname: string, href: string) => pathname === href || pathname.startsWith(`${href}/`);

/**
 * Menu mobile en tiroir (demande ANAQ-Sup du 15/10/2026) : il glisse de la droite sous l'en-tête, voile et floute la
 * page ; ☰ devient ✕. C'est un <details> : il s'ouvre et se ferme sans JavaScript. Avec, la fermeture glisse aussi,
 * et se fait par Échap, un toucher sur le voile ou un changement de page ; la page ne défile plus derrière.
 */
export function MobileMenu({ groups, contact, labels }: {
  groups: ResolvedGroup[];
  contact: { phone: string; phoneHref: string; email: string };
  labels: { menu: string; close: string; nav: string };
}) {
  const pathname = usePathname();
  const detailsRef = useRef<HTMLDetailsElement>(null);
  const [open, setOpen] = useState(false);
  const [closing, setClosing] = useState(false);

  const close = () => {
    const details = detailsRef.current;
    if (!details?.open) return;
    // Animation de sortie (250 ms), puis fermeture réelle au bout de sa durée : un délai fixe plutôt que la fin de
    // l'animation, qui ne se produit pas si le navigateur suspend les animations (onglet en arrière-plan).
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      details.open = false;
      return;
    }
    setClosing(true);
    window.setTimeout(() => (details.open = false), CLOSE_MS);
  };

  // Ouvert : page figée, haut du tiroir calé sous l'en-tête, Échap pour fermer.
  useEffect(() => {
    if (!open) return;
    const header = detailsRef.current?.closest("header");
    if (header) document.documentElement.style.setProperty("--menu-top", `${Math.round(header.getBoundingClientRect().bottom)}px`);
    const overflow = document.documentElement.style.overflow;
    document.documentElement.style.overflow = "hidden";
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") close();
    };
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.documentElement.style.overflow = overflow;
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  // Changement de page : fermeture immédiate.
  useEffect(() => {
    if (detailsRef.current) detailsRef.current.open = false;
  }, [pathname]);

  return (
    <details
      ref={detailsRef}
      onToggle={(event) => {
        setOpen(event.currentTarget.open);
        if (!event.currentTarget.open) setClosing(false);
      }}
      className="group xl:hidden"
    >
      <summary
        onClick={(event) => {
          // Déjà ouvert : on joue l'animation de sortie au lieu de fermer d'un coup.
          if (detailsRef.current?.open) {
            event.preventDefault();
            close();
          }
        }}
        aria-label={open ? labels.close : labels.menu}
        className="relative z-[60] flex size-9.5 shrink-0 cursor-pointer list-none items-center justify-center rounded-control border text-brand-ink transition-colors hover:border-primary hover:bg-muted group-open:border-primary group-open:bg-primary/15 [&::-webkit-details-marker]:hidden"
      >
        <MenuIcon className="size-[18px] group-open:hidden" aria-hidden="true" />
        <XIcon className="hidden size-[18px] group-open:block" aria-hidden="true" />
      </summary>

      {/* Voile : la page sous l'en-tête, assombrie et floutée. */}
      <div
        aria-hidden="true"
        onClick={close}
        className={cx(
          "fixed inset-x-0 bottom-0 top-[var(--menu-top,6.75rem)] z-50 bg-anthracite/55 backdrop-blur-[2px] duration-300 motion-reduce:animate-none",
          closing ? "animate-out fade-out fill-mode-forwards" : "animate-in fade-in",
        )}
      />

      <nav
        aria-label={labels.nav}
        className={cx(
          "fixed right-0 bottom-0 top-[var(--menu-top,6.75rem)] z-50 flex w-[86%] max-w-sm flex-col bg-background shadow-[-24px_0_60px_-20px_rgb(0_0_0/0.35)] ease-out motion-reduce:animate-none",
          closing ? "animate-out slide-out-to-right fill-mode-forwards duration-250" : "animate-in slide-in-from-right duration-350",
        )}
      >
        {/* Le logotype reste visible dans l'en-tête, juste au-dessus : un intitulé discret suffit ici. */}
        <div aria-hidden="true" className="flex items-center gap-3 px-7 pt-6 pb-2">
          <span className="text-xs font-bold tracking-[0.2em] text-muted-foreground uppercase">{labels.menu}</span>
          <span className="h-0.5 flex-1 bg-gradient-to-r from-primary to-transparent" />
        </div>

        <ul className="flex-1 overflow-y-auto overscroll-contain px-3 py-4">
          {groups.map((group, index) => {
            const active = [group.href, ...(group.children ?? []).map((c) => c.href), ...group.section].some((href) => href && within(pathname, href));
            // Entrée échelonnée des rubriques (décalage de 40 ms chacune).
            const stagger = { animationDelay: `${120 + index * 40}ms` };
            const item = cx(
              "flex w-full items-center justify-between rounded-control px-4 py-3.5 text-[17px] font-semibold transition-colors hover:bg-muted",
              active ? "bg-primary/10 text-brand-ink" : "text-foreground",
            );
            return (
              <li key={group.id} style={stagger} className="animate-in fade-in slide-in-from-right-4 fill-mode-backwards duration-300 motion-reduce:animate-none">
                {group.children ? (
                  <details className="group/sub" open={active}>
                    <summary className={cx(item, "cursor-pointer list-none [&::-webkit-details-marker]:hidden")}>
                      {group.label}
                      <ChevronDownIcon className="size-4 text-muted-foreground transition-transform duration-300 group-open/sub:rotate-180" aria-hidden="true" />
                    </summary>
                    <ul className="mt-1 mb-2 ml-5 flex flex-col gap-0.5 border-l-2 border-primary/40 pl-3">
                      {group.children.map((child) => (
                        <li key={child.href}>
                          <a
                            href={child.href}
                            aria-current={pathname === child.href ? "page" : undefined}
                            className="block rounded-control px-3 py-2.5 text-[15px] text-muted-foreground transition-colors hover:bg-muted hover:text-brand-ink aria-[current=page]:font-semibold aria-[current=page]:text-brand-ink"
                          >
                            {child.label}
                          </a>
                        </li>
                      ))}
                    </ul>
                  </details>
                ) : (
                  <a href={group.href} aria-current={pathname === group.href ? "page" : undefined} className={item}>
                    {group.label}
                  </a>
                )}
              </li>
            );
          })}
        </ul>

        <div className="grid gap-2 border-t bg-muted/40 px-5 py-5 text-sm">
          <a href={contact.phoneHref} className="inline-flex items-center gap-3 font-medium text-foreground hover:text-brand-ink">
            <span className="flex size-9 items-center justify-center rounded-control bg-primary/15 text-brand-ink"><PhoneIcon className="size-4" aria-hidden="true" /></span>
            {contact.phone}
          </a>
          <a href={`mailto:${contact.email}`} className="inline-flex items-center gap-3 font-medium text-foreground hover:text-brand-ink">
            <span className="flex size-9 items-center justify-center rounded-control bg-primary/15 text-brand-ink"><MailIcon className="size-4" aria-hidden="true" /></span>
            {contact.email}
          </a>
        </div>
      </nav>
    </details>
  );
}
