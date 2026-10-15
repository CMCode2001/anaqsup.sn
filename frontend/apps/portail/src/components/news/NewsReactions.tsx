"use client";

import { ThumbsDownIcon, ThumbsUpIcon } from "lucide-react";
import { useSyncExternalStore } from "react";

/** Classes sans conflit entre elles : simple assemblage, sans tailwind-merge (≈ 8 Ko de moins sur la page). */
const cn = (...classes: (string | false | null | undefined)[]) => classes.filter(Boolean).join(" ");

type Reaction = "LIKE" | "DISLIKE";
type State = { likes: number | null; mine: Reaction | null; message: "thanks" | "removed" | "error" | null; saving: boolean };

/** Avant chargement (et rendu serveur) : boutons inactifs, aucun chiffre. */
const INITIAL: State = { likes: null, mine: null, message: null, saving: false };

/*
 * État partagé par article : les boutons de la barre d'outils et ceux de fin d'article restent synchrones,
 * avec un seul appel à /api/reactions. Les chiffres sont lus à l'ouverture de la page (la page elle-même est
 * mise en cache, pas les votes).
 */
const stores = new Map<string, { state: State; listeners: Set<() => void>; loading: boolean }>();

function entry(slug: string) {
  let found = stores.get(slug);
  if (!found) {
    found = { state: INITIAL, listeners: new Set(), loading: false };
    stores.set(slug, found);
  }
  return found;
}

function update(slug: string, patch: Partial<State>) {
  const current = entry(slug);
  current.state = { ...current.state, ...patch };
  current.listeners.forEach((listener) => listener());
}

function subscribe(slug: string, listener: () => void) {
  const current = entry(slug);
  current.listeners.add(listener);
  if (!current.loading) {
    current.loading = true;
    void fetch(`/api/reactions/${slug}`)
      .then((response) => (response.ok ? (response.json() as Promise<{ likes: number; mine: Reaction | null }>) : null))
      .then((data) => data && update(slug, { likes: data.likes, mine: data.mine }))
      .catch(() => undefined); // API indisponible : les boutons restent inactifs
  }
  return () => {
    current.listeners.delete(listener);
  };
}

/** Clic sur l'avis déjà donné : il est retiré. Affichage immédiat, rétabli si l'enregistrement échoue. */
async function vote(slug: string, target: Reaction) {
  const before = entry(slug).state;
  if (before.likes === null || before.saving) return;
  const next = before.mine === target ? null : target;
  const likes = before.likes + (next === "LIKE" ? 1 : 0) - (before.mine === "LIKE" ? 1 : 0);
  update(slug, { mine: next, likes, saving: true, message: null });
  try {
    const response = await fetch(`/api/reactions/${slug}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ reaction: next === null ? "none" : next.toLowerCase() }),
    });
    if (!response.ok) throw new Error(String(response.status));
    const data = (await response.json()) as { likes: number; mine: Reaction | null };
    update(slug, { likes: data.likes, mine: data.mine, saving: false, message: data.mine ? "thanks" : "removed" });
  } catch {
    update(slug, { likes: before.likes, mine: before.mine, saving: false, message: "error" });
  }
}

export type ReactionLabels = {
  group: string;
  question: string;
  like: string;
  dislike: string;
  /** « J'aime ({count}) » : nom accessible du bouton, avec le nombre. */
  likeCount: string;
  thanks: string;
  removed: string;
  error: string;
};

/**
 * Mentions « J'aime » / « Je n'aime pas » d'une actualité (décision ANAQ-Sup du 14/10/2026) : seul le nombre de
 * « J'aime » s'affiche ; un vote par navigateur, modifiable ou annulable.
 * `toolbar` : à côté de A− / A+ / Imprimer / Partager ; `panel` : en fin d'article, avec la question.
 */
export function NewsReactions({ slug, variant, labels }: { slug: string; variant: "toolbar" | "panel"; labels: ReactionLabels }) {
  const state = useSyncExternalStore(
    (listener) => subscribe(slug, listener),
    () => entry(slug).state,
    () => INITIAL,
  );
  const ready = state.likes !== null;
  const likeName = ready ? labels.likeCount.replace("{count}", String(state.likes)) : labels.like;
  const base = cn(
    "inline-flex items-center justify-center rounded-control border text-sm font-semibold shadow-sm transition-colors",
    "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:pointer-events-none disabled:opacity-50",
  );
  const liked = state.mine === "LIKE";
  const disliked = state.mine === "DISLIKE";
  const likeClass = cn(base, liked ? "border-primary bg-primary text-primary-foreground" : "bg-background text-foreground hover:border-primary hover:bg-muted");
  const dislikeClass = cn(base, disliked ? "border-anthracite bg-anthracite text-white" : "bg-background text-foreground hover:border-anthracite hover:bg-muted");
  const message = state.message && labels[state.message];

  if (variant === "toolbar") {
    return (
      <div role="group" aria-label={labels.group} className="flex items-center gap-1.5 sm:gap-2 print:hidden">
        <button type="button" className={cn(likeClass, "h-9 min-w-9 gap-1.5 px-2.5 sm:h-11 sm:min-w-11 sm:gap-2 sm:px-3")} disabled={!ready} aria-pressed={liked}
                aria-label={likeName} title={labels.like} onClick={() => void vote(slug, "LIKE")}>
          <ThumbsUpIcon className={cn("size-4 sm:size-5", liked && "fill-current")} aria-hidden="true" />
          {ready && <span aria-hidden="true" className="tabular-nums">{state.likes}</span>}
        </button>
        <button type="button" className={cn(dislikeClass, "size-9 sm:size-11")} disabled={!ready} aria-pressed={disliked}
                aria-label={labels.dislike} title={labels.dislike} onClick={() => void vote(slug, "DISLIKE")}>
          <ThumbsDownIcon className={cn("size-4 sm:size-5", disliked && "fill-current")} aria-hidden="true" />
        </button>
      </div>
    );
  }

  return (
    <section aria-label={labels.group} className="mt-12 flex flex-col gap-4 rounded-box border bg-muted/40 p-5 sm:flex-row sm:items-center sm:justify-between md:p-6 print:hidden">
      <div>
        <p className="font-heading text-lg font-bold text-foreground">{labels.question}</p>
        <p role="status" className={cn("mt-1 min-h-5 text-sm", state.message === "error" ? "text-destructive" : "text-muted-foreground")}>{message}</p>
      </div>
      <div className="flex shrink-0 gap-3">
        <button type="button" className={cn(likeClass, "h-11 flex-1 gap-2 px-3 whitespace-nowrap sm:flex-none sm:px-4")} disabled={!ready} aria-pressed={liked}
                aria-label={likeName} onClick={() => void vote(slug, "LIKE")}>
          <ThumbsUpIcon className={cn("size-5", liked && "fill-current")} aria-hidden="true" />
          <span aria-hidden="true">{labels.like}</span>
          {ready && <span aria-hidden="true" className={cn("rounded-full px-2 py-0.5 text-xs tabular-nums", liked ? "bg-white/30" : "bg-muted")}>{state.likes}</span>}
        </button>
        <button type="button" className={cn(dislikeClass, "h-11 flex-1 gap-2 px-3 whitespace-nowrap sm:flex-none sm:px-4")} disabled={!ready} aria-pressed={disliked}
                onClick={() => void vote(slug, "DISLIKE")}>
          <ThumbsDownIcon className={cn("size-5", disliked && "fill-current")} aria-hidden="true" />
          {labels.dislike}
        </button>
      </div>
    </section>
  );
}
