import { createApiClient } from "@anaqsup/api-client";
import { useQuery } from "@tanstack/react-query";
import { ArrowRightIcon, CircleAlertIcon } from "lucide-react";
import { Link } from "react-router";

import { Alert, AlertDescription, AlertTitle } from "@anaqsup/ui/components/alert";
import { Badge } from "@anaqsup/ui/components/badge";
import { Card, CardDescription, CardHeader, CardTitle } from "@anaqsup/ui/components/card";
import { BlurFade } from "@anaqsup/ui/components/magicui/blur-fade";
import { BorderBeam } from "@anaqsup/ui/components/magicui/border-beam";
import { NumberTicker } from "@anaqsup/ui/components/magicui/number-ticker";
import { Skeleton } from "@anaqsup/ui/components/skeleton";

import { BASE_PATH } from "@/lib/api";
import { useCan, useMe } from "@/lib/auth";
import { PERMISSIONS } from "@/lib/permissions";

// Lectures publiques (indicateurs), via le même préfixe d'accès que l'API d'administration.
const publicApi = createApiClient(`${window.location.origin}${BASE_PATH}`);

const KPIS = [
  { key: "habilitatedEstablishments", label: "Établissements — avis favorable" },
  { key: "accreditedPrograms", label: "Formations — avis favorable" },
] as const;

const SECTIONS: Record<string, string> = {
  ACTUALITES: "/actualites", EVENEMENTS: "/evenements", DOCUMENTS: "/ressources", PAGES: "/pages", AVIS: "/avis",
  PARTENAIRES: "/partenaires", PARAMETRES: "/parametres", COMPTES: "/comptes",
};

export function Dashboard() {
  const me = useMe();
  const can = useCan();
  const statistics = useQuery({
    queryKey: ["statistics"],
    queryFn: async () => {
      const { data, error } = await publicApi.GET("/public/statistics");
      if (error) throw new Error(error.title);
      return data;
    },
  });

  return (
    <section className="space-y-8">
      <BlurFade inView>
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="text-2xl font-extrabold text-brand-ink">Bonjour, {me.displayName}</h1>
          <Badge className="bg-primary text-primary-foreground">{me.role.name}</Badge>
        </div>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Bienvenue dans l&apos;espace de gestion du site anaqsup.sn. Les contenus enregistrés ici sont publiés directement.
        </p>
      </BlurFade>

      {statistics.isError && (
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden="true" />
          <AlertTitle>Indicateurs indisponibles</AlertTitle>
          <AlertDescription>L&apos;API n&apos;a pas répondu. Aucun chiffre n&apos;est affiché plutôt qu&apos;un chiffre faux.</AlertDescription>
        </Alert>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        {KPIS.map(({ key, label }, index) => (
          <BlurFade key={key} delay={0.08 * index} inView>
            <Card className="relative overflow-hidden">
              <CardHeader>
                <CardDescription>{label}</CardDescription>
                <CardTitle className="font-heading text-4xl text-brand-ink tabular-nums">
                  {statistics.data ? <NumberTicker value={statistics.data[key]} className="text-brand-ink" /> : <Skeleton className="h-10 w-24" />}
                </CardTitle>
              </CardHeader>
              {index === 0 && statistics.data && <BorderBeam size={120} duration={8} colorFrom="#c7b62e" colorTo="#109939" />}
            </Card>
          </BlurFade>
        ))}
      </div>

      <div>
        <h2 className="text-lg font-bold text-brand-ink">Vos rubriques</h2>
        <ul className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {PERMISSIONS.filter((p) => can(p.value)).map((permission, index) => (
            <li key={permission.value}>
              <BlurFade delay={0.05 * index} inView>
                <Link to={SECTIONS[permission.value] ?? "/"} className="group block h-full rounded-3xl focus-visible:outline-none">
                  <Card className="h-full transition-all duration-300 group-hover:scale-[1.03] group-hover:shadow-xl group-focus-visible:ring-2 group-focus-visible:ring-ring">
                    <CardHeader>
                      <CardTitle className="flex items-center justify-between gap-2 font-heading text-brand-ink">
                        {permission.label}
                        <ArrowRightIcon className="size-4 text-primary transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
                      </CardTitle>
                      <CardDescription>{permission.description}</CardDescription>
                    </CardHeader>
                  </Card>
                </Link>
              </BlurFade>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
