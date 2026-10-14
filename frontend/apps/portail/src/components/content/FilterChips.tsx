import { cn } from "@anaqsup/ui/lib/utils";

import { type Href, Link } from "@/i18n/navigation";

/** Filtres par liens (sans JavaScript, partageables). L'option active porte aria-current. */
export function FilterChips({ label, options, active }: {
  label: string;
  options: { href: Href; label: string; value: string | undefined }[];
  active: string | undefined;
}) {
  return (
    <nav aria-label={label} className="flex flex-wrap gap-2">
      {options.map((option) => {
        const current = option.value === active;
        return (
          <Link
            key={option.value ?? "all"}
            href={option.href}
            aria-current={current ? "page" : undefined}
            className={cn(
              "rounded-full border px-3.5 py-1.5 text-sm font-medium transition-colors",
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
