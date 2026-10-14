import { getLocale } from "@/i18n/server";

import { permanentRedirect } from "@/i18n/navigation";

/** La rubrique n'a pas de page d'accueil propre : son premier élément est « Publications ». */
export default async function RessourcesDocumentaires() {
  permanentRedirect({ href: "/ressources-documentaires/publications", locale: await getLocale() });
}
