import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";

import { FrenchOnlyNotice } from "@/components/content/FrenchOnlyNotice";
import { RichText } from "@/components/content/RichText";
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

const ENTITIES: Record<string, string> = { amp: "&", lt: "<", gt: ">", quot: "\"", "#39": "'", apos: "'", nbsp: " " };

/** Ancre « section-N » sur chaque intertitre (h2) du texte, et la liste de ces intertitres pour le sommaire. */
function withAnchors(html: string): { html: string; headings: { id: string; text: string }[] } {
  const headings: { id: string; text: string }[] = [];
  const anchored = html.replace(/<h2(?:\s[^>]*)?>([\s\S]*?)<\/h2>/g, (_, inner: string) => {
    const id = `section-${headings.length + 1}`;
    const text = inner.replace(/<[^>]+>/g, "").replace(/&(#39|[a-z]+);/g, (entity, name: string) => ENTITIES[name] ?? entity).trim();
    headings.push({ id, text });
    return `<h2 id="${id}">${inner}</h2>`;
  });
  return { html: anchored, headings: headings.filter((h) => h.text) };
}

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

function Contents({ label, items }: { label: string; items: { id: string; text: string }[] }) {
  if (items.length === 0) return null;
  return (
    <nav aria-label={label} className="mt-8 rounded-xl border bg-muted p-5">
      <ol className="list-decimal space-y-1 pl-5 text-sm">
        {items.map((item) => <li key={item.id}><a href={`#${item.id}`}>{item.text}</a></li>)}
      </ol>
    </nav>
  );
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
        <Contents label={t("Procedures.contents")} items={SECTIONS.map((section) => ({ id: section, text: t(`Procedures.${section}`) }))} />
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
      <div lang={contentLang}><Contents label={t("Procedures.contents")} items={headings} /></div>
      <RichText html={html} lang={contentLang} className="mt-10 [&_h2]:mt-10 [&_h2]:scroll-mt-40" />
      <p className="mt-10 text-xs text-muted-foreground">{t("Common.updatedOn", { date: formatDate(page.updatedAt, lang) })}</p>
    </article>
  );
}
