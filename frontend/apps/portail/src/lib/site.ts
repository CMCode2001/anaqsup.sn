export const site = {
  url: (process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000").replace(/\/$/, ""),
  shortName: "ANAQ-Sup",
  // Nom complet et ministère : traduits, dans messages/*.json (clés Site.name, Site.ministry).
  // Coordonnées, réseaux sociaux et liens utiles : back-office → Paramètres du site (lib/settings.ts).
} as const;

/** Lien `tel:` au format E.164, sans espaces. */
export const telHref = (phone: string) => `tel:${phone.replaceAll(" ", "")}`;
