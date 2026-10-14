import type { MetadataRoute } from "next";

import { getPathname, type Href } from "@/i18n/navigation";
import { type Locale, type Pathname, routing, type StaticPathname } from "@/i18n/routing";
import { api } from "@/lib/api";
import { site } from "@/lib/site";

export const revalidate = 3600;

/** Pages fixes : toutes les routes sans paramètre, sauf la recherche et la redirection de rubrique. */
const EXCLUDED: Pathname[] = ["/recherche", "/ressources-documentaires"];
const STATIC_ROUTES = (Object.keys(routing.pathnames) as Pathname[]).filter(
  (pathname): pathname is StaticPathname => !pathname.includes("[") && !EXCLUDED.includes(pathname),
);

/** Toutes les fiches du registre, par pages de 100 (≈ 1 300 fiches → ~15 appels par heure). */
async function registryRoutes(): Promise<Href[]> {
  const routes: Href[] = [];
  for (const [pathname, fetchPage] of [
    ["/formations/[slug]", (page: number) => api.programs({ page, size: 100 })],
    ["/etablissements/[slug]", (page: number) => api.establishments({ page, size: 100 })],
  ] as const) {
    for (let page = 0; ; page++) {
      const result = await fetchPage(page);
      if (!result) break;
      routes.push(...result.content.map((entry) => ({ pathname, params: { slug: entry.slug } })));
      if (page + 1 >= result.page.totalPages) break;
    }
  }
  return routes;
}

/** Une entrée par page et par langue, avec ses versions linguistiques (hreflang). */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  // Si l'API est indisponible (ex. au build), on publie au moins les pages fixes.
  const dynamicRoutes = await registryRoutes().catch(() => []);
  return [...STATIC_ROUTES, ...dynamicRoutes].flatMap((href) => {
    const url = (locale: Locale) => `${site.url}${getPathname({ locale, href })}`;
    const languages = Object.fromEntries(routing.locales.map((locale) => [locale, url(locale)]));
    return routing.locales.map((locale) => ({ url: url(locale), alternates: { languages } }));
  });
}
