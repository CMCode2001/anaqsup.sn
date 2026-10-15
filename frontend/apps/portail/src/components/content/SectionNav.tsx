import { getTranslations } from "@/i18n/server";

import { cn } from "@anaqsup/ui/lib/utils";

import { Link } from "@/i18n/navigation";
import type { StaticPathname } from "@/i18n/routing";
import { NAV } from "@/lib/navigation";

/**
 * Sous-navigation d'une rubrique (onglets en pilules, sans JavaScript) : reprend exactement les
 * sous-éléments du menu, pour passer d'une page à l'autre de la rubrique sans remonter au menu.
 */
export async function SectionNav({ group, current }: { group: "about" | "news" | "resources"; current?: StaticPathname }) {
  const t = await getTranslations("Nav");
  const items = NAV.find((entry) => entry.label === group)?.children ?? [];
  return (
    <nav aria-label={t(group)} className="-mx-4 mt-6 flex gap-2 overflow-x-auto px-4 pb-1 [scrollbar-width:none] sm:mx-0 sm:flex-wrap sm:overflow-visible sm:px-0 sm:pb-0 [&::-webkit-scrollbar]:hidden">
      {items.map((item) => (
        <Link
          key={item.href}
          href={item.href}
          aria-current={item.href === current ? "page" : undefined}
          className={cn(
            "shrink-0 rounded-full border px-4 py-2 text-sm font-semibold whitespace-nowrap transition-colors",
            item.href === current ? "border-primary bg-primary text-primary-foreground" : "bg-background hover:bg-muted",
          )}
        >
          {t(item.label)}
        </Link>
      ))}
    </nav>
  );
}
