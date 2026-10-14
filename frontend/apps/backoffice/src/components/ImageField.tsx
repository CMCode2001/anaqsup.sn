import { ImageUpIcon, Loader2Icon, Trash2Icon } from "lucide-react";
import { useRef, useState } from "react";

import { Button } from "@anaqsup/ui/components/button";
import { toast } from "@anaqsup/ui/components/sonner";

import { previewUrl, uploadImage } from "@/lib/api";

/**
 * Image d'un contenu (photo, logo) : téléversement immédiat, aperçu, retrait.
 * `value` est l'adresse publique (/media/…) ; `onChange` reçoit la clé et l'adresse de la nouvelle image.
 */
export function ImageField({ label, value, onChange, aspect = "aspect-[4/5]", hint, fit = "cover" }: {
  label: string;
  value: string | null | undefined;
  onChange: (image: { key: string; url: string } | null) => void;
  aspect?: string;
  hint?: string;
  /** « contain » : aperçu de l'image entière (organigramme, logo), sans recadrage. */
  fit?: "cover" | "contain";
}) {
  const input = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const preview = previewUrl(value);

  const upload = async (file: File | undefined) => {
    if (!file) return;
    setUploading(true);
    try {
      const uploaded = await uploadImage(file);
      onChange({ key: uploaded.key, url: uploaded.url });
      toast.success("Image téléversée. Pensez à enregistrer.");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Téléversement impossible.");
    } finally {
      setUploading(false);
      if (input.current) input.current.value = "";
    }
  };

  return (
    <div className="flex flex-col gap-3">
      <span className="text-sm font-medium">{label}</span>
      <div className={`relative overflow-hidden rounded-3xl border bg-muted ${aspect}`}>
        {preview ? (
          <img src={preview} alt="" className={`absolute inset-0 size-full ${fit === "contain" ? "object-contain p-2" : "object-cover"}`} />
        ) : (
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 p-4 text-center text-sm text-muted-foreground">
            <ImageUpIcon className="size-8 text-brand-ink" aria-hidden="true" />
            {value ? "Aperçu indisponible ici (image de démonstration)" : "Aucune image"}
          </div>
        )}
      </div>
      <input ref={input} type="file" accept="image/png,image/jpeg,image/webp" className="sr-only" tabIndex={-1} onChange={(e) => void upload(e.target.files?.[0])} />
      <div className="flex flex-wrap gap-2">
        <Button type="button" variant="outline" size="sm" disabled={uploading} onClick={() => input.current?.click()}>
          {uploading ? <Loader2Icon className="animate-spin" aria-hidden="true" /> : <ImageUpIcon data-icon="inline-start" aria-hidden="true" />}
          {value ? "Remplacer" : "Téléverser"}
        </Button>
        {value && (
          <Button type="button" variant="ghost" size="sm" onClick={() => onChange(null)}>
            <Trash2Icon data-icon="inline-start" aria-hidden="true" />Retirer
          </Button>
        )}
      </div>
      <p className="text-xs text-muted-foreground">{hint ?? "PNG, JPEG ou WebP, 5 Mo maximum."}</p>
    </div>
  );
}
