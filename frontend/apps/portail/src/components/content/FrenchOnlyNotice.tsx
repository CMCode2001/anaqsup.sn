import type { Lang } from "@anaqsup/api-client";
import { LanguagesIcon } from "lucide-react";
import { getLocale, getTranslations } from "@/i18n/server";

import { langAttr } from "@/lib/lang";

/** Sur le site anglais, prévient que le contenu affiché n'a pas encore de version anglaise. */
export async function FrenchOnlyNotice({ lang, className }: { lang: Lang | undefined; className?: string }) {
  const locale = await getLocale();
  const t = await getTranslations("Common");
  if (!langAttr(lang, locale)) return null;
  return (
    <p role="note" className={className}>
      <span className="inline-flex items-center gap-2 rounded-full border border-primary/40 bg-muted px-4 py-2 text-sm font-medium text-brand-ink">
        <LanguagesIcon className="size-4" aria-hidden="true" />
        {t("frenchOnly")}
      </span>
    </p>
  );
}
