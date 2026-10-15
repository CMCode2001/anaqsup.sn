import { BriefcaseBusinessIcon, GraduationCapIcon, LandmarkIcon, UsersIcon } from "lucide-react";

import { plainTitle, type Section, SectionCard } from "@/components/content/SectionCards";

const isSecretariat = (title: string) => /secr[ée]tariat|secretariat|ex[ée]cuti/.test(plainTitle(title));

/** Pictogramme d'après l'intitulé (français ou anglais), pas d'après la position : l'ordre peut changer. */
function OrganIcon({ title }: { title: string }) {
  const text = plainTitle(title);
  if (/administration|board/.test(text)) return <LandmarkIcon aria-hidden="true" />;
  if (/scientifi/.test(text)) return <GraduationCapIcon aria-hidden="true" />;
  if (isSecretariat(text)) return <BriefcaseBusinessIcon aria-hidden="true" />;
  return <UsersIcon aria-hidden="true" />;
}

/**
 * Page « Gouvernance » (design « organigramme » choisi par l'ANAQ-Sup le 15/10/2026) : le Conseil d'administration
 * et le Conseil scientifique en haut, reliés par des traits or au Secrétariat exécutif en dessous. Sans Secrétariat
 * exécutif reconnu (ou avec un autre nombre d'organes), simple grille de cartes.
 */
export function GovernanceCards({ sections, lang }: { sections: Section[]; lang?: string }) {
  const card = (section: Section, index: number, connected: boolean) => (
    <SectionCard key={index} section={section} index={index} icon={<OrganIcon title={section.title} />} connected={connected} />
  );
  const bottomIndex = sections.findIndex((s) => isSecretariat(s.title));
  if (sections.length !== 3 || bottomIndex === -1) {
    return <div lang={lang} className="grid gap-12 pt-7 lg:grid-cols-3">{sections.map((s, i) => card(s, i, i > 0))}</div>;
  }
  const top = sections.map((s, i) => ({ s, i })).filter(({ i }) => i !== bottomIndex);
  const line = "absolute bg-primary";
  return (
    // Téléphone : cartes pleine largeur sans écrin, reliées médaillon à médaillon. Tablette et plus : écrin, deux
    // conseils en haut reliés au Secrétariat exécutif.
    <div lang={lang} className="relative pt-6 md:rounded-box md:bg-muted/50 md:px-6 md:py-14 lg:px-10">
      <div className="grid gap-12 md:grid-cols-2 md:gap-6 lg:gap-10">
        {top.map(({ s, i }, n) => card(s, i, n > 0))}
      </div>
      {/* Grand écran : les deux conseils reliés au Secrétariat exécutif. */}
      <div aria-hidden="true" className="relative hidden h-20 md:block">
        <span className={`${line} top-0 left-1/4 h-1/2 w-0.5`} />
        <span className={`${line} top-0 right-1/4 h-1/2 w-0.5`} />
        <span className={`${line} top-1/2 right-1/4 left-1/4 h-0.5`} />
        <span className={`${line} top-1/2 left-1/2 h-1/2 w-0.5 -translate-x-1/2`} />
        <span className="absolute top-1/2 left-1/2 size-3 -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary ring-4 ring-muted" />
      </div>
      <div className="mt-12 md:mx-auto md:mt-7 md:w-3/4 lg:w-2/3">
        {card(sections[bottomIndex]!, bottomIndex, true)}
      </div>
    </div>
  );
}
