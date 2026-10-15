import { BadgeCheckIcon, Building2Icon, ClipboardListIcon, GemIcon, GraduationCapIcon, SearchCheckIcon, TargetIcon } from "lucide-react";

import { plainTitle, type Section, SectionCard } from "@/components/content/SectionCards";

/** Pictogramme d'après l'intitulé (français ou anglais), comme sur l'ancien site ; l'ordre peut changer. */
function MissionIcon({ title }: { title: string }) {
  const text = plainTitle(title);
  if (/standard/.test(text)) return <BadgeCheckIcon aria-hidden="true" />;
  if (/syst[eè]me|system/.test(text)) return <GemIcon aria-hidden="true" />;
  if (/proc[ée]dure/.test(text)) return <ClipboardListIcon aria-hidden="true" />;
  if (/avis|opinion/.test(text)) return <SearchCheckIcon aria-hidden="true" />;
  if (/programme|formation/.test(text)) return <GraduationCapIcon aria-hidden="true" />;
  if (/[ée]tablissement|institution/.test(text)) return <Building2Icon aria-hidden="true" />;
  return <TargetIcon aria-hidden="true" />;
}

/**
 * Page « Missions » (demande ANAQ-Sup du 15/10/2026, même style que la Gouvernance) : une carte premium par mission,
 * trois par ligne sur grand écran, deux sur tablette, l'une sous l'autre sur téléphone.
 */
export function MissionCards({ sections, lang }: { sections: Section[]; lang?: string }) {
  return (
    <div lang={lang} className="relative pt-6 md:rounded-box md:bg-muted/50 md:px-6 md:py-14 lg:px-10">
      <div className="grid gap-x-6 gap-y-12 md:grid-cols-2 md:gap-y-14 lg:grid-cols-3 lg:gap-x-8">
        {sections.map((section, index) => (
          <SectionCard key={index} section={section} index={index} icon={<MissionIcon title={section.title} />} />
        ))}
      </div>
    </div>
  );
}
