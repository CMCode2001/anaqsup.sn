import "server-only";

import { getLocale } from "./server";
import NextLink from "next/link";
import { permanentRedirect as nextPermanentRedirect } from "next/navigation";
import type { ComponentProps } from "react";

import { getPathname, type Href } from "./paths";
import type { Locale } from "./routing";

export { getPathname, type Href };

/**
 * Lien interne localisé, résolu sur le serveur : il rend un `next/link` ordinaire avec l'adresse de la
 * langue de la page (<Link href="/actualites"> → /actualites ou /en/news).
 * Volontairement sans `next-intl/navigation` : son Link est un composant client qui embarquerait le
 * moteur de messages dans chaque page. Les composants client reçoivent des adresses déjà résolues.
 */
export async function Link({ href, ...props }: Omit<ComponentProps<typeof NextLink>, "href"> & { href: Href }) {
  return <NextLink href={getPathname({ locale: await getLocale(), href })} {...props} />;
}

export function permanentRedirect({ href, locale }: { href: Href; locale: Locale }): never {
  return nextPermanentRedirect(getPathname({ locale, href }));
}
