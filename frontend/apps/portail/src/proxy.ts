import createMiddleware from "next-intl/middleware";

import { routing } from "@/i18n/routing";

/**
 * Aiguillage des langues : /actualites est servi par app/[locale] avec locale = fr, /en/news avec
 * locale = en ; /fr/… redirige vers la version sans préfixe, et une adresse anglaise non traduite
 * (/en/actualites) vers sa forme traduite (/en/news).
 */
export default createMiddleware(routing);

export const config = {
  // Ni l'API interne, ni les fichiers Next.js, ni les images téléversées (/media/…), ni les fichiers
  // statiques (tout chemin avec une extension).
  matcher: "/((?!api|_next|_vercel|media/|.*\\..*).*)",
};
