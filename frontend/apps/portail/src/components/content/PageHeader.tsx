import type { ReactNode } from "react";

/** En-tête de rubrique commun : surtitre, titre, chapeau. `lang` : langue du titre s'il n'est pas traduit. */
export function PageHeader({ eyebrow, title, lead, lang, children }: {
  eyebrow?: string;
  title: string;
  lead?: string | null;
  lang?: string;
  children?: ReactNode;
}) {
  return (
    <header className="border-b bg-muted/60">
      <div className="mx-auto max-w-7xl px-4 py-10 md:py-14">
        {eyebrow && <p className="text-sm font-semibold uppercase tracking-wide text-brand-green">{eyebrow}</p>}
        <h1 lang={lang} className="mt-2 text-3xl font-extrabold text-brand-ink md:text-4xl">{title}</h1>
        {lead && <p lang={lang} className="mt-3 max-w-3xl text-lg text-muted-foreground">{lead}</p>}
        {children}
      </div>
    </header>
  );
}
