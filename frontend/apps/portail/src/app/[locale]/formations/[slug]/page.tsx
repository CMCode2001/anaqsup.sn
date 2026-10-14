import { Building2Icon } from "lucide-react";
import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";
import { notFound } from "next/navigation";

import { AvisBadge, avisLabel } from "@anaqsup/ui/components/avis-badge";
import {
  Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator,
} from "@anaqsup/ui/components/breadcrumb";
import { Button } from "@anaqsup/ui/components/button";
import { Card, CardContent } from "@anaqsup/ui/components/card";

import { getPathname, Link } from "@/i18n/navigation";
import { api } from "@/lib/api";
import { alternates } from "@/lib/metadata";
import { site } from "@/lib/site";

export const revalidate = 600;

export function generateStaticParams() {
  return [];
}

type Props = { params: Promise<{ slug: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const locale = await getLocale();
  const t = await getTranslations("Programmes");
  const program = await api.program(slug);
  if (!program) return { title: t("notFound"), robots: { index: false } };
  const title = `${program.title} — ${program.establishment.name}`;
  const description = t("metaOpinion", { opinion: avisLabel(program.accreditationAvis, program.accreditationAvisYear, locale) });
  return {
    title,
    description,
    alternates: alternates(locale, { pathname: "/formations/[slug]", params: { slug } }),
    openGraph: { title, description, type: "article" },
  };
}

export default async function FormationPage({ params }: Props) {
  const { slug } = await params;
  const locale = await getLocale();
  const t = await getTranslations();
  const program = await api.program(slug);
  if (!program) notFound();

  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "EducationalOccupationalProgram",
    name: program.title,
    inLanguage: "fr",
    url: `${site.url}${getPathname({ locale, href: { pathname: "/formations/[slug]", params: { slug } } })}`,
    provider: {
      "@type": "CollegeOrUniversity",
      name: program.establishment.name,
      url: `${site.url}${getPathname({ locale, href: { pathname: "/etablissements/[slug]", params: { slug: program.establishment.slug } } })}`,
    },
  };

  return (
    <article className="mx-auto max-w-7xl px-4 py-10 [&>*]:max-w-5xl">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd).replace(/</g, "\\u003c") }} />
      <Breadcrumb>
        <BreadcrumbList>
          <BreadcrumbItem><BreadcrumbLink asChild><Link href="/formations">{t("Programmes.breadcrumb")}</Link></BreadcrumbLink></BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem><BreadcrumbPage lang="fr">{program.title}</BreadcrumbPage></BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      <h1 lang="fr" className="mt-4 text-3xl font-extrabold text-brand-ink md:text-4xl">{program.title}</h1>
      <p className="mt-2 text-muted-foreground">
        {t(`Level.${program.level}`)}{program.domain ? ` · ${program.domain}` : ""}
      </p>

      <Card className="mt-8">
        <CardContent className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">{t("Programmes.opinion")}</p>
            <AvisBadge avis={program.accreditationAvis} year={program.accreditationAvisYear} size="lg" locale={locale} className="mt-2" />
          </div>
          <Button asChild variant="outline">
            <Link href={{ pathname: "/etablissements/[slug]", params: { slug: program.establishment.slug } }}>
              <Building2Icon data-icon="inline-start" aria-hidden="true" /><span lang="fr">{program.establishment.name}</span>
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
