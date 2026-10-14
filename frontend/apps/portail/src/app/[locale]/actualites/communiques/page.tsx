import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";

import { NewsList } from "@/components/content/NewsList";
import { alternates } from "@/lib/metadata";

// Rendue à chaque requête : contenus vivants ou filtres dans l'URL (données en cache côté serveur).
export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("News");
  return { title: t("pressTitle"), description: t("pressLead"), alternates: alternates(await getLocale(), "/actualites/communiques") };
}

type Props = { searchParams: Promise<{ page?: string }> };

export default async function CommuniquesPage({ searchParams }: Props) {
  const t = await getTranslations("News");
  return (
    <NewsList
      pathname="/actualites/communiques"
      category="communiques"
      title={t("pressTitle")}
      searchParams={await searchParams}
    />
  );
}
