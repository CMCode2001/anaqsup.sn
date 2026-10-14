import type { Metadata } from "next";

import { getPathname, type Href } from "@/i18n/navigation";
import { type Locale, routing } from "@/i18n/routing";

/**
 * Adresse canonique et versions linguistiques (hreflang) d'une page.
 * `contentLocale` : langue réelle du contenu. Une page anglaise qui affiche un contenu non traduit
 * déclare la version française comme canonique, pour ne pas publier deux fois le même texte.
 */
export function alternates(locale: Locale, href: Href, contentLocale: Locale = locale): Metadata["alternates"] {
  const languages = Object.fromEntries(routing.locales.map((l) => [l, getPathname({ locale: l, href })]));
  return {
    canonical: getPathname({ locale: contentLocale, href }),
    languages: { ...languages, "x-default": getPathname({ locale: routing.defaultLocale, href }) },
  };
}
