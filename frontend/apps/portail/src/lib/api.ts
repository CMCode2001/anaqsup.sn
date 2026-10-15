import "server-only";

import type {
  Establishment,
  EstablishmentPage,
  Event,
  EventPage,
  InstitutionalPage,
  Lang,
  NewsCategory,
  NewsItem,
  NewsPage,
  Partner,
  ProgramPage,
  ProgramSummary,
  PublicationPage,
  SearchPage,
  SearchType,
  SiteSettings,
} from "@anaqsup/api-client";

import { serverEnv } from "./env";

/** Durées de cache (secondes) du cache de données Next.js. */
const REVALIDATE = { fiche: 600, liste: 120 } as const;

export class ApiUnavailableError extends Error {
  constructor(readonly status: number, readonly path: string) {
    super(`API ${status} sur ${path}`);
  }
}

/**
 * GET serveur vers l'API publique, avec le cache de données de Next.js.
 * Renvoie `null` sur 404 ; lève `ApiUnavailableError` sinon (capturée par error.tsx).
 */
async function get<T>(path: string, cache: { revalidate: number; tags?: string[] } | "no-store"): Promise<T | null> {
  const response = await fetch(`${serverEnv.apiInternalUrl}/api/v1/public${path}`, {
    headers: { Accept: "application/json" },
    ...(cache === "no-store" ? { cache } : { next: cache }),
  });
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new ApiUnavailableError(response.status, path);
  }
  return (await response.json()) as T;
}

function query(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") search.set(key, String(value));
  }
  const encoded = search.toString();
  return encoded ? `?${encoded}` : "";
}

type Paging = { page?: number; size?: number };

/*
 * Contenus éditoriaux : `lang` demande la version anglaise, avec repli sur le français contenu par contenu
 * (champ `lang` de chaque élément). La langue fait partie de l'URL, donc du cache : une entrée par langue.
 */
export const api = {
  // ------------------------------------------------------------------ contenus institutionnels
  news: (params: Paging & { lang: Lang; category?: string }) =>
    get<NewsPage>(`/news${query(params)}`, { revalidate: REVALIDATE.liste, tags: ["news"] }),

  newsItem: (slug: string, lang: Lang) =>
    get<NewsItem>(`/news/${encodeURIComponent(slug)}${query({ lang })}`, { revalidate: REVALIDATE.fiche, tags: ["news", `news:${slug}`] }),

  newsCategories: (lang: Lang) =>
    get<NewsCategory[]>(`/news/categories${query({ lang })}`, { revalidate: REVALIDATE.fiche, tags: ["news"] }),

  events: (params: Paging & { lang: Lang; period?: "upcoming" | "past" }) =>
    get<EventPage>(`/events${query(params)}`, { revalidate: REVALIDATE.liste, tags: ["events"] }),

  event: (slug: string, lang: Lang) =>
    get<Event>(`/events/${encodeURIComponent(slug)}${query({ lang })}`, { revalidate: REVALIDATE.fiche, tags: [`event:${slug}`] }),

  publications: (params: Paging & { lang: Lang; q?: string; kind?: string; year?: number }) =>
    get<PublicationPage>(`/publications${query(params)}`, { revalidate: REVALIDATE.liste, tags: ["publications"] }),

  publicationYears: (kind?: string) =>
    get<number[]>(`/publications/years${query({ kind })}`, { revalidate: REVALIDATE.liste, tags: ["publications"] }),

  page: (slug: string, lang: Lang) =>
    get<InstitutionalPage>(`/pages/${encodeURIComponent(slug)}${query({ lang })}`, { revalidate: REVALIDATE.fiche, tags: [`page:${slug}`] }),

  partners: () =>
    get<Partner[]>("/partners", { revalidate: REVALIDATE.fiche, tags: ["partners"] }),

  // Coordonnées, réseaux sociaux, liens utiles et textes d'introduction (back-office → Paramètres du site).
  settings: () =>
    get<SiteSettings>("/settings", { revalidate: REVALIDATE.fiche, tags: ["settings"] }),

  // --------------------------------------------------------- habilitations et accréditations
  establishments: (params: Paging & { q?: string; city?: string; kind?: string; avis?: string }) =>
    get<EstablishmentPage>(`/establishments${query(params)}`, { revalidate: REVALIDATE.liste, tags: ["establishments"] }),

  establishment: (slug: string) =>
    get<Establishment>(`/establishments/${encodeURIComponent(slug)}`, {
      revalidate: REVALIDATE.fiche,
      tags: [`establishment:${slug}`],
    }),

  programs: (params: Paging & { q?: string; establishment?: string; level?: string; avis?: string }) =>
    get<ProgramPage>(`/programs${query(params)}`, { revalidate: REVALIDATE.liste, tags: ["programs"] }),

  program: (slug: string) =>
    get<ProgramSummary>(`/programs/${encodeURIComponent(slug)}`, { revalidate: REVALIDATE.fiche, tags: [`program:${slug}`] }),

  // ------------------------------------------------------------------------------- recherche
  // Hors cache de données : une entrée par texte saisi le remplirait sans limite (l'API garde une minute de cache).
  search: (params: Paging & { q: string; lang: Lang; type?: SearchType; period?: string; category?: string; kind?: string }) =>
    get<SearchPage>(`/search${query(params)}`, "no-store"),
};

/** Pour l'accueil : une rubrique indisponible ne doit pas faire tomber toute la page. */
export async function orNull<T>(promise: Promise<T | null>): Promise<T | null> {
  try {
    return await promise;
  } catch {
    return null;
  }
}
