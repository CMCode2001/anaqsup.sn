import type { NewsPage } from "@anaqsup/api-client";
import { ArrowRightIcon, StarIcon } from "lucide-react";

import { CoverImage } from "@/components/content/CoverImage";
import { Link } from "@/i18n/navigation";
import { getLocale, getTranslations } from "@/i18n/server";
import { formatDate } from "@/lib/format";
import { langAttr } from "@/lib/lang";

type NewsSummary = NewsPage["content"][number];

/**
 * Grande carte « À la une ». À partir de la tablette : image en plein cadre, texte en surimpression. Sur téléphone :
 * photo entière au format des photos de l'ANAQ-Sup (3:2), texte en dessous ; un écran étroit et haut recadrait
 * sinon la photo jusqu'à n'en montrer qu'une bande zoomée.
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
   * Bandeau de l'accueil : pleine largeur et, à partir de la tablette, toute la hauteur de l'écran sous l'en-tête
   * (109 px), pour ne voir que lui à l'arrivée ; le texte reste aligné sur le site.
   */
  bleed?: boolean;
}) {
  const locale = await getLocale();
  const t = await getTranslations();
  const textLang = langAttr(item.lang, locale);
  const href = { pathname: "/actualites/[slug]", params: { slug: item.slug } } as const;

  return (
    <article className={`group relative isolate flex h-full flex-col overflow-hidden bg-anthracite md:flex-row ${bleed ? "md:h-[calc(100svh-109px)] md:min-h-[30rem]" : "md:min-h-[32rem]"} ${framed ? "rounded-box shadow-xl" : ""}`}>
      {/* Téléphone : photo entière (3:2) en haut. Tablette et plus : photo en fond de toute la carte. */}
      {/* Pas d'overflow-hidden ici : la carte rogne déjà, et le fondu doit déborder (voir plus bas). */}
      <div className="relative aspect-[3/2] w-full shrink-0 md:absolute md:inset-0 md:-z-20 md:aspect-auto">
        <CoverImage
          src={item.coverImageUrl}
          sizes={bleed ? "100vw" : "(min-width: 1280px) 1248px, 100vw"}
          eager={eager}
          // Bandeau : cadré un peu au-dessus du centre (visages), sans zoom au survol. Carte : zoom au survol à la
          // souris seulement (grand écran), la photo débordant sinon sur le texte.
          className={bleed ? "md:object-[center_35%]" : "transition-transform duration-700 ease-out md:group-hover:scale-105"}
        />
        {/* Téléphone : fondu de la photo vers le fond du texte. Il dépasse d'un pixel : la hauteur de la photo
            tombe souvent entre deux pixels (3:2 d'une largeur quelconque) et laissait voir un liseré clair. */}
        <div aria-hidden="true" className="absolute inset-x-0 -bottom-px h-12 bg-gradient-to-t from-anthracite to-transparent md:hidden" />
      </div>
      {/* Voile sombre sous le texte en surimpression : lisible quelle que soit la photo (contraste ≥ 4,5:1). */}
      <div aria-hidden="true" className="absolute inset-0 -z-10 hidden bg-gradient-to-r from-black/85 via-black/55 to-transparent md:block" />

      <div className={bleed ? "mx-auto w-full max-w-7xl px-4 pt-4 pb-6 md:mt-auto md:pt-20 md:pb-14" : "p-5 pt-4 md:mt-auto md:p-10"}>
      <div className="flex max-w-3xl flex-col gap-3 text-white md:gap-4">
        <span className="inline-flex w-fit items-center gap-1.5 rounded-full bg-primary px-3.5 py-1.5 text-xs font-bold tracking-wide text-primary-foreground uppercase">
          {badge === "featured" && <StarIcon className="size-3.5 fill-current" aria-hidden="true" />}
          {badge === "featured" ? t("News.featured") : item.category.label}
        </span>
        <h2 lang={textLang} className="line-clamp-3 text-2xl leading-tight font-extrabold text-balance md:line-clamp-none md:text-4xl">
          {/* Lien étendu à toute la carte (after:inset-0) : un seul lien pour les lecteurs d'écran. */}
          <Link href={href} className="after:absolute after:inset-0 after:content-[''] focus-visible:outline-none">
            {item.title}
          </Link>
        </h2>
        {item.summary && <p lang={textLang} className="line-clamp-2 text-base text-white/85 md:line-clamp-3 md:text-lg">{item.summary}</p>}
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
      <span aria-hidden="true" className={`pointer-events-none absolute inset-0 ring-primary group-has-[:focus-visible]:ring-4 ${framed ? "rounded-box ring-offset-2" : "ring-inset"}`} />
    </article>
  );
}
