import { cn } from "@anaqsup/ui/lib/utils";

/**
 * Légende d'une photo de personne (ex. le Secrétaire exécutif) : le nom en évidence, un trait or,
 * puis la fonction en dessous. Nom et fonction sont saisis séparément dans le back-office.
 */
export function PersonCaption({ name, role, size = "lg" }: {
  name: string | null | undefined;
  role: string | null | undefined;
  size?: "lg" | "md";
}) {
  if (!name && !role) return null;
  return (
    <figcaption className={cn("border-t text-center", size === "lg" ? "px-6 py-5" : "px-5 py-4")}>
      {name && (
        <p className={cn("font-heading leading-tight font-extrabold text-brand-ink", size === "lg" ? "text-xl" : "text-lg")}>{name}</p>
      )}
      {name && role && <span aria-hidden="true" className="mx-auto my-2 block h-0.5 w-10 rounded-[1px] bg-primary" />}
      {role && <p className="text-sm font-medium text-muted-foreground">{role}</p>}
    </figcaption>
  );
}
