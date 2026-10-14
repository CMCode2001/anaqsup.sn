import type { Locale } from "@/i18n/routing";

// Français du Sénégal, anglais britannique (orthographe du site : programme, organisation) ; heure de Dakar.
const INTL_LOCALE: Record<Locale, string> = { fr: "fr-SN", en: "en-GB" };
const TIME_ZONE = "Africa/Dakar";

const formatters = (locale: Locale) => {
  const tag = INTL_LOCALE[locale];
  return {
    date: new Intl.DateTimeFormat(tag, { dateStyle: "long", timeZone: TIME_ZONE }),
    shortDate: new Intl.DateTimeFormat(tag, { day: "2-digit", month: "short", year: "numeric", timeZone: TIME_ZONE }),
    integer: new Intl.NumberFormat(tag, { maximumFractionDigits: 0 }),
    decimal: new Intl.NumberFormat(tag, { maximumFractionDigits: 1 }),
    dateTime: new Intl.DateTimeFormat(tag, { dateStyle: "full", timeStyle: "short", timeZone: TIME_ZONE }),
    day: new Intl.DateTimeFormat(tag, { day: "2-digit", timeZone: TIME_ZONE }),
    month: new Intl.DateTimeFormat(tag, { month: "short", timeZone: TIME_ZONE }),
    weekday: new Intl.DateTimeFormat(tag, { weekday: "short", timeZone: TIME_ZONE }),
    monthLong: new Intl.DateTimeFormat(tag, { month: "long", timeZone: TIME_ZONE }),
    time: new Intl.DateTimeFormat(tag, { hour: "2-digit", minute: "2-digit", timeZone: TIME_ZONE }),
  };
};
const FORMATS = { fr: formatters("fr"), en: formatters("en") } as const;

/** `2026-10-13` ou un horodatage → « 13 octobre 2026 » / « 13 October 2026 ». */
export function formatDate(iso: string | null | undefined, locale: Locale): string {
  if (!iso) return "—";
  return FORMATS[locale].date.format(new Date(iso.length === 10 ? `${iso}T12:00:00Z` : iso));
}

/** `2026-01-04` → « 04 janv. 2026 » / « 04 Jan 2026 » (cartes de documents). */
export function formatShortDate(iso: string, locale: Locale): string {
  return FORMATS[locale].shortDate.format(new Date(iso.length === 10 ? `${iso}T12:00:00Z` : iso));
}

/** Taille d'un fichier : « 864 ko », « 1,2 Mo » / « 864 KB », « 1.2 MB ». */
export function formatFileSize(bytes: number, locale: Locale): string {
  const f = FORMATS[locale];
  const kb = bytes / 1024;
  if (kb < 1024) return `${f.integer.format(Math.max(1, kb))} ${locale === "fr" ? "ko" : "KB"}`;
  return `${f.decimal.format(kb / 1024)} ${locale === "fr" ? "Mo" : "MB"}`;
}

/** « lundi 26 octobre 2026 à 09:00 » / « Monday 26 October 2026 at 09:00 ». */
export function formatDateTime(iso: string, locale: Locale): string {
  return FORMATS[locale].dateTime.format(new Date(iso));
}

/** Pastille de calendrier : { day: "26", month: "oct." }. */
export function calendarDay(iso: string, locale: Locale): { day: string; month: string } {
  const date = new Date(iso);
  return { day: FORMATS[locale].day.format(date), month: FORMATS[locale].month.format(date) };
}

const capitalize = (value: string) => value.charAt(0).toUpperCase() + value.slice(1);

/** Pastille d'agenda : { weekday: "Mer", day: "14", month: "Octobre", time: "09:00" }. */
export function agendaDay(iso: string, locale: Locale) {
  const date = new Date(iso);
  const f = FORMATS[locale];
  return {
    weekday: capitalize(f.weekday.format(date).replace(".", "")),
    day: String(Number(f.day.format(date))),
    month: capitalize(f.monthLong.format(date)),
    time: f.time.format(date),
  };
}

/** Heure de publication : « 20h31 » (usage français) / « 8:31 pm ». */
export function formatTime(iso: string, locale: Locale): string {
  const time = FORMATS[locale].time.format(new Date(iso));
  return locale === "fr" ? time.replace(":", "h") : new Intl.DateTimeFormat("en-GB", {
    hour: "numeric", minute: "2-digit", hour12: true, timeZone: TIME_ZONE,
  }).format(new Date(iso));
}

/** Durée de lecture d'un texte HTML, à 200 mots par minute (1 minute au moins). */
export function readingMinutes(html: string): number {
  const words = html.replace(/<[^>]+>/g, " ").split(/\s+/).filter(Boolean).length;
  return Math.max(1, Math.round(words / 200));
}

/** « il y a 14 h » / « 14 hours ago » le premier jour, puis la date courte. */
export function sinceOrDate(iso: string, locale: Locale, now: Date = new Date()): string {
  const hours = Math.floor((now.getTime() - new Date(iso).getTime()) / 3_600_000);
  if (hours >= 0 && hours < 24) {
    const relative = new Intl.RelativeTimeFormat(INTL_LOCALE[locale], { numeric: "auto", style: "short" });
    return hours === 0 ? relative.format(0, "hour") : relative.format(-hours, "hour");
  }
  return formatDate(iso, locale);
}
