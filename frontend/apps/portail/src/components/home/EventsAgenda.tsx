import type { EventPage } from "@anaqsup/api-client";
import { ArrowRightIcon, MapPinIcon } from "lucide-react";

import { Link } from "@/i18n/navigation";
import { getLocale, getTranslations } from "@/i18n/server";
import { agendaDay } from "@/lib/format";
import { langAttr } from "@/lib/lang";

type EventSummary = EventPage["content"][number];

/**
 * Agenda de l'accueil (modèle validé) : pastille de date à gauche, intitulé et « Lire la suite » à droite.
 * Téléphone : une seule carte, avec une petite pastille de date calée en haut (elle s'étirait sur toute la hauteur).
 */
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
              className="group flex items-start gap-4 rounded-box bg-muted p-3 transition-transform duration-300 hover:scale-[1.01] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary sm:grid sm:grid-cols-[9rem_minmax(0,1fr)] sm:items-stretch sm:gap-5 sm:bg-transparent sm:p-0"
            >
              <time
                dateTime={event.startsAt}
                className="flex w-[4.5rem] shrink-0 flex-col items-center justify-center rounded-control bg-primary px-1 py-2.5 text-primary-foreground shadow-sm sm:w-auto sm:px-2 sm:py-4"
              >
                <span className="text-xs font-semibold sm:text-base">{date.weekday}</span>
                <span className="text-2xl leading-none font-extrabold sm:text-4xl">{date.day}</span>
                <span className="text-xs font-semibold sm:text-base">{date.month}</span>
              </time>
              <span className="flex min-w-0 flex-1 flex-col gap-2 py-0.5 sm:flex-row sm:items-center sm:justify-between sm:gap-3 sm:rounded-box sm:border-t-[3px] sm:border-anthracite sm:bg-muted sm:px-8 sm:py-5 sm:transition-shadow sm:group-hover:shadow-lg">
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
