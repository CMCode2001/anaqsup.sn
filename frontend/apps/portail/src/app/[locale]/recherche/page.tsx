import type { Metadata } from "next";
import { getTranslations } from "@/i18n/server";

import { PageHeader } from "@/components/content/PageHeader";
import { EstablishmentCard, ProgramCard } from "@/components/registre/RegistryCards";
import { SearchForm } from "@/components/registre/SearchForm";
import { Link } from "@/i18n/navigation";
import { api } from "@/lib/api";

// Rendue à chaque requête : contenus vivants ou filtres dans l'URL (données en cache côté serveur).
export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Search");
  return { title: t("metaTitle"), robots: { index: false, follow: true } };
}

type Props = { searchParams: Promise<{ q?: string }> };

/** Résultats séparés : établissements (habilitation) d'un côté, formations (accréditation) de l'autre. */
export default async function RecherchePage({ searchParams }: Props) {
  const { q = "" } = await searchParams;
  const t = await getTranslations("Search");
  const query = q.trim();
  const [establishments, programs] = query.length >= 2
    ? await Promise.all([api.establishments({ q: query, size: 6 }), api.programs({ q: query, size: 6 })])
    : [null, null];

  return (
    <>
      <PageHeader eyebrow={t("eyebrow")} title={t("title")}>
        <div className="mt-6 max-w-2xl"><SearchForm defaultValue={query} /></div>
      </PageHeader>

      {query.length >= 2 && (
        <div className="mx-auto grid max-w-7xl gap-12 px-4 py-10">
          <section aria-labelledby="res-etablissements">
            <div className="flex items-end justify-between gap-3">
              <h2 id="res-etablissements" className="text-xl font-bold text-brand-ink">
                {t("institutions", { count: establishments?.page.totalElements ?? 0 })}
              </h2>
              <Link href={{ pathname: "/etablissements", query: { q: query } }} className="text-sm font-semibold text-brand-ink">{t("seeAll")}</Link>
            </div>
            <ul className="mt-4 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {establishments?.content.map((e) => <li key={e.slug}><EstablishmentCard establishment={e} /></li>)}
            </ul>
          </section>

          <section aria-labelledby="res-formations">
            <div className="flex items-end justify-between gap-3">
              <h2 id="res-formations" className="text-xl font-bold text-brand-ink">
                {t("programmes", { count: programs?.page.totalElements ?? 0 })}
              </h2>
              <Link href={{ pathname: "/formations", query: { q: query } }} className="text-sm font-semibold text-brand-ink">{t("seeAll")}</Link>
            </div>
            <ul className="mt-4 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {programs?.content.map((p) => <li key={p.slug}><ProgramCard program={p} /></li>)}
            </ul>
          </section>
        </div>
      )}
    </>
  );
}
