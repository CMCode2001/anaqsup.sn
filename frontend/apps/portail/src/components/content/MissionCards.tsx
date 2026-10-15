import { BadgeCheckIcon, Building2Icon, ClipboardListIcon, GemIcon, GraduationCapIcon, SearchCheckIcon, TargetIcon } from "lucide-react";

import { plainTitle, type Section } from "@/components/content/SectionCards";

/** Pictogramme d'après l'intitulé (français ou anglais), comme sur l'ancien site ; l'ordre peut changer. */
function MissionIcon({ title }: { title: string }) {
  const text = plainTitle(title);
  if (/standard/.test(text)) return <BadgeCheckIcon className="size-5" aria-hidden="true" />;
  if (/syst[eè]me|system/.test(text)) return <GemIcon className="size-5" aria-hidden="true" />;
  if (/proc[ée]dure/.test(text)) return <ClipboardListIcon className="size-5" aria-hidden="true" />;
  if (/avis|opinion/.test(text)) return <SearchCheckIcon className="size-5" aria-hidden="true" />;
  if (/programme|formation/.test(text)) return <GraduationCapIcon className="size-5" aria-hidden="true" />;
  if (/[ée]tablissement|institution/.test(text)) return <Building2Icon className="size-5" aria-hidden="true" />;
  return <TargetIcon className="size-5" aria-hidden="true" />;
}

/** Chiffre détouré en or (contour seul), qui se remplit d'or au survol. */
const OUTLINE = "text-transparent [-webkit-text-stroke:1.5px_color-mix(in_oklch,var(--primary)_55%,transparent)]";

/**
 * Page « Missions » (design « liste éditoriale » choisi par l'ANAQ-Sup le 15/10/2026, parmi trois propositions) :
 * deux colonnes sans boîtes ; pour chaque mission, grand chiffre détouré et pictogramme à gauche, intitulé, trait or
 * et texte à droite ; filets fins entre les missions. Une colonne sur téléphone et tablette.
 */
export function MissionCards({ sections, lang }: { sections: Section[]; lang?: string }) {
  return (
    <ol lang={lang} className="grid gap-x-16 lg:grid-cols-2">
      {sections.map((section, index) => {
        const id = `mission-${index + 1}`;
        return (
          <li key={id} className="group flex gap-5 border-t border-foreground/10 py-9 md:gap-8 md:py-10">
            {/* Colonne de largeur fixe : les intitulés restent alignés quelle que soit la largeur du chiffre. */}
            <div className="flex w-16 shrink-0 flex-col items-center gap-4 md:w-20">
              <span aria-hidden="true" className={`font-heading text-5xl leading-none font-extrabold transition-colors duration-500 group-hover:text-primary md:text-7xl ${OUTLINE}`}>
                {String(index + 1).padStart(2, "0")}
              </span>
              <span className="flex size-11 items-center justify-center rounded-control bg-anthracite text-primary shadow-md">
                <MissionIcon title={section.title} />
              </span>
            </div>
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
