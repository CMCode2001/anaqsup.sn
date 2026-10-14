import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";

import { PublicationList, type PublicationSearchParams } from "@/components/content/PublicationList";
import { alternates } from "@/lib/metadata";
import { intro } from "@/lib/settings";

// Rendue à chaque requête : contenus vivants ou filtres dans l'URL (données en cache côté serveur).
export const dynamic = "force-dynamic";

const PATHNAME = "/ressources-documentaires/publications";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Resources");
  return { title: t("publicationsTitle"), description: await intro("Resources.publicationsLead"), alternates: alternates(await getLocale(), PATHNAME) };
}

type Props = { searchParams: Promise<PublicationSearchParams> };

export default async function Page({ searchParams }: Props) {
  const t = await getTranslations("Resources");
  return <PublicationList pathname={PATHNAME} title={t("publicationsTitle")} lead={await intro("Resources.publicationsLead")} searchParams={await searchParams} />;
}
