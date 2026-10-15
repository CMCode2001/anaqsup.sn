import { getTranslations } from "@/i18n/server";

import { cn } from "@anaqsup/ui/lib/utils";

import { SocialGlyph } from "@/components/layout/SocialGlyph";
import { NETWORK_LABELS, siteSettings } from "@/lib/settings";
import { site } from "@/lib/site";

const VARIANTS = {
  // Bandeau supérieur (modèle du MESRI, choisi par l'ANAQ-Sup le 15/10/2026) : glyphes blancs sans pastille, or au survol.
  topbar: "size-8 text-white hover:text-primary [&_svg]:size-[18px]",
  // Pied de page : pastilles translucides qui passent à l'or au survol.
  footer: "size-10 bg-white/10 text-white ring-1 ring-white/15 hover:bg-primary hover:text-primary-foreground hover:ring-primary [&_svg]:size-4",
} as const;

/** Écart entre les liens : serré sur téléphone dans le bandeau (glyphes sans pastille, zone tactile de 32 px). */
const GAPS = { topbar: "gap-0.5 sm:gap-2", footer: "gap-2" } as const;

export async function SocialLinks({ variant, className }: { variant: keyof typeof VARIANTS; className?: string }) {
  const [t, settings] = await Promise.all([getTranslations("TopBar"), siteSettings()]);
  if (settings.social.length === 0) return null;
  return (
    <ul className={cn("flex items-center", GAPS[variant], className)} aria-label={t("social", { name: site.shortName })}>
      {settings.social.map((item) => (
        <li key={item.network}>
          <a
            href={item.url}
            target="_blank"
            rel="noopener noreferrer"
            title={NETWORK_LABELS[item.network]}
            className={cn(
              "inline-flex items-center justify-center rounded-control transition duration-200 hover:scale-110 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary",
              VARIANTS[variant],
            )}
          >
            <SocialGlyph network={item.network} />
            <span className="sr-only">{t("socialLink", { name: site.shortName, network: NETWORK_LABELS[item.network] })}</span>
          </a>
        </li>
      ))}
    </ul>
  );
}
