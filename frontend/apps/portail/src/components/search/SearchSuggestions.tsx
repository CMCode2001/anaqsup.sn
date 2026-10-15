"use client";

import {
  ArrowRightIcon,
  CalendarDaysIcon,
  FileTextIcon,
  GraduationCapIcon,
  LandmarkIcon,
  LoaderCircleIcon,
  NewspaperIcon,
  PanelsTopLeftIcon,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useId, useRef, useState } from "react";

import type { Suggestion } from "@/app/api/search/route";
import { cx } from "@/lib/cx";

const DEBOUNCE_MS = 200;

const ICONS: Record<string, typeof FileTextIcon> = {
  news: NewspaperIcon,
  events: CalendarDaysIcon,
  publications: FileTextIcon,
  establishments: LandmarkIcon,
  programs: GraduationCapIcon,
  pages: PanelsTopLeftIcon,
};

export type SuggestionLabels = { suggestions: string; loading: string; empty: string; seeAll: string };

type State = { status: "idle" | "loading" | "done"; items: Suggestion[] };

/**
 * Suggestions sous le grand champ de la page de recherche (schéma « combobox » du W3C) : flèches haut et bas pour
 * parcourir, Entrée pour ouvrir, Échap ou un clic ailleurs pour fermer. Le champ est rendu par le serveur
 * (formulaire utilisable sans JavaScript) : ce module, chargé à la première frappe, s'y branche et lui ajoute ses
 * attributs ARIA.
 */
