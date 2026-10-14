import type { PublicationPage } from "@anaqsup/api-client";
import { ArrowRightIcon, DownloadIcon, EyeIcon, FileTextIcon } from "lucide-react";

import { Reveal } from "@anaqsup/ui/components/motion/motion";

import { CoverImage } from "@/components/content/CoverImage";
import { getLocale, getTranslations } from "@/i18n/server";
import { Link } from "@/i18n/navigation";
import type { Locale } from "@/i18n/routing";
import { formatDate, formatFileSize } from "@/lib/format";
import { langAttr } from "@/lib/lang";

type Publication = PublicationPage["content"][number];

const FOCUS = "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

/** Première page du PDF, entière (format portrait), sur fond blanc comme une feuille ; sinon une icône. */
function Page({ publication, sizes, className }: { publication: Publication; sizes: string; className: string }) {
  return (
    <div className={`relative shrink-0 overflow-hidden bg-white shadow-lg ring-1 ring-black/10 ${className}`}>
      {publication.coverImageUrl ? (
        <CoverImage src={publication.coverImageUrl} sizes={sizes} className="object-contain transition-transform duration-500 ease-out group-hover:scale-105" />
      ) : (
        <FileTextIcon aria-hidden="true" className="absolute inset-0 m-auto size-1/3 text-brand-ink" />
      )}
    </div>
  );
}

/** « Rapport annuel · 25 novembre 2024 ». */
async function Meta({ publication, locale }: { publication: Publication; locale: Locale }) {
  const t = await getTranslations("PublicationKind");
  return (
    <p className="text-sm text-white/65">
      <span className="font-semibold tracking-wide text-primary uppercase">{t(publication.kind)}</span>
      <span aria-hidden="true" className="mx-2">·</span>
      <time dateTime={publication.publishedOn}>{formatDate(publication.publishedOn, locale)}</time>
    </p>
  );
}

/** La dernière publication en grand : page entière, résumé, format et taille, Consulter et Télécharger. */
async function Featured({ publication, locale }: { publication: Publication; locale: Locale }) {
  const t = await getTranslations();
  const textLang = langAttr(publication.lang, locale);
  const { title, fileUrl, fileType, fileSizeBytes } = publication;
  return (
    <article className="group flex h-full flex-col gap-6 rounded-2xl bg-white/[0.06] p-5 ring-1 ring-white/10 sm:flex-row sm:items-center md:gap-8 md:p-8">
      <Page publication={publication} sizes="(min-width: 768px) 240px, 176px" className="aspect-[210/297] w-44 rounded-lg md:w-60" />
      <div className="flex min-w-0 flex-1 flex-col">
        <Meta publication={publication} locale={locale} />
        <h3 lang={textLang} className="mt-3 font-heading text-xl leading-snug font-bold text-white md:text-2xl">{title}</h3>
        {publication.summary && <p lang={textLang} className="mt-3 line-clamp-3 text-white/75">{publication.summary}</p>}
        {(fileType || fileSizeBytes) && (
          <p className="mt-4 text-sm font-semibold text-white/65">
            {fileType}
            {fileType && fileSizeBytes ? <span aria-hidden="true" className="mx-1.5">•</span> : null}
            {fileSizeBytes ? formatFileSize(fileSizeBytes, locale) : null}
          </p>
        )}
        {fileUrl ? (
          <div className="mt-6 flex flex-wrap gap-3">
            <a
              href={fileUrl}
              target="_blank"
              rel="noopener"
              aria-label={t("Resources.consultLabel", { title })}
              className={`inline-flex h-11 items-center gap-2 rounded-full bg-primary px-6 text-sm font-semibold text-primary-foreground shadow-sm transition-colors hover:bg-primary/90 ${FOCUS}`}
            >
              <EyeIcon className="size-4" aria-hidden="true" />{t("Resources.consult")}
            </a>
            <a
              href={fileUrl}
              download
              aria-label={t("Resources.downloadLabel", { title })}
              className={`inline-flex h-11 items-center gap-2 rounded-full px-6 text-sm font-semibold text-white ring-1 ring-white/25 transition-colors hover:bg-white/10 hover:ring-white/40 ${FOCUS}`}
            >
              <DownloadIcon className="size-4" aria-hidden="true" />{t("Common.download")}
            </a>
          </div>
        ) : (
          <p className="mt-6 text-sm text-white/65">{t("Common.comingSoon")}</p>
        )}
      </div>
    </article>
  );
}

