/**
 * Concaténation de classes pour les composants client du portail. Contrairement à `cn` (fusion
 * Tailwind), elle ne résout pas les conflits : c'est inutile ici, chaque classe y est écrite une seule
 * fois, et cela évite d'envoyer le moteur de fusion dans le JavaScript de chaque page.
 */
export function cx(...classes: (string | false | null | undefined)[]): string {
  return classes.filter(Boolean).join(" ");
}
