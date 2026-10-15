import type { MetadataRoute } from "next";

import { getPathname } from "@/i18n/navigation";
import { routing } from "@/i18n/routing";
import { site } from "@/lib/site";

export default function robots(): MetadataRoute.Robots {
  // Pages de résultats de recherche : utiles aux visiteurs, pas aux moteurs (contenu dupliqué).
  const searchPages = routing.locales.map((locale) => getPathname({ locale, href: "/recherches" }));
  return {
    rules: [{ userAgent: "*", allow: "/", disallow: ["/api/", ...searchPages] }],
    sitemap: `${site.url}/sitemap.xml`,
    host: site.url,
  };
}
