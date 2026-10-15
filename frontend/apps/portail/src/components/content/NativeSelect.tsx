import { ChevronDownIcon } from "lucide-react";
import type { ReactNode } from "react";

import { cn } from "@anaqsup/ui/lib/utils";

/** Liste déroulante native : accessible, utilisable sans JavaScript. `pill` : forme pilule (page de recherche). */
export function NativeSelect({ id, name, label, defaultValue, pill = false, className, children }: {
  id: string;
  name: string;
  label: string;
  defaultValue: string;
  pill?: boolean;
  className?: string;
  children: ReactNode;
}) {
  return (
    <div className={cn("relative", className)}>
      <label htmlFor={id} className="sr-only">{label}</label>
      <select
        id={id}
        name={name}
        defaultValue={defaultValue}
        className={cn(
          "h-12 w-full cursor-pointer appearance-none truncate border bg-background pr-11 pl-5 text-sm transition-colors outline-none hover:border-primary focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/40",
          pill ? "rounded-full" : "rounded-control",
        )}
      >
        {children}
      </select>
      <ChevronDownIcon className="pointer-events-none absolute top-1/2 right-4 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
    </div>
  );
}
