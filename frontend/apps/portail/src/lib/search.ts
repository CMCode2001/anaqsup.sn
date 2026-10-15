import "server-only";

import type { PublicationKind, SearchHit } from "@anaqsup/api-client";

import { KIND_PAGES } from "@/components/content/PublicationList";
import { getPathname } from "@/i18n/navigation";
import type { Locale, StaticPathname } from "@/i18n/routing";

/** Pages institutionnelles du back-office et leur adresse sur le site. */
const PAGE_ROUTES: Record<string, StaticPathname> = {
  presentation: "/anaq-sup",
  missions: "/anaq-sup/missions",
  gouvernance: "/anaq-sup/gouvernance",
  organigramme: "/anaq-sup/organigramme",
  "mot-du-secretaire-executif": "/anaq-sup/mot-du-secretaire-executif",
  "procedure-habilitation": "/procedures/habilitation",
  "procedure-accreditation": "/procedures/accreditation",
  "mentions-legales": "/mentions-legales",
  "donnees-personnelles": "/donnees-personnelles",
};

export type HitLink = { href: string; newTab: boolean };

/**
 * Adresse d'un résultat dans la langue de la page : article, événement, fiche du registre, page du site, ou PDF
 * du document (ouvert dans un nouvel onglet, comme dans « Ressources documentaires »). `null` : page sans adresse
 * publique, à ne pas afficher.
 */
export function hitLink(hit: SearchHit, locale: Locale): HitLink | null {
  const params = { slug: hit.slug };
  const internal = (href: Parameters<typeof getPathname>[0]["href"]): HitLink => ({ href: getPathname({ locale, href }), newTab: false });
  switch (hit.type) {
    case "news":
      return internal({ pathname: "/actualites/[slug]", params });
    case "events":
      return internal({ pathname: "/evenements/[slug]", params });
    case "establishments":
      return internal({ pathname: "/etablissements/[slug]", params });
    case "programs":
      return internal({ pathname: "/formations/[slug]", params });
    case "publications": {
      if (hit.fileUrl) return { href: hit.fileUrl, newTab: true };
      // Document sans fichier (annoncé) : sa rubrique, filtrée sur son titre.
      const pathname = KIND_PAGES[hit.code as keyof typeof KIND_PAGES] ?? "/ressources-documentaires/publications";
      return internal({ pathname, query: { q: hit.title } });
    }
    case "pages": {
      const pathname = PAGE_ROUTES[hit.slug];
      return pathname ? internal(pathname) : null;
    }
  }
}

/** Précision affichée sous le titre : rubrique, lieu, ville, établissement, ou type de document. */
export function hitDetail(hit: SearchHit, kindLabel: (kind: PublicationKind) => string): string | null {
  return hit.type === "publications" && hit.code ? kindLabel(hit.code as PublicationKind) : (hit.label ?? null);
}
