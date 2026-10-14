import type { Metadata } from "next";
import Link from "next/link";

import "@anaqsup/ui/globals.css";

export const metadata: Metadata = { title: "Page introuvable · Page not found", robots: { index: false } };

/**
 * 404 des adresses qui n'atteignent aucune langue (fichier inexistant, ex. /ancien.pdf).
 * Les autres adresses inconnues affichent la 404 traduite de app/[locale]/not-found.tsx.
 */
export default function GlobalNotFound() {
  return (
    <html lang="fr">
      <body className="grid min-h-dvh place-items-center bg-background p-6 font-sans">
        <main className="max-w-md text-center">
          <h1 className="text-3xl font-extrabold text-brand-ink">Page introuvable</h1>
          <p lang="en" className="mt-1 text-lg text-muted-foreground">Page not found</p>
          <p className="mt-6 flex justify-center gap-3">
            <Link href="/" className="rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground">Accueil</Link>
            <Link href="/en" lang="en" className="rounded-full border px-5 py-2.5 text-sm font-semibold">Home</Link>
          </p>
        </main>
      </body>
    </html>
  );
}
