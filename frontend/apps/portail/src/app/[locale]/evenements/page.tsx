import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";

import { EventCard } from "@/components/content/Cards";
import { FilterChips } from "@/components/content/FilterChips";
import { PageHeader } from "@/components/content/PageHeader";
import { SectionNav } from "@/components/content/SectionNav";
import { Pagination } from "@/components/registre/Pagination";
import { api } from "@/lib/api";
import { alternates } from "@/lib/metadata";
import { intro } from "@/lib/settings";

// Rendue à chaque requête : contenus vivants ou filtres dans l'URL (données en cache côté serveur).
export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Events");
  return { title: t("title"), description: t("metaDescription"), alternates: alternates(await getLocale(), "/evenements") };
}

type Props = { searchParams: Promise<{ periode?: string; page?: string }> };

export default async function EvenementsPage({ searchParams }: Props) {
  const { periode, page } = await searchParams;
  const lang = await getLocale();
  const t = await getTranslations("Events");
  const past = periode === "passes";
  const pageNumber = Math.max(0, Number.parseInt(page ?? "0", 10) || 0);
  const events = await api.events({ lang, period: past ? "past" : "upcoming", page: pageNumber, size: 12 });

  return (
    <>
      <PageHeader eyebrow={t("eyebrow")} title={t("title")} lead={await intro("Events.lead")}>
        <SectionNav group="news" current="/evenements" />
        <div className="mt-4">
          <FilterChips
            label={t("period")}
            active={past ? "passes" : "a-venir"}
            options={[
              { href: "/evenements", label: t("upcoming"), value: "a-venir" },
              { href: { pathname: "/evenements", query: { periode: "passes" } }, label: t("past"), value: "passes" },
            ]}
          />
        </div>
      </PageHeader>
      <div className="mx-auto max-w-7xl px-4 py-10">
        {events && events.content.length > 0 ? (
          <>
            <ul className="grid gap-6 md:grid-cols-2">
              {events.content.map((event) => <li key={event.slug}><EventCard event={event} /></li>)}
            </ul>
            <Pagination pathname="/evenements" params={{ periode: past ? "passes" : undefined }} page={pageNumber} totalPages={events.page.totalPages} />
          </>
        ) : (
          <p className="text-muted-foreground">{past ? t("emptyPast") : t("emptyUpcoming")}</p>
        )}
      </div>
    </>
  );
}
