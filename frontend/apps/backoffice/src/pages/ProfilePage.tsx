import { toast } from "@anaqsup/ui/components/sonner";

import { Badge } from "@anaqsup/ui/components/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@anaqsup/ui/components/card";

import { PageTitle } from "@/components/PageTitle";
import { PasswordForm } from "@/components/PasswordForm";
import { useMe } from "@/lib/auth";
import { permissionLabel } from "@/lib/permissions";

export function ProfilePage() {
  const me = useMe();
  return (
    <section className="space-y-6">
      <PageTitle title="Mon profil" />
      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="font-heading text-brand-ink">{me.displayName}</CardTitle>
            <CardDescription>{me.email}</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-4 text-sm">
            <div>
              <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">Rôle</p>
              <Badge className="mt-1 bg-primary text-primary-foreground">{me.role.name}</Badge>
            </div>
            <div>
              <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">Rubriques autorisées</p>
              <ul className="mt-2 flex flex-wrap gap-1.5">
                {me.permissions.map((p) => <li key={p}><Badge variant="secondary" className="text-brand-ink">{permissionLabel(p)}</Badge></li>)}
              </ul>
            </div>
          </CardContent>
        </Card>
        <Card id="mot-de-passe">
          <CardHeader>
            <CardTitle className="font-heading text-brand-ink">Changer mon mot de passe</CardTitle>
          </CardHeader>
          <CardContent>
            <PasswordForm onDone={() => toast.success("Mot de passe changé.")} />
          </CardContent>
        </Card>
      </div>
    </section>
  );
}
