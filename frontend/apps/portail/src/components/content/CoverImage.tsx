import { getImageProps } from "next/image";

import { cn } from "@anaqsup/ui/lib/utils";

/**
 * Image mise en avant d'un contenu, en <img> natif optimisé par Next.js (formats AVIF/WebP, tailles
 * adaptées à l'écran via srcset) sans le composant client <Image> : zéro JavaScript ajouté.
 * Décorative (alt vide) : le titre du contenu est toujours affiché à côté.
 * Sans image, un fond aux couleurs du logotype prend le relais.
 */
export function CoverImage({ src, sizes, eager = false, className }: {
  src: string | null | undefined;
  sizes: string;
  /** Image visible dès l'ouverture de la page (grande carte « À la une ») : chargée en priorité. */
  eager?: boolean;
  className?: string;
}) {
  if (!src) {
    return (
      <div
        aria-hidden="true"
        className={cn(
          "absolute inset-0 bg-anthracite bg-[radial-gradient(60%_80%_at_20%_20%,color-mix(in_oklch,var(--primary)_45%,transparent),transparent),linear-gradient(135deg,var(--anthracite),oklch(0.33_0.01_255))]",
          className,
        )}
      />
    );
  }
  const { props } = getImageProps({
    src,
    alt: "",
    fill: true,
    sizes,
    // Les SVG (illustrations de démonstration) ne passent pas par l'optimiseur d'images.
    unoptimized: src.endsWith(".svg"),
    loading: eager ? "eager" : "lazy",
    fetchPriority: eager ? "high" : undefined,
  });
  // eslint-disable-next-line @next/next/no-img-element -- <img> voulu : props générées par getImageProps.
  return <img {...props} alt="" className={cn("object-cover", className)} />;
}
