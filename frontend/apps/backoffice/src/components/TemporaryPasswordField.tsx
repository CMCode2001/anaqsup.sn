import { CopyIcon, RefreshCwIcon } from "lucide-react";
import { toast } from "@anaqsup/ui/components/sonner";

import { Button } from "@anaqsup/ui/components/button";
import { Input } from "@anaqsup/ui/components/input";
import { Label } from "@anaqsup/ui/components/label";

import { MIN_PASSWORD_LENGTH } from "@/components/PasswordForm";
import { generatePassword } from "@/lib/api";

/** Mot de passe provisoire : généré automatiquement, modifiable, à transmettre à la personne. */
export function TemporaryPasswordField({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  const copy = async () => {
    await navigator.clipboard.writeText(value);
    toast.success("Mot de passe provisoire copié.");
  };
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor="temporary-password">Mot de passe provisoire</Label>
      <div className="flex gap-2">
        <Input id="temporary-password" value={value} onChange={(e) => onChange(e.target.value)} className="font-mono" autoComplete="off" spellCheck={false} />
        <Button type="button" variant="outline" size="icon" onClick={() => onChange(generatePassword())} aria-label="Générer un autre mot de passe">
          <RefreshCwIcon aria-hidden="true" />
        </Button>
        <Button type="button" variant="outline" size="icon" onClick={copy} disabled={!value} aria-label="Copier le mot de passe">
          <CopyIcon aria-hidden="true" />
        </Button>
      </div>
      <p className="text-xs text-muted-foreground">
        Au moins {MIN_PASSWORD_LENGTH} caractères. Transmettez-le à la personne : elle devra le changer à sa première connexion.
      </p>
    </div>
  );
}
