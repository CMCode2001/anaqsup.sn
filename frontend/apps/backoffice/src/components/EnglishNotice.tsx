import { Badge } from "@anaqsup/ui/components/badge";

/** Rappel de la règle de traduction (DeepL) en tête de l'onglet anglais d'un contenu. */
export function EnglishNotice({ auto, note }: { auto: boolean; note?: string }) {
  return (
    <p className="rounded-2xl bg-muted px-4 py-3 text-sm text-muted-foreground">
      {auto
        ? "Traduction automatique (DeepL), refaite à chaque modification du français. Relisez-la : dès que vous la corrigez, elle devient définitive."
        : "Laissez le titre et le texte vides : la traduction automatique (DeepL) les remplira juste après l'enregistrement. Une version saisie ou corrigée ici n'est jamais écrasée."}
      {note && <> {note}</>}
    </p>
  );
}

/** État de la version anglaise dans une liste : en attente, automatique (non relue) ou rien (relue). */
export function EnglishBadge({ hasEnglish, auto }: { hasEnglish: boolean; auto: boolean }) {
  if (!hasEnglish) return <Badge variant="outline">Anglais en attente</Badge>;
  return auto ? <Badge variant="outline">Anglais automatique</Badge> : null;
}

/** Corps de texte vide pour l'éditeur (Tiptap rend « <p></p> » quand rien n'est saisi). */
export const blankHtml = (html: string) => html.replace(/<p>\s*<\/p>/g, "").trim() === "";
