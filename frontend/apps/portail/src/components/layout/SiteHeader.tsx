import { MailIcon, PhoneIcon, SearchIcon } from "lucide-react";
import { getLocale, getTranslations } from "@/i18n/server";

import { BrandLogo } from "@anaqsup/ui/components/brand-logo";

import { DesktopNav, type ResolvedGroup } from "@/components/layout/DesktopNav";
import { LocaleSwitcher } from "@/components/layout/LocaleSwitcher";
import { MobileMenu } from "@/components/layout/MobileMenu";
import { SocialLinks } from "@/components/layout/SocialLinks";
import { getPathname, type Href, Link } from "@/i18n/navigation";
import { LOCALE_NAMES } from "@/i18n/routing";
import { NAV } from "@/lib/navigation";
import { siteSettings } from "@/lib/settings";
import { site, telHref } from "@/lib/site";

/**
 * En-tête : bandeau supérieur (coordonnées, réseaux sociaux, langue), logotype, menu et loupe de recherche.
 * Le menu large est un petit composant client (sous-menus au survol) ; le menu mobile est un tiroir (<details>,
 * fonctionnel sans JavaScript). La loupe mène directement à la page de recherche (choix ANAQ-Sup : aucun panneau
 * par-dessus la page).
 */
export async function SiteHeader() {
  const locale = await getLocale();
  const t = await getTranslations("Nav");
  const tTop = await getTranslations("TopBar");
  const tSearch = await getTranslations("Search");
  const { contact } = await siteSettings();
  const href = (pathname: Href) => getPathname({ locale, href: pathname });

  const groups: ResolvedGroup[] = NAV.map((group) => ({
    id: `menu-${group.label}`,
    label: t(group.label),
    href: group.href && href(group.href),
    children: group.children?.map((child) => ({ label: t(child.label), href: href(child.href) })),
    section: (group.section ?? []).map(href),
  }));

  return (
    // Fond opaque : translucide, l'en-tête virait au gris au-dessus du pied de page et le logotype
    // n'était plus sur fond blanc (charte).
    <header className="sticky top-0 z-40 border-b bg-background print:hidden">
      {/* Bandeau supérieur : coordonnées à gauche ; réseaux sociaux puis langue à droite. */}
      <div className="bg-anthracite text-[13px] text-white/90">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-2 px-4 py-1.5 sm:gap-4">
          <ul className="flex items-center sm:gap-x-8">
            <li>
              <a href={telHref(contact.phone)} className="inline-flex min-h-8 min-w-8 items-center justify-center gap-2 font-medium hover:text-primary sm:justify-start">
                <PhoneIcon className="size-4 text-primary" aria-hidden="true" />
                <span className="sr-only sm:not-sr-only">{contact.phone}</span>
              </a>
            </li>
            <li>
              <a href={`mailto:${contact.email}`} className="inline-flex min-h-8 min-w-8 items-center justify-center gap-2 font-medium hover:text-primary sm:justify-start">
                <MailIcon className="size-4 text-primary" aria-hidden="true" />
                <span className="sr-only sm:not-sr-only">{contact.email}</span>
              </a>
            </li>
          </ul>
          <div className="flex items-center gap-1.5 sm:gap-3">
            <SocialLinks variant="topbar" />
            <span aria-hidden="true" className="h-4 w-px bg-white/20" />
            <LocaleSwitcher locale={locale} label={tTop("chooseLanguage", { language: LOCALE_NAMES[locale] })} />
          </div>
        </div>
      </div>

      <div className="relative mx-auto flex max-w-7xl items-center justify-between gap-6 px-4 py-3">
        {/* Logotype officiel, sur fond blanc, sans modification (charte : docs/charte/). */}
        <Link href="/" className="flex items-center gap-4" aria-label={t("homeLink", { name: site.shortName })}>
          <BrandLogo height={44} />
        </Link>

        <div className="flex items-center gap-2 xl:gap-3">
          <DesktopNav label={t("main")} groups={groups} />

          <Link
            href="/recherches"
            aria-label={tSearch("open")}
            className="flex size-9.5 shrink-0 items-center justify-center rounded-control border text-brand-ink transition-colors hover:border-primary hover:bg-muted"
          >
            <SearchIcon className="size-[18px]" aria-hidden="true" />
          </Link>

          <MobileMenu
            groups={groups}
            contact={{ phone: contact.phone, phoneHref: telHref(contact.phone), email: contact.email }}
            labels={{ menu: t("menu"), close: t("closeMenu"), nav: t("mobile") }}
          />
        </div>
      </div>
    </header>
  );
}