/** Les suivantes : petite page entière, type et date, titre ; toute la carte ouvre le PDF (nouvel onglet). */
async function Compact({ publication, locale }: { publication: Publication; locale: Locale }) {
  const t = await getTranslations();
  const textLang = langAttr(publication.lang, locale);
  const card = "group relative flex items-center gap-4 rounded-2xl bg-white/[0.06] p-3 pr-5 ring-1 ring-white/10 transition-colors";
  const content = (
    <>
      <Page publication={publication} sizes="80px" className="aspect-[210/297] w-16 rounded-md sm:w-20" />
      <div className="min-w-0">
        <Meta publication={publication} locale={locale} />
        <p lang={textLang} className="mt-1.5 line-clamp-3 leading-snug font-semibold text-white group-hover:text-primary">
          {publication.title}
        </p>
      </div>
    </>
  );
  return (
    <li>
      {publication.fileUrl ? (
        <a href={publication.fileUrl} target="_blank" rel="noopener" className={`${card} hover:bg-white/[0.1] ${FOCUS}`}>
          {content}
          <span className="sr-only"> {t("Footer.newTab")}</span>
        </a>
      ) : (
        <div className={card}>{content}</div>
      )}
    </li>
  );
}

/**
 * Accueil — publications récentes (modèle de section « Vidéos » fourni par l'ANAQ-Sup, 14/10/2026) :
 * fond anthracite, la dernière publication en grand à gauche, les trois suivantes à côté.
 */
export async function RecentPublications({ publications }: { publications: Publication[] }) {
  const locale = await getLocale();
  const t = await getTranslations("Home");
  const [latest, ...others] = publications;

  return (
    <section aria-labelledby="publications" className="bg-anthracite text-white">
      <Reveal className="mx-auto max-w-7xl px-4 py-16">
        <div className="relative flex flex-wrap items-end justify-between gap-x-3 border-b border-white/15">
          <h2 id="publications" className="flex items-center gap-3 pb-4 text-2xl font-bold md:text-3xl">
            <span className="flex size-10 items-center justify-center rounded-full ring-2 ring-white">
              <FileTextIcon className="size-5" aria-hidden="true" />
            </span>
            {t("recentPublications")}
          </h2>
          {/* Trait or posé sur le filet, sous le titre (même quand le lien passe à la ligne sur mobile). */}
          <span aria-hidden="true" className="absolute -bottom-px left-0 h-1 w-20 rounded-full bg-primary" />
          <Link
            href="/ressources-documentaires/publications"
            className={`mb-4 inline-flex items-center gap-1.5 text-sm font-semibold text-white transition-colors hover:text-primary ${FOCUS}`}
          >
            {t("allPublications")} <ArrowRightIcon className="size-4" aria-hidden="true" />
          </Link>
        </div>

        {latest ? (
          <div className={others.length > 0 ? "mt-8 grid gap-6 lg:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)]" : "mt-8"}>
            <Featured publication={latest} locale={locale} />
            {others.length > 0 && (
              <ul className="flex flex-col gap-4">
                {others.map((publication) => <Compact key={publication.slug} publication={publication} locale={locale} />)}
              </ul>
            )}
          </div>
        ) : (
          <p className="mt-8 text-white/70">{t("noPublications")}</p>
        )}
      </Reveal>
    </section>
  );
}
