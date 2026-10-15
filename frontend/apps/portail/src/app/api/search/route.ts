import type { NextRequest } from "next/server";
import { getTranslations } from "next-intl/server";

import type { Locale } from "@/i18n/routing";
import { api } from "@/lib/api";
import { hitDetail, hitLink } from "@/lib/search";

/** Nombre de suggestions sous le grand champ de la page de recherche. */
const SUGGESTIONS = 6;

export type Suggestion = { type: string; typeLabel: string; title: string; lang: string; detail: string | null; href: string; newTab: boolean };

/**
 * Suggestions pendant la frappe (grand champ de la page de recherche), avec leurs adresses déjà calculées et traduites :
 * le navigateur n'a rien à connaître des routes du site. Débit limité par adresse IP dans Nginx.
 */
export async function GET(request: NextRequest) {
  const params = request.nextUrl.searchParams;
  const locale: Locale = params.get("lang") === "en" ? "en" : "fr";
  const q = params.get("q")?.trim() ?? "";
  if (q.length < 2 || q.length > 100) {
    return Response.json({ title: "Texte de 2 à 100 caractères attendu", status: 400 }, { status: 400 });
  }

  let page;
  try {
    page = await api.search({ q, lang: locale, size: SUGGESTIONS });
  } catch {
    return Response.json({ title: "Recherche indisponible", status: 502 }, { status: 502 });
  }
  const t = await getTranslations({ locale });
  const items = (page?.content ?? []).flatMap((hit): Suggestion[] => {
    const link = hitLink(hit, locale);
    if (!link) return [];
    const detail = hitDetail(hit, (kind) => t(`PublicationKind.${kind}`));
    return [{ type: hit.type, typeLabel: t(`Search.type.${hit.type}`), title: hit.title, lang: hit.lang, detail, ...link }];
  });
  // Contenus publics : une minute dans le navigateur, comme dans l'API.
  return Response.json({ items }, { headers: { "Cache-Control": "public, max-age=60" } });
}
