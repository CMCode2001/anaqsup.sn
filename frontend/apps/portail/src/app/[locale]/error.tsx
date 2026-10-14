"use client";

import { RotateCwIcon } from "lucide-react";
import { useParams } from "next/navigation";

// Composant client chargé sur TOUTES les pages : textes en dur plutôt que la bibliothèque i18n côté navigateur.
const TEXT = {
  fr: {
    title: "Service momentanément indisponible",
    body: "Les informations demandées ne peuvent pas être affichées pour le moment. Réessayez dans quelques instants.",
    retry: "Réessayer",
  },
  en: {
    title: "Service temporarily unavailable",
    body: "The requested information cannot be displayed at the moment. Please try again shortly.",
    retry: "Try again",
  },
} as const;

/**
 * Erreur de rendu (ex. API indisponible et cache vide) : message clair, sans détail technique.
 * Bouton natif : le Button shadcn tirerait Radix Slot dans le JavaScript de chaque page.
 */
export default function ErrorPage({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const { locale } = useParams<{ locale: string }>();
  const text = TEXT[locale === "en" ? "en" : "fr"];
  return (
    <div className="mx-auto max-w-4xl px-4 py-16">
      <h1 className="text-3xl font-extrabold text-brand-ink">{text.title}</h1>
      <p className="mt-3 text-muted-foreground">{text.body}</p>
      <button type="button" onClick={reset} className="mt-6 inline-flex h-11 items-center gap-2 rounded-full bg-primary px-6 text-sm font-semibold text-primary-foreground hover:bg-primary/85 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none">
        <RotateCwIcon className="size-4" aria-hidden="true" /> {text.retry}
      </button>
    </div>
  );
}
