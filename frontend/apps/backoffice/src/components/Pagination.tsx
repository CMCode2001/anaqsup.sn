import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react";

import { Button } from "@anaqsup/ui/components/button";

/** « Page 2 sur 9 · 172 actualités », avec Précédente / Suivante ; rien s'il n'y a qu'une page. */
export function Pagination({ page, totalPages, total, noun, onChange }: {
  page: number;
  totalPages: number;
  total: number | undefined;
  noun: string;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <div className="flex flex-wrap items-center justify-between gap-4 text-sm text-muted-foreground">
      <span>Page {page + 1} sur {totalPages} · {total} {noun}</span>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" disabled={page === 0} onClick={() => onChange(page - 1)}>
          <ChevronLeftIcon data-icon="inline-start" aria-hidden="true" />Précédente
        </Button>
        <Button variant="outline" size="sm" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
          Suivante<ChevronRightIcon data-icon="inline-end" aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}
