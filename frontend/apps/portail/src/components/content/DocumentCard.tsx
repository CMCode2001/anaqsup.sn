import type { PublicationPage } from "@anaqsup/api-client";
import { DownloadIcon, EyeIcon, FileTextIcon } from "lucide-react";

import { CoverImage } from "@/components/content/CoverImage";
import { getLocale, getTranslations } from "@/i18n/server";
import { formatFileSize, formatShortDate } from "@/lib/format";
import { langAttr } from "@/lib/lang";

type Publication = PublicationPage["content"][number];

/**
 * Carte de document : vignette (première page du PDF), catégorie, titre, format, taille et date,
 * puis « Télécharger » et « Consulter » (le PDF s'ouvre dans un nouvel onglet).
 */
export async function DocumentCard({ publication }: { publication: Publication }) {
  const locale = await getLocale();
  const t = await getTranslations();
  const textLang = langAttr(publication.lang, locale);
  const { title, fileUrl, fileType, fileSizeBytes, publishedOn } = publication;

  return (
    <article className="group flex h-full flex-col rounded-box border bg-card p-4 shadow-sm transition-all duration-300 ease-out hover:scale-[1.02] hover:shadow-xl">
      <div className="relative aspect-[4/3] overflow-hidden rounded-box border bg-muted">
        {publication.coverImageUrl ? (
          <CoverImage
            src={publication.coverImageUrl}
            sizes="(min-width: 1024px) 352px, (min-width: 640px) 50vw, 100vw"
            className="object-top transition-transform duration-500 ease-out group-hover:scale-105"
          />
        ) : (
          <div aria-hidden="true" className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-primary/10">
            <span className="flex size-14 items-center justify-center rounded-full bg-background text-brand-ink shadow-sm">
              <FileTextIcon className="size-6" />
            </span>
            <span className="text-xs font-semibold tracking-[0.2em] text-brand-ink uppercase">{t("Resources.documentPlaceholder")}</span>
          </div>
        )}
      </div>

      <p className="mt-5 text-xs font-semibold tracking-wide text-muted-foreground uppercase">{t(`PublicationKind.${publication.kind}`)}</p>
      <h3 lang={textLang} className="mt-2 line-clamp-3 font-heading text-base leading-snug font-bold text-brand-ink">{title}</h3>
      <p className="mt-3 text-sm font-semibold text-foreground">
        {fileType && <>{fileType}<span aria-hidden="true" className="mx-1.5 text-muted-foreground">•</span></>}
        {publishedOn.slice(0, 4)}
      </p>
      <div className="mt-1 flex items-center justify-between gap-3 text-sm text-muted-foreground">
        <span>{fileSizeBytes ? formatFileSize(fileSizeBytes, locale) : ""}</span>
        <time dateTime={publishedOn}>{formatShortDate(publishedOn, locale)}</time>
      </div>

      <div className="mt-auto pt-5">
        <div className="flex gap-2 border-t pt-4">
          {fileUrl ? (
            <>
              <a
                href={fileUrl}
                download
                aria-label={t("Resources.downloadLabel", { title })}
                className="inline-flex size-10 shrink-0 items-center justify-center rounded-full border text-brand-ink transition-colors hover:border-primary hover:bg-primary/10 focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
              >
                <DownloadIcon className="size-4" aria-hidden="true" />
              </a>
              <a
                href={fileUrl}
                target="_blank"
                rel="noopener"
                aria-label={t("Resources.consultLabel", { title })}
                className="inline-flex h-10 flex-1 items-center justify-center gap-2 rounded-full bg-primary px-4 text-sm font-semibold text-primary-foreground shadow-sm transition-colors hover:bg-primary/90 focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:outline-none"
              >
                <EyeIcon className="size-4" aria-hidden="true" />
                {t("Resources.consult")}
              </a>
            </>
          ) : (
            <span className="inline-flex h-10 flex-1 items-center justify-center rounded-full bg-muted px-4 text-sm text-muted-foreground">
              {t("Common.comingSoon")}
            </span>
          )}
        </div>
      </div>
    </article>
  );
}
