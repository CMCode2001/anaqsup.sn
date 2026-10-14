import { defineRouting } from "next-intl/routing";

/**
 * Site bilingue (décision ANAQ-Sup du 13/10/2026).
 * - Français à la racine, sans préfixe (anaqsup.sn/actualites) ; anglais sous /en avec des adresses traduites
 *   (anaqsup.sn/en/news).
 * - Pas de redirection automatique selon la langue du navigateur, et donc pas de cookie de langue :
 *   chacun arrive en français et choisit l'anglais dans le bandeau.
 *
 * Les clés sont les chemins INTERNES (dossiers de `app/[locale]`, en français) ; les liens s'écrivent
 * toujours avec ces clés, et next-intl produit l'adresse publique de chaque langue.
 */
export const routing = defineRouting({
  locales: ["fr", "en"],
  defaultLocale: "fr",
  localePrefix: "as-needed",
  localeDetection: false,
  localeCookie: false,
  pathnames: {
    "/": "/",

    // L'ANAQ-Sup
    "/anaq-sup": { fr: "/anaq-sup", en: "/about" },
    "/anaq-sup/missions": { fr: "/anaq-sup/missions", en: "/about/missions" },
    "/anaq-sup/gouvernance": { fr: "/anaq-sup/gouvernance", en: "/about/governance" },
    "/anaq-sup/organigramme": { fr: "/anaq-sup/organigramme", en: "/about/organisation-chart" },
    "/anaq-sup/mot-du-secretaire-executif": {
      fr: "/anaq-sup/mot-du-secretaire-executif",
      en: "/about/executive-secretary-message",
    },

    // Actualités
    "/actualites": { fr: "/actualites", en: "/news" },
    "/actualites/cooperation-et-partenariats": {
      fr: "/actualites/cooperation-et-partenariats",
      en: "/news/cooperation-and-partnerships",
    },
    "/actualites/communiques": { fr: "/actualites/communiques", en: "/news/press-releases" },
    "/actualites/[slug]": { fr: "/actualites/[slug]", en: "/news/[slug]" },
    "/evenements": { fr: "/evenements", en: "/events" },
    "/evenements/[slug]": { fr: "/evenements/[slug]", en: "/events/[slug]" },

    // Habilitations & accréditations
    "/habilitations-et-accreditations": { fr: "/habilitations-et-accreditations", en: "/approvals-and-accreditations" },
    "/etablissements": { fr: "/etablissements", en: "/institutions" },
    "/etablissements/[slug]": { fr: "/etablissements/[slug]", en: "/institutions/[slug]" },
    "/formations": { fr: "/formations", en: "/programmes" },
    "/formations/[slug]": { fr: "/formations/[slug]", en: "/programmes/[slug]" },
    "/recherche": { fr: "/recherche", en: "/search" },
    "/procedures/habilitation": { fr: "/procedures/habilitation", en: "/procedures/institutional-approval" },
    "/procedures/accreditation": { fr: "/procedures/accreditation", en: "/procedures/programme-accreditation" },

    // Ressources documentaires
    "/ressources-documentaires": { fr: "/ressources-documentaires", en: "/resources" },
    "/ressources-documentaires/publications": {
      fr: "/ressources-documentaires/publications",
      en: "/resources/publications",
    },
    "/ressources-documentaires/textes-reglementaires": {
      fr: "/ressources-documentaires/textes-reglementaires",
      en: "/resources/regulations",
    },
    "/ressources-documentaires/referentiels": {
      fr: "/ressources-documentaires/referentiels",
      en: "/resources/quality-standards",
    },
    "/ressources-documentaires/guide-auto-evaluation": {
      fr: "/ressources-documentaires/guide-auto-evaluation",
      en: "/resources/self-assessment-guide",
    },

    "/contact": "/contact",

    // Pages légales (pied de page)
    "/mentions-legales": { fr: "/mentions-legales", en: "/legal-notice" },
    "/donnees-personnelles": { fr: "/donnees-personnelles", en: "/privacy" },
  },
});

export type Locale = (typeof routing.locales)[number];
export type Pathname = keyof typeof routing.pathnames;

/** Nom de chaque langue dans sa propre langue (choix ANAQ-Sup : « Français / English »). */
export const LOCALE_NAMES: Record<Locale, string> = { fr: "Français", en: "English" };

/** Routes sans paramètre (listes, pages fixes) : seules celles-ci acceptent un lien `{ pathname, query }`. */
export type StaticPathname = {
  [P in Pathname]: P extends `${string}[${string}]${string}` ? never : P;
}[Pathname];
