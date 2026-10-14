// Dakar vit à l'heure UTC toute l'année (pas d'heure d'été) : l'heure saisie dans un champ
// « date et heure » est directement l'heure UTC envoyée à l'API.

/** Instant de l'API → valeur d'un <input type="datetime-local"> (AAAA-MM-JJTHH:MM). */
export const toLocalInput = (iso: string | null | undefined) => (iso ? new Date(iso).toISOString().slice(0, 16) : "");

/** Valeur d'un <input type="datetime-local"> → instant de l'API (null si vide). */
export const fromLocalInput = (value: string) => (value ? `${value}:00Z` : null);

export const shortDate = new Intl.DateTimeFormat("fr-SN", { dateStyle: "medium", timeZone: "Africa/Dakar" });
export const dayMonth = new Intl.DateTimeFormat("fr-SN", { day: "2-digit", month: "short", timeZone: "Africa/Dakar" });
export const hourMinute = new Intl.DateTimeFormat("fr-SN", { hour: "2-digit", minute: "2-digit", timeZone: "Africa/Dakar" });