export default function SearchSuggestions({ inputId, lang, labels }: { inputId: string; lang: string; labels: SuggestionLabels }) {
  const listId = useId();
  const router = useRouter();
  const containerRef = useRef<HTMLDivElement>(null);
  const [state, setState] = useState<State>({ status: "idle", items: [] });
  const [active, setActive] = useState(-1);
  const [dismissed, setDismissed] = useState(false);
  const items = state.items;
  const visible = !dismissed && state.status !== "idle";
  const field = () => document.getElementById(inputId) as HTMLInputElement | null;

  const openSuggestion = (item: Suggestion) => {
    setDismissed(true);
    if (item.newTab) window.open(item.href, "_blank", "noopener");
    else router.push(item.href);
  };

  // Requête après une courte pause de frappe ; la précédente est abandonnée.
  useEffect(() => {
    const input = document.getElementById(inputId) as HTMLInputElement | null;
    if (!input) return;
    let timer: number | undefined;
    let controller: AbortController | undefined;
    const load = () => {
      window.clearTimeout(timer);
      controller?.abort();
      setActive(-1);
      setDismissed(false);
      const query = input.value.trim();
      if (query.length < 2) {
        setState({ status: "idle", items: [] });
        return;
      }
      setState((previous) => ({ ...previous, status: "loading" }));
      timer = window.setTimeout(async () => {
        controller = new AbortController();
        try {
          const response = await fetch(`/api/search?${new URLSearchParams({ q: query, lang })}`, { signal: controller.signal });
          if (!response.ok) throw new Error(`HTTP ${response.status}`);
          setState({ status: "done", items: ((await response.json()) as { items: Suggestion[] }).items });
        } catch (error) {
          // Recherche indisponible : le formulaire reste utilisable.
          if ((error as Error).name !== "AbortError") setState({ status: "idle", items: [] });
        }
      }, DEBOUNCE_MS);
    };
    // Validation du formulaire, clic ailleurs : la liste se ferme.
    const close = () => setDismissed(true);
    const onPointerDown = (event: PointerEvent) => {
      const target = event.target as Node;
      if (!containerRef.current?.contains(target) && !input.contains(target)) close();
    };
    input.addEventListener("input", load);
    input.form?.addEventListener("submit", close);
    document.addEventListener("pointerdown", onPointerDown);
    load(); // texte saisi avant le chargement de ce module
    return () => {
      input.removeEventListener("input", load);
      input.form?.removeEventListener("submit", close);
      document.removeEventListener("pointerdown", onPointerDown);
      window.clearTimeout(timer);
      controller?.abort();
    };
  }, [inputId, lang]);

  // Attributs ARIA du champ et clavier : flèches, Entrée sur une suggestion, Échap.
  useEffect(() => {
    const input = document.getElementById(inputId) as HTMLInputElement | null;
    if (!input) return;
    const expanded = visible && items.length > 0;
    input.setAttribute("role", "combobox");
    input.setAttribute("aria-autocomplete", "list");
    input.setAttribute("aria-controls", listId);
    input.setAttribute("aria-expanded", String(expanded));
    if (expanded && active >= 0) input.setAttribute("aria-activedescendant", `${listId}-${active}`);
    else input.removeAttribute("aria-activedescendant");

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && visible) {
        event.preventDefault();
        setDismissed(true);
        return;
      }
      if (!expanded) return;
      if (event.key === "ArrowDown") {
        event.preventDefault();
        setActive((index) => (index + 1) % items.length);
      } else if (event.key === "ArrowUp") {
        event.preventDefault();
        setActive((index) => (index <= 0 ? items.length - 1 : index - 1));
      } else if (event.key === "Enter" && active >= 0) {
        event.preventDefault();
        openSuggestion(items[active]!);
      }
    };
    input.addEventListener("keydown", onKeyDown);
    return () => input.removeEventListener("keydown", onKeyDown);
  });

  const status = !visible ? "" : state.status === "loading" ? labels.loading : items.length ? labels.suggestions : labels.empty;

  return (
    <>
      <p role="status" className="sr-only">{status}</p>
      {visible && (
        <div ref={containerRef} className="absolute inset-x-0 top-full z-30 mt-2 overflow-hidden rounded-3xl border bg-card text-foreground shadow-xl">
          {items.length > 0 ? (
            <ul id={listId} role="listbox" aria-label={labels.suggestions} className={cx("divide-y transition-opacity", state.status === "loading" && "opacity-60")}>
              {items.map((item, index) => {
                const Icon = ICONS[item.type] ?? FileTextIcon;
                return (
                  <li key={`${item.type}-${item.href}`} role="presentation">
                    <Link
                      id={`${listId}-${index}`}
                      role="option"
                      aria-selected={index === active}
                      href={item.href}
                      prefetch={false}
                      tabIndex={-1}
                      target={item.newTab ? "_blank" : undefined}
                      rel={item.newTab ? "noopener" : undefined}
                      onPointerMove={() => setActive(index)}
                      onClick={() => setDismissed(true)}
                      className={cx("flex items-center gap-4 px-5 py-3 transition-colors", index === active && "bg-muted")}
                    >
                      <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary/15 text-brand-ink">
                        <Icon className="size-[18px]" aria-hidden="true" />
                      </span>
                      <span className="min-w-0 flex-1">
                        <span lang={item.lang === lang ? undefined : item.lang} className="block truncate font-semibold text-brand-ink">{item.title}</span>
                        <span className="block truncate text-sm text-muted-foreground">
                          {item.typeLabel}
                          {item.detail && <> <span aria-hidden="true">·</span> {item.detail}</>}
                        </span>
                      </span>
                    </Link>
                  </li>
                );
              })}
            </ul>
          ) : state.status === "loading" ? (
            <p className="flex items-center gap-2 px-5 py-4 text-sm text-muted-foreground">
              <LoaderCircleIcon className="size-4 animate-spin" aria-hidden="true" /> {labels.loading}
            </p>
          ) : (
            <p className="px-5 py-4 text-sm text-muted-foreground">{labels.empty}</p>
          )}
          {items.length > 0 && (
            // Mêmes filtres que la page : on valide le formulaire plutôt que de suivre un lien.
            <button
              type="button"
              onClick={() => field()?.form?.requestSubmit()}
              className="flex w-full items-center justify-between gap-3 border-t bg-muted/50 px-5 py-3 text-left text-sm font-semibold text-brand-ink transition-colors hover:bg-muted"
            >
              {labels.seeAll}
              <ArrowRightIcon className="size-4" aria-hidden="true" />
            </button>
          )}
        </div>
      )}
    </>
  );
}
