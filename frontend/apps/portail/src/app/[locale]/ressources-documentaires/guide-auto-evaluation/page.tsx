import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";

import { PublicationList, type PublicationSearchParams } from "@/components/content/PublicationList";
import { alternates } from "@/lib/metadata";
import { intro } from "@/lib/settings";

// Rendue à chaque requête : contenus vivants ou filtres dans l'URL (données en cache côté serveur).
export const dynamic = "force-dynamic";

const PATHNAME = "/ressources-documentaires/guide-auto-evaluation";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Resources");
  return { title: t("selfAssessmentTitle"), description: await intro("Resources.selfAssessmentLead"), alternates: alternates(await getLocale(), PATHNAME) };
}

type Props = { searchParams: Promise<PublicationSearchParams> };

export default async function Page({ searchParams }: Props) {
  const t = await getTranslations("Resources");
  return <PublicationList pathname={PATHNAME} kind="GUIDE" title={t("selfAssessmentTitle")} lead={await intro("Resources.selfAssessmentLead")} searchParams={await searchParams} />;
}
