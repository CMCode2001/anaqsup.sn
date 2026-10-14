import type { ReactNode } from "react";

import { BrandLogo } from "@anaqsup/ui/components/brand-logo";

// Visuels décoratifs (baobab aux couleurs du drapeau, 10 % d'opacité) : chemins relatifs à cause de <base href>.
const decor = "pointer-events-none absolute h-[clamp(14rem,45vh,28rem)] w-auto select-none";

/** Mise en page des écrans de connexion : fond blanc, baobab dans deux coins, formulaire centré. */
export function AuthShell({ children }: { children: ReactNode }) {
  return (
    <main className="relative flex min-h-dvh items-center justify-center overflow-hidden bg-white px-6 py-12">
      <img src="brand/fondTopLeft.svg" alt="" aria-hidden="true" width={158} height={371} className={`${decor} top-0 left-0`} />
      <img src="brand/fondBottomRight.svg" alt="" aria-hidden="true" width={220} height={371} className={`${decor} right-0 bottom-0`} />

      <div className="relative z-10 flex w-full max-w-sm flex-col">
        {/* Charte : logotype officiel, sur fond blanc, sans modification. */}
        <BrandLogo height={64} src="brand/logo-anaqsup.png" className="self-center" />
        {children}
      </div>
    </main>
  );
}
