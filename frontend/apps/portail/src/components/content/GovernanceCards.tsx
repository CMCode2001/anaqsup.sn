import { BriefcaseBusinessIcon, GraduationCapIcon, LandmarkIcon, UsersIcon } from "lucide-react";

import { RichText } from "@/components/content/RichText";

type Section = { title: string; html: string };

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

/** Pictogramme d'après l'intitulé (français ou anglais), pas d'après la position : l'ordre peut changer. */
function iconFor(title: string) {
  const text = title.replace(/<[^>]+>/g, "").toLowerCase();
  if (/administration|board/.test(text)) return LandmarkIcon;
  if (/scientifi/.test(text)) return GraduationCapIcon;
  if (/secr[ée]tariat|secretariat|ex[ée]cuti/.test(text)) return BriefcaseBusinessIcon;
  return UsersIcon;
}

/**
 * Page « Gouvernance » (choix ANAQ-Sup du 15/10/2026) : une carte par organe — pictogramme, numéro, intitulé et
 * texte — côte à côte sur grand écran, l'une sous l'autre sinon.
 */
export function GovernanceCards({ sections, lang }: { sections: Section[]; lang?: string }) {
  return (
    <div lang={lang} className="grid gap-6 lg:grid-cols-3">
      {sections.map((section, index) => {
        const Icon = iconFor(section.title);
        const id = `organe-${index + 1}`;
        return (
          <section key={id} aria-labelledby={id} className="flex flex-col rounded-box border bg-card p-6 shadow-sm md:p-8">
            <div className="flex items-start justify-between gap-4">
              <span className="flex size-12 items-center justify-center rounded-control bg-primary/15 text-brand-ink">
                <Icon className="size-6" aria-hidden="true" />
              </span>
              <span aria-hidden="true" className="font-heading text-4xl leading-none font-extrabold text-primary/45">
                {String(index + 1).padStart(2, "0")}
              </span>
            </div>
            {/* Intitulé saisi dans le back-office (HTML assaini à l'écriture). */}
            <h2 id={id} className="mt-6 font-heading text-xl leading-snug font-bold text-brand-ink" dangerouslySetInnerHTML={{ __html: section.title }} />
            <span aria-hidden="true" className="mt-4 block h-1 w-12 rounded-full bg-primary" />
            <RichText html={section.html} className="mt-5" />
          </section>
        );
      })}
    </div>
  );
}
