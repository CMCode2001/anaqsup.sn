import { getTranslations } from "@/i18n/server";

import { cn } from "@anaqsup/ui/lib/utils";

import { SocialGlyph } from "@/components/layout/SocialGlyph";
import { NETWORK_LABELS, siteSettings } from "@/lib/settings";
import { site } from "@/lib/site";

const VARIANTS = {
  // Bandeau supérieur : pastilles blanches, glyphe anthracite (modèle fourni par l'ANAQ-Sup).
  topbar: "size-8 bg-white text-anthracite hover:bg-primary [&_svg]:size-3.5",
  // Pied de page : pastilles translucides qui passent à l'or au survol.
  footer: "size-10 bg-white/10 text-white ring-1 ring-white/15 hover:bg-primary hover:text-primary-foreground hover:ring-primary [&_svg]:size-4",
} as const;

export async function SocialLinks({ variant, className }: { variant: keyof typeof VARIANTS; className?: string }) {
  const [t, settings] = await Promise.all([getTranslations("TopBar"), siteSettings()]);
  if (settings.social.length === 0) return null;
  return (
    <ul className={cn("flex items-center gap-2", className)} aria-label={t("social", { name: site.shortName })}>
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
