import { cn } from "@anaqsup/ui/lib/utils";

/**
 * Bande drapeau du pied de page : reprend `public/brand/drapeau.svg` (mêmes couleurs, même étoile).
 *
 * Elle est inline plutôt qu'en <img> : le fichier fait 1440 × 6 et un SVG garde ses proportions,
 * il serait donc réduit à 1,5 px de haut sur mobile ou laissé incomplet sur un écran large.
 * Ici les trois bandes s'étirent sur toute la largeur et l'étoile garde sa taille, centrée.
 */
export function FlagBand({ className }: { className?: string }) {
  return (
    <svg className={cn("block h-1.5 w-full", className)} aria-hidden="true" focusable="false">
      <rect width="33.34%" height="100%" fill="#109939" />
      <rect x="33.33%" width="33.34%" height="100%" fill="#FFE500" />
      <rect x="66.66%" width="33.34%" height="100%" fill="#DE2627" />
      <svg x="50%" overflow="visible">
        <path
          transform="translate(-720 0)"
          d="M718.456 5.25L718.862 3.49375L717.5 2.3125L719.3 2.15625L720 0.5L720.7 2.15625L722.5 2.3125L721.137 3.49375L721.544 5.25L720 4.31875L718.456 5.25Z"
          fill="#109939"
        />
      </svg>
    </svg>
  );
}
