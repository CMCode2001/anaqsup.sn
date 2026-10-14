import type { Lang } from "@anaqsup/api-client";

/**
 * Langue réellement servie pour un contenu. Une API antérieure au bilinguisme (déploiement en cours,
 * cache de données) ne renvoie pas `lang` : elle ne sert que du français.
 */
export function servedLang(lang: Lang | undefined): Lang {
  return lang ?? "fr";
}

/** Attribut `lang` à poser sur un texte dont la langue diffère de celle de la page, sinon `undefined`. */
export function langAttr(lang: Lang | undefined, pageLocale: string): Lang | undefined {
  const served = servedLang(lang);
  return served !== pageLocale ? served : undefined;
}
