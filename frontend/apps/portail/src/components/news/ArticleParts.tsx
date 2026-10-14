import type { NewsItem, NewsPage } from "@anaqsup/api-client";
import { ArrowLeftIcon, ArrowRightIcon, BookOpenIcon, CalendarDaysIcon, ClockIcon, RefreshCwIcon, ZapIcon } from "lucide-react";

import { cn } from "@anaqsup/ui/lib/utils";

import { categoryTextClass } from "@/components/content/CategoryBadge";
import { CoverImage } from "@/components/content/CoverImage";
import { SocialGlyph, type Glyph } from "@/components/layout/SocialGlyph";
import { Link } from "@/i18n/navigation";
import { getLocale, getTranslations } from "@/i18n/server";
import { formatDate, formatTime, readingMinutes, sinceOrDate } from "@/lib/format";
import { langAttr } from "@/lib/lang";

type NewsSummary = NewsPage["content"][number];

/** Auteur, date, heure, durée de lecture et date de mise à jour (si l'article a été modifié ensuite). */
export async function ArticleMeta({ item }: { item: NewsItem }) {
  const locale = await getLocale();
  const t = await getTranslations("News");
  const updated = new Date(item.updatedAt).getTime() - new Date(item.publishedAt).getTime() > 3_600_000;
  const meta = "inline-flex items-center gap-1.5";
  return (
    <div className="flex flex-wrap items-center gap-x-6 gap-y-3 border-y py-5 text-sm text-muted-foreground">
      <span className="inline-flex items-center gap-3">
        <span aria-hidden="true" className="flex size-11 items-center justify-center rounded-full bg-brand-green font-heading text-base font-bold text-white">A</span>
        <span className="leading-tight">
          <span className="block text-xs">{t("by")}</span>
          <span className="font-semibold text-foreground">{t("author")}</span>
        </span>
      </span>
      <time dateTime={item.publishedAt} className={meta}>
        <CalendarDaysIcon className="size-4" aria-hidden="true" />{formatDate(item.publishedAt, locale)}
      </time>
      <span className={meta}><ClockIcon className="size-4" aria-hidden="true" />{formatTime(item.publishedAt, locale)}</span>
      <span className={meta}><BookOpenIcon className="size-4" aria-hidden="true" />{t("readingTime", { minutes: readingMinutes(item.bodyHtml) })}</span>
      {updated && (
        <span className={meta}>
          <RefreshCwIcon className="size-4" aria-hidden="true" />
          {t("updated", { date: formatDate(item.updatedAt, locale), time: formatTime(item.updatedAt, locale) })}
        </span>
      )}
    </div>
  );
}

/** Liens de partage (sans script tiers) : verticaux à côté du texte sur grand écran, en ligne sinon. */
export async function ShareLinks({ url, title, orientation }: { url: string; title: string; orientation: "vertical" | "horizontal" }) {
  const t = await getTranslations("News");
  const u = encodeURIComponent(url);
  const links: { network: Glyph; label: string; href: string }[] = [
    { network: "facebook", label: "Facebook", href: `https://www.facebook.com/sharer/sharer.php?u=${u}` },
    { network: "x", label: "X", href: `https://x.com/intent/tweet?url=${u}&text=${encodeURIComponent(title)}` },
    { network: "linkedin", label: "LinkedIn", href: `https://www.linkedin.com/sharing/share-offsite/?url=${u}` },
    { network: "whatsapp", label: "WhatsApp", href: `https://wa.me/?text=${encodeURIComponent(`${title} ${url}`)}` },
  ];
  return (
    <ul aria-label={t("shareLabel")} className={cn("flex gap-3 print:hidden", orientation === "vertical" ? "flex-col" : "flex-row flex-wrap")}>
      {links.map((link) => (
        <li key={link.network}>
          <a
            href={link.href}
            target="_blank"
            rel="noopener noreferrer"
            title={link.label}
            className="inline-flex size-11 items-center justify-center rounded-[0.625rem] border bg-background text-foreground shadow-sm transition hover:scale-110 hover:border-primary hover:bg-primary hover:text-primary-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
          >
            <SocialGlyph network={link.network} className="size-5" />
            <span className="sr-only">{t("shareOn", { network: link.label })}</span>
          </a>
        </li>
      ))}
    </ul>
  );
}

