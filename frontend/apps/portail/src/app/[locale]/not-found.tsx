import { getTranslations } from "@/i18n/server";

import { SearchForm } from "@/components/registre/SearchForm";
import { Link } from "@/i18n/navigation";

export default async function NotFound() {
  const t = await getTranslations("NotFound");
  return (
    <div className="mx-auto max-w-4xl px-4 py-16">
      <h1 className="font-heading text-3xl font-extrabold text-brand-ink">{t("title")}</h1>
      <p className="mt-3 text-foreground">{t("text")}</p>
      <div className="mt-6">
        <SearchForm scope="site" />
      </div>
      <p className="mt-6 text-sm">
        <Link href="/" className="underline underline-offset-2">{t("home")}</Link>
      </p>
    </div>
  );
}
