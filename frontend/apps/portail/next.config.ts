import { networkInterfaces } from "node:os";
import path from "node:path";

import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

// Traductions : next-intl lit la configuration de requête dans src/i18n/request.ts.
const withNextIntl = createNextIntlPlugin("./src/i18n/request.ts");

const nextConfig: NextConfig = {
  // Image Docker minimale : server.js + dépendances tracées (voir Dockerfile).
  output: "standalone",
  // Monorepo pnpm : tracer les dépendances depuis la racine du workspace.
  outputFileTracingRoot: path.join(__dirname, "../../"),
  transpilePackages: ["@anaqsup/ui", "@anaqsup/api-client"],
  // `pnpm dev` seulement : ouvrir le site depuis un téléphone du même réseau (http://<adresse du poste>:3000).
  // Sans cela, Next.js bloque ses scripts de développement pour toute autre adresse que localhost.
  allowedDevOrigins: Object.values(networkInterfaces())
    .flat()
    .filter((address) => address?.family === "IPv4" && !address.internal)
    .map((address) => address!.address),
  experimental: {
    // Imports « barrel » : ne garder que les modules réellement utilisés.
    optimizePackageImports: ["motion", "radix-ui", "lucide-react"],
    // 404 des adresses hors langue : la mise en page racine est app/[locale] (voir app/global-not-found.tsx).
    globalNotFound: true,
  },
  poweredByHeader: false,
  images: {
    formats: ["image/avif", "image/webp"],
  },
  async redirects() {
    return [
      // Rubrique « Publications » devenue « Ressources documentaires » (menu validé le 13/10/2026).
      { source: "/publications", destination: "/ressources-documentaires/publications", permanent: true },
      // Page de recherche renommée au pluriel (recherche sur tout le site, demande ANAQ-Sup du 15/10/2026).
      { source: "/recherche", destination: "/recherches", permanent: true },
    ];
  },
  // Les en-têtes de sécurité (HSTS, CSP…) sont posés par Nginx : devops/nginx/snippets/.
};

export default withNextIntl(nextConfig);
