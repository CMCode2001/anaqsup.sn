"use client";

import { ChevronLeftIcon, ChevronRightIcon, PauseIcon, PlayIcon } from "lucide-react";
import { Children, type ReactNode, useEffect, useRef, useState } from "react";

const DELAY_MS = 6000;

/** Boutons en verre dépoli, lisibles sur toutes les images (sur la photo à partir de la tablette). */
const control =
  "inline-flex size-9 items-center justify-center rounded-full bg-white/15 md:size-10 text-white ring-1 ring-white/25 backdrop-blur-sm transition-colors hover:bg-white hover:text-anthracite focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white";

/**
 * Carrousel « À la une » (décision ANAQ-Sup du 14/10/2026) : bandeau pleine largeur en haut de l'accueil ;
 * fait défiler les diapositives (rendues sur le serveur) toutes les 6 secondes, sans s'arrêter ; après
 * un clic sur une flèche ou un point, le compte repart de zéro. Seul le bouton pause l'arrête (WCAG 2.2.2).
 * Si l'utilisateur limite les animations, les diapositives changent sans glisser. Glissement du doigt.
 */
export function HeroCarousel({ children, labels }: {
  children: ReactNode;
  labels: { region: string; previous: string; next: string; pause: string; play: string; goTo: string; slide: string };
}) {
  const slides = Children.toArray(children);
  const count = slides.length;
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const touchX = useRef<number | null>(null);

  // Un minuteur par diapositive : changer de diapositive (auto ou clic) relance les 6 secondes.
  useEffect(() => {
    if (paused || count < 2) return;
    const timer = window.setTimeout(() => setIndex((i) => (i + 1) % count), DELAY_MS);
    return () => window.clearTimeout(timer);
  }, [paused, index, count]);

  if (count === 0) return null;
  const go = (target: number) => setIndex((target + count) % count);
  const label = (template: string, n: number) => template.replace("{n}", String(n)).replace("{total}", String(count));

  return (
    <section
      aria-roledescription="carousel"
      aria-label={labels.region}
      className="relative overflow-hidden"
      onTouchStart={(event) => { touchX.current = event.touches[0]?.clientX ?? null; }}
      onTouchEnd={(event) => {
        const start = touchX.current;
        const end = event.changedTouches[0]?.clientX;
        touchX.current = null;
        if (start == null || end == null || Math.abs(end - start) < 50) return;
        go(end < start ? index + 1 : index - 1);
      }}
    >
      <div
        aria-live={paused ? "polite" : "off"}
        className="flex transition-transform duration-700 ease-out motion-reduce:transition-none"
        style={{ transform: `translateX(-${index * 100}%)` }}
      >
        {slides.map((slide, i) => (
          <div
            key={i}
            role="group"
            aria-roledescription="slide"
            aria-label={label(labels.slide, i + 1)}
            aria-hidden={i !== index}
            inert={i !== index}
            className="w-full shrink-0"
          >
            {slide}
          </div>
        ))}
      </div>

      {count > 1 && (
        // Téléphone : barre sous le texte, sur le fond anthracite (sur la photo, elles en cachaient une partie) :
        // points à gauche, boutons à droite. À partir de la tablette : sur la photo, alignées sur le bord droit du
        // site, à la hauteur de « Lire la suite ».
        <div className="bg-anthracite pb-6 md:absolute md:inset-x-0 md:bottom-14 md:z-10 md:bg-transparent md:pb-0">
          <div className="mx-auto flex max-w-7xl items-center justify-between gap-3 px-4 md:justify-end">
          <div className="flex items-center gap-1.5">
            {slides.map((_, i) => (
              <button
                key={i}
                type="button"
                onClick={() => go(i)}
                aria-label={label(labels.goTo, i + 1)}
                aria-current={i === index ? "true" : undefined}
                className="group/dot flex h-6 items-center px-0.5 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
              >
                <span className={`block h-2 rounded-full transition-all duration-300 ${i === index ? "w-7 bg-primary" : "w-2 bg-white/60 group-hover/dot:bg-white"}`} />
              </button>
            ))}
          </div>
          <div className="flex items-center gap-2">
            <button type="button" className={control} onClick={() => setPaused((p) => !p)} aria-label={paused ? labels.play : labels.pause} title={paused ? labels.play : labels.pause}>
              {paused ? <PlayIcon className="size-4" aria-hidden="true" /> : <PauseIcon className="size-4" aria-hidden="true" />}
            </button>
            <button type="button" className={control} onClick={() => go(index - 1)} aria-label={labels.previous} title={labels.previous}>
              <ChevronLeftIcon className="size-5" aria-hidden="true" />
            </button>
            <button type="button" className={control} onClick={() => go(index + 1)} aria-label={labels.next} title={labels.next}>
              <ChevronRightIcon className="size-5" aria-hidden="true" />
            </button>
          </div>
          </div>
        </div>
      )}
    </section>
  );
}
