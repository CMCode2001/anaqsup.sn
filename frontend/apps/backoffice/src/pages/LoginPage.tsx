import { useMutation, useQueryClient } from "@tanstack/react-query";
import { CircleAlertIcon, Loader2Icon, LogInIcon } from "lucide-react";
import { type FormEvent, useState } from "react";

import { Alert, AlertDescription } from "@anaqsup/ui/components/alert";
import { Button } from "@anaqsup/ui/components/button";
import { Input } from "@anaqsup/ui/components/input";
import { Label } from "@anaqsup/ui/components/label";

import { AuthShell } from "@/components/AuthShell";
import { PasswordInput } from "@/components/PasswordInput";
import { api, unwrap } from "@/lib/api";
import { meQueryKey } from "@/lib/auth";

export function LoginPage() {
  const queryClient = useQueryClient();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const login = useMutation({
    mutationFn: () => unwrap(api.POST("/admin/auth/login", { body: { email, password } })),
    onSuccess: (me) => queryClient.setQueryData(meQueryKey, me),
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    login.mutate();
  };

  return (
    <AuthShell>
      <h1 className="mt-10 text-center text-2xl font-extrabold text-brand-ink">Espace de gestion</h1>
      <p className="mt-2 text-center text-sm text-muted-foreground">Connectez-vous !</p>

      <form onSubmit={submit} className="mt-8 flex flex-col gap-5" noValidate>
        {login.isError && (
          <Alert variant="destructive" aria-live="assertive">
            <CircleAlertIcon aria-hidden="true" />
            <AlertDescription>{login.error.message}</AlertDescription>
          </Alert>
        )}
        <div className="flex flex-col gap-2">
          <Label htmlFor="email">Adresse e-mail</Label>
          <Input id="email" type="email" autoComplete="username" required autoFocus value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor="password">Mot de passe</Label>
          <PasswordInput id="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        <Button type="submit" size="lg" disabled={login.isPending || !email || !password} className="mt-2 w-full">
          {login.isPending ? <Loader2Icon className="animate-spin" aria-hidden="true" /> : <LogInIcon data-icon="inline-start" aria-hidden="true" />}
          Se connecter
        </Button>
      </form>

      <p className="mt-8 text-center text-xs leading-relaxed text-muted-foreground">
        Mot de passe oublié ? <br/>Demandez à l&apos;administrateur un nouveau mot de passe provisoire.
      </p>
    </AuthShell>
  );
}
