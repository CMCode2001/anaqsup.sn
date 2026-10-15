import { ArrowLeftIcon, CalendarIcon, MapPinIcon } from "lucide-react";
import type { Metadata } from "next";
import { getLocale, getTranslations } from "@/i18n/server";
import { notFound } from "next/navigation";

import { Button } from "@anaqsup/ui/components/button";

import { FrenchOnlyNotice } from "@/components/content/FrenchOnlyNotice";
import { RichText } from "@/components/content/RichText";
import { Link } from "@/i18n/navigation";
import { api } from "@/lib/api";
import { langAttr, servedLang } from "@/lib/lang";
import { formatDateTime } from "@/lib/format";
import { alternates } from "@/lib/metadata";

export const revalidate = 600;

export function generateStaticParams() {
  return [];
}

type Props = { params: Promise<{ slug: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const lang = await getLocale();
  const event = await api.event(slug, lang);
  if (!event) return { title: (await getTranslations("Events"))("notFound"), robots: { index: false } };
  return {
    title: event.title,
    description: event.summary ?? undefined,
    alternates: alternates(lang, { pathname: "/evenements/[slug]", params: { slug } }, servedLang(event.lang)),
  };
}

export default async function EvenementPage({ params }: Props) {
  const { slug } = await params;
  const lang = await getLocale();
  const t = await getTranslations("Events");
  const event = await api.event(slug, lang);
  if (!event) notFound();
  const contentLang = langAttr(event.lang, lang);

  return (
    <article className="mx-auto max-w-7xl px-4 py-12 [&>*]:max-w-4xl">
      <Button asChild variant="ghost" size="sm">
        <Link href="/evenements"><ArrowLeftIcon data-icon="inline-start" aria-hidden="true" />{t("back")}</Link>
      </Button>
      <h1 lang={contentLang} className="mt-6 text-3xl font-extrabold leading-tight text-brand-ink md:text-4xl">{event.title}</h1>
      <FrenchOnlyNotice lang={event.lang} className="mt-6" />
      <div className="mt-6 grid gap-3 rounded-box bg-muted p-6 sm:grid-cols-2">
        <p className="flex items-start gap-2">
          <CalendarIcon className="mt-0.5 size-5 shrink-0 text-brand-ink" aria-hidden="true" />
          <span>
            <time dateTime={event.startsAt}>{formatDateTime(event.startsAt, lang)}</time>
            {event.endsAt && (
              <>
                <br />
                <span className="text-sm text-muted-foreground">{t("until", { date: formatDateTime(event.endsAt, lang) })}</span>
              </>
            )}
          </span>
        </p>
        {event.location && (
          <p className="flex items-start gap-2"><MapPinIcon className="mt-0.5 size-5 shrink-0 text-brand-ink" aria-hidden="true" />{event.location}</p>
        )}
      </div>
      <div className="mt-8 rounded-box border bg-card p-6 shadow-sm md:p-10">
        <RichText html={event.bodyHtml} lang={contentLang} />
      </div>
    </article>
  );
}
