import { getRequestConfig } from "next-intl/server";
import { locale as rootLocale } from "next/root-params";

import { isLocale } from "./paths";
import { routing } from "./routing";

/** Langue de la requête, lue depuis le segment racine `app/[locale]` (next/root-params). */
export default getRequestConfig(async ({ locale: override }) => {
  const requested = override ?? (await rootLocale());
  const locale = isLocale(requested) ? requested : routing.defaultLocale;
  return {
    locale,
    messages: (await import(`../../messages/${locale}.json`)).default,
    timeZone: "Africa/Dakar",
  };
});
