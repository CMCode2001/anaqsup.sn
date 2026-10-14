import { cn } from "@anaqsup/ui/lib/utils";

/**
 * Logotype officiel de l'ANAQ-Sup — charte (docs/charte/README.md) :
 * 1. le logotype ne peut en aucun cas être modifié (ni recoloré, ni filtré, ni redessiné, ni déformé) ;
 * 2. il est utilisé tel quel sur tous les supports ;
 * 3. il est uniquement utilisé sur fond blanc et n'a pas de variante (pas de version sombre ni monochrome).
 *
 * Le composant impose donc : le fichier officiel (marges blanches seulement recadrées), le ratio d'origine
 * (689 × 294) et un fond blanc, y compris en thème sombre ou sur une surface colorée.
 */
export function BrandLogo({ height = 48, className, src = "/brand/logo-anaqsup.png" }: {
  height?: number;
  className?: string;
  src?: string;
}) {
  const width = Math.round((height * 689) / 294);
  return (
    <span className={cn("inline-flex shrink-0 bg-white", className)}>
      {/* <img> natif : composant partagé portail (Next.js) et back-office (Vite) ; PNG de 15 Ko. */}
      <img
        src={src}
        alt="ANAQ-Sup"
        width={width}
        height={height}
        style={{ width, height, objectFit: "contain", filter: "none" }}
        decoding="async"
      />
    </span>
  );
}
