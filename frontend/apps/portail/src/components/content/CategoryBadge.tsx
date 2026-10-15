import { BadgeCheckIcon, HandshakeIcon, LandmarkIcon, MegaphoneIcon, NewspaperIcon, TagIcon } from "lucide-react";

import { cn } from "@anaqsup/ui/lib/utils";

/** Icône et teinte de chaque rubrique (couleurs du drapeau ; texte assombri pour rester lisible). */
const STYLES: Record<string, { Icon: typeof TagIcon; tone: string }> = {
  "vie-institutionnelle": { Icon: LandmarkIcon, tone: "green" },
  "assurance-qualite": { Icon: BadgeCheckIcon, tone: "green" },
  "cooperation-et-partenariats": { Icon: HandshakeIcon, tone: "green" },
  "appels-a-candidatures": { Icon: MegaphoneIcon, tone: "gold" },
  communiques: { Icon: NewspaperIcon, tone: "red" },
};

const TONES: Record<string, string> = {
  green: "border-brand-green/30 bg-brand-green/10 text-[color-mix(in_oklch,var(--brand-green)_70%,black)]",
  gold: "border-primary/40 bg-primary/15 text-brand-ink",
  red: "border-brand-red/30 bg-brand-red/10 text-[color-mix(in_oklch,var(--brand-red)_80%,black)]",
};

/** Pastille de rubrique d'une actualité : icône + libellé en capitales (cartes, page d'article). */
export function CategoryBadge({ slug, label, size = "md", className }: {
  slug: string;
  label: string;
  size?: "sm" | "md";
  className?: string;
}) {
  const { Icon, tone } = STYLES[slug] ?? { Icon: TagIcon, tone: "green" };
  return (
    <span
      className={cn(
        "inline-flex max-w-full items-center gap-1.5 rounded-full border font-semibold tracking-wide uppercase",
        size === "sm" ? "px-2.5 py-0.5 text-xs" : "px-3.5 py-1 text-xs",
        TONES[tone],
        className,
      )}
    >
      <Icon className={size === "sm" ? "size-3" : "size-3.5"} aria-hidden="true" />
      <span className="truncate">{label}</span>
    </span>
  );
}

/** Couleur du libellé seul (liste « Articles récents »), même teinte que la pastille. */
export function categoryTextClass(slug: string): string {
  const tone = STYLES[slug]?.tone ?? "green";
  return TONES[tone]!.split(" ").filter((c) => c.startsWith("text-")).join(" ");
}
