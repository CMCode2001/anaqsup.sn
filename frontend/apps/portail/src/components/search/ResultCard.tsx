import type { Avis, PublicationKind, SearchHit } from "@anaqsup/api-client";
import {
  ArrowUpRightIcon,
  Building2Icon,
  CalendarDaysIcon,
  FileTextIcon,
  GraduationCapIcon,
  MapPinIcon,
  NewspaperIcon,
  PanelsTopLeftIcon,
} from "lucide-react";
import Link from "next/link";

import { AvisBadge } from "@anaqsup/ui/components/avis-badge";
import { cn } from "@anaqsup/ui/lib/utils";

import { CategoryBadge } from "@/components/content/CategoryBadge";
import { CoverImage } from "@/components/content/CoverImage";
import { getLocale, getTranslations } from "@/i18n/server";
import { formatDate } from "@/lib/format";
import { langAttr } from "@/lib/lang";
import type { HitLink } from "@/lib/search";

const ICONS: Record<SearchHit["type"], typeof FileTextIcon> = {
  news: NewspaperIcon,
  events: CalendarDaysIcon,
  publications: FileTextIcon,
  establishments: Building2Icon,
  programs: GraduationCapIcon,
  pages: PanelsTopLeftIcon,
};

const CHIP = "inline-flex max-w-full items-center gap-1.5 rounded-full border bg-background px-2.5 py-0.5 text-xs font-medium text-foreground";

/**
 * Résultat de recherche : vignette (image du contenu, sinon pictogramme du type), pastilles (type, rubrique, lieu,
 * établissement ou type de document), titre, extrait et date ; avis pour le registre. Toute la carte est cliquable.
 */
export async function ResultCard({ hit, link }: { hit: SearchHit; link: HitLink }) {
  const locale = await getLocale();
  const t = await getTranslations();
  const Icon = ICONS[hit.type];
  const textLang = langAttr(hit.lang, locale);
  const registry = hit.type === "establishments" || hit.type === "programs";

  return (
    <article className="group relative flex gap-4 rounded-box border bg-card p-3 shadow-sm transition-shadow duration-300 hover:shadow-lg sm:gap-6 sm:p-4">
      <div className="relative aspect-square w-20 shrink-0 self-start overflow-hidden rounded-box border bg-muted sm:aspect-[4/3] sm:w-52">
        {hit.imageUrl ? (
          <CoverImage
            src={hit.imageUrl}
            sizes="(min-width: 640px) 208px, 80px"
            // Couverture de document (première page du PDF) : cadrée sur le haut, comme dans les ressources.
            className={cn("transition-transform duration-500 ease-out group-hover:scale-105", hit.type === "publications" && "object-top")}
          />
        ) : (
          <div aria-hidden="true" className="absolute inset-0 grid place-items-center bg-primary/10">
            <span className="grid size-10 place-items-center rounded-control bg-background text-brand-ink shadow-sm sm:size-14">
              <Icon className="size-5 sm:size-6" />
            </span>
          </div>
        )}
      </div>

      <div className="flex min-w-0 flex-1 flex-col gap-2 py-0.5 sm:py-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center gap-1.5 rounded-full border border-primary/40 bg-primary/15 px-2.5 py-0.5 text-xs font-semibold tracking-wide text-brand-ink uppercase">
            <Icon className="size-3" aria-hidden="true" />
            {t(`Search.type.${hit.type}`)}
          </span>
          {hit.type === "news" && hit.code && hit.label && <CategoryBadge slug={hit.code} label={hit.label} size="sm" />}
          {hit.type === "events" && hit.label && (
            <span className={CHIP}><MapPinIcon className="size-3 shrink-0" aria-hidden="true" /><span className="truncate">{hit.label}</span></span>
          )}
          {hit.type === "publications" && hit.code && <span className={CHIP}>{t(`PublicationKind.${hit.code as PublicationKind}`)}</span>}
          {registry && hit.label && (
            <span lang="fr" className={CHIP}><Building2Icon className="size-3 shrink-0" aria-hidden="true" /><span className="truncate">{hit.label}</span></span>
          )}
        </div>

        <h2 lang={textLang} className="font-heading text-base leading-snug font-bold text-brand-ink sm:text-lg">
          <Link
            href={link.href}
            prefetch={false}
            target={link.newTab ? "_blank" : undefined}
            rel={link.newTab ? "noopener" : undefined}
            className="rounded-sm after:absolute after:inset-0 after:rounded-box group-hover:underline group-hover:decoration-primary group-hover:decoration-2 group-hover:underline-offset-4 focus-visible:outline-none focus-visible:after:ring-2 focus-visible:after:ring-ring"
          >
            {hit.title}
            {link.newTab && (
              <>
                <ArrowUpRightIcon className="ml-1 inline size-4 align-[-0.125em] text-muted-foreground" aria-hidden="true" />
                <span className="sr-only"> {t("Search.opensInNewTab")}</span>
              </>
            )}
          </Link>
        </h2>

        {hit.excerpt && <p lang={textLang} className="line-clamp-2 text-sm text-muted-foreground sm:text-[15px]">{hit.excerpt}</p>}

        {(hit.date || (registry && hit.code)) && (
          <div className="mt-auto flex flex-wrap items-center gap-3 pt-1 text-sm text-muted-foreground">
            {hit.date && <time dateTime={hit.date}>{formatDate(hit.date, locale)}</time>}
            {registry && hit.code && <AvisBadge avis={hit.code as Avis} locale={locale} />}
          </div>
        )}
      </div>
    </article>
  );
}
