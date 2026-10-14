import type { Partner } from "@anaqsup/api-client";
import { getImageProps } from "next/image";

import { getTranslations } from "@/i18n/server";
import { intro } from "@/lib/settings";

const REPEAT = 4; // assez de copies pour couvrir la largeur, même avec peu de partenaires

function PartnerCard({ partner, hidden, newTab }: { partner: Partner; hidden: boolean; newTab: string }) {
  const content = partner.logoUrl ? (
    // eslint-disable-next-line @next/next/no-img-element -- <img> optimisé via getImageProps, sans JavaScript.
    <img
      {...getImageProps({ src: partner.logoUrl, alt: partner.name, width: 180, height: 72, unoptimized: partner.logoUrl.endsWith(".svg") }).props}
      alt={partner.name}
      className="max-h-14 w-auto object-contain"
    />
  ) : (
    // En attendant le logo officiel : le nom, sobrement.
    <span className="text-xl font-extrabold tracking-wide text-brand-ink">{partner.name}</span>
  );
  const className = "group flex h-24 w-56 shrink-0 items-center justify-center rounded-2xl border bg-background px-6 shadow-xs transition hover:border-primary hover:shadow-md";
  return partner.url ? (
    <a href={partner.url} target="_blank" rel="noopener noreferrer" tabIndex={hidden ? -1 : undefined} className={className} title={partner.name}>
      {content}
      <span className="sr-only"> {newTab}</span>
    </a>
  ) : (
    <span className={className}>{content}</span>
  );
}

/**
 * Bandeau des partenaires (géré dans le back-office) : défilement continu en CSS, pause au survol ou au focus,
 * immobile si la personne a désactivé les animations. Les copies servant à la boucle sont masquées aux lecteurs d'écran.
 */
export async function PartnersMarquee({ partners }: { partners: Partner[] }) {
  const t = await getTranslations();
  if (partners.length === 0) return null;
  return (
    <section aria-labelledby="partenaires" className="border-t bg-muted/50">
      <div className="mx-auto max-w-7xl px-4 pt-14">
        <h2 id="partenaires" className="inline-block text-2xl font-extrabold text-brand-ink md:text-3xl">
          {t("Home.partners")}
          <span aria-hidden="true" className="mt-2 block h-1 w-16 rounded-full bg-primary" />
        </h2>
        <p className="mt-3 text-muted-foreground">{await intro("Home.partnersLead")}</p>
      </div>
      <div
        className="group/marquee relative mt-8 flex overflow-hidden pb-14 [--duration:40s] [--gap:1.5rem] [mask-image:linear-gradient(to_right,transparent,black_8%,black_92%,transparent)] motion-reduce:justify-center motion-reduce:[mask-image:none]"
        style={{ gap: "var(--gap)" }}
      >
        {Array.from({ length: REPEAT }, (_, copy) => (
          <ul
            key={copy}
            aria-hidden={copy > 0 ? true : undefined}
            className={`flex shrink-0 animate-marquee justify-around group-hover/marquee:[animation-play-state:paused] group-focus-within/marquee:[animation-play-state:paused] motion-reduce:animate-none motion-reduce:flex-wrap motion-reduce:justify-center ${copy > 0 ? "motion-reduce:hidden" : ""}`}
            style={{ gap: "var(--gap)" }}
          >
            {partners.map((partner) => (
              <li key={`${copy}-${partner.name}`}><PartnerCard partner={partner} hidden={copy > 0} newTab={t("Footer.newTab")} /></li>
            ))}
          </ul>
        ))}
      </div>
    </section>
  );
}
