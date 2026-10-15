import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";

import { FilterChips } from "@/components/content/FilterChips";
import { PageHeader } from "@/components/content/PageHeader";
import { Pagination } from "@/components/registre/Pagination";
import { EstablishmentCard } from "@/components/registre/RegistryCards";
import { SearchForm } from "@/components/registre/SearchForm";
import { api } from "@/lib/api";
import { alternates } from "@/lib/metadata";
import { intro } from "@/lib/settings";

// Rendue à chaque requête : contenus vivants ou filtres dans l'URL (données en cache côté serveur).
export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Institutions");
  return { title: t("metaTitle"), description: t("metaDescription"), alternates: alternates(await getLocale(), "/etablissements") };
}

type Props = { searchParams: Promise<{ q?: string; avis?: string; type?: string; page?: string }> };

/** Lien vers la liste avec les filtres donnés (les filtres vides sont omis). */
function href(params: Record<string, string | undefined>) {
  const query = Object.fromEntries(Object.entries(params).filter((entry): entry is [string, string] => !!entry[1]));
  return { pathname: "/etablissements" as const, query };
}

export default async function EtablissementsPage({ searchParams }: Props) {
  const { q, avis: rawAvis, type: rawType, page } = await searchParams;
  const t = await getTranslations();
  const avis = rawAvis === "FAVORABLE" || rawAvis === "DEFAVORABLE" ? rawAvis : undefined;
  const kind = rawType === "PUBLIC" || rawType === "PRIVE" ? rawType : undefined;
  const pageNumber = Math.max(0, Number.parseInt(page ?? "0", 10) || 0);
  const result = await api.establishments({ q, avis, kind, page: pageNumber, size: 18 });

  return (
    <>
      <PageHeader eyebrow={t("Institutions.eyebrow")} title={t("Institutions.title")} lead={await intro("Institutions.lead")}>
        <div className="mt-6 max-w-2xl"><SearchForm defaultValue={q} /></div>
        <div className="mt-5 flex flex-col gap-3">
          <FilterChips label={t("Registry.avisFilter")} active={avis} options={[
            { href: href({ q, type: kind }), label: t("Registry.allAvis"), value: undefined },
            { href: href({ q, type: kind, avis: "FAVORABLE" }), label: t("Registry.favourable"), value: "FAVORABLE" },
            { href: href({ q, type: kind, avis: "DEFAVORABLE" }), label: t("Registry.unfavourable"), value: "DEFAVORABLE" },
          ]} />
          <FilterChips label={t("Institutions.kindFilter")} active={kind} options={[
            { href: href({ q, avis }), label: t("Institutions.allKinds"), value: undefined },
            { href: href({ q, avis, type: "PUBLIC" }), label: t("Institutions.public"), value: "PUBLIC" },
            { href: href({ q, avis, type: "PRIVE" }), label: t("Institutions.private"), value: "PRIVE" },
          ]} />
        </div>
      </PageHeader>

      <div className="mx-auto max-w-7xl px-4 py-10">
        {result && result.content.length > 0 ? (
          <>
            <p className="mb-4 text-sm text-muted-foreground" aria-live="polite">
              {t("Institutions.count", { count: result.page.totalElements })}
            </p>
            <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {result.content.map((establishment) => <li key={establishment.slug}><EstablishmentCard establishment={establishment} /></li>)}
            </ul>
            <Pagination pathname="/etablissements" params={{ q, avis, type: kind }} page={pageNumber} totalPages={result.page.totalPages} />
          </>
        ) : (
          <p className="rounded-box bg-muted p-6">{t("Institutions.empty")}</p>
        )}
      </div>
    </>
  );
}
