import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";

import { NewsList } from "@/components/content/NewsList";
import { alternates } from "@/lib/metadata";

// Rendue à chaque requête : contenus vivants ou filtres dans l'URL (données en cache côté serveur).
export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("News");
  return {
    title: t("cooperationTitle"),
    description: t("cooperationLead"),
    alternates: alternates(await getLocale(), "/actualites/cooperation-et-partenariats"),
  };
}

type Props = { searchParams: Promise<{ page?: string }> };

export default async function CooperationPage({ searchParams }: Props) {
  const t = await getTranslations("News");
  return (
    <NewsList
      pathname="/actualites/cooperation-et-partenariats"
      category="cooperation-et-partenariats"
      title={t("cooperationTitle")}
      searchParams={await searchParams}
    />
  );
}
