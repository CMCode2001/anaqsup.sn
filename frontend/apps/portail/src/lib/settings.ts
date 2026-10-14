import "server-only";

import type { SiteSettings } from "@anaqsup/api-client";
import { cache } from "react";

import { getLocale, getTranslations } from "@/i18n/server";
import type { Locale } from "@/i18n/routing";

import { api } from "./api";

/**
 * Valeurs du site au 13/10/2026 (fournies par l'ANAQ-Sup), reprises par la migration V12 : affichées tant que
 * l'API ne répond pas, pour que le bandeau, le pied de page et la page Contact ne soient jamais vides.
 */
const FALLBACK: SiteSettings = {
  contact: {
    phone: "+221 33 825 75 18",
    email: "secretariat@anaqsup.sn",
    address: "Résidence Adja Aby Gueye, Sacré-Cœur 3 Pyrotechnie, Lot N° R126, Dakar",
    addressEn: null,
  },
  social: [
    { network: "facebook", url: "https://www.facebook.com/ANAQSup/" },
    { network: "x", url: "https://x.com/ANAQSUP" },
    { network: "linkedin", url: "https://www.linkedin.com/company/anaq-sup/" },
    { network: "youtube", url: "https://www.youtube.com/channel/UCUFWBm9s_oO1i5-_ihzdWiA" },
  ],
  usefulLinks: [
    { label: "MESRI", description: "Ministère de l'Enseignement supérieur, de la Recherche et de l'Innovation", descriptionEn: "Ministry of Higher Education, Research and Innovation", url: "https://mesrisenegal.sn" },
    { label: "CAMES", description: "Conseil africain et malgache pour l'enseignement supérieur", descriptionEn: "African and Malagasy Council for Higher Education", url: "https://www.lecames.org" },
    { label: "CAMPUSEN", description: "Plateforme d'orientation des nouveaux bacheliers", descriptionEn: "Guidance platform for new baccalaureate holders", url: "https://www.campusen.sn" },
    { label: "UNESCO", description: "Bureau régional de l'UNESCO à Dakar", descriptionEn: "UNESCO Regional Office in Dakar", url: "https://www.unesco.org/fr/fieldoffice/dakar" },
  ],
  intros: {},
};

/** Paramètres du site (une seule lecture par requête, cache de données étiqueté « settings »). */
export const siteSettings = cache(async (): Promise<SiteSettings> => {
  try {
    return (await api.settings()) ?? FALLBACK;
  } catch {
    return FALLBACK;
  }
});

export type SocialNetwork = SiteSettings["social"][number]["network"];
export const NETWORK_LABELS: Record<SocialNetwork, string> = { facebook: "Facebook", x: "X", linkedin: "LinkedIn", youtube: "YouTube" };

/** Adresse dans la langue de la page (repli sur le français). */
export const contactAddress = (settings: SiteSettings, locale: Locale) =>
  (locale === "en" && settings.contact.addressEn) || settings.contact.address;

/** Lien utile dans la langue de la page (repli sur le français, champ par champ). */
export function usefulLink(link: SiteSettings["usefulLinks"][number], locale: Locale) {
  const en = locale === "en";
  return {
    label: (en && link.labelEn) || link.label,
    description: (en && link.descriptionEn) || link.description,
    url: link.url,
  };
}

/** Textes d'introduction modifiables dans le back-office (liste identique à SiteSettings.INTRO_KEYS côté API). */
export type IntroKey =
  | "Home.approvalsLead" | "Home.partnersLead" | "Events.lead"
  | "Resources.publicationsLead" | "Resources.regulationsLead" | "Resources.standardsLead" | "Resources.selfAssessmentLead"
  | "Approvals.lead" | "Institutions.lead" | "Programmes.lead";

/**
 * Texte d'introduction : celui saisi dans le back-office pour la langue de la page, sinon le texte d'origine
 * du site (messages). Un texte saisi en français seulement ne remplace pas le texte anglais d'origine.
 */
export async function intro(key: IntroKey): Promise<string> {
  const [settings, locale, t] = await Promise.all([siteSettings(), getLocale(), getTranslations()]);
  return settings.intros[key]?.[locale]?.trim() || t(key);
}
