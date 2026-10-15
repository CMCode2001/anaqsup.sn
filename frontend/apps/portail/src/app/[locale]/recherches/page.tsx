import type { Metadata } from "next";
import { ArrowRightIcon, SearchIcon, SearchXIcon } from "lucide-react";

import { AutoSubmitForm } from "@/components/content/AutoSubmitForm";
import { NativeSelect } from "@/components/content/NativeSelect";
import { KINDS } from "@/components/content/PublicationList";
import { Pagination } from "@/components/registre/Pagination";
import { EstablishmentCard, ProgramCard } from "@/components/registre/RegistryCards";
import { LiveSuggestions } from "@/components/search/LiveSuggestions";
import { ResultCard } from "@/components/search/ResultCard";
import { getPathname, Link } from "@/i18n/navigation";
import { getLocale, getTranslations } from "@/i18n/server";
import { api } from "@/lib/api";
import { hitLink } from "@/lib/search";

// Rendue à chaque requête : le texte recherché et les filtres sont dans l'adresse.
export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Search");
  return { title: t("metaTitle"), robots: { index: false, follow: true } };
}

/** Filtre « Type de contenu » ; « registry » réunit établissements et formations (champs « Vérifier »). */
const TYPES = ["news", "events", "publications", "registry", "pages"] as const;

/** Premiers documents repris de l'ancien site. */
const FIRST_YEAR = 2012;
const PAGE_SIZE = 10;
/** Limite de l'API (pages 0 à 49). */
const MAX_PAGES = 50;
const SLUG = /^[a-z0-9]+(-[a-z0-9]+)*$/;

type Params = { q?: string; type?: string; period?: string; category?: string; kind?: string; page?: string };
type Props = { searchParams: Promise<Params> };

/**
 * Recherche sur tout le site (décision ANAQ-Sup du 14/10/2026, sur le modèle de la page du MESRI) : une seule
 * liste, tous types confondus, filtrable par type, période, rubrique ou type de document. Le filtre
 * « Habilitations & accréditations » garde la présentation de la vérification (établissements, puis formations).
 */
