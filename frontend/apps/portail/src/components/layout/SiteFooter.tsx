import { ArrowUpRightIcon, MailIcon, MapPinIcon, PhoneIcon } from "lucide-react";
import { getLocale, getTranslations } from "@/i18n/server";

import { Button } from "@anaqsup/ui/components/button";

import { FlagBand } from "@/components/layout/FlagBand";
import { SocialLinks } from "@/components/layout/SocialLinks";
import { Link } from "@/i18n/navigation";
import type { Pathname } from "@/i18n/routing";
import { contactAddress, siteSettings, usefulLink } from "@/lib/settings";
import { site, telHref } from "@/lib/site";

const RUBRIQUES = [
  { href: "/anaq-sup", label: "Nav.about" },
  { href: "/anaq-sup/mot-du-secretaire-executif", label: "Footer.executiveMessage" },
  { href: "/actualites", label: "Nav.news" },
  { href: "/evenements", label: "Nav.events" },
  { href: "/ressources-documentaires/publications", label: "Nav.resources" },
  { href: "/habilitations-et-accreditations", label: "Nav.approvals" },
  { href: "/contact", label: "Nav.contact" },
] as const satisfies readonly { href: Pathname; label: string }[];

function FooterHeading({ children }: { children: React.ReactNode }) {
  return <h2 className="mb-5 text-xs font-semibold tracking-[0.18em] text-primary uppercase">{children}</h2>;
}

/**
 * Pied de page anthracite (choix ANAQ-Sup), coiffé de la bande drapeau.
 * Pas de logotype : la charte le réserve au fond blanc (docs/charte/).
 */
export async function SiteFooter() {
  const [t, locale, settings] = await Promise.all([getTranslations(), getLocale(), siteSettings()]);
  const links = settings.usefulLinks.map((link) => usefulLink(link, locale));
  return (
    <footer className="mt-20 bg-anthracite text-sm text-white/75 print:hidden">
      <FlagBand />

      <div className="mx-auto grid max-w-7xl gap-x-10 gap-y-12 px-4 pt-14 pb-12 sm:grid-cols-2 lg:grid-cols-[1.25fr_1fr_1.1fr_1.15fr]">
        <div>
          <p className="text-xs font-medium tracking-[0.18em] text-white/55 uppercase">{t("Site.republic")}</p>
          <p className="mt-2 text-3xl font-bold tracking-tight text-white">{site.shortName}</p>
          <p className="mt-3 max-w-xs leading-relaxed">{t("Site.name")}</p>
          <SocialLinks variant="footer" className="mt-7" />
        </div>

        <nav aria-label={t("Footer.sectionsLabel")}>
          <FooterHeading>{t("Footer.sections")}</FooterHeading>
          <ul className="space-y-3">
            {RUBRIQUES.map((item) => (
              <li key={item.href}>
                <Link href={item.href} className="inline-block transition duration-200 hover:translate-x-1 hover:text-white">
                  {t(item.label)}
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        {/* Liens utiles : back-office → Paramètres du site. */}
        <nav aria-label={t("Footer.usefulLinks")}>
          <FooterHeading>{t("Footer.usefulLinks")}</FooterHeading>
          <ul className="-mx-3 space-y-1">
            {links.map((item) => (
              <li key={item.url}>
                <a
                  href={item.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="group flex items-start justify-between gap-3 rounded-2xl px-3 py-2.5 transition duration-200 hover:bg-white/[0.06]"
                >
                  <span>
                    <span className="block font-semibold text-white">{item.label}</span>
                    {item.description && <span className="mt-0.5 block text-xs leading-snug text-white/60">{item.description}</span>}
                    <span className="sr-only"> {t("Footer.newTab")}</span>
                  </span>
                  <ArrowUpRightIcon
                    className="mt-0.5 size-4 shrink-0 text-primary transition duration-200 group-hover:translate-x-0.5 group-hover:-translate-y-0.5"
                    aria-hidden="true"
                  />
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div>
          <FooterHeading>{t("Footer.contactUs")}</FooterHeading>
          <address className="space-y-4 not-italic">
            <p className="flex gap-3">
              <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-white/10 text-primary">
                <MapPinIcon className="size-4" aria-hidden="true" />
              </span>
              <span className="pt-1 leading-relaxed">{contactAddress(settings, locale)}</span>
            </p>
            <p className="flex items-center gap-3">
              <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-white/10 text-primary">
                <MailIcon className="size-4" aria-hidden="true" />
              </span>
              <a href={`mailto:${settings.contact.email}`} className="break-all hover:text-white">{settings.contact.email}</a>
            </p>
            <p className="flex items-center gap-3">
              <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-white/10 text-primary">
                <PhoneIcon className="size-4" aria-hidden="true" />
              </span>
              <a href={telHref(settings.contact.phone)} className="hover:text-white">{settings.contact.phone}</a>
            </p>
          </address>
          <Button asChild className="mt-6">
            <Link href="/contact">{t("Footer.writeUs")}</Link>
          </Button>
        </div>
      </div>

      <div className="border-t border-white/10">
        <p className="mx-auto max-w-7xl px-4 py-5 text-xs text-white/55">
          {t("Footer.copyright", { year: new Date().getFullYear(), name: site.shortName })}
        </p>
      </div>
    </footer>
  );
}
