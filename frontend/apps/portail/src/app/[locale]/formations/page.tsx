import type { ProgramLevel } from "@anaqsup/api-client";
import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";

import { FilterChips } from "@/components/content/FilterChips";
import { PageHeader } from "@/components/content/PageHeader";
import { Pagination } from "@/components/registre/Pagination";
import { ProgramCard } from "@/components/registre/RegistryCards";
import { SearchForm } from "@/components/registre/SearchForm";
import { api } from "@/lib/api";
import { alternates } from "@/lib/metadata";
import { intro } from "@/lib/settings";

// Rendue à chaque requête : contenus vivants ou filtres dans l'URL (données en cache côté serveur).
export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Programmes");
  return { title: t("metaTitle"), description: t("metaDescription"), alternates: alternates(await getLocale(), "/formations") };
}

const LEVELS = ["LICENCE", "MASTER", "DOCTORAT", "DUT", "BTS", "AUTRE"] as const satisfies ProgramLevel[];

type Props = { searchParams: Promise<{ q?: string; avis?: string; niveau?: string; etablissement?: string; page?: string }> };

/** Lien vers la liste avec les filtres donnés (les filtres vides sont omis). */
function href(params: Record<string, string | undefined>) {
  const query = Object.fromEntries(Object.entries(params).filter((entry): entry is [string, string] => !!entry[1]));
  return { pathname: "/formations" as const, query };
}

export default async function FormationsPage({ searchParams }: Props) {
  const { q, avis: rawAvis, niveau, etablissement, page } = await searchParams;
  const t = await getTranslations();
  const avis = rawAvis === "FAVORABLE" || rawAvis === "DEFAVORABLE" ? rawAvis : undefined;
  const level = LEVELS.find((value) => value === niveau);
  const establishment = etablissement && /^[a-z0-9]+(-[a-z0-9]+)*$/.test(etablissement) ? etablissement : undefined;
  const pageNumber = Math.max(0, Number.parseInt(page ?? "0", 10) || 0);
  const result = await api.programs({ q, avis, level, establishment, page: pageNumber, size: 18 });
  const keep = { q, etablissement: establishment };

  return (
    <>
      <PageHeader eyebrow={t("Programmes.eyebrow")} title={t("Programmes.title")} lead={await intro("Programmes.lead")}>
        <div className="mt-6 max-w-2xl"><SearchForm defaultValue={q} /></div>
        <div className="mt-5 flex flex-col gap-3">
          <FilterChips label={t("Registry.avisFilter")} active={avis} options={[
            { href: href({ ...keep, niveau: level }), label: t("Registry.allAvis"), value: undefined },
            { href: href({ ...keep, niveau: level, avis: "FAVORABLE" }), label: t("Registry.favourable"), value: "FAVORABLE" },
            { href: href({ ...keep, niveau: level, avis: "DEFAVORABLE" }), label: t("Registry.unfavourable"), value: "DEFAVORABLE" },
          ]} />
          <FilterChips label={t("Programmes.levelFilter")} active={level} options={[
            { href: href({ ...keep, avis }), label: t("Programmes.allLevels"), value: undefined },
            ...LEVELS.map((value) => ({ href: href({ ...keep, avis, niveau: value }), label: t(`Level.${value}`), value })),
          ]} />
        </div>
      </PageHeader>

      <div className="mx-auto max-w-7xl px-4 py-10">
        {result && result.content.length > 0 ? (
          <>
            <p className="mb-4 text-sm text-muted-foreground" aria-live="polite">
              {t("Programmes.count", { count: result.page.totalElements })}
            </p>
            <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {result.content.map((program) => <li key={program.slug}><ProgramCard program={program} /></li>)}
            </ul>
            <Pagination pathname="/formations" params={{ ...keep, avis, niveau: level }} page={pageNumber} totalPages={result.page.totalPages} />
          </>
        ) : (
          <p className="rounded-box bg-muted p-6">{t("Programmes.empty")}</p>
        )}
      </div>
    </>
  );
}
