import { BriefcaseBusinessIcon, GraduationCapIcon, LandmarkIcon, UsersIcon } from "lucide-react";

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
function OrganIcon({ title, className }: { title: string; className: string }) {
  const text = title.replace(/<[^>]+>/g, "").toLowerCase();
  if (/administration|board/.test(text)) return <LandmarkIcon className={className} aria-hidden="true" />;
  if (/scientifi/.test(text)) return <GraduationCapIcon className={className} aria-hidden="true" />;
  if (isSecretariat(text)) return <BriefcaseBusinessIcon className={className} aria-hidden="true" />;
  return <UsersIcon className={className} aria-hidden="true" />;
}

function isSecretariat(title: string) {
  return /secr[ée]tariat|secretariat|ex[ée]cuti/.test(title.replace(/<[^>]+>/g, "").toLowerCase());
}

const number = (index: number) => String(index + 1).padStart(2, "0");

/** Texte d'un organe (HTML du back-office, assaini à l'écriture), sans la typographie générique des articles. */
function Body({ html, className }: { html: string; className: string }) {
  return <div className={`[&_a]:font-medium [&_a]:underline [&_a]:underline-offset-4 [&_p+p]:mt-3 ${className}`} dangerouslySetInnerHTML={{ __html: html }} />;
}

/** Trait or du téléphone : relie le médaillon de la carte à la carte précédente (colonne vertébrale). */
const SPINE = "before:absolute before:-top-12 before:left-[calc(1.25rem+1.5rem-1px)] before:h-6 before:w-0.5 before:bg-primary md:before:hidden";

function OrgCard({ section, index, connected = false }: { section: Section; index: number; connected?: boolean }) {
  const id = `organe-${index + 1}`;
  return (
    <section aria-labelledby={id} className={`group relative rounded-box bg-card px-5 pt-11 pb-7 shadow-[0_30px_70px_-35px_rgb(0_0_0/0.35)] ring-1 ring-foreground/5 transition duration-500 ease-out hover:-translate-y-1 hover:ring-primary/40 md:px-6 md:pt-12 md:pb-8 lg:px-9 ${connected ? SPINE : ""}`}>
      <span className="absolute -top-6 left-5 flex size-12 items-center justify-center rounded-control bg-anthracite text-primary shadow-lg ring-4 ring-background md:-top-7 md:left-6 md:size-14 lg:left-9">
        <OrganIcon title={section.title} className="size-6 md:size-7" />
      </span>
      <span aria-hidden="true" className="absolute top-4 right-5 rounded-full bg-primary/15 px-3 py-1 font-heading text-xs font-bold tracking-widest text-brand-ink">{number(index)}</span>
      <h2 id={id} className="font-heading text-xl leading-snug font-bold text-brand-ink md:text-2xl" dangerouslySetInnerHTML={{ __html: section.title }} />
      <span aria-hidden="true" className="mt-4 block h-0.5 w-12 bg-primary transition-all duration-500 group-hover:w-20" />
      <Body html={section.html} className="mt-5 text-[15px] leading-relaxed text-muted-foreground" />
    </section>
  );
}

/**
 * Page « Gouvernance » (design « organigramme » choisi par l'ANAQ-Sup le 15/10/2026) : le Conseil d'administration
 * et le Conseil scientifique en haut, reliés par des traits or au Secrétariat exécutif en dessous ; cartes blanches
 * flottantes, médaillon posé sur le bord, numéro de l'organe. Sans Secrétariat exécutif reconnu (ou avec un autre
 * nombre d'organes), simple grille de cartes.
 */
export function GovernanceCards({ sections, lang }: { sections: Section[]; lang?: string }) {
  const bottomIndex = sections.findIndex((s) => isSecretariat(s.title));
  if (sections.length !== 3 || bottomIndex === -1) {
    return <div lang={lang} className="grid gap-12 pt-7 lg:grid-cols-3">{sections.map((s, i) => <OrgCard key={i} section={s} index={i} connected={i > 0} />)}</div>;
  }
  const top = sections.map((s, i) => ({ s, i })).filter(({ i }) => i !== bottomIndex);
  const line = "absolute bg-primary";
  return (
    // Téléphone : cartes pleine largeur sans écrin, reliées médaillon à médaillon. Tablette et plus : écrin, deux
    // conseils en haut reliés au Secrétariat exécutif.
    <div lang={lang} className="relative pt-6 md:rounded-box md:bg-muted/50 md:px-6 md:py-14 lg:px-10">
      <div className="grid gap-12 md:grid-cols-2 md:gap-6 lg:gap-10">
        {top.map(({ s, i }, n) => <OrgCard key={i} section={s} index={i} connected={n > 0} />)}
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
        <OrgCard section={sections[bottomIndex]!} index={bottomIndex} connected />
      </div>
    </div>
  );
}
