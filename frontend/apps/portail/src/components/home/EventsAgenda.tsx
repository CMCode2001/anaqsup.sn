import type { EventPage } from "@anaqsup/api-client";
import { ArrowRightIcon, MapPinIcon } from "lucide-react";

import { Link } from "@/i18n/navigation";
import { getLocale, getTranslations } from "@/i18n/server";
import { agendaDay } from "@/lib/format";
import { langAttr } from "@/lib/lang";

type EventSummary = EventPage["content"][number];

/** Agenda de l'accueil (modèle validé) : pastille de date à gauche, intitulé et « Lire la suite » à droite. */
export async function EventsAgenda({ events }: { events: EventSummary[] }) {
  const locale = await getLocale();
  const t = await getTranslations("Common");
  return (
    <ul className="flex flex-col gap-5">
      {events.map((event) => {
        const date = agendaDay(event.startsAt, locale);
        const textLang = langAttr(event.lang, locale);
        return (
          <li key={event.slug}>
            <Link
              href={{ pathname: "/evenements/[slug]", params: { slug: event.slug } }}
              className="group grid grid-cols-[6.5rem_minmax(0,1fr)] gap-3 rounded-3xl transition-transform duration-300 hover:scale-[1.01] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary sm:grid-cols-[9rem_minmax(0,1fr)] sm:gap-5"
            >
              <time
                dateTime={event.startsAt}
                className="flex flex-col items-center justify-center rounded-2xl bg-primary px-2 py-4 text-primary-foreground shadow-sm"
              >
                <span className="text-sm font-semibold sm:text-base">{date.weekday}</span>
                <span className="text-3xl leading-none font-extrabold sm:text-4xl">{date.day}</span>
                <span className="text-sm font-semibold sm:text-base">{date.month}</span>
              </time>
              <span className="flex flex-col justify-center gap-3 rounded-2xl border-t-[3px] border-anthracite bg-muted px-5 py-5 transition-shadow group-hover:shadow-lg sm:flex-row sm:items-center sm:justify-between sm:px-8">
                <span>
                  <span lang={textLang} className="block font-bold leading-snug text-foreground">{event.title}</span>
                  {event.location && (
                    <span className="mt-1 inline-flex items-center gap-1 text-sm text-muted-foreground">
                      <MapPinIcon className="size-3.5" aria-hidden="true" />
                      {event.location} · {date.time}
                    </span>
                  )}
                </span>
                <span className="inline-flex shrink-0 items-center gap-1.5 text-sm font-semibold text-brand-ink">
                  {t("readMore")}
                  <ArrowRightIcon className="size-4 transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
                </span>
              </span>
            </Link>
          </li>
        );
      })}
    </ul>
  );
}
