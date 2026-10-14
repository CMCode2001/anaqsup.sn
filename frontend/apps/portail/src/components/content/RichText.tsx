import { cn } from "@anaqsup/ui/lib/utils";

/**
 * Corps HTML d'un contenu publié. Le HTML est assaini par le backend à l'écriture (back-office) ;
 * il est servi tel quel ici. Typographie appliquée par sélecteurs (pas de plugin supplémentaire).
 * `lang` : langue du texte quand elle diffère de celle de la page (contenu non traduit).
 * Variante `article` : texte plus grand, réglable (A− / A+ via --article-scale), justifié avec coupure des mots
 * (pas de grands blancs entre les mots), titres intermédiaires alignés à gauche, lettrine verte.
 */
export const ARTICLE_TEXT = cn(
  "text-[calc(1.0625rem*var(--article-scale,1))] leading-[1.85] md:text-[calc(1.125rem*var(--article-scale,1))]",
  "text-justify hyphens-auto [&_h2]:text-left [&_h3]:text-left",
  "[&>p:first-child]:first-letter:float-left [&>p:first-child]:first-letter:mt-1 [&>p:first-child]:first-letter:mr-3",
  "[&>p:first-child]:first-letter:font-heading [&>p:first-child]:first-letter:text-[4.25em] [&>p:first-child]:first-letter:leading-[0.8]",
  "[&>p:first-child]:first-letter:font-extrabold [&>p:first-child]:first-letter:text-brand-green",
);

export function RichText({ html, lang, className, id }: { html: string; lang?: string; className?: string; id?: string }) {
  return (
    <div
      id={id}
      lang={lang}
      className={cn(
        "max-w-none text-base leading-relaxed text-foreground",
        "[&_p]:mt-4 [&_p:first-child]:mt-0 [&_h2]:mt-8 [&_h2]:font-heading [&_h2]:text-xl [&_h2]:font-bold [&_h2]:text-brand-ink",
        "[&_h3]:mt-6 [&_h3]:font-semibold [&_ul]:mt-4 [&_ul]:list-disc [&_ul]:space-y-1.5 [&_ul]:pl-6 [&_ol]:mt-4 [&_ol]:list-decimal [&_ol]:pl-6",
        "[&_a]:font-medium [&_a]:text-brand-ink [&_a]:underline [&_a]:underline-offset-4 [&_em]:text-muted-foreground",
        className,
      )}
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}
