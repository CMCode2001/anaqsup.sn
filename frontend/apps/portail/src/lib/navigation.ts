import type { StaticPathname } from "@/i18n/routing";

import type messages from "../../messages/fr.json";

/** Clé de libellé dans l'espace de traduction `Nav`. */
type Label = keyof (typeof messages)["Nav"];

export type NavLink = { label: Label; href: StaticPathname };
export type NavGroup = {
  label: Label;
  /** Lien direct (rubrique sans sous-menu) ou sous-menu en carte. */
  href?: StaticPathname;
  children?: NavLink[];
  /** Autres pages rattachées à la rubrique, pour la mettre en évidence (ex. fiches du registre). */
  section?: StaticPathname[];
};

/**
 * Menu validé par l'ANAQ-Sup (13/10/2026). Chaque sous-élément a sa propre page.
 * « Le mot du Secrétaire exécutif » n'y figure pas : il est mis en avant sur l'accueil.
 */
export const NAV: NavGroup[] = [
  {
    label: "about",
    children: [
      { label: "presentation", href: "/anaq-sup" },
      { label: "missions", href: "/anaq-sup/missions" },
      { label: "governance", href: "/anaq-sup/gouvernance" },
      { label: "orgChart", href: "/anaq-sup/organigramme" },
    ],
    section: ["/anaq-sup/mot-du-secretaire-executif"],
  },
  {
    label: "news",
    children: [
      { label: "allNews", href: "/actualites" },
      { label: "cooperation", href: "/actualites/cooperation-et-partenariats" },
      { label: "pressReleases", href: "/actualites/communiques" },
      { label: "events", href: "/evenements" },
    ],
  },
  {
    label: "approvals",
    href: "/habilitations-et-accreditations",
    section: ["/etablissements", "/formations", "/procedures/habilitation", "/procedures/accreditation"],
  },
  {
    label: "resources",
    children: [
      { label: "publications", href: "/ressources-documentaires/publications" },
      { label: "regulations", href: "/ressources-documentaires/textes-reglementaires" },
      { label: "standards", href: "/ressources-documentaires/referentiels" },
      { label: "selfAssessment", href: "/ressources-documentaires/guide-auto-evaluation" },
    ],
  },
  { label: "contact", href: "/contact" },
];
