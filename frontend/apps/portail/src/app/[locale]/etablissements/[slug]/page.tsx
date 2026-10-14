import { GraduationCapIcon, MapPinIcon } from "lucide-react";
import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";
import { notFound } from "next/navigation";

import { AvisBadge, avisLabel } from "@anaqsup/ui/components/avis-badge";
import {
  Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator,
} from "@anaqsup/ui/components/breadcrumb";
import { Button } from "@anaqsup/ui/components/button";
import { Card, CardContent } from "@anaqsup/ui/components/card";

import { Link } from "@/i18n/navigation";
import { api } from "@/lib/api";
import { alternates } from "@/lib/metadata";

export const revalidate = 600;

export function generateStaticParams() {
  return [];
}

type Props = { params: Promise<{ slug: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const locale = await getLocale();
  const t = await getTranslations("Institutions");
  const establishment = await api.establishment(slug);
  if (!establishment) return { title: t("notFound"), robots: { index: false } };
  const title = establishment.acronym ? `${establishment.name} (${establishment.acronym})` : establishment.name;
  const description = t("metaOpinion", { opinion: avisLabel(establishment.habilitationAvis, establishment.habilitationAvisYear, locale) });
  return {
    title,
    description,
    alternates: alternates(locale, { pathname: "/etablissements/[slug]", params: { slug } }),
    openGraph: { title, description, type: "article" },
  };
}

export default async function EtablissementPage({ params }: Props) {
  const { slug } = await params;
  const locale = await getLocale();
  const t = await getTranslations();
  const establishment = await api.establishment(slug);
  if (!establishment) notFound();
  const programCount = establishment.programCount ?? 0;

  return (
    <article className="mx-auto max-w-7xl px-4 py-10 [&>*]:max-w-5xl">
      <Breadcrumb>
        <BreadcrumbList>
          <BreadcrumbItem><BreadcrumbLink asChild><Link href="/etablissements">{t("Institutions.breadcrumb")}</Link></BreadcrumbLink></BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem><BreadcrumbPage lang="fr">{establishment.acronym ?? establishment.name}</BreadcrumbPage></BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      <h1 lang="fr" className="mt-4 text-3xl font-extrabold text-brand-ink md:text-4xl">{establishment.name}</h1>
      <p className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-muted-foreground">
        {establishment.acronym && <span className="font-semibold text-foreground">{establishment.acronym}</span>}
        <span>{establishment.kind === "PUBLIC" ? t("Institutions.publicInstitution") : t("Institutions.privateInstitution")}</span>
        {establishment.city && <span className="inline-flex items-center gap-1"><MapPinIcon className="size-4" aria-hidden="true" />{establishment.city}</span>}
      </p>

      <Card className="mt-8">
        <CardContent className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">{t("Institutions.opinion")}</p>
            <AvisBadge avis={establishment.habilitationAvis} year={establishment.habilitationAvisYear} size="lg" locale={locale} className="mt-2" />
          </div>
          <Button asChild>
            <Link href={{ pathname: "/formations", query: { etablissement: slug } }}>
              <GraduationCapIcon data-icon="inline-start" aria-hidden="true" />
              {t("Institutions.programmes", { count: programCount })}
            </Link>
          </Button>
        </CardContent>
      </Card>

      <p className="mt-6 text-sm text-muted-foreground">
        <Link href="/habilitations-et-accreditations" className="font-semibold text-brand-ink underline underline-offset-4">{t("Registry.howToRead")}</Link>
      </p>
    </article>
  );
}
