import type { EventPage, NewsPage } from "@anaqsup/api-client";
import { ArrowRightIcon, ClockIcon, MapPinIcon } from "lucide-react";
import { getLocale, getTranslations } from "@/i18n/server";

import { Card, CardContent } from "@anaqsup/ui/components/card";

import { CategoryBadge } from "@/components/content/CategoryBadge";
import { CoverImage } from "@/components/content/CoverImage";
import { Link } from "@/i18n/navigation";
import { calendarDay, formatDate } from "@/lib/format";
import { langAttr } from "@/lib/lang";

type NewsSummary = NewsPage["content"][number];
type EventSummary = EventPage["content"][number];

/**
 * Carte d'actualité, sobre et compacte (modèle fourni par l'ANAQ-Sup) : image en haut, dégagée (aucun badge
 * posé dessus) ; puis rubrique, titre, chapeau, et sous un filet pointillé la date et « Lire la suite ».
 * Au survol, seule l'image zoome.
 */
export async function NewsCard({ item }: { item: NewsSummary }) {
  const locale = await getLocale();
  const t = await getTranslations("Common");
  const textLang = langAttr(item.lang, locale);
  return (
    <Link href={{ pathname: "/actualites/[slug]", params: { slug: item.slug } }} className="group block h-full rounded-box focus-visible:outline-none">
      <article className="flex h-full flex-col overflow-hidden rounded-box border bg-card shadow-[0_1px_3px_rgb(0_0_0/0.05)] transition-shadow duration-300 group-hover:shadow-lg group-focus-visible:ring-2 group-focus-visible:ring-ring">
        <div className="relative aspect-[16/10] overflow-hidden bg-muted">
          <CoverImage
            src={item.coverImageUrl}
            sizes="(min-width: 1280px) 400px, (min-width: 640px) 50vw, 100vw"
            className="transition-transform duration-500 ease-out group-hover:scale-105"
          />
        </div>
        <div className="flex flex-1 flex-col px-6 pt-5 pb-5">
          <CategoryBadge slug={item.category.slug} label={item.category.label} className="self-start" />
          <h3 lang={textLang} className="mt-3.5 line-clamp-3 font-heading text-lg leading-snug font-bold text-foreground transition-colors group-hover:text-brand-ink">
            {item.title}
          </h3>
          {item.summary && <p lang={textLang} className="mt-2 line-clamp-3 text-[0.9375rem] leading-relaxed text-muted-foreground">{item.summary}</p>}
          <div className="mt-auto pt-5">
            <div className="flex items-center justify-between gap-3 border-t border-dashed pt-4 text-sm">
              <time dateTime={item.publishedAt} className="inline-flex items-center gap-1.5 text-muted-foreground">
                <ClockIcon className="size-4" aria-hidden="true" />
                {formatDate(item.publishedAt, locale)}
              </time>
              <span className="inline-flex shrink-0 items-center gap-1.5 font-semibold text-brand-ink">
                {t("readMore")}
                <ArrowRightIcon className="size-4 transition-transform group-hover:translate-x-1" aria-hidden="true" />
              </span>
            </div>
          </div>
        </div>
      </article>
    </Link>
  );
}

/** Carte d'événement avec pastille de date. */
export async function EventCard({ event }: { event: EventSummary }) {
  const locale = await getLocale();
  const { day, month } = calendarDay(event.startsAt, locale);
  const textLang = langAttr(event.lang, locale);
  return (
    <Link href={{ pathname: "/evenements/[slug]", params: { slug: event.slug } }} className="group block h-full rounded-box focus-visible:outline-none">
      <Card className="h-full transition-all duration-300 ease-out group-hover:scale-[1.03] group-hover:shadow-xl group-focus-visible:ring-2 group-focus-visible:ring-ring">
        <CardContent className="flex gap-4">
          <time dateTime={event.startsAt} className="flex size-16 shrink-0 flex-col items-center justify-center rounded-control bg-primary text-primary-foreground shadow-sm">
            <span className="font-heading text-2xl font-extrabold leading-none">{day}</span>
            <span className="text-xs font-semibold uppercase">{month}</span>
          </time>
          <div>
            <p lang={textLang} className="font-heading font-bold leading-snug text-brand-ink">{event.title}</p>
            {event.location && (
              <p className="mt-1 inline-flex items-center gap-1 text-sm text-muted-foreground">
                <MapPinIcon className="size-3.5" aria-hidden="true" /> {event.location}
              </p>
            )}
            {event.summary && <p lang={textLang} className="mt-2 text-sm text-muted-foreground">{event.summary}</p>}
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
