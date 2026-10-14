import type { NewsPage } from "@anaqsup/api-client";
import { ArrowRightIcon, StarIcon } from "lucide-react";

import { CoverImage } from "@/components/content/CoverImage";
import { Link } from "@/i18n/navigation";
import { getLocale, getTranslations } from "@/i18n/server";
import { formatDate } from "@/lib/format";
import { langAttr } from "@/lib/lang";

type NewsSummary = NewsPage["content"][number];

/**
 * Grande carte « À la une » : image mise en avant en plein cadre, texte en surimpression.
 * L'article affiché est celui que l'éditeur a épinglé, sinon le plus récent (ordre renvoyé par l'API).
 * Dans le carrousel de l'accueil : `framed` faux (cadre porté par le carrousel), seule la première image
 * est chargée en priorité, et les diapositives suivantes affichent leur rubrique au lieu de « À la une ».
 */
export async function NewsHero({ item, framed = true, eager = true, badge = "featured", bleed = false }: {
  item: NewsSummary;
  framed?: boolean;
  eager?: boolean;
  badge?: "featured" | "category";
  /**
   * Bandeau de l'accueil : pleine largeur et toute la hauteur de l'écran sous l'en-tête (109 px), pour ne
   * voir que lui à l'arrivée ; le texte reste aligné sur le site.
   */
  bleed?: boolean;
}) {
  const locale = await getLocale();
  const t = await getTranslations();
  const textLang = langAttr(item.lang, locale);
  const href = { pathname: "/actualites/[slug]", params: { slug: item.slug } } as const;

  return (
    <article className={`group relative isolate flex overflow-hidden bg-anthracite ${bleed ? "h-[calc(100svh-109px)] min-h-[30rem]" : "h-full min-h-[26rem] md:min-h-[32rem]"} ${framed ? "rounded-3xl shadow-xl" : ""}`}>
      <CoverImage
        src={item.coverImageUrl}
        sizes={bleed ? "100vw" : "(min-width: 1280px) 1248px, 100vw"}
        eager={eager}
        // Bandeau : photo pleine largeur, cadrée un peu au-dessus du centre (visages), sans zoom au survol.
        className={bleed ? "-z-20 object-[center_35%]" : "-z-20 transition-transform duration-700 ease-out group-hover:scale-105"}
      />
      {/* Voile sombre : texte blanc lisible quelle que soit la photo (contraste ≥ 4,5:1). */}
      <div aria-hidden="true" className="absolute inset-0 -z-10 bg-gradient-to-t from-black/85 via-black/45 to-black/10 md:bg-gradient-to-r md:from-black/85 md:via-black/55 md:to-transparent" />

      <div className={bleed ? "mx-auto mt-auto w-full max-w-7xl px-4 pt-20 pb-10 md:pb-14" : "mt-auto p-6 md:p-10"}>
      <div className="flex max-w-3xl flex-col gap-4 text-white">
        <span className="inline-flex w-fit items-center gap-1.5 rounded-full bg-primary px-3.5 py-1.5 text-xs font-bold tracking-wide text-primary-foreground uppercase">
          {badge === "featured" && <StarIcon className="size-3.5 fill-current" aria-hidden="true" />}
          {badge === "featured" ? t("News.featured") : item.category.label}
        </span>
        <h2 lang={textLang} className="text-2xl leading-tight font-extrabold text-balance md:text-4xl">
          {/* Lien étendu à toute la carte (after:inset-0) : un seul lien pour les lecteurs d'écran. */}
          <Link href={href} className="after:absolute after:inset-0 after:content-[''] focus-visible:outline-none">
            {item.title}
          </Link>
        </h2>
        {item.summary && <p lang={textLang} className="line-clamp-3 text-base text-white/85 md:text-lg">{item.summary}</p>}
        <div className="mt-2 flex flex-wrap items-center gap-4">
          <span className="inline-flex h-11 items-center gap-2 rounded-full bg-white px-5 text-sm font-semibold text-anthracite shadow-sm transition-colors group-hover:bg-primary">
            <ArrowRightIcon className="size-4 transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
            {t("Common.readMore")}
          </span>
          <time dateTime={item.publishedAt} className="text-sm text-white/80">{formatDate(item.publishedAt, locale)}</time>
        </div>
      </div>
      </div>
      {/* Anneau de focus visible quand le lien étendu reçoit le focus clavier. */}
      <span aria-hidden="true" className={`pointer-events-none absolute inset-0 ring-primary group-has-[:focus-visible]:ring-4 ${framed ? "rounded-3xl ring-offset-2" : "ring-inset"}`} />
    </article>
  );
}
