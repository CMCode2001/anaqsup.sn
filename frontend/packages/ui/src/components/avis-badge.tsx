import type { Avis } from "@anaqsup/api-client";
import { CircleCheckIcon, CircleMinusIcon, CircleXIcon } from "lucide-react";

import { Badge } from "@anaqsup/ui/components/badge";
import { cn } from "@anaqsup/ui/lib/utils";

type Locale = "fr" | "en";

const LABELS: Record<Locale, Record<Avis | "NONE", string>> = {
  fr: { FAVORABLE: "Avis favorable", DEFAVORABLE: "Avis défavorable", NONE: "Avis non renseigné" },
  en: { FAVORABLE: "Favourable opinion", DEFAVORABLE: "Unfavourable opinion", NONE: "No opinion recorded" },
};

const AVIS = {
  FAVORABLE: { Icon: CircleCheckIcon, tone: "bg-status-ok-surface text-status-ok" },
  DEFAVORABLE: { Icon: CircleXIcon, tone: "bg-status-stop-surface text-status-stop" },
} as const;

/**
 * Avis d'habilitation ou d'accréditation, saisi tel quel par l'ANAQ-Sup, avec son année.
 * Libellé + icône + couleur (jamais la couleur seule, WCAG 1.4.1). Sans état : rendu serveur, zéro JS.
 * `locale` : le back-office reste en français ; le portail passe la langue de la page.
 */
export function AvisBadge({ avis, year, size = "default", locale = "fr", className }: {
  avis: Avis | null | undefined;
  year?: number | null;
  size?: "default" | "lg";
  locale?: Locale;
  className?: string;
}) {
  const entry = avis ? AVIS[avis] : null;
  const Icon = entry?.Icon ?? CircleMinusIcon;
  return (
    <Badge
      className={cn(
        "font-semibold",
        size === "lg" && "h-7 gap-1.5 px-3 text-sm [&>svg]:size-4!",
        entry?.tone ?? "bg-status-neutral-surface text-status-neutral",
        className,
      )}
    >
      <Icon aria-hidden="true" data-icon="inline-start" />
      {avisLabel(avis, year, locale)}
    </Badge>
  );
}

export function avisLabel(avis: Avis | null | undefined, year?: number | null, locale: Locale = "fr"): string {
  if (!avis) return LABELS[locale].NONE;
  return year ? `${LABELS[locale][avis]} (${year})` : LABELS[locale][avis];
}