export default async function RecherchePage({ searchParams }: Props) {
  const params = await searchParams;
  const locale = await getLocale();
  const t = await getTranslations();

  const q = (params.q ?? "").trim().slice(0, 100);
  const type = TYPES.find((value) => value === params.type);
  // Filtres sans objet pour le type choisi (pages et registre sans date, rubrique des seules actualités…) : ignorés.
  const dated = type === undefined || type === "news" || type === "events" || type === "publications";
  const period = dated && /^(month|year|(19|20)\d{2})$/.test(params.period ?? "") ? params.period : undefined;
  const category = type === "news" && SLUG.test(params.category ?? "") ? params.category : undefined;
  const kind = type === "publications" ? KINDS.find((value) => value === params.kind) : undefined;
  const page = Math.min(MAX_PAGES - 1, Math.max(0, Number.parseInt(params.page ?? "0", 10) || 0));
  const ready = q.length >= 2;

  const [categories, results, registry] = await Promise.all([
    type === "news" ? api.newsCategories(locale) : null,
    ready && type !== "registry"
      ? api.search({ q, lang: locale, type, period, category, kind, page, size: PAGE_SIZE })
      : null,
    ready && type === "registry"
      ? Promise.all([api.establishments({ q, size: 6 }), api.programs({ q, size: 6 })])
      : null,
  ]);
  const [establishments, programs] = registry ?? [null, null];

  const total = registry
    ? (establishments?.page.totalElements ?? 0) + (programs?.page.totalElements ?? 0)
    : (results?.page.totalElements ?? 0);
  const totalPages = Math.min(results?.page.totalPages ?? 0, MAX_PAGES);
  const filtered = Boolean(type || period);
  const action = getPathname({ locale, href: "/recherches" });
  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: currentYear - FIRST_YEAR + 1 }, (_, index) => currentYear - index);
  // Les champs non modifiés gardent les autres filtres ; la clé remonte le formulaire à chaque changement.
  const formKey = JSON.stringify({ q, type, period, category, kind });

  return (
    <>
      <header className="relative bg-anthracite text-white">
        <div aria-hidden="true" className="pointer-events-none absolute inset-0 bg-[radial-gradient(50%_90%_at_85%_10%,color-mix(in_oklch,var(--primary)_28%,transparent),transparent)]" />
        <div className="relative mx-auto max-w-7xl px-4 py-10 md:py-14">
          <h1 className="font-heading text-4xl font-extrabold md:text-5xl">{t("Search.pageTitle")}</h1>

          <AutoSubmitForm key={formKey} action={action} role="search" aria-label={t("Search.filters")} className="mt-6 md:mt-8">
            <div className="relative">
              {/* Barre en pilule (exception voulue par l'ANAQ-Sup à la règle « jamais de pilule ») : loupe, champ, bouton. */}
              <div className="flex items-center gap-2 rounded-full bg-background p-1.5 pl-5 shadow-xl shadow-black/25 ring-1 ring-white/10 transition-shadow focus-within:ring-4 focus-within:ring-primary/45 md:gap-3 md:p-2 md:pl-6">
                <label htmlFor="search-page-q" className="sr-only">{t("Search.siteLabel")}</label>
                <SearchIcon className="hidden size-6 shrink-0 text-muted-foreground md:block" aria-hidden="true" />
                <input
                  id="search-page-q"
                  name="q"
                  type="search"
                  required
                  minLength={2}
                  maxLength={100}
                  defaultValue={q}
                  placeholder={t("Search.sitePlaceholder")}
                  autoComplete="off"
                  // Arrivée par la loupe du menu : on peut taper tout de suite.
                  autoFocus={!q}
                  className="h-11 min-w-0 flex-1 bg-transparent text-base text-foreground outline-none placeholder:text-muted-foreground md:h-12 md:text-lg [&::-webkit-search-cancel-button]:hidden"
                />
                <button
                  type="submit"
                  className="inline-flex size-11 shrink-0 items-center justify-center gap-2 rounded-full bg-primary font-semibold text-primary-foreground shadow-sm transition-colors hover:bg-primary/90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white md:h-12 md:w-auto md:px-8 md:text-lg"
                >
                  {/* Téléphone et petite tablette : loupe seule, pour laisser la place au texte saisi. */}
                  <SearchIcon className="size-5 md:hidden" aria-hidden="true" />
                  <span className="sr-only md:not-sr-only">{t("Search.submit")}</span>
                </button>
              </div>
              <LiveSuggestions
                inputId="search-page-q"
                lang={locale}
                labels={{
                  suggestions: t("Search.suggestions"),
                  loading: t("Search.loading"),
                  empty: t("Search.noSuggestion"),
                  seeAll: t("Search.seeAllSuggestions"),
                }}
              />
            </div>

            <div className="mt-4 flex flex-wrap gap-3 text-foreground [&>*]:w-full sm:[&>*]:w-auto sm:[&>*]:min-w-56">
              <NativeSelect pill id="search-type" name="type" label={t("Search.filterType")} defaultValue={type ?? ""}>
                <option value="">{t("Search.allTypes")}</option>
                {TYPES.map((value) => <option key={value} value={value}>{t(`Search.filter.${value}`)}</option>)}
              </NativeSelect>
              {dated && (
                <NativeSelect pill id="search-period" name="period" label={t("Search.filterPeriod")} defaultValue={period ?? ""}>
                  <option value="">{t("Search.allDates")}</option>
                  <option value="month">{t("Search.lastMonth")}</option>
                  <option value="year">{t("Search.lastYear")}</option>
                  {years.map((year) => <option key={year} value={year}>{year}</option>)}
                </NativeSelect>
              )}
              {type === "news" && (
                <NativeSelect pill id="search-category" name="category" label={t("Search.filterCategory")} defaultValue={category ?? ""}>
                  <option value="">{t("Search.allCategories")}</option>
                  {(categories ?? []).map((c) => <option key={c.slug} value={c.slug}>{c.label}</option>)}
                </NativeSelect>
              )}
              {type === "publications" && (
                <NativeSelect pill id="search-kind" name="kind" label={t("Search.filterKind")} defaultValue={kind ?? ""}>
                  <option value="">{t("Search.allKinds")}</option>
                  {KINDS.map((value) => <option key={value} value={value}>{t(`PublicationKinds.${value}`)}</option>)}
                </NativeSelect>
              )}
              {/* Sans JavaScript, les listes ne s'appliquent pas seules. */}
              <noscript>
                <button type="submit" className="h-12 rounded-full border border-white/30 px-6 text-sm font-semibold text-white">{t("Search.apply")}</button>
              </noscript>
            </div>
          </AutoSubmitForm>
        </div>
      </header>

      <div className="bg-muted/40">
        <div className="mx-auto max-w-7xl px-4 py-8 md:py-10">
          {!ready ? (
            <p className="max-w-3xl text-muted-foreground">{t("Search.hint")}</p>
          ) : (
            <>
              <p role="status" className="text-muted-foreground">
                <span className="font-semibold text-foreground">{t("Search.results", { count: total })}</span>{" "}
                {t("Search.resultsFor", { query: q })}
              </p>

              {total === 0 && (
                <div className="mt-6 flex flex-col items-start gap-4 rounded-box border bg-card p-6 shadow-sm sm:flex-row sm:items-center">
                  <span className="grid size-12 shrink-0 place-items-center rounded-control bg-muted text-brand-ink">
                    <SearchXIcon className="size-6" aria-hidden="true" />
                  </span>
                  <p className="flex-1 text-muted-foreground">{t("Search.noResults")}</p>
                  {filtered && (
                    <Link href={{ pathname: "/recherches", query: { q } }} className="inline-flex items-center gap-2 font-semibold text-brand-ink hover:underline">
                      {t("Search.showAll")} <ArrowRightIcon className="size-4" aria-hidden="true" />
                    </Link>
                  )}
                </div>
              )}

              {results && results.content.length > 0 && (
                <ol className="mt-6 grid gap-4">
                  {results.content.map((hit) => {
                    const link = hitLink(hit, locale);
                    return link && <li key={`${hit.type}-${hit.slug}`}><ResultCard hit={hit} link={link} /></li>;
                  })}
                </ol>
              )}

              {registry && total > 0 && (
                <div className="mt-6 grid gap-12">
                  {[
                    { id: "res-etablissements", title: t("Search.institutions", { count: establishments?.page.totalElements ?? 0 }), all: "/etablissements" as const, items: establishments?.content.map((e) => <li key={e.slug}><EstablishmentCard establishment={e} /></li>) },
                    { id: "res-formations", title: t("Search.programmes", { count: programs?.page.totalElements ?? 0 }), all: "/formations" as const, items: programs?.content.map((p) => <li key={p.slug}><ProgramCard program={p} /></li>) },
                  ].map((section) => (
                    <section key={section.id} aria-labelledby={section.id}>
                      <div className="flex items-end justify-between gap-3">
                        <h2 id={section.id} className="text-xl font-bold text-brand-ink">{section.title}</h2>
                        <Link href={{ pathname: section.all, query: { q } }} className="text-sm font-semibold text-brand-ink">{t("Search.seeAll")}</Link>
                      </div>
                      <ul className="mt-4 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">{section.items}</ul>
                    </section>
                  ))}
                </div>
              )}

              {results && (
                <Pagination
                  pathname="/recherches"
                  params={{ q, type, period, category, kind }}
                  page={page}
                  totalPages={totalPages}
                  summary={t("Search.resultsTotal", { count: total })}
                />
              )}
            </>
          )}
        </div>
      </div>
    </>
  );
}
