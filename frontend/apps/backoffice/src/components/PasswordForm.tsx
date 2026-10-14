import { useMutation, useQueryClient } from "@tanstack/react-query";
import { CircleAlertIcon, Loader2Icon } from "lucide-react";
import { type FormEvent, useState } from "react";

import { Alert, AlertDescription } from "@anaqsup/ui/components/alert";
import { Button } from "@anaqsup/ui/components/button";
import { Label } from "@anaqsup/ui/components/label";

import { PasswordInput } from "@/components/PasswordInput";
import { api, unwrap } from "@/lib/api";
import { meQueryKey } from "@/lib/auth";

export const MIN_PASSWORD_LENGTH = 8;

/** Changement de son propre mot de passe (mot de passe actuel, nouveau, confirmation). */
export function PasswordForm({ currentLabel = "Mot de passe actuel", onDone }: { currentLabel?: string; onDone?: () => void }) {
  const queryClient = useQueryClient();
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const change = useMutation({
    mutationFn: () => unwrap(api.POST("/admin/auth/password", { body: { currentPassword: current, newPassword: next } })),
    onSuccess: async () => {
      setCurrent(""); setNext(""); setConfirm("");
      await queryClient.invalidateQueries({ queryKey: meQueryKey });
      onDone?.();
    },
  });

  const tooShort = next.length > 0 && next.length < MIN_PASSWORD_LENGTH;
  const mismatch = confirm.length > 0 && confirm !== next;
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!tooShort && !mismatch) change.mutate();
  };

  return (
    <form onSubmit={submit} className="flex flex-col gap-5" noValidate>
      {change.isError && (
        <Alert variant="destructive" aria-live="assertive">
          <CircleAlertIcon aria-hidden="true" />
          <AlertDescription>{change.error.message}</AlertDescription>
        </Alert>
      )}
      <div className="flex flex-col gap-2">
        <Label htmlFor="current">{currentLabel}</Label>
        <PasswordInput id="current" autoComplete="current-password" required value={current} onChange={(e) => setCurrent(e.target.value)} />
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor="next">Nouveau mot de passe</Label>
        <PasswordInput id="next" autoComplete="new-password" required aria-invalid={tooShort} aria-describedby="next-help" value={next} onChange={(e) => setNext(e.target.value)} />
        <p id="next-help" className={tooShort ? "text-xs text-destructive" : "text-xs text-muted-foreground"}>
          Au moins {MIN_PASSWORD_LENGTH} caractères.
        </p>
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor="confirm">Confirmer le nouveau mot de passe</Label>
        <PasswordInput id="confirm" autoComplete="new-password" required aria-invalid={mismatch} value={confirm} onChange={(e) => setConfirm(e.target.value)} />
        {mismatch && <p className="text-xs text-destructive">Les deux mots de passe ne correspondent pas.</p>}
      </div>
      <Button type="submit" size="lg" disabled={change.isPending || !current || next.length < MIN_PASSWORD_LENGTH || confirm !== next} className="mt-2">
        {change.isPending && <Loader2Icon className="animate-spin" aria-hidden="true" />}
        Enregistrer le mot de passe
      </Button>
    </form>
  );
}
