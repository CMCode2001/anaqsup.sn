import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";

import { NewsList } from "@/components/content/NewsList";
import { alternates } from "@/lib/metadata";

// Rendue à chaque requête : contenus vivants ou filtres dans l'URL (données en cache côté serveur).
export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("News");
  return { title: t("eyebrow"), description: t("metaDescription"), alternates: alternates(await getLocale(), "/actualites") };
}

type Props = { searchParams: Promise<{ categorie?: string; page?: string }> };

export default async function ActualitesPage({ searchParams }: Props) {
  const t = await getTranslations("News");
  return <NewsList pathname="/actualites" title={t("title")} searchParams={await searchParams} />;
}
