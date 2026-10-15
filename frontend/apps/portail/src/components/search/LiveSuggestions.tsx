"use client";

import { lazy, Suspense, useEffect, useState } from "react";

import type { SuggestionLabels } from "@/components/search/SearchSuggestions";

// Chargé à la première frappe seulement : la page ne porte pas le code des suggestions avant.
const SearchSuggestions = lazy(() => import("@/components/search/SearchSuggestions"));

/** Suggestions pendant la frappe dans le champ `inputId` (rendu par le serveur, utilisable sans JavaScript). */
export function LiveSuggestions({ inputId, lang, labels }: { inputId: string; lang: string; labels: SuggestionLabels }) {
  const [started, setStarted] = useState(false);

  useEffect(() => {
    const input = document.getElementById(inputId);
    if (!input) return;
    const start = () => setStarted(true);
    input.addEventListener("input", start, { once: true });
    return () => input.removeEventListener("input", start);
  }, [inputId]);

  return started ? (
    <Suspense>
      <SearchSuggestions inputId={inputId} lang={lang} labels={labels} />
    </Suspense>
  ) : null;
}
