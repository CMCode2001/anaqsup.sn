import { SearchIcon } from "lucide-react";
import { getLocale, getTranslations } from "@/i18n/server";

import { Button } from "@anaqsup/ui/components/button";
import { Input } from "@anaqsup/ui/components/input";
import { ShimmerButton } from "@anaqsup/ui/components/magicui/shimmer-button";
import { cn } from "@anaqsup/ui/lib/utils";

import { getPathname } from "@/i18n/navigation";

/**
 * Recherche en GET pur : fonctionne sans JavaScript, partageable (l'URL porte la requête),
 * compatible avec le bouton « retour ». Variante `hero` : bouton Magic UI (animation 100 % CSS).
 */
export async function SearchForm({ defaultValue = "", variant = "default" }: { defaultValue?: string; variant?: "hero" | "default" }) {
  const locale = await getLocale();
  const t = await getTranslations("Search");
  const hero = variant === "hero";
  return (
    <form action={getPathname({ locale, href: "/recherche" })} method="get" role="search" className="flex w-full flex-col gap-3 sm:flex-row">
      {/* <label> natif : le Label shadcn (Radix, "use client") ajouterait du JS pour rien. */}
      <label htmlFor="q" className="sr-only">{t("label")}</label>
      <div className="relative w-full">
        <SearchIcon className="pointer-events-none absolute top-1/2 left-3.5 size-5 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
        <Input
          id="q"
          name="q"
          type="search"
          required
          minLength={2}
          maxLength={100}
          defaultValue={defaultValue}
          placeholder={t("placeholder")}
          autoComplete="off"
          className={cn("bg-background pl-11", hero ? "h-14 text-lg shadow-sm" : "h-12 text-base")}
        />
      </div>
      {hero ? (
        <ShimmerButton type="submit" background="var(--primary)" shimmerColor="#ffffff" borderRadius="9999px" className="h-14 px-8 font-semibold text-primary-foreground">
          {t("verify")}
        </ShimmerButton>
      ) : (
        <Button type="submit" size="lg">{t("submit")}</Button>
      )}
    </form>
  );
}
