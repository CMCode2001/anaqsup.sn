import { fillPattern, localeOf, patternOf, stripPrefix, withPrefix } from "./paths";
import { type Locale, type Pathname, routing } from "./routing";

/**
 * Compare une adresse publique sans préfixe de langue (ex. "/news/session-du-conseil") à un motif
 * de route (ex. "/news/[slug]"). Renvoie les paramètres capturés ({ slug: "session-du-conseil" })
 * si l'adresse correspond au motif, sinon `null`.
 */
function matchPattern(pathname: string, pattern: string): Record<string, string> | null {
  // TODO(human)
  return pathname === pattern ? {} : null;
}

/**
 * Adresse de la même page dans une autre langue, pour le sélecteur « Français / English ».
 * Les routes statiques sont essayées avant les routes à paramètres (/news/press-releases avant /news/[slug]).
 * Si aucune route ne correspond, on renvoie l'accueil de la langue demandée.
 */
export function alternatePath(pathname: string, target: Locale): string {
  const current = localeOf(pathname);
  const bare = stripPrefix(pathname, current);
  const routes = (Object.keys(routing.pathnames) as Pathname[]).sort(
    (a, b) => Number(a.includes("[")) - Number(b.includes("[")),
  );
  for (const internal of routes) {
    const params = matchPattern(bare, patternOf(internal, current));
    if (params) return withPrefix(fillPattern(patternOf(internal, target), params), target);
  }
  return withPrefix("/", target);
}
