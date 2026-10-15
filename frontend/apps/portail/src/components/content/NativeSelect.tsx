import { ChevronDownIcon } from "lucide-react";
import type { ReactNode } from "react";

import { cn } from "@anaqsup/ui/lib/utils";

/** Liste déroulante native : accessible, utilisable sans JavaScript. */
export function NativeSelect({ id, name, label, defaultValue, className, children }: {
  id: string;
  name: string;
  label: string;
  defaultValue: string;
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
        className="h-12 w-full cursor-pointer appearance-none truncate rounded-full border bg-background pr-11 pl-5 text-sm transition-colors outline-none hover:border-primary focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/40"
      >
        {children}
      </select>
      <ChevronDownIcon className="pointer-events-none absolute top-1/2 right-4 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
    </div>
  );
}
