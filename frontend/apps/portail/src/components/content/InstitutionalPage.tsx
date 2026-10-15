import { DownloadIcon, Maximize2Icon } from "lucide-react";
import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";
import { notFound } from "next/navigation";

import { CoverImage } from "@/components/content/CoverImage";
import { FrenchOnlyNotice } from "@/components/content/FrenchOnlyNotice";
import { PageHeader } from "@/components/content/PageHeader";
import { PersonCaption } from "@/components/content/PersonCaption";
import { RichText } from "@/components/content/RichText";
import { SectionNav } from "@/components/content/SectionNav";
import type { Pathname } from "@/i18n/routing";
import { api } from "@/lib/api";
import { langAttr, servedLang } from "@/lib/lang";
import { formatDate } from "@/lib/format";
import { alternates } from "@/lib/metadata";

/** Pages de la rubrique « L'ANAQ-Sup » : contenu saisi dans le back-office, une adresse par page. */
const PAGES = {
  presentation: "/anaq-sup",
  missions: "/anaq-sup/missions",
  gouvernance: "/anaq-sup/gouvernance",
  organigramme: "/anaq-sup/organigramme",
  "mot-du-secretaire-executif": "/anaq-sup/mot-du-secretaire-executif",
} as const satisfies Record<string, Pathname>;

type Slug = keyof typeof PAGES;

export async function institutionalMetadata(slug: Slug): Promise<Metadata> {
  const lang = await getLocale();
  const page = await api.page(slug, lang);
  if (!page) return {};
  return { title: page.title, description: page.summary ?? undefined, alternates: alternates(lang, PAGES[slug], servedLang(page.lang)) };
}

export async function InstitutionalPageView({ slug }: { slug: Slug }) {
  const lang = await getLocale();
  const t = await getTranslations();
  const page = await api.page(slug, lang);
  if (!page) notFound();
  const contentLang = langAttr(page.lang, lang);

  return (
    <>
      <PageHeader eyebrow={t("About.eyebrow")} title={page.title} lead={page.summary} lang={contentLang}>
        {/* Le mot du Secrétaire exécutif n'est pas un onglet de la rubrique (mis en avant sur l'accueil). */}
        <SectionNav group="about" current={PAGES[slug]} />
      </PageHeader>
      {slug === "organigramme" && page.imageUrl ? (
        <OrgChart imageUrl={page.imageUrl} title={page.imageName ?? page.title} subtitle={page.imageRole}
          labels={{ download: t("About.orgChartDownload"), fullSize: t("About.orgChartFullSize") }} />
      ) : (
      <article className={page.imageUrl ? "mx-auto grid max-w-7xl gap-10 px-4 py-12 md:grid-cols-[18rem_minmax(0,1fr)] lg:gap-14" : "mx-auto max-w-7xl px-4 py-12"}>
        {/* Photo, nom et fonction, saisis dans le back-office : ex. le mot du Secrétaire exécutif. */}
        {page.imageUrl && (
          <figure className="mx-auto w-full max-w-xs self-start overflow-hidden rounded-box bg-card shadow-lg ring-1 ring-foreground/5 md:sticky md:top-40">
            <div className="relative aspect-[4/5] bg-muted">
              <CoverImage src={page.imageUrl} sizes="(min-width: 768px) 288px, 80vw" eager />
            </div>
            <PersonCaption name={page.imageName} role={page.imageRole} size="md" />
          </figure>
        )}
        {/* Aligné sur l'en-tête de la page ; ligne de lecture confortable. */}
        <div className="max-w-4xl">
          <FrenchOnlyNotice lang={page.lang} className="mb-8" />
          <RichText html={page.bodyHtml} lang={contentLang} />
          <p className="mt-10 text-xs text-muted-foreground">
            {t("Common.updatedOn", { date: formatDate(page.updatedAt, lang) })}
          </p>
        </div>
      </article>
      )}
    </>
  );
}

/**
 * Organigramme (modèle finances.gouv.sn) : image téléversée dans le back-office, montrée en entier dans une
 * carte, avec son titre et son sous-titre, et deux actions : télécharger, voir en taille réelle.
 * Tant qu'aucune image n'est téléversée, la page affiche son texte.
 */
function OrgChart({ imageUrl, title, subtitle, labels }: {
  imageUrl: string;
  title: string;
  subtitle: string | null | undefined;
  labels: { download: string; fullSize: string };
}) {
  const action = "inline-flex h-10 items-center gap-2 rounded-control border bg-background px-4 text-sm font-semibold text-foreground shadow-sm transition-colors hover:border-primary hover:bg-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";
  return (
    <section className="mx-auto max-w-7xl px-4 py-12">
      <figure className="mx-auto max-w-6xl rounded-box border bg-card p-5 shadow-sm md:p-8">
        <figcaption className="text-center">
          <h2 className="text-2xl font-extrabold text-foreground md:text-3xl">{title}</h2>
          {subtitle && <p className="mt-2 text-muted-foreground">{subtitle}</p>}
        </figcaption>
        <a href={imageUrl} target="_blank" rel="noopener" className="mt-8 block overflow-hidden rounded-box border bg-muted/40 p-3 md:p-6">
          {/* Image entière, à sa taille réelle au plus : le texte de l'organigramme doit rester lisible. */}
          {/* eslint-disable-next-line @next/next/no-img-element -- dimensions inconnues, image lue en pleine résolution. */}
          <img src={imageUrl} alt={title} className="mx-auto h-auto max-w-full" loading="eager" />
        </a>
        <div className="mt-6 flex flex-wrap justify-center gap-3">
          <a href={imageUrl} download="organigramme-anaq-sup" className={action}>
            <DownloadIcon className="size-4" aria-hidden="true" />{labels.download}
          </a>
          <a href={imageUrl} target="_blank" rel="noopener" className={action}>
            <Maximize2Icon className="size-4" aria-hidden="true" />{labels.fullSize}
          </a>
        </div>
      </figure>
    </section>
  );
}
