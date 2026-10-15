import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";

import { FrenchOnlyNotice } from "@/components/content/FrenchOnlyNotice";
import { RichText } from "@/components/content/RichText";
import { TableOfContents, withAnchors } from "@/components/content/TableOfContents";
import { api } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { langAttr, servedLang } from "@/lib/lang";
import { alternates } from "@/lib/metadata";

/**
 * Fiches pratiques : pages « procedure-habilitation » et « procedure-accreditation », rédigées dans le
 * back-office (Pages institutionnelles). Le sommaire reprend les intertitres du texte, dans l'ordre.
 * Tant que la page n'existe pas (API antérieure), la structure d'origine s'affiche, contenu à fournir.
 */
const PROCEDURES = {
  habilitation: { pathname: "/procedures/habilitation", title: "approvalTitle", summary: "approvalSummary" },
  accreditation: { pathname: "/procedures/accreditation", title: "accreditationTitle", summary: "accreditationSummary" },
} as const;

const SECTIONS = ["s1", "s2", "s3", "s4", "s5", "s6", "s7"] as const;

type Procedure = keyof typeof PROCEDURES;

export async function procedureMetadata(procedure: Procedure): Promise<Metadata> {
  const [t, lang] = await Promise.all([getTranslations("Procedures"), getLocale()]);
  const entry = PROCEDURES[procedure];
  const page = await api.page(`procedure-${procedure}`, lang);
  return {
    title: page?.title ?? t(entry.title),
    description: page ? page.summary ?? undefined : t(entry.summary),
    alternates: alternates(lang, entry.pathname, page ? servedLang(page.lang) : undefined),
  };
}

export async function ProcedurePage({ procedure }: { procedure: Procedure }) {
  const [t, lang] = await Promise.all([getTranslations(), getLocale()]);
  const entry = PROCEDURES[procedure];
  const page = await api.page(`procedure-${procedure}`, lang);

  if (!page) {
    return (
      <article className="mx-auto max-w-7xl px-4 py-10 [&>*]:max-w-4xl">
        <p className="text-sm font-semibold uppercase tracking-wide text-brand-green">{t("Procedures.factSheet")}</p>
        <h1 className="mt-2 font-heading text-3xl font-extrabold text-brand-ink">{t(`Procedures.${entry.title}`)}</h1>
        <p className="mt-3 text-lg text-foreground">{t(`Procedures.${entry.summary}`)}</p>
        <TableOfContents label={t("Procedures.contents")} items={SECTIONS.map((section) => ({ id: section, text: t(`Procedures.${section}`) }))} />
        {SECTIONS.map((section) => (
          <section key={section} id={section} className="mt-10">
            <h2 className="font-heading text-xl font-bold text-brand-ink">{t(`Procedures.${section}`)}</h2>
            <p className="mt-3 text-sm italic text-muted-foreground">{t("Procedures.toProvide")}</p>
          </section>
        ))}
      </article>
    );
  }

  const contentLang = langAttr(page.lang, lang);
  const { html, headings } = withAnchors(page.bodyHtml);
  return (
    <article className="mx-auto max-w-7xl px-4 py-10 [&>*]:max-w-4xl">
      <p className="text-sm font-semibold uppercase tracking-wide text-brand-green">{t("Procedures.factSheet")}</p>
      <h1 lang={contentLang} className="mt-2 font-heading text-3xl font-extrabold text-brand-ink">{page.title}</h1>
      {page.summary && <p lang={contentLang} className="mt-3 text-lg text-foreground">{page.summary}</p>}
      <FrenchOnlyNotice lang={page.lang} className="mt-6" />
      <TableOfContents label={t("Procedures.contents")} items={headings} lang={contentLang} />
      <RichText html={html} lang={contentLang} className="mt-10 [&_h2]:mt-10 [&_h2]:scroll-mt-40" />
      <p className="mt-10 text-xs text-muted-foreground">{t("Common.updatedOn", { date: formatDate(page.updatedAt, lang) })}</p>
    </article>
  );
}
