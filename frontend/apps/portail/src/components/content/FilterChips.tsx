import { cn } from "@anaqsup/ui/lib/utils";

import { type Href, Link } from "@/i18n/navigation";

/** Filtres par liens (sans JavaScript, partageables). L'option active porte aria-current. */
export function FilterChips({ label, options, active }: {
  label: string;
  options: { href: Href; label: string; value: string | undefined }[];
  active: string | undefined;
}) {
  return (
    <nav
      aria-label={label}
      // Téléphone : une ligne qui défile jusqu'au bord de l'écran (la dernière pastille coupée invite à faire glisser).
      className="-mx-4 flex gap-2 overflow-x-auto px-4 pb-1 [scrollbar-width:none] sm:mx-0 sm:flex-wrap sm:overflow-visible sm:px-0 sm:pb-0 [&::-webkit-scrollbar]:hidden"
    >
      {options.map((option) => {
        const current = option.value === active;
        return (
          <Link
            key={option.value ?? "all"}
            href={option.href}
            aria-current={current ? "page" : undefined}
            className={cn(
              "shrink-0 rounded-full border px-3.5 py-1.5 text-sm font-medium whitespace-nowrap transition-colors",
              current ? "border-primary bg-primary text-primary-foreground" : "bg-background hover:bg-muted",
            )}
          >
            {option.label}
          </Link>
        );
      })}
    </nav>
  );
}
