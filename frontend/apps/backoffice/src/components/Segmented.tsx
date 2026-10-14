import { cn } from "@anaqsup/ui/lib/utils";

/** Onglets en pastilles (langue d'un contenu, établissements / formations…). */
export function Segmented<T extends string>({ label, value, options, onChange }: {
  label: string;
  value: T;
  options: { value: T; label: string }[];
  onChange: (value: T) => void;
}) {
  return (
    <div role="tablist" aria-label={label} className="inline-flex w-fit rounded-full border bg-muted p-1">
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          role="tab"
          aria-selected={value === option.value}
          onClick={() => onChange(option.value)}
          className={cn(
            "rounded-full px-5 py-1.5 text-sm font-semibold transition-colors",
            value === option.value ? "bg-primary text-primary-foreground shadow-sm" : "text-muted-foreground hover:text-foreground",
          )}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}

export const LANGUAGES = [{ value: "fr" as const, label: "Français" }, { value: "en" as const, label: "English" }];
