import { Loader2Icon, RotateCwIcon } from "lucide-react";

import { BrandLogo } from "@anaqsup/ui/components/brand-logo";
import { Button } from "@anaqsup/ui/components/button";

/** Chargement initial (vérification de la session) ou API injoignable. */
export function SplashScreen({ error = false }: { error?: boolean }) {
  return (
    <main className="grid min-h-dvh place-items-center bg-background p-6">
      <div className="flex flex-col items-center gap-6 text-center">
        <BrandLogo height={56} src="brand/logo-anaqsup.png" />
        {error ? (
          <>
            <p className="max-w-sm text-sm text-muted-foreground">Le service ne répond pas pour le moment. Réessayez dans quelques instants.</p>
            <Button onClick={() => window.location.reload()}><RotateCwIcon data-icon="inline-start" aria-hidden="true" />Réessayer</Button>
          </>
        ) : (
          <Loader2Icon className="size-6 animate-spin text-brand-ink" aria-label="Chargement" />
        )}
      </div>
    </main>
  );
}
