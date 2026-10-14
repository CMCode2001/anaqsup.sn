import type { EstablishmentPage, ProgramPage } from "@anaqsup/api-client";
import { Building2Icon, GraduationCapIcon, MapPinIcon } from "lucide-react";
import { getLocale, getTranslations } from "@/i18n/server";

import { AvisBadge } from "@anaqsup/ui/components/avis-badge";
import { Card, CardContent } from "@anaqsup/ui/components/card";

import { Link } from "@/i18n/navigation";

type EstablishmentSummary = EstablishmentPage["content"][number];
type ProgramSummary = ProgramPage["content"][number];

const ZOOM = "h-full transition-all duration-300 ease-out group-hover:scale-[1.03] group-hover:shadow-xl";

// Les noms d'établissements et de formations sont des noms propres : ils ne se traduisent pas.
export async function EstablishmentCard({ establishment }: { establishment: EstablishmentSummary }) {
  const locale = await getLocale();
  const t = await getTranslations("Institutions");
  return (
    <Link href={{ pathname: "/etablissements/[slug]", params: { slug: establishment.slug } }} className="group block h-full rounded-3xl focus-visible:outline-none">
      <Card className={ZOOM}>
        <CardContent className="flex h-full flex-col gap-4">
          <div className="flex items-start gap-3">
            <span className="grid size-11 shrink-0 place-items-center rounded-full bg-muted text-brand-ink">
              <Building2Icon className="size-5" aria-hidden="true" />
            </span>
            <div>
              <p lang="fr" className="font-heading font-bold leading-snug text-brand-ink">
                {establishment.name}{establishment.acronym ? ` (${establishment.acronym})` : ""}
              </p>
              <p className="mt-1 flex flex-wrap items-center gap-x-2 text-sm text-muted-foreground">
                {establishment.kind === "PUBLIC" ? t("public") : t("private")}
                {establishment.city && <span className="inline-flex items-center gap-1"><MapPinIcon className="size-3.5" aria-hidden="true" />{establishment.city}</span>}
              </p>
            </div>
          </div>
          <div className="mt-auto"><AvisBadge avis={establishment.habilitationAvis} year={establishment.habilitationAvisYear} locale={locale} /></div>
        </CardContent>
      </Card>
    </Link>
  );
}

export async function ProgramCard({ program }: { program: ProgramSummary }) {
  const locale = await getLocale();
  const t = await getTranslations("Level");
  return (
    <Link href={{ pathname: "/formations/[slug]", params: { slug: program.slug } }} className="group block h-full rounded-3xl focus-visible:outline-none">
      <Card className={ZOOM}>
        <CardContent className="flex h-full flex-col gap-4">
          <div className="flex items-start gap-3">
            <span className="grid size-11 shrink-0 place-items-center rounded-full bg-muted text-brand-ink">
              <GraduationCapIcon className="size-5" aria-hidden="true" />
            </span>
            <div>
              <p lang="fr" className="font-heading font-bold leading-snug text-brand-ink">{program.title}</p>
              <p className="mt-1 text-sm text-muted-foreground">{t(program.level)} · <span lang="fr">{program.establishment.name}</span></p>
            </div>
          </div>
          <div className="mt-auto"><AvisBadge avis={program.accreditationAvis} year={program.accreditationAvisYear} locale={locale} /></div>
        </CardContent>
      </Card>
    </Link>
  );
}
