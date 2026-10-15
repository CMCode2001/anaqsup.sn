"use client";

import { CheckIcon, PrinterIcon, Share2Icon } from "lucide-react";
import { useEffect, useState } from "react";

const SCALES = [0.9, 1, 1.12, 1.25] as const;

const button =
  "inline-flex size-9 items-center justify-center rounded-control border bg-background text-foreground shadow-sm transition-colors hover:border-primary hover:bg-muted disabled:pointer-events-none disabled:opacity-40 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary sm:size-11";

/**
 * Outils de lecture d'un article : taille du texte (A− / A+), impression, partage (menu de partage du
 * téléphone, sinon copie du lien). Libellés déjà traduits : aucun moteur de traduction côté navigateur.
 */
export function ArticleToolbar({ targetId, title, labels }: {
  targetId: string;
  title: string;
  labels: { tools: string; smaller: string; larger: string; print: string; share: string; copied: string };
}) {
  const [scale, setScale] = useState(1);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    document.getElementById(targetId)?.style.setProperty("--article-scale", String(SCALES[scale]));
  }, [scale, targetId]);

  const share = async () => {
    const url = window.location.href;
    if (navigator.share) {
      await navigator.share({ title, url }).catch(() => undefined); // partage annulé : rien à faire
      return;
    }
    await navigator.clipboard.writeText(url);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 2500);
  };

  return (
    <div role="toolbar" aria-label={labels.tools} className="flex items-center gap-1.5 sm:gap-2 print:hidden">
      <button type="button" className={button} onClick={() => setScale((s) => s - 1)} disabled={scale === 0} aria-label={labels.smaller} title={labels.smaller}>
        <span className="text-xs font-bold sm:text-sm" aria-hidden="true">A−</span>
      </button>
      <button type="button" className={button} onClick={() => setScale((s) => s + 1)} disabled={scale === SCALES.length - 1} aria-label={labels.larger} title={labels.larger}>
        <span className="text-xs font-bold sm:text-sm" aria-hidden="true">A+</span>
      </button>
      <button type="button" className={button} onClick={() => window.print()} aria-label={labels.print} title={labels.print}>
        <PrinterIcon className="size-4 sm:size-5" aria-hidden="true" />
      </button>
      <button type="button" className={button} onClick={() => void share()} aria-label={labels.share} title={labels.share}>
        {copied ? <CheckIcon className="size-4 text-brand-green sm:size-5" aria-hidden="true" /> : <Share2Icon className="size-4 sm:size-5" aria-hidden="true" />}
      </button>
      <span role="status" className="sr-only">{copied ? labels.copied : ""}</span>
    </div>
  );
}
