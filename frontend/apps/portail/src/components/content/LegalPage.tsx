import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { FrenchOnlyNotice } from "@/components/content/FrenchOnlyNotice";
import { PageHeader } from "@/components/content/PageHeader";
import { RichText } from "@/components/content/RichText";
import { TableOfContents, withAnchors } from "@/components/content/TableOfContents";
import { getLocale, getTranslations } from "@/i18n/server";
import type { Pathname } from "@/i18n/routing";
import { api } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { langAttr, servedLang } from "@/lib/lang";
import { alternates } from "@/lib/metadata";

/**
 * Pages légales (décision ANAQ-Sup du 14/10/2026) : texte saisi dans le back-office (Pages institutionnelles),
 * sommaire tiré des intertitres, liens depuis le pied de page.
 */
const LEGAL_PAGES = {
  "mentions-legales": "/mentions-legales",
  "donnees-personnelles": "/donnees-personnelles",
} as const satisfies Record<string, Pathname>;

type LegalSlug = keyof typeof LEGAL_PAGES;

export async function legalMetadata(slug: LegalSlug): Promise<Metadata> {
  const lang = await getLocale();
  const page = await api.page(slug, lang);
  if (!page) return {};
  return { title: page.title, description: page.summary ?? undefined, alternates: alternates(lang, LEGAL_PAGES[slug], servedLang(page.lang)) };
}

export async function LegalPageView({ slug }: { slug: LegalSlug }) {
  const [lang, t] = await Promise.all([getLocale(), getTranslations()]);
  const page = await api.page(slug, lang);
  if (!page) notFound();
  const contentLang = langAttr(page.lang, lang);
  const { html, headings } = withAnchors(page.bodyHtml);

  return (
    <>
      <PageHeader eyebrow={t("Legal.eyebrow")} title={page.title} lead={page.summary} lang={contentLang} />
      <article className="mx-auto max-w-7xl px-4 py-12">
        <div className="max-w-4xl">
          <FrenchOnlyNotice lang={page.lang} className="mb-8" />
          <TableOfContents label={t("Legal.contents")} items={headings} lang={contentLang} />
          <RichText html={html} lang={contentLang} className="mt-10 [&_h2]:mt-10 [&_h2]:scroll-mt-40 [&_h3]:mt-6" />
          <p className="mt-10 text-xs text-muted-foreground">{t("Common.updatedOn", { date: formatDate(page.updatedAt, lang) })}</p>
        </div>
      </article>
    </>
  );
}
