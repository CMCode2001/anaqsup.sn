import type { Metadata, Viewport } from "next";
import { getTranslations } from "@/i18n/server";
import { Poppins } from "next/font/google";
import { notFound } from "next/navigation";

import "@anaqsup/ui/globals.css";
import { cn } from "@anaqsup/ui/lib/utils";

import { BackToTop } from "@/components/layout/BackToTop";
import { SiteFooter } from "@/components/layout/SiteFooter";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { isLocale } from "@/i18n/paths";
import { routing } from "@/i18n/routing";
import { site } from "@/lib/site";

// Poppins (choix de l'ANAQ-Sup), auto-hébergée au build : aucun appel à Google à l'exécution.
const poppins = Poppins({ subsets: ["latin"], variable: "--font-sans", display: "swap", weight: ["400", "500", "600", "700", "800"] });

type Props = { children: React.ReactNode; params: Promise<{ locale: string }> };

/*
 * Aucune page pré-générée au build : la CI et l'image Docker se construisent sans API. Chaque page est
 * rendue à sa première visite puis mise en cache (ISR, `revalidate` de la page). Les langues inconnues
 * sont écartées plus bas (404).
 */
export function generateStaticParams() {
  return [];
}

export async function generateMetadata({ params }: Omit<Props, "children">): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale: isLocale(locale) ? locale : routing.defaultLocale, namespace: "Metadata" });
  return {
    metadataBase: new URL(site.url),
    title: { default: t("title"), template: `%s · ${site.shortName}` },
    description: t("description"),
    openGraph: { type: "website", locale: locale === "en" ? "en_GB" : "fr_SN", siteName: site.shortName },
    // iPhone : sans cela, WebKit transforme lui-même numéros, dates et adresses en liens (attributs
    // x-apple-data-detectors) avant que React ne prenne la main, d'où une erreur d'hydratation. Les liens
    // tel: et mailto: utiles sont déjà posés par le site.
    formatDetection: { telephone: false, date: false, address: false, email: false },
  };
}

export const viewport: Viewport = {
  themeColor: "#c7b62e",
};

/** Mise en page racine, une par langue (`app/[locale]`) : <html lang> suit la langue de la page. */
export default async function LocaleLayout({ children, params }: Props) {
  const { locale } = await params;
  if (!isLocale(locale)) notFound();
  const t = await getTranslations("Layout");

  return (
    <html lang={locale} className={cn("antialiased", poppins.variable, "font-sans")}>
      <body className="flex min-h-dvh flex-col">
        {/* Sans JavaScript : blocs <Reveal> visibles, sous-menus et choix de langue ouverts au survol. */}
        <noscript>
          <style>
            {"[data-reveal]{opacity:1!important;transform:none!important}" +
              "[data-nav-group]:hover>[data-nav-panel],[data-nav-group]:focus-within>[data-nav-panel]{visibility:visible!important;opacity:1!important;pointer-events:auto!important}"}
          </style>
        </noscript>
        <a href="#contenu" className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:rounded-md focus:bg-background focus:px-4 focus:py-2">
          {t("skipToContent")}
        </a>
        <SiteHeader />
        <main id="contenu" className="flex-1">
          {children}
        </main>
        <SiteFooter />
        <BackToTop label={t("backToTop")} />
      </body>
    </html>
  );
}
