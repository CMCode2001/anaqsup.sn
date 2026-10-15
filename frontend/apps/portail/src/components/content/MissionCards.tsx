import type { Section } from "@/components/content/SectionCards";

/** Chiffre détouré en or (contour seul), qui se remplit d'or au survol. */
const OUTLINE = "text-transparent [-webkit-text-stroke:1.5px_color-mix(in_oklch,var(--primary)_55%,transparent)]";

/**
 * Page « Missions » (design « liste éditoriale » choisi par l'ANAQ-Sup le 15/10/2026, parmi trois propositions) :
 * deux colonnes sans boîtes ; pour chaque mission, grand chiffre détouré à gauche (sans pictogramme, à la demande de
 * l'ANAQ-Sup), intitulé, trait or et texte à droite ; filets fins entre les missions. Une colonne sur téléphone et
 * tablette.
 */
export function MissionCards({ sections, lang }: { sections: Section[]; lang?: string }) {
  return (
    <ol lang={lang} className="grid gap-x-16 lg:grid-cols-2">
      {sections.map((section, index) => {
        const id = `mission-${index + 1}`;
        return (
          <li key={id} className="group flex gap-5 border-t border-foreground/10 py-9 md:gap-8 md:py-10">
            {/* Colonne de largeur fixe : les intitulés restent alignés quelle que soit la largeur du chiffre. */}
            <span aria-hidden="true" className={`w-16 shrink-0 text-center font-heading text-5xl leading-none font-extrabold transition-colors duration-500 group-hover:text-primary md:w-20 md:text-7xl ${OUTLINE}`}>
              {String(index + 1).padStart(2, "0")}
            </span>
            <section aria-labelledby={id} className="min-w-0 pt-1">
              {/* Intitulé et texte saisis dans le back-office (HTML assaini à l'écriture). */}
              <h2 id={id} className="font-heading text-xl leading-tight font-extrabold text-brand-ink md:text-2xl" dangerouslySetInnerHTML={{ __html: section.title }} />
              <span aria-hidden="true" className="mt-4 block h-1 w-12 bg-primary transition-all duration-500 group-hover:w-20" />
              <div
                className="mt-4 text-base leading-relaxed text-foreground/80 [&_a]:font-medium [&_a]:underline [&_a]:underline-offset-4 [&_p+p]:mt-3"
                dangerouslySetInnerHTML={{ __html: section.html }}
              />
            </section>
          </li>
        );
      })}
    </ol>
  );
}
