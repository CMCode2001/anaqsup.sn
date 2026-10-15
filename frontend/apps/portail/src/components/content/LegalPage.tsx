import { ChevronRightIcon, CircleHelpIcon } from "lucide-react";
import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { BrandLogo } from "@anaqsup/ui/components/brand-logo";
import { Button } from "@anaqsup/ui/components/button";
import { cn } from "@anaqsup/ui/lib/utils";

import { FrenchOnlyNotice } from "@/components/content/FrenchOnlyNotice";
import { RichText } from "@/components/content/RichText";
import { withAnchors } from "@/components/content/TableOfContents";
import { Link } from "@/i18n/navigation";
import type { Pathname } from "@/i18n/routing";
import { getLocale, getTranslations } from "@/i18n/server";
import { api } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { langAttr, servedLang } from "@/lib/lang";
import { alternates } from "@/lib/metadata";

/**
 * Pages légales (décision ANAQ-Sup du 14/10/2026), sur le modèle de finances.gouv.sn : menu « Informations
 * légales » et aide à gauche ; à droite, le texte dans une carte (titre, logotype, intertitres numérotés).
 * Texte saisi dans le back-office (Pages institutionnelles).
 */
const LEGAL_PAGES = {
  "mentions-legales": { pathname: "/mentions-legales", label: "legalNotice" },
  "donnees-personnelles": { pathname: "/donnees-personnelles", label: "privacy" },
} as const satisfies Record<string, { pathname: Pathname; label: string }>;

type LegalSlug = keyof typeof LEGAL_PAGES;

/** Texte légal : intertitres en capitales soulignés d'un filet, corps aéré. */
const LEGAL_TEXT = cn(
  "mt-2 text-[1.0625rem] leading-[1.85] [&_p]:mt-5 [&_li]:mt-2",
  "[&_h2]:mt-12 [&_h2]:scroll-mt-40 [&_h2]:border-b [&_h2]:pb-4 [&_h2]:text-xl [&_h2]:font-extrabold [&_h2]:tracking-tight [&_h2]:uppercase md:[&_h2]:text-2xl",
  "[&_h3]:mt-8 [&_h3]:text-lg [&_h3]:font-bold [&_h3]:text-foreground",
);

export async function legalMetadata(slug: LegalSlug): Promise<Metadata> {
  const lang = await getLocale();
  const page = await api.page(slug, lang);
  if (!page) return {};
  return { title: page.title, description: page.summary ?? undefined, alternates: alternates(lang, LEGAL_PAGES[slug].pathname, servedLang(page.lang)) };
}

/** « Besoin d'aide ? » : renvoi vers la page Contact. */
async function HelpCard({ className }: { className?: string }) {
  const t = await getTranslations("Legal");
  return (
    <div className={cn("rounded-box border bg-card p-6 shadow-sm", className)}>
      <p className="flex items-center gap-2 font-bold text-foreground">
        <CircleHelpIcon className="size-5 text-brand-ink" aria-hidden="true" />{t("helpTitle")}
      </p>
      <p className="mt-3 text-sm leading-relaxed text-muted-foreground">{t("helpText")}</p>
      <Button asChild className="mt-5 w-full"><Link href="/contact">{t("contact")}</Link></Button>
    </div>
  );
}

export async function LegalPageView({ slug }: { slug: LegalSlug }) {
  const [lang, t] = await Promise.all([getLocale(), getTranslations()]);
  const page = await api.page(slug, lang);
  if (!page) notFound();
  const contentLang = langAttr(page.lang, lang);
  const { html } = withAnchors(page.bodyHtml, { numbered: true });

  return (
    <>
      {/* Bandeau sous le menu, comme l'en-tête des autres rubriques. */}
      <div aria-hidden="true" className="hidden h-16 border-b bg-muted/60 md:block" />
      <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 md:py-14 lg:grid-cols-[19.5rem_minmax(0,1fr)] lg:gap-10">
        <aside className="flex flex-col gap-6 lg:sticky lg:top-36 lg:self-start">
          <nav aria-label={t("Legal.eyebrow")} className="overflow-hidden rounded-box border bg-card shadow-sm">
            <p className="border-b px-5 py-4 text-xs font-bold tracking-[0.16em] text-foreground uppercase">{t("Legal.eyebrow")}</p>
            <ul className="py-2">
              {(Object.keys(LEGAL_PAGES) as LegalSlug[]).map((key) => {
                const current = key === slug;
                return (
                  <li key={key}>
                    <Link
                      href={LEGAL_PAGES[key].pathname}
                      aria-current={current ? "page" : undefined}
                      className={cn(
                        "flex items-center justify-between gap-3 border-l-[3px] px-5 py-3 text-[0.9375rem] transition-colors",
                        current
                          ? "border-primary bg-muted font-semibold text-foreground"
                          : "border-transparent text-muted-foreground hover:bg-muted/50 hover:text-brand-ink",
                      )}
                    >
                      {t(`Legal.${LEGAL_PAGES[key].label}`)}
                      {current && <ChevronRightIcon className="size-4 shrink-0" aria-hidden="true" />}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </nav>
          <HelpCard className="hidden lg:block" />
        </aside>

        <article className="min-w-0 rounded-box border bg-card p-6 shadow-sm md:p-12">
          <header className="flex items-start justify-between gap-6 border-b pb-8">
            <h1 lang={contentLang} className="text-3xl font-extrabold text-brand-ink md:text-4xl">{page.title}</h1>
            <BrandLogo height={40} className="hidden sm:inline-flex" />
          </header>
          <FrenchOnlyNotice lang={page.lang} className="mt-8" />
          <RichText html={html} lang={contentLang} className={LEGAL_TEXT} />
          <p className="mt-12 border-t pt-6 text-xs text-muted-foreground">{t("Common.updatedOn", { date: formatDate(page.updatedAt, lang) })}</p>
        </article>

        {/* Sur téléphone, l'aide vient après le texte. */}
        <HelpCard className="lg:hidden" />
      </div>
    </>
  );
}
