import type { InstitutionalPage } from "@anaqsup/api-client";
import { ArrowRightIcon, UserRoundIcon } from "lucide-react";

import { CoverImage } from "@/components/content/CoverImage";
import { PersonCaption } from "@/components/content/PersonCaption";
import { Link } from "@/i18n/navigation";
import { getLocale, getTranslations } from "@/i18n/server";
import { langAttr } from "@/lib/lang";

/** Arc décoratif (couleurs du logotype), comme sur le modèle validé par l'ANAQ-Sup. */
function Arc({ className, color }: { className: string; color: string }) {
  return (
    <svg aria-hidden="true" viewBox="0 0 120 120" fill="none" className={className}>
      <path d="M12 12a96 96 0 0 1 96 96" stroke={color} strokeWidth="22" strokeLinecap="round" />
    </svg>
  );
}

/**
 * Le mot du Secrétaire exécutif, mis en avant sur l'accueil : photo, nom et fonction à gauche ; texte d'accroche
 * et « Lire la suite » à droite. Photo, légende et textes se modifient dans le back-office (Pages institutionnelles).
 */
export async function SecretaryMessage({ message }: { message: InstitutionalPage }) {
  const locale = await getLocale();
  const t = await getTranslations();
  const textLang = langAttr(message.lang, locale);

  return (
    <section aria-labelledby="mot" className="relative isolate overflow-hidden">
      <Arc className="pointer-events-none absolute top-10 right-[4%] -z-10 hidden size-40 lg:block" color="var(--primary)" />
      <Arc className="pointer-events-none absolute bottom-10 left-[3%] -z-10 hidden size-36 rotate-180 lg:block" color="var(--anthracite)" />

      <div className="mx-auto grid max-w-7xl items-center gap-10 px-4 py-16 md:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] md:gap-14 md:py-24">
        <figure className="mx-auto w-full max-w-sm overflow-hidden rounded-3xl bg-card shadow-xl ring-1 ring-foreground/5 transition-transform duration-500 hover:scale-[1.02]">
          <div className="relative aspect-[4/5] bg-muted">
            {message.imageUrl ? (
              <CoverImage src={message.imageUrl} sizes="(min-width: 768px) 384px, 90vw" />
            ) : (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 text-brand-ink">
                <span className="flex size-24 items-center justify-center rounded-full bg-background shadow-sm">
                  <UserRoundIcon className="size-12" aria-hidden="true" />
                </span>
                <span className="px-6 text-center text-xs text-muted-foreground">{t("Home.messagePhoto")}</span>
              </div>
            )}
          </div>
          <PersonCaption name={message.imageName} role={message.imageRole} />
        </figure>

        <div>
          <h2 id="mot" className="text-4xl leading-[1.05] font-extrabold tracking-tight text-foreground md:text-6xl">
            {t("Home.messageTitleStart")}
            <span className="block text-brand-ink">{t("Home.messageTitleEnd")}</span>
          </h2>
          {message.summary && (
            <p lang={textLang} className="mt-8 text-lg leading-relaxed text-muted-foreground">{message.summary}</p>
          )}
          <Link
            href="/anaq-sup/mot-du-secretaire-executif"
            className="group mt-8 inline-flex h-12 items-center gap-2 rounded-full bg-primary/15 px-6 text-sm font-semibold text-brand-ink transition-colors hover:bg-primary hover:text-primary-foreground"
          >
            {t("Common.readMore")}
            <ArrowRightIcon className="size-4 transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
          </Link>
        </div>
      </div>
    </section>
  );
}
