import type { ComponentProps } from "react";

/** Zone de texte aux formes du back-office (coins arrondis, anneau de focus de la charte). */
export function Textarea(props: ComponentProps<"textarea">) {
  return <textarea {...props} className="min-h-28 w-full rounded-3xl border bg-background px-5 py-3 text-sm leading-relaxed focus-visible:ring-3 focus-visible:ring-ring/40 focus-visible:outline-none" />;
}
