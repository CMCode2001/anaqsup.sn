import { useQuery } from "@tanstack/react-query";
import { ArrowRightIcon, ImageIcon, LanguagesIcon } from "lucide-react";
import { Link } from "react-router";

import { Badge } from "@anaqsup/ui/components/badge";
import { Card, CardDescription, CardHeader, CardTitle } from "@anaqsup/ui/components/card";
import { Skeleton } from "@anaqsup/ui/components/skeleton";

import { PageTitle } from "@/components/PageTitle";
import { api, dateTime, unwrap } from "@/lib/api";

/** Ordre et rôle des pages, tels qu'on les voit sur le site. */
export const PAGE_INFO: Record<string, { label: string; where: string }> = {
  "mot-du-secretaire-executif": { label: "Le mot du Secrétaire exécutif", where: "Accueil (photo, nom et fonction, accroche) et page complète" },
  presentation: { label: "Présentation", where: "L'ANAQ-Sup → Présentation" },
  missions: { label: "Missions", where: "L'ANAQ-Sup → Missions" },
  gouvernance: { label: "Gouvernance", where: "L'ANAQ-Sup → Gouvernance" },
  organigramme: { label: "Organigramme", where: "L'ANAQ-Sup → Organigramme" },
  "procedure-habilitation": { label: "Procédure d'habilitation", where: "Habilitations & accréditations → fiche pratique « Habilitation d'un établissement »" },
  "procedure-accreditation": { label: "Procédure d'accréditation", where: "Habilitations & accréditations → fiche pratique « Accréditation d'un programme »" },
};
const ORDER = Object.keys(PAGE_INFO);

export function PagesPage() {
  const pages = useQuery({ queryKey: ["pages"], queryFn: () => unwrap(api.GET("/admin/pages")) });
  const sorted = [...(pages.data ?? [])].sort((a, b) => ORDER.indexOf(a.slug) - ORDER.indexOf(b.slug));

  return (
    <section className="space-y-6">
      <PageTitle title="Pages institutionnelles" description="Textes de la rubrique « L'ANAQ-Sup », le mot du Secrétaire exécutif et les fiches procédures. Publication directe : vos modifications sont en ligne dès l'enregistrement." />
      <ul className="grid gap-4 md:grid-cols-2">
        {pages.isPending && Array.from({ length: 4 }, (_, i) => <li key={i}><Skeleton className="h-32 rounded-3xl" /></li>)}
        {sorted.map((page) => (
          <li key={page.slug}>
            <Link to={`/pages/${page.slug}`} className="group block h-full rounded-3xl focus-visible:outline-none">
              <Card className="h-full transition-all duration-300 group-hover:scale-[1.02] group-hover:shadow-xl group-focus-visible:ring-2 group-focus-visible:ring-ring">
                <CardHeader>
                  <CardTitle className="flex items-center justify-between gap-2 font-heading text-brand-ink">
                    {PAGE_INFO[page.slug]?.label ?? page.title}
                    <ArrowRightIcon className="size-4 text-primary transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
                  </CardTitle>
                  <CardDescription>{PAGE_INFO[page.slug]?.where}</CardDescription>
                  <div className="mt-3 flex flex-wrap gap-1.5">
                    {page.hasEnglish
                      ? <Badge variant="secondary" className="gap-1 text-brand-ink"><LanguagesIcon data-icon="inline-start" aria-hidden="true" />Traduite</Badge>
                      : <Badge variant="outline" className="gap-1"><LanguagesIcon data-icon="inline-start" aria-hidden="true" />Français seulement</Badge>}
                    {page.hasImage && <Badge variant="secondary" className="gap-1 text-brand-ink"><ImageIcon data-icon="inline-start" aria-hidden="true" />Photo</Badge>}
                    <span className="text-xs text-muted-foreground">Modifiée le {dateTime.format(new Date(page.updatedAt))}</span>
                  </div>
                </CardHeader>
              </Card>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
