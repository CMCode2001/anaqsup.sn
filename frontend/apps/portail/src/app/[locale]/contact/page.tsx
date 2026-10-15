import { MailIcon, MapPinIcon, PhoneIcon } from "lucide-react";
import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";

import { Card, CardContent } from "@anaqsup/ui/components/card";

import { PageHeader } from "@/components/content/PageHeader";
import { alternates } from "@/lib/metadata";
import { contactAddress, siteSettings } from "@/lib/settings";
import { telHref } from "@/lib/site";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Contact");
  return { title: t("eyebrow"), description: t("metaDescription"), alternates: alternates(await getLocale(), "/contact") };
}

export default async function ContactPage() {
  const [t, locale, settings] = await Promise.all([getTranslations(), getLocale(), siteSettings()]);
  const { contact } = settings;
  const coordonnees = [
    { Icon: MapPinIcon, label: t("Contact.address"), value: contactAddress(settings, locale), href: undefined },
    { Icon: MailIcon, label: t("Contact.email"), value: contact.email, href: `mailto:${contact.email}` },
    { Icon: PhoneIcon, label: t("Contact.phone"), value: contact.phone, href: telHref(contact.phone) },
  ];

  return (
    <>
      <PageHeader eyebrow={t("Contact.eyebrow")} title={t("Contact.title")} lead={t("Site.name")} />
      <div className="mx-auto max-w-7xl px-4 py-12">
        <ul className="grid gap-6 md:grid-cols-3">
          {coordonnees.map(({ Icon, label, value, href }) => (
            <li key={label}>
              <Card className="h-full transition-all duration-300 hover:scale-[1.03] hover:shadow-xl">
                <CardContent>
                  <span className="grid size-12 place-items-center rounded-control bg-primary text-primary-foreground">
                    <Icon className="size-5" aria-hidden="true" />
                  </span>
                  <p className="mt-4 text-sm font-semibold uppercase tracking-wide text-muted-foreground">{label}</p>
                  {href ? (
                    <a href={href} className="mt-1 block text-lg font-semibold text-brand-ink underline-offset-4 hover:underline">{value}</a>
                  ) : (
                    <p className="mt-1 text-lg font-semibold">{value}</p>
                  )}
                </CardContent>
              </Card>
            </li>
          ))}
        </ul>
      </div>
    </>
  );
}
