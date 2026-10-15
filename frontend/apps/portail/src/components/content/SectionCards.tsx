import type { ReactNode } from "react";

export type Section = { title: string; html: string };

/**
 * Texte de la page découpé à chaque intertitre <h2> : ce qui précède le premier intertitre, puis une section par
 * intertitre. Le texte reste celui du back-office ; seule la présentation change.
 */
export function splitSections(html: string): { intro: string; sections: Section[] } {
  const parts = html.split(/<h2(?:\s[^>]*)?>([\s\S]*?)<\/h2>/);
  const sections: Section[] = [];
  for (let i = 1; i < parts.length; i += 2) {
    sections.push({ title: parts[i]!.trim(), html: (parts[i + 1] ?? "").trim() });
  }
  return { intro: (parts[0] ?? "").trim(), sections };
}

/** Intitulé en clair (sans balises, en minuscules) : pour choisir un pictogramme d'après le sens. */
export const plainTitle = (title: string) => title.replace(/<[^>]+>/g, "").toLowerCase();

/**
 * Carte premium des pages institutionnelles (Gouvernance, Missions ; style choisi par l'ANAQ-Sup le 15/10/2026) :
 * carte blanche flottante, médaillon anthracite posé sur le bord, numéro, intitulé, trait or, texte.
 * `connected` : trait or vers la carte précédente sur téléphone (cartes empilées, alignées sur le médaillon).
 */
export function SectionCard({ section, index, icon, connected = false }: {
  section: Section;
  index: number;
  icon: ReactNode;
  connected?: boolean;
}) {
  const id = `section-${index + 1}`;
  return (
    <section
      aria-labelledby={id}
      className={`group relative rounded-box bg-card px-5 pt-11 pb-7 shadow-[0_30px_70px_-35px_rgb(0_0_0/0.35)] ring-1 ring-foreground/5 transition duration-500 ease-out hover:-translate-y-1 hover:ring-primary/40 md:px-6 md:pt-12 md:pb-8 lg:px-9 ${connected ? "before:absolute before:-top-12 before:left-[calc(1.25rem+1.5rem-1px)] before:h-6 before:w-0.5 before:bg-primary md:before:hidden" : ""}`}
    >
      <span className="absolute -top-6 left-5 flex size-12 items-center justify-center rounded-control bg-anthracite text-primary shadow-lg ring-4 ring-background md:-top-7 md:left-6 md:size-14 lg:left-9 [&_svg]:size-6 md:[&_svg]:size-7">
        {icon}
      </span>
      <span aria-hidden="true" className="absolute top-4 right-5 rounded-full bg-primary/15 px-3 py-1 font-heading text-xs font-bold tracking-widest text-brand-ink">
        {String(index + 1).padStart(2, "0")}
      </span>
      {/* Intitulé et texte saisis dans le back-office (HTML assaini à l'écriture). */}
      <h2 id={id} className="font-heading text-xl leading-snug font-bold text-brand-ink md:text-2xl" dangerouslySetInnerHTML={{ __html: section.title }} />
      <span aria-hidden="true" className="mt-4 block h-0.5 w-12 bg-primary transition-all duration-500 group-hover:w-20" />
      <div
        className="mt-5 text-[15px] leading-relaxed text-muted-foreground [&_a]:font-medium [&_a]:underline [&_a]:underline-offset-4 [&_p+p]:mt-3"
        dangerouslySetInnerHTML={{ __html: section.html }}
      />
    </section>
  );
}
