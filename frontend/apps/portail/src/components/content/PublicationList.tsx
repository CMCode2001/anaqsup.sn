import type { PublicationKind } from "@anaqsup/api-client";
import { ChevronDownIcon, SearchIcon, XIcon } from "lucide-react";

import { Button } from "@anaqsup/ui/components/button";
import { Input } from "@anaqsup/ui/components/input";
import { cn } from "@anaqsup/ui/lib/utils";

import { AutoSubmitForm } from "@/components/content/AutoSubmitForm";
import { DocumentCard } from "@/components/content/DocumentCard";
import { NativeSelect } from "@/components/content/NativeSelect";
import { PageHeader } from "@/components/content/PageHeader";
import { SectionNav } from "@/components/content/SectionNav";
import { Pagination } from "@/components/registre/Pagination";
import { getPathname, Link } from "@/i18n/navigation";
import type { StaticPathname } from "@/i18n/routing";
import { getLocale, getTranslations } from "@/i18n/server";
import { api } from "@/lib/api";

/** Types de document qui ont leur propre page dans « Ressources documentaires ». */
export const KIND_PAGES = {
  TEXTE_REGLEMENTAIRE: "/ressources-documentaires/textes-reglementaires",
  REFERENTIEL: "/ressources-documentaires/referentiels",
  GUIDE: "/ressources-documentaires/guide-auto-evaluation",
} as const satisfies Partial<Record<PublicationKind, StaticPathname>>;

/** Ordre de la liste « Catégorie » de « Publications » (et du filtre de la recherche). */
export const KINDS = ["TEXTE_REGLEMENTAIRE", "REFERENTIEL", "GUIDE", "RAPPORT_ANNUEL", "ETUDE", "DISCOURS", "AUTRE"] as const satisfies PublicationKind[];

const SIZES = [12, 24, 48] as const;
const DEFAULT_SIZE = 12;

/** Paramètres d'adresse : recherche, catégorie, année, page (à partir de 0), taille, et page saisie (à partir de 1). */
export type PublicationSearchParams = { q?: string; type?: string; year?: string; page?: string; size?: string; goto?: string };

type Filters = { q?: string; type?: string; year?: string; size?: string };

/**
 * Documents d'une rubrique. « Publications » montre TOUS les documents (choix ANAQ-Sup) et propose une
 * liste « Catégorie » ; les autres pages sont limitées à leur type. Recherche, filtres et page sont
 * dans l'adresse : résultats partageables, indexables, utilisables sans JavaScript.
 */
