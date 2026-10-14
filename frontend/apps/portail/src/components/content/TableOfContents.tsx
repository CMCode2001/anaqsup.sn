const ENTITIES: Record<string, string> = { amp: "&", lt: "<", gt: ">", quot: "\"", "#39": "'", apos: "'", nbsp: " " };

export type Heading = { id: string; text: string };

/**
 * Ancre « section-N » sur chaque intertitre (h2) d'un texte saisi dans le back-office, et la liste de ces
 * intertitres pour le sommaire (fiches procédures, pages légales). `numbered` : intertitres numérotés
 * (« 1. », « 2. »…) sans que la rédaction ait à le faire.
 */
export function withAnchors(html: string, { numbered = false }: { numbered?: boolean } = {}): { html: string; headings: Heading[] } {
  const headings: Heading[] = [];
  const anchored = html.replace(/<h2(?:\s[^>]*)?>([\s\S]*?)<\/h2>/g, (_, inner: string) => {
    const id = `section-${headings.length + 1}`;
    const text = inner.replace(/<[^>]+>/g, "").replace(/&(#39|[a-z]+);/g, (entity, name: string) => ENTITIES[name] ?? entity).trim();
    headings.push({ id, text });
    return `<h2 id="${id}">${numbered ? `${headings.length}. ` : ""}${inner}</h2>`;
  });
  return { html: anchored, headings: headings.filter((h) => h.text) };
}

/** Sommaire numéroté, vers les intertitres de la page. */
export function TableOfContents({ label, items, lang }: { label: string; items: Heading[]; lang?: string }) {
  if (items.length === 0) return null;
  return (
    <nav aria-label={label} lang={lang} className="mt-8 rounded-xl border bg-muted p-5 print:hidden">
      <ol className="list-decimal space-y-1 pl-5 text-sm">
        {items.map((item) => <li key={item.id}><a href={`#${item.id}`}>{item.text}</a></li>)}
      </ol>
    </nav>
  );
}
