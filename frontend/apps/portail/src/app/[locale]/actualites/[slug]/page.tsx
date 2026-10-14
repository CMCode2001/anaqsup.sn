import { ArrowRightIcon } from "lucide-react";
import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { NewsCard } from "@/components/content/Cards";
import { CategoryBadge } from "@/components/content/CategoryBadge";
import { CoverImage } from "@/components/content/CoverImage";
import { FrenchOnlyNotice } from "@/components/content/FrenchOnlyNotice";
import { CATEGORY_PAGES } from "@/components/content/NewsList";
import { ARTICLE_TEXT, RichText } from "@/components/content/RichText";
import { ArticleMeta, ArticleNeighbours, RecentArticles, ShareLinks } from "@/components/news/ArticleParts";
import { ArticleToolbar } from "@/components/news/ArticleToolbar";
import { NewsReactions, type ReactionLabels } from "@/components/news/NewsReactions";
import { getPathname, type Href, Link } from "@/i18n/navigation";
import { getLocale, getTranslations } from "@/i18n/server";
import { api, orNull } from "@/lib/api";
import { langAttr, servedLang } from "@/lib/lang";
import { alternates } from "@/lib/metadata";
import { site } from "@/lib/site";

export const revalidate = 600;

export function generateStaticParams() {
  return [];
}

type Props = { params: Promise<{ slug: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const lang = await getLocale();
  const item = await api.newsItem(slug, lang);
  if (!item) return { title: (await getTranslations("News"))("notFound"), robots: { index: false } };
  return {
    title: item.title,
    description: item.summary ?? undefined,
    alternates: alternates(lang, { pathname: "/actualites/[slug]", params: { slug } }, servedLang(item.lang)),
    // Aperçu riche lors du partage (WhatsApp, Facebook…).
    openGraph: {
      title: item.title,
      description: item.summary ?? undefined,
      type: "article",
      publishedTime: item.publishedAt,
      modifiedTime: item.updatedAt,
      images: item.coverImageUrl ? [{ url: item.coverImageUrl }] : undefined,
    },
  };
}

/**
 * Article : rubrique, titre, auteur et dates, grande image ; texte avec lettrine, outils de lecture et
 * partage ; « Articles récents » à droite ; article précédent/suivant et « Articles similaires »
 * (même rubrique) en bas.
 */
export default async function ActualitePage({ params }: Props) {
  const { slug } = await params;
  const lang = await getLocale();
  const t = await getTranslations("News");
  const item = await api.newsItem(slug, lang);
  if (!item) notFound();
  const contentLang = langAttr(item.lang, lang);

  // Colonnes annexes : une rubrique indisponible ne doit pas faire tomber l'article.
  const [recent, sameCategory] = await Promise.all([
    orNull(api.news({ lang, size: 6 })),
    orNull(api.news({ lang, category: item.category.slug, size: 4 })),
  ]);
  const recentItems = (recent?.content ?? [])
    .filter((n) => n.slug !== item.slug)
    .sort((a, b) => b.publishedAt.localeCompare(a.publishedAt))
    .slice(0, 5);
  const similarItems = (sameCategory?.content ?? []).filter((n) => n.slug !== item.slug).slice(0, 3);

  // Mentions « J'aime » / « Je n'aime pas » : seul le nombre de « J'aime » est public.
  const reactions: ReactionLabels = {
    group: t("reactionsGroup"), question: t("reactionsQuestion"), like: t("like"), dislike: t("dislike"),
    // Gabarit complété par le composant ({count}) : passé tel quel.
    likeCount: t.raw("likeCount") as string,
    thanks: t("reactionThanks"), removed: t("reactionRemoved"), error: t("reactionError"),
  };

  const url = site.url + getPathname({ locale: lang, href: { pathname: "/actualites/[slug]", params: { slug } } });
  const categoryHref: Href = item.category.slug in CATEGORY_PAGES
    ? CATEGORY_PAGES[item.category.slug as keyof typeof CATEGORY_PAGES]
    : { pathname: "/actualites", query: { categorie: item.category.slug } };

  return (
    <>
      <article className="mx-auto max-w-7xl px-4 pt-10 pb-16 md:pt-14">
        <header className="max-w-4xl">
          <CategoryBadge slug={item.category.slug} label={item.category.label} />
          <h1 lang={contentLang} className="mt-5 text-3xl leading-tight font-extrabold text-brand-ink md:text-5xl md:leading-[1.1]">
            {item.title}
          </h1>
          <div className="mt-8"><ArticleMeta item={item} /></div>
        </header>

        {item.coverImageUrl && (
          <figure className="relative mt-8 aspect-[16/9] overflow-hidden rounded-3xl bg-muted shadow-lg md:aspect-[2/1]">
            <CoverImage src={item.coverImageUrl} sizes="(min-width: 1152px) 1120px, 100vw" eager />
          </figure>
        )}

        <div className="mt-10 grid gap-10 lg:grid-cols-[3rem_minmax(0,1fr)_20rem] xl:grid-cols-[3rem_minmax(0,1fr)_25rem] xl:gap-12">
          <div className="hidden lg:block">
            <div className="sticky top-36"><ShareLinks url={url} title={item.title} orientation="vertical" /></div>
          </div>

          <div className="min-w-0">
            <div className="flex flex-wrap items-center justify-end gap-2">
              <NewsReactions slug={item.slug} variant="toolbar" labels={reactions} />
              <span aria-hidden="true" className="mx-1 h-6 w-px bg-border print:hidden" />
              <ArticleToolbar
                targetId="article-body"
                title={item.title}
                labels={{ tools: t("tools"), smaller: t("smaller"), larger: t("larger"), print: t("print"), share: t("share"), copied: t("copied") }}
              />
            </div>
            <FrenchOnlyNotice lang={item.lang} className="mt-6" />
            <RichText id="article-body" html={item.bodyHtml} lang={contentLang} className={`mt-6 ${ARTICLE_TEXT}`} />
            <NewsReactions slug={item.slug} variant="panel" labels={reactions} />
            <div className="mt-10 lg:hidden"><ShareLinks url={url} title={item.title} orientation="horizontal" /></div>
            <ArticleNeighbours previous={item.previous} next={item.next} />
          </div>

          <div className="lg:sticky lg:top-36 lg:self-start">
            <RecentArticles items={recentItems} />
          </div>
        </div>
      </article>

      {similarItems.length > 0 && (
        <section aria-labelledby="similar-articles" className="border-t bg-muted/40 py-16 print:hidden">
          <div className="mx-auto max-w-7xl px-4">
            <div className="flex flex-wrap items-end justify-between gap-4 border-b pb-4">
              <h2 id="similar-articles" className="relative text-2xl font-extrabold text-foreground after:absolute after:-bottom-[18px] after:left-0 after:h-1 after:w-20 after:rounded-full after:bg-brand-green md:text-3xl">
                {t("similar")}
              </h2>
              <Link href={categoryHref} className="group inline-flex items-center gap-1.5 font-semibold text-brand-ink">
                {item.category.label}
                <ArrowRightIcon className="size-4 transition-transform group-hover:translate-x-1" aria-hidden="true" />
              </Link>
            </div>
            <ul className="mt-10 grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
              {similarItems.map((news) => <li key={news.slug}><NewsCard item={news} /></li>)}
            </ul>
          </div>
        </section>
      )}
    </>
  );
}
