import { ChevronLeftIcon, ChevronRightIcon, ChevronsLeftIcon, ChevronsRightIcon } from "lucide-react";
import type { ReactNode } from "react";

import { cn } from "@anaqsup/ui/lib/utils";

import { Link } from "@/i18n/navigation";
import type { StaticPathname } from "@/i18n/routing";
import { getTranslations } from "@/i18n/server";

const WINDOW = 2; // pages affichées de part et d'autre de la page courante

/** Numéros à afficher : 1 … 4 5 [6] 7 8 … 20 ("…" = null). */
function pageNumbers(page: number, totalPages: number): (number | null)[] {
  const pages: (number | null)[] = [];
  for (let i = 0; i < totalPages; i++) {
    const edge = i === 0 || i === totalPages - 1;
    if (edge || Math.abs(i - page) <= WINDOW) pages.push(i);
    else if (pages.at(-1) !== null) pages.push(null);
  }
  return pages;
}

/**
 * Pagination numérotée par liens : indexable, partageable, sans JavaScript. Les filtres restent dans l'URL.
 * `summary` : complément du résumé (« 58 articles au total ») ; `aside` : affiché à droite du résumé
 * (ex. « Aller à la page »).
 */
export async function Pagination({ pathname, params, page, totalPages, summary, aside }: {
  pathname: StaticPathname;
  params: Record<string, string | undefined>;
  page: number;
  totalPages: number;
  summary?: string;
  aside?: ReactNode;
}) {
  const t = await getTranslations("Pagination");
  if (totalPages <= 1) return null;
  const href = (target: number) => {
    const query: Record<string, string> = {};
    for (const [key, value] of Object.entries(params)) if (value) query[key] = value;
    if (target > 0) query.page = String(target);
    return { pathname, query };
  };
  const square = "inline-flex size-10 items-center justify-center rounded-full border bg-background text-sm font-semibold transition-colors";
  const arrow = (target: number, label: string, Icon: typeof ChevronLeftIcon, disabled: boolean) =>
    disabled ? (
      <span className={cn(square, "pointer-events-none opacity-40")} aria-hidden="true"><Icon className="size-4" /></span>
    ) : (
      <Link href={href(target)} aria-label={label} className={cn(square, "hover:border-primary hover:bg-muted")}>
        <Icon className="size-4" aria-hidden="true" />
      </Link>
    );

  return (
    <nav aria-label={t("label")} className="mt-10 flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-muted-foreground">
          {t("pageOf", { page: page + 1, total: totalPages })}
          {summary && ` (${summary})`}
        </p>
        {aside}
      </div>
      <ul className="flex flex-wrap items-center justify-center gap-2">
        <li>{arrow(0, t("first"), ChevronsLeftIcon, page === 0)}</li>
        <li>{arrow(page - 1, t("previous"), ChevronLeftIcon, page === 0)}</li>
        {pageNumbers(page, totalPages).map((number, index) =>
          number === null ? (
            <li key={`gap-${index}`} aria-hidden="true" className="px-1 text-muted-foreground">…</li>
          ) : (
            <li key={number}>
              <Link
                href={href(number)}
                aria-label={t("page", { page: number + 1 })}
                aria-current={number === page ? "page" : undefined}
                className={cn(square, number === page ? "border-primary bg-primary text-primary-foreground shadow-sm" : "hover:border-primary hover:bg-muted")}
              >
                {number + 1}
              </Link>
            </li>
          ),
        )}
        <li>{arrow(page + 1, t("next"), ChevronRightIcon, page + 1 >= totalPages)}</li>
        <li>{arrow(totalPages - 1, t("last"), ChevronsRightIcon, page + 1 >= totalPages)}</li>
      </ul>
    </nav>
  );
}
