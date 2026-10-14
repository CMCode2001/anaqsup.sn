import "server-only";

import { getLocale as getIntlLocale } from "next-intl/server";

import { isLocale } from "./paths";
import { type Locale, routing } from "./routing";

/** Langue de la page en cours, typée "fr" | "en" (next-intl la renvoie sous forme de chaîne). */
export async function getLocale(): Promise<Locale> {
  const locale = await getIntlLocale();
  return isLocale(locale) ? locale : routing.defaultLocale;
}

export { getTranslations } from "next-intl/server";