export async function PublicationList({ pathname, title, lead, kind, searchParams }: {
  pathname: "/ressources-documentaires/publications" | (typeof KIND_PAGES)[keyof typeof KIND_PAGES];
  title: string;
  lead: string;
  kind?: keyof typeof KIND_PAGES;
  searchParams: PublicationSearchParams;
}) {
  const locale = await getLocale();
  const t = await getTranslations();

  const q = searchParams.q?.trim().slice(0, 100) || undefined;
  const type = kind ? undefined : KINDS.find((k) => k === searchParams.type);
  const year = /^\d{4}$/.test(searchParams.year ?? "") ? Number(searchParams.year) : undefined;
  const size = SIZES.find((s) => String(s) === searchParams.size) ?? DEFAULT_SIZE;
  const goto = Number.parseInt(searchParams.goto ?? "", 10);
  const page = goto > 0 ? goto - 1 : Math.max(0, Number.parseInt(searchParams.page ?? "0", 10) || 0);

  const [result, years] = await Promise.all([
    api.publications({ lang: locale, q, kind: kind ?? type, year, page, size }),
    api.publicationYears(kind ?? type),
  ]);

  const action = getPathname({ locale, href: pathname });
  const filters: Filters = { q, type, year: year ? String(year) : undefined, size: size === DEFAULT_SIZE ? undefined : String(size) };
  // Les champs de formulaire non modifiés gardent les autres filtres ; la clé remonte les champs à chaque changement.
  const formKey = JSON.stringify(filters);
  const hidden = (...omit: (keyof Filters)[]) =>
    Object.entries(filters)
      .filter(([name, value]) => value && !omit.includes(name as keyof Filters))
      .map(([name, value]) => <input key={name} type="hidden" name={name} value={value} />);
  const href = (changes: Partial<Filters>) => {
    const query = Object.fromEntries(Object.entries({ ...filters, ...changes }).filter((entry): entry is [string, string] => !!entry[1]));
    return { pathname, query };
  };

  const total = result?.page.totalElements ?? 0;
  const totalPages = result?.page.totalPages ?? 0;
  const from = total === 0 ? 0 : page * size + 1;
  const to = Math.min(total, (page + 1) * size);
  const chips = [
    q && { name: "q" as const, label: t("Resources.chipQuery", { q }) },
    type && { name: "type" as const, label: t("Resources.chipCategory", { label: t(`PublicationKinds.${type}`) }) },
    year && { name: "year" as const, label: t("Resources.chipYear", { year: String(year) }) },
  ].filter((chip) => !!chip);

  return (
    <>
      <PageHeader eyebrow={t("Resources.eyebrow")} title={title} lead={lead}>
        <SectionNav group="resources" current={pathname} />
      </PageHeader>

      <div className="mx-auto max-w-7xl px-4 py-10">
        <AutoSubmitForm
          key={formKey}
          action={action}
          role="search"
          aria-label={t("Resources.filters")}
          className={cn(
            "grid gap-3 rounded-box border bg-card p-4 shadow-sm",
            kind ? "md:grid-cols-[minmax(0,2fr)_minmax(0,1fr)_auto]" : "md:grid-cols-[minmax(0,2fr)_minmax(0,1fr)_minmax(0,1fr)_auto]",
          )}
        >
          {hidden("q", "type", "year")}
          <div className="relative">
            <label htmlFor="document-search" className="sr-only">{t("Resources.searchLabel")}</label>
            <SearchIcon className="pointer-events-none absolute top-1/2 left-4 size-5 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
            <Input
              id="document-search"
              name="q"
              type="search"
              maxLength={100}
              defaultValue={q}
              placeholder={t("Resources.searchPlaceholder")}
              autoComplete="off"
              className="h-12 rounded-full! bg-background pl-12 text-base"
            />
          </div>
          {!kind && (
            <NativeSelect pill id="document-type" name="type" label={t("Resources.category")} defaultValue={type ?? ""}>
              <option value="">{t("Resources.allCategories")}</option>
              {KINDS.map((value) => <option key={value} value={value}>{t(`PublicationKinds.${value}`)}</option>)}
            </NativeSelect>
          )}
          <NativeSelect pill id="document-year" name="year" label={t("Resources.year")} defaultValue={year ? String(year) : ""}>
            <option value="">{t("Resources.allYears")}</option>
            {/* L'année filtrée reste proposée même si aucun document de la catégorie choisie n'en porte. */}
            {[...new Set([...(years ?? []), ...(year ? [year] : [])])].sort((a, b) => b - a).map((value) => (
              <option key={value} value={value}>{value}</option>
            ))}
          </NativeSelect>
          <Button type="submit" size="lg" className="h-12 rounded-full!">{t("Resources.submit")}</Button>
        </AutoSubmitForm>

        {chips.length > 0 && (
          <div role="group" aria-label={t("Resources.activeFilters")} className="mt-5 flex flex-wrap items-center gap-2">
            {chips.map((chip) => (
              <Link
                key={chip.name}
                href={href({ [chip.name]: undefined })}
                aria-label={t("Resources.removeFilter", { label: chip.label })}
                className="inline-flex items-center gap-1.5 rounded-full border border-primary/50 bg-primary/10 px-3.5 py-1.5 text-sm font-medium text-brand-ink transition-colors hover:bg-primary/20"
              >
                {chip.label}
                <XIcon className="size-3.5" aria-hidden="true" />
              </Link>
            ))}
            <Link href={href({ q: undefined, type: undefined, year: undefined })} className="px-2 text-sm font-medium text-muted-foreground underline-offset-4 hover:text-brand-ink hover:underline">
              {t("Resources.clearAll")}
            </Link>
          </div>
        )}

        <div className="mt-6 flex flex-wrap items-center justify-between gap-4 text-sm text-muted-foreground">
          <p aria-live="polite" className="font-medium text-foreground">{t("Resources.found", { count: total })}</p>
          {total > 0 && (
            <div className="flex flex-wrap items-center gap-4">
              <AutoSubmitForm key={formKey} action={action} className="flex items-center gap-2">
                {hidden("size")}
                <label htmlFor="document-size">{t("Resources.perPage")}</label>
                <div className="relative">
                  <select
                    id="document-size"
                    name="size"
                    defaultValue={String(size)}
                    className="h-9 cursor-pointer appearance-none rounded-full border bg-background pr-8 pl-4 text-sm text-foreground outline-none hover:border-primary focus-visible:ring-3 focus-visible:ring-ring/40"
                  >
                    {SIZES.map((value) => <option key={value} value={value}>{value}</option>)}
                  </select>
                  <ChevronDownIcon className="pointer-events-none absolute top-1/2 right-3 size-3.5 -translate-y-1/2" aria-hidden="true" />
                </div>
                <noscript>
                  <button type="submit" className="h-9 rounded-full border px-3 font-semibold text-foreground">{t("Resources.apply")}</button>
                </noscript>
              </AutoSubmitForm>
              <span>{t("Resources.showing", { from, to, total })}</span>
            </div>
          )}
        </div>

        {result && result.content.length > 0 ? (
          <>
            <ul className="mt-6 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {result.content.map((publication) => <li key={publication.slug}><DocumentCard publication={publication} /></li>)}
            </ul>
            <Pagination
              pathname={pathname}
              params={filters}
              page={page}
              totalPages={totalPages}
              summary={t("Resources.total", { count: total })}
              aside={
                <AutoSubmitForm key={formKey} action={action} className="flex items-center gap-2 text-sm text-muted-foreground">
                  {hidden()}
                  <label htmlFor="document-goto">{t("Resources.goToPage")}</label>
                  <input
                    id="document-goto"
                    name="goto"
                    type="number"
                    inputMode="numeric"
                    min={1}
                    max={totalPages}
                    defaultValue={page + 1}
                    className="h-9 w-20 rounded-full border bg-background px-3 text-center text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/40"
                  />
                  <button type="submit" className="h-9 rounded-full border px-4 font-semibold text-foreground transition-colors hover:border-primary hover:bg-muted">
                    {t("Resources.go")}
                  </button>
                </AutoSubmitForm>
              }
            />
          </>
        ) : (
          <p className="mt-6 rounded-box bg-muted p-6">{chips.length > 0 ? t("Resources.emptyFiltered") : t("Resources.empty")}</p>
        )}
      </div>
    </>
  );
}
