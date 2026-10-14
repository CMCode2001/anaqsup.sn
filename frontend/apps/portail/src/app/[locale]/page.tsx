import type { Metadata } from "next";
import { ArrowRightIcon, Building2Icon, GraduationCapIcon } from "lucide-react";
import { getLocale, getTranslations } from "@/i18n/server";

import { Button } from "@anaqsup/ui/components/button";
import { MotionProvider, Reveal } from "@anaqsup/ui/components/motion/motion";

import { NewsHero } from "@/components/content/NewsHero";
import { HeroCarousel } from "@/components/home/HeroCarousel";
import { EventsAgenda } from "@/components/home/EventsAgenda";
import { PartnersMarquee } from "@/components/home/PartnersMarquee";
import { RecentPublications } from "@/components/home/RecentPublications";
import { SecretaryMessage } from "@/components/home/SecretaryMessage";
import { SearchForm } from "@/components/registre/SearchForm";
import { Link } from "@/i18n/navigation";
import type { StaticPathname } from "@/i18n/routing";
import { api, orNull } from "@/lib/api";
import { alternates } from "@/lib/metadata";
import { intro } from "@/lib/settings";

// Rendue à chaque requête : contenus vivants ou filtres dans l'URL (données en cache côté serveur).
export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  return { alternates: alternates(await getLocale(), "/") };
}

function SectionTitle({ id, title, href, linkLabel }: { id: string; title: string; href: StaticPathname; linkLabel: string }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-3">
      <h2 id={id} className="text-2xl font-bold text-brand-ink md:text-3xl">{title}</h2>
      <Link href={href} className="inline-flex items-center gap-1 text-sm font-semibold text-brand-ink hover:underline underline-offset-4">
        {linkLabel} <ArrowRightIcon className="size-4" aria-hidden="true" />
      </Link>
    </div>
  );
}

/**
 * Accueil — ordre validé par l'ANAQ-Sup (13/10/2026) : l'article à la une, le mot du Secrétaire exécutif
 * (mis en avant ici, hors du menu), l'agenda des événements, les publications, les habilitations et
 * accréditations, puis les partenaires.
 */
export default async function HomePage() {
  const lang = await getLocale();
  const t = await getTranslations();
  const [news, upcoming, message, publications, partners] = await Promise.all([
    orNull(api.news({ lang, size: 5 })),
    orNull(api.events({ lang, period: "upcoming", size: 4 })),
    orNull(api.page("mot-du-secretaire-executif", lang)),
    orNull(api.publications({ lang, size: 4 })),
    orNull(api.partners()),
  ]);
  // Carrousel « À la une » : l'article épinglé d'abord, puis les plus récents (ordre de l'API).
  const headlines = news?.content ?? [];
  // Sans événement à venir, l'agenda montre les plus récents.
  const past = upcoming && upcoming.content.length === 0 ? await orNull(api.events({ lang, period: "past", size: 4 })) : null;
  const agenda = upcoming?.content.length ? upcoming.content : (past?.content ?? []);

  return (
    <MotionProvider>
      {/* 1. À la une : bandeau pleine largeur, carrousel de l'article épinglé puis des plus récents.
          Visible immédiatement : aucune animation d'apparition sur le haut de page (LCP). */}
      <section aria-labelledby="une" className="relative">
        {/* Titre lu par les lecteurs d'écran ; à l'écran, le bandeau parle de lui-même (décision ANAQ-Sup). */}
        <h2 id="une" className="sr-only">{t("Home.featured")}</h2>
        {headlines.length > 0 ? (
          <HeroCarousel
            labels={{
              region: t("Home.carousel"), previous: t("Home.carouselPrevious"), next: t("Home.carouselNext"),
              pause: t("Home.carouselPause"), play: t("Home.carouselPlay"),
              // Gabarits complétés par le carrousel ({n}, {total}) : passés tels quels.
              goTo: t.raw("Home.carouselGoTo") as string, slide: t.raw("Home.carouselSlide") as string,
            }}
          >
            {headlines.map((item, i) => (
              <NewsHero key={item.slug} item={item} framed={false} bleed eager={i === 0} badge="category" />
            ))}
          </HeroCarousel>
        ) : (
          <p className="mx-auto max-w-7xl px-4 py-10 text-muted-foreground">{t("Home.noNews")}</p>
        )}
      </section>

      {/* 2. Le mot du Secrétaire exécutif (photo, nom et fonction, accroche), géré dans le back-office. */}
      {message && (
        <Reveal>
          <SecretaryMessage message={message} />
        </Reveal>
      )}

      {/* 3. Événements : agenda. */}
      <section aria-labelledby="evenements" className="border-t">
        <div className="mx-auto max-w-7xl px-4 py-14">
          <Reveal>
            <SectionTitle
              id="evenements"
              title={upcoming?.content.length ? t("Home.upcomingEvents") : t("Home.recentEvents")}
              href="/evenements"
              linkLabel={t("Home.allEvents")}
            />
            {agenda.length > 0 ? (
              <div className="mt-8"><EventsAgenda events={agenda} /></div>
            ) : (
              <p className="mt-6 text-muted-foreground">{t("Home.noEvents")}</p>
            )}
          </Reveal>
        </div>
      </section>

      {/* 4. Publications récentes : la dernière en grand, les trois suivantes à côté (fond anthracite). */}
      <RecentPublications publications={publications?.content ?? []} />

      {/* 5. Habilitations et accréditations */}
      <section aria-labelledby="habilitations" className="border-t">
        <div className="mx-auto max-w-7xl px-4 py-14">
          <Reveal>
            <h2 id="habilitations" className="text-2xl font-bold text-brand-ink md:text-3xl">{t("Home.approvalsTitle")}</h2>
            <p className="mt-3 max-w-3xl text-muted-foreground">{await intro("Home.approvalsLead")}</p>
            <div className="mt-6 max-w-3xl"><SearchForm variant="hero" /></div>
            <div className="mt-6 flex flex-wrap gap-3">
              <Button asChild variant="outline"><Link href="/etablissements"><Building2Icon data-icon="inline-start" aria-hidden="true" />{t("Home.institutions")}</Link></Button>
              <Button asChild variant="outline"><Link href="/formations"><GraduationCapIcon data-icon="inline-start" aria-hidden="true" />{t("Home.programmes")}</Link></Button>
              <Button asChild variant="link"><Link href="/habilitations-et-accreditations">{t("Home.understand")}</Link></Button>
            </div>
          </Reveal>
        </div>
      </section>

      {/* 6. Partenaires (géré dans le back-office), juste avant le pied de page. */}
      {partners && <PartnersMarquee partners={partners} />}
    </MotionProvider>
  );
}