/** Colonne « Articles récents » : vignette, rubrique, titre, ancienneté. */
export async function RecentArticles({ items }: { items: NewsSummary[] }) {
  const locale = await getLocale();
  const t = await getTranslations("News");
  if (items.length === 0) return null;
  return (
    <section aria-labelledby="recent-articles" className="rounded-3xl border bg-card p-6 shadow-sm print:hidden">
      <h2 id="recent-articles" className="flex items-center gap-2 text-sm font-bold tracking-[0.15em] text-foreground uppercase">
        <ZapIcon className="size-4 text-brand-green" aria-hidden="true" />{t("recent")}
      </h2>
      <ul className="mt-5 flex flex-col gap-5">
        {items.map((item) => (
          <li key={item.slug}>
            <Link href={{ pathname: "/actualites/[slug]", params: { slug: item.slug } }} className="group flex gap-4 rounded-2xl focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary">
              <span className="relative size-20 shrink-0 overflow-hidden rounded-xl bg-muted xl:size-24">
                <CoverImage src={item.coverImageUrl} sizes="96px" className="transition-transform duration-500 group-hover:scale-110" />
              </span>
              <span className="min-w-0">
                <span className={cn("block truncate text-[0.6875rem] font-semibold tracking-wide uppercase", categoryTextClass(item.category.slug))}>
                  {item.category.label}
                </span>
                <span lang={langAttr(item.lang, locale)} className="mt-1 line-clamp-3 text-[0.9375rem] leading-snug font-semibold text-foreground group-hover:text-brand-ink">
                  {item.title}
                </span>
                <time dateTime={item.publishedAt} className="mt-1 block text-xs text-muted-foreground">{sinceOrDate(item.publishedAt, locale)}</time>
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

/** « Article précédent » (plus ancien) et « Article suivant » (plus récent). */
export async function ArticleNeighbours({ previous, next }: { previous: NewsItem["previous"]; next: NewsItem["next"] }) {
  const locale = await getLocale();
  const t = await getTranslations("News");
  if (!previous && !next) return null;
  const card = "group flex h-full flex-col gap-2 rounded-3xl border bg-card p-6 shadow-sm transition hover:border-primary hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";
  return (
    <nav aria-label={t("neighbours")} className="mt-8 grid gap-4 sm:grid-cols-2 print:hidden">
      <div>
        {previous && (
          <Link href={{ pathname: "/actualites/[slug]", params: { slug: previous.slug } }} rel="prev" className={card}>
            <span className="inline-flex items-center gap-1.5 text-xs font-semibold tracking-[0.15em] text-muted-foreground uppercase">
              <ArrowLeftIcon className="size-3.5 transition-transform group-hover:-translate-x-1" aria-hidden="true" />{t("previous")}
            </span>
            <span lang={langAttr(previous.lang, locale)} className="line-clamp-2 font-heading font-bold text-brand-ink">{previous.title}</span>
          </Link>
        )}
      </div>
      <div>
        {next && (
          <Link href={{ pathname: "/actualites/[slug]", params: { slug: next.slug } }} rel="next" className={cn(card, "items-end text-right")}>
            <span className="inline-flex items-center gap-1.5 text-xs font-semibold tracking-[0.15em] text-muted-foreground uppercase">
              {t("next")}<ArrowRightIcon className="size-3.5 transition-transform group-hover:translate-x-1" aria-hidden="true" />
            </span>
            <span lang={langAttr(next.lang, locale)} className="line-clamp-2 font-heading font-bold text-brand-ink">{next.title}</span>
          </Link>
        )}
      </div>
    </nav>
  );
}
