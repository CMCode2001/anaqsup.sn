import { Building2Icon, CircleCheckIcon, CircleXIcon, GraduationCapIcon, ScrollTextIcon } from "lucide-react";
import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";

import { Card, CardDescription, CardHeader, CardTitle } from "@anaqsup/ui/components/card";

import { PageHeader } from "@/components/content/PageHeader";
import { SearchForm } from "@/components/registre/SearchForm";
import { Link } from "@/i18n/navigation";
import { alternates } from "@/lib/metadata";
import { intro } from "@/lib/settings";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Approvals");
  return { title: t("metaTitle"), description: t("metaDescription"), alternates: alternates(await getLocale(), "/habilitations-et-accreditations") };
}

/** Rubrique pédagogique : comprendre avant de consulter les listes. */
export default async function HabilitationsAccreditationsPage() {
  const t = await getTranslations("Approvals");
  const entries = [
    { href: "/etablissements", Icon: Building2Icon, title: t("institutionsTitle"), text: t("institutionsText"), cta: t("institutionsCta") },
    { href: "/formations", Icon: GraduationCapIcon, title: t("programmesTitle"), text: t("programmesText"), cta: t("programmesCta") },
  ] as const;

  return (
    <>
      <PageHeader eyebrow={t("eyebrow")} title={t("title")} lead={await intro("Approvals.lead")}>
        <div className="mt-8 max-w-3xl"><SearchForm variant="hero" /></div>
      </PageHeader>

      <div className="mx-auto max-w-7xl px-4 py-12">
        <ul className="grid gap-6 md:grid-cols-2">
          {entries.map(({ href, Icon, title, text, cta }) => (
            <li key={href}>
              <Link href={href} className="group block h-full rounded-box focus-visible:outline-none">
                <Card className="h-full transition-all duration-300 ease-out group-hover:scale-[1.03] group-hover:shadow-xl">
                  <CardHeader>
                    <span className="mb-4 grid size-14 place-items-center rounded-control bg-primary text-primary-foreground">
                      <Icon className="size-6" aria-hidden="true" />
                    </span>
                    <CardTitle className="font-heading text-xl text-brand-ink">{title}</CardTitle>
                    <CardDescription className="mt-2 text-base">{text}</CardDescription>
                    <span className="mt-4 inline-flex rounded-control bg-muted px-4 py-2 text-sm font-semibold text-brand-ink">{cta} →</span>
                  </CardHeader>
                </Card>
              </Link>
            </li>
          ))}
        </ul>

        <section aria-labelledby="lire-avis" className="mt-14">
          <h2 id="lire-avis" className="text-2xl font-bold text-brand-ink">{t("howToRead")}</h2>
          <div className="mt-6 grid gap-4 md:grid-cols-2">
            <div className="flex gap-4 rounded-box bg-status-ok-surface p-6">
              <CircleCheckIcon className="size-7 shrink-0 text-status-ok" aria-hidden="true" />
              <div>
                <p className="font-semibold text-status-ok">{t("favourable")}</p>
                <p className="mt-1 text-sm">{t("favourableText")}</p>
              </div>
            </div>
            <div className="flex gap-4 rounded-box bg-status-stop-surface p-6">
              <CircleXIcon className="size-7 shrink-0 text-status-stop" aria-hidden="true" />
              <div>
                <p className="font-semibold text-status-stop">{t("unfavourable")}</p>
                <p className="mt-1 text-sm">{t("unfavourableText")}</p>
              </div>
            </div>
          </div>
          <p className="mt-4 text-sm text-muted-foreground">
            {t.rich("note", {
              link: (chunks) => <Link href="/contact" className="font-semibold text-brand-ink underline underline-offset-4">{chunks}</Link>,
            })}
          </p>
        </section>

        <section aria-labelledby="procedures" className="mt-14">
          <h2 id="procedures" className="text-2xl font-bold text-brand-ink">{t("forInstitutions")}</h2>
          <ul className="mt-6 grid gap-4 md:grid-cols-2">
            {([
              { href: "/procedures/habilitation", label: t("approvalProcedure") },
              { href: "/procedures/accreditation", label: t("accreditationProcedure") },
            ] as const).map((item) => (
              <li key={item.href}>
                <Link href={item.href} className="flex items-center gap-3 rounded-control border bg-card px-6 py-4 font-semibold text-brand-ink transition-all hover:scale-[1.02] hover:shadow-md">
                  <ScrollTextIcon className="size-5" aria-hidden="true" /> {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </>
  );
}
