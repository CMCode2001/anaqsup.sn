import { type Locale, type Pathname, routing, type StaticPathname } from "./routing";

/*
 * Adresses publiques des deux langues, calculées à partir de la table `routing.pathnames`.
 * Module sans dépendance React ni next-intl : il sert au serveur (liens, métadonnées, sitemap)
 * comme au navigateur (sélecteur de langue), sans alourdir les pages.
 */

const PREFIX = `/${"en" satisfies Locale}`;

type ParamsOf<P extends string> = P extends `${string}[${infer Name}]${infer Rest}` ? Record<Name, string> & ParamsOf<Rest> : unknown;
type DynamicPathname = Exclude<Pathname, StaticPathname>;
type Query = Record<string, string>;

/** Destination d'un lien : chemin interne (français), paramètres de route et paramètres d'URL. */
export type Href =
  | StaticPathname
  | { pathname: StaticPathname; query?: Query }
  | { [P in DynamicPathname]: { pathname: P; params: ParamsOf<P>; query?: Query } }[DynamicPathname];

/** Langue d'une adresse publique : l'anglais est sous /en, le français à la racine. */
export function localeOf(pathname: string): Locale {
  return pathname === PREFIX || pathname.startsWith(`${PREFIX}/`) ? "en" : "fr";
}

export function stripPrefix(pathname: string, locale: Locale): string {
  if (locale === routing.defaultLocale) return pathname;
  return pathname.slice(PREFIX.length) || "/";
}

export function withPrefix(pathname: string, locale: Locale): string {
  if (locale === routing.defaultLocale) return pathname;
  return pathname === "/" ? PREFIX : `${PREFIX}${pathname}`;
}

/** Forme publique d'une route dans une langue : "/news/[slug]" pour la clé interne "/actualites/[slug]". */
export function patternOf(internal: Pathname, locale: Locale): string {
  const entry = routing.pathnames[internal];
  return typeof entry === "string" ? entry : entry[locale];
}

/** Remplace les paramètres d'un motif : "/events/[slug]" + { slug: "x" } → "/events/x". */
export function fillPattern(pattern: string, params: Record<string, string>): string {
  return pattern.replace(/\[(\w+)\]/g, (_, name: string) => encodeURIComponent(params[name] ?? ""));
}

/** getPathname({ locale: "en", href: "/actualites" }) → "/en/news". */
export function getPathname({ locale, href }: { locale: Locale; href: Href }): string {
  const { pathname, params, query } = typeof href === "string" ? { pathname: href, params: undefined, query: undefined } : { params: undefined, ...href };
  const path = withPrefix(fillPattern(patternOf(pathname, locale), (params ?? {}) as Record<string, string>), locale);
  const search = query ? new URLSearchParams(query).toString() : "";
  return search ? `${path}?${search}` : path;
}

export function isLocale(value: string | undefined): value is Locale {
  return (routing.locales as readonly string[]).includes(value ?? "");
}
