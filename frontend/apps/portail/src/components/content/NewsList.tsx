import { NewsCard } from "@/components/content/Cards";
import { FilterChips } from "@/components/content/FilterChips";
import { NewsHero } from "@/components/content/NewsHero";
import { Pagination } from "@/components/registre/Pagination";
import type { Href } from "@/i18n/navigation";
import type { StaticPathname } from "@/i18n/routing";
import { getLocale, getTranslations } from "@/i18n/server";
import { api } from "@/lib/api";

/** Catégories qui ont leur propre page dans le menu « Actualités ». */
export const CATEGORY_PAGES = {
  "cooperation-et-partenariats": "/actualites/cooperation-et-partenariats",
  communiques: "/actualites/communiques",
} as const satisfies Record<string, StaticPathname>;

const SLUG = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const PAGE_SIZE = 9;

/**
 * Liste d'actualités, commune à « Toutes les actualités » et aux pages de catégorie (modèle validé par
 * l'ANAQ-Sup) : l'article à la une en grand, les rubriques, puis les autres articles en grille.
 * Les rubriques qui ont leur page mènent à cette page ; les autres filtrent la liste (?categorie=…).
 */
export async function NewsList({ pathname, title, category, searchParams }: {
  pathname: "/actualites" | (typeof CATEGORY_PAGES)[keyof typeof CATEGORY_PAGES];
  title: string;
  category?: keyof typeof CATEGORY_PAGES;
  searchParams: { categorie?: string; page?: string };
}) {
  const lang = await getLocale();
  const t = await getTranslations();
  const pageNumber = Math.max(0, Number.parseInt(searchParams.page ?? "0", 10) || 0);
  const filter = !category && searchParams.categorie && SLUG.test(searchParams.categorie) ? searchParams.categorie : undefined;
  const [news, categories] = await Promise.all([
    api.news({ lang, category: category ?? filter, page: pageNumber, size: PAGE_SIZE }),
    api.newsCategories(lang),
  ]);

  // La première page s'ouvre sur l'article à la une (épinglé, sinon le plus récent : ordre de l'API).
  const items = news?.content ?? [];
  const [featured, ...others] = pageNumber === 0 ? items : [undefined, ...items];
  const active = category ?? filter;
  const chipHref = (slug: string): Href =>
    slug in CATEGORY_PAGES
      ? CATEGORY_PAGES[slug as keyof typeof CATEGORY_PAGES]
      : { pathname: "/actualites", query: { categorie: slug } };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 md:py-10">
      <h1 className="sr-only">{title}</h1>
      {featured && <NewsHero item={featured} />}

      <div className="mt-10">
        <FilterChips
          label={t("News.categories")}
          active={active}
          options={[
            { href: "/actualites", label: t("Nav.allNews"), value: undefined },
            ...(categories ?? []).map((c) => ({ href: chipHref(c.slug), label: c.label, value: c.slug })),
          ]}
        />
      </div>

      <section aria-labelledby="autres" className="mt-12">
        <h2 id="autres" className="inline-block text-2xl font-extrabold text-brand-ink md:text-3xl">
          {featured ? t("News.others") : title}
          <span aria-hidden="true" className="mt-2 block h-1 w-16 rounded-full bg-primary" />
        </h2>
        {others.length > 0 ? (
          <ul className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3 lg:gap-8">
            {others.map((item) => item && <li key={item.slug}><NewsCard item={item} /></li>)}
          </ul>
        ) : (
          !featured && <p className="mt-6 text-muted-foreground">{t("News.empty")}</p>
        )}
        {news && (
          <Pagination
            pathname={pathname}
            params={{ categorie: filter }}
            page={pageNumber}
            totalPages={news.page.totalPages}
            summary={t("News.count", { count: news.page.totalElements })}
          />
        )}
      </section>
    </div>
  );
}
