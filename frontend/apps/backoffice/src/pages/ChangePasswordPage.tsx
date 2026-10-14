import type { AdminMe } from "@anaqsup/api-client";
import { KeyRoundIcon } from "lucide-react";

import { Button } from "@anaqsup/ui/components/button";

import { AuthShell } from "@/components/AuthShell";
import { PasswordForm } from "@/components/PasswordForm";
import { useLogout } from "@/lib/auth";

/** Première connexion (ou mot de passe réinitialisé) : le mot de passe provisoire doit être remplacé. */
export function ChangePasswordPage({ me }: { forced: true; me: AdminMe }) {
  const logout = useLogout();
  return (
    <AuthShell>
      <span className="mt-10 flex size-12 items-center self-center justify-center rounded-full bg-primary text-primary-foreground">
        <KeyRoundIcon className="size-5" aria-hidden="true" />
      </span>
      <h1 className="mt-4 text-center text-2xl font-extrabold text-brand-ink">Choisissez votre mot de passe</h1>
      <p className="mt-2 text-center text-sm text-muted-foreground">
        Bonjour {me.displayName}. Vous utilisez un mot de passe provisoire : remplacez-le pour accéder à l&apos;espace de gestion.
      </p>
      <div className="mt-8">
        <PasswordForm currentLabel="Mot de passe provisoire" />
      </div>
      <Button variant="link" className="mt-6 self-center px-0" onClick={() => logout.mutate()}>Se déconnecter</Button>
    </AuthShell>
  );
}
