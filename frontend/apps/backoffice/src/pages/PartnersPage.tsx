import type { AdminPartner } from "@anaqsup/api-client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowDownIcon, ArrowUpIcon, ExternalLinkIcon, Loader2Icon, PencilIcon, PlusIcon, Trash2Icon } from "lucide-react";
import { type FormEvent, useState } from "react";

import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter,
  AlertDialogHeader, AlertDialogTitle,
} from "@anaqsup/ui/components/alert-dialog";
import { Badge } from "@anaqsup/ui/components/badge";
import { Button } from "@anaqsup/ui/components/button";
import { Card } from "@anaqsup/ui/components/card";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@anaqsup/ui/components/dialog";
import { Input } from "@anaqsup/ui/components/input";
import { Label } from "@anaqsup/ui/components/label";
import { Skeleton } from "@anaqsup/ui/components/skeleton";
import { toast } from "@anaqsup/ui/components/sonner";
import { Switch } from "@anaqsup/ui/components/switch";

import { ImageField } from "@/components/ImageField";
import { PageTitle } from "@/components/PageTitle";
import { api, previewUrl, unwrap } from "@/lib/api";

const partnersKey = ["partners"] as const;

/** Ajout ou modification : nom, site officiel, logo, affichage. */
function PartnerDialog({ partner, onClose }: { partner?: AdminPartner; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState(partner?.name ?? "");
  const [url, setUrl] = useState(partner?.url ?? "https://");
  const [logo, setLogo] = useState<{ key: string | null; url: string | null }>({ key: partner?.logoKey ?? null, url: partner?.logoUrl ?? null });
  const [enabled, setEnabled] = useState(partner?.enabled ?? true);
  const save = useMutation({
    mutationFn: () => {
      const body = { name, url: url === "https://" || !url.trim() ? null : url.trim(), logoKey: logo.key, enabled };
      return partner
        ? unwrap(api.PUT("/admin/partners/{id}", { params: { path: { id: partner.id } }, body }))
        : unwrap(api.POST("/admin/partners", { body }));
    },
    onSuccess: (saved) => {
      toast.success(partner ? `« ${saved.name} » modifié.` : `« ${saved.name} » ajouté au bandeau.`);
      void queryClient.invalidateQueries({ queryKey: partnersKey });
      onClose();
    },
    onError: (error) => toast.error(error.message),
  });
  const submit = (event: FormEvent) => { event.preventDefault(); save.mutate(); };

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <form onSubmit={submit} className="flex flex-col gap-5">
          <DialogHeader>
            <DialogTitle>{partner ? "Modifier le partenaire" : "Ajouter un partenaire"}</DialogTitle>
            <DialogDescription>Sans logo, le bandeau affiche le nom du partenaire.</DialogDescription>
          </DialogHeader>
          <div className="flex flex-col gap-2">
            <Label htmlFor="partner-name">Nom</Label>
            <Input id="partner-name" required maxLength={120} value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="partner-url">Site officiel</Label>
            <Input id="partner-url" type="url" value={url} onChange={(e) => setUrl(e.target.value)} />
          </div>
          <ImageField label="Logo" value={logo.url} aspect="aspect-[5/2]" hint="Logo officiel fourni par le partenaire, fond transparent de préférence (PNG ou WebP)." onChange={(image) => setLogo({ key: image?.key ?? null, url: image?.url ?? null })} />
          <div className="flex items-center justify-between gap-4 rounded-2xl border p-4">
            <div>
              <Label htmlFor="partner-enabled">Affiché sur l&apos;accueil</Label>
              <p className="text-xs text-muted-foreground">Masquez-le sans le supprimer, si besoin.</p>
            </div>
            <Switch id="partner-enabled" checked={enabled} onCheckedChange={setEnabled} />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Annuler</Button>
            <Button type="submit" disabled={save.isPending || !name.trim()}>
              {save.isPending && <Loader2Icon className="animate-spin" aria-hidden="true" />}{partner ? "Enregistrer" : "Ajouter"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

type Editing = { kind: "create" } | { kind: "edit" | "delete"; partner: AdminPartner } | null;

export function PartnersPage() {
  const queryClient = useQueryClient();
  const partners = useQuery({ queryKey: partnersKey, queryFn: () => unwrap(api.GET("/admin/partners")) });
  const [editing, setEditing] = useState<Editing>(null);

  const reorder = useMutation({
    mutationFn: (ids: string[]) => unwrap(api.PUT("/admin/partners/order", { body: { ids } })),
    onSuccess: (list) => queryClient.setQueryData(partnersKey, list),
    onError: (error) => toast.error(error.message),
  });
  const remove = useMutation({
    mutationFn: (id: string) => unwrap(api.DELETE("/admin/partners/{id}", { params: { path: { id } } })),
    onSuccess: () => { toast.success("Partenaire retiré."); void queryClient.invalidateQueries({ queryKey: partnersKey }); setEditing(null); },
    onError: (error) => toast.error(error.message),
  });

  const move = (index: number, offset: -1 | 1) => {
    const ids = (partners.data ?? []).map((p) => p.id);
    const target = index + offset;
    [ids[index], ids[target]] = [ids[target]!, ids[index]!];
    reorder.mutate(ids);
  };

  return (
    <section className="space-y-6">
      <PageTitle
        title="Partenaires"
        description="Le bandeau qui défile en bas de l'accueil, dans l'ordre ci-dessous."
        actions={<Button onClick={() => setEditing({ kind: "create" })}><PlusIcon data-icon="inline-start" aria-hidden="true" />Ajouter un partenaire</Button>}
      />
      <Card className="divide-y py-0">
        {partners.isPending && Array.from({ length: 4 }, (_, i) => <div key={i} className="p-4"><Skeleton className="h-14" /></div>)}
        {partners.data?.map((partner, index) => (
          <div key={partner.id} className="flex flex-wrap items-center gap-4 px-6 py-4">
            <div className="flex flex-col">
              <Button variant="ghost" size="icon-sm" aria-label={`Monter ${partner.name}`} disabled={index === 0 || reorder.isPending} onClick={() => move(index, -1)}><ArrowUpIcon aria-hidden="true" /></Button>
              <Button variant="ghost" size="icon-sm" aria-label={`Descendre ${partner.name}`} disabled={index === partners.data.length - 1 || reorder.isPending} onClick={() => move(index, 1)}><ArrowDownIcon aria-hidden="true" /></Button>
            </div>
            <div className="flex h-16 w-40 items-center justify-center rounded-2xl border bg-background px-3">
              {previewUrl(partner.logoUrl)
                ? <img src={previewUrl(partner.logoUrl)} alt="" className="max-h-12 w-auto object-contain" />
                : <span className="text-sm font-extrabold text-brand-ink">{partner.name}</span>}
            </div>
            <div className="min-w-48 flex-1">
              <p className="font-semibold">{partner.name}</p>
              {partner.url && (
                <a href={partner.url} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 text-xs text-muted-foreground hover:text-brand-ink">
                  {partner.url}<ExternalLinkIcon className="size-3" aria-hidden="true" />
                </a>
              )}
            </div>
            <div className="flex flex-wrap gap-1.5">
              {partner.enabled ? <Badge className="bg-status-ok-surface text-status-ok">Affiché</Badge> : <Badge className="bg-status-neutral-surface text-status-neutral">Masqué</Badge>}
              {!partner.logoUrl && <Badge variant="outline">Logo à fournir</Badge>}
            </div>
            <div className="flex gap-1">
              <Button variant="ghost" size="icon" aria-label={`Modifier ${partner.name}`} onClick={() => setEditing({ kind: "edit", partner })}><PencilIcon aria-hidden="true" /></Button>
              <Button variant="ghost" size="icon" aria-label={`Retirer ${partner.name}`} onClick={() => setEditing({ kind: "delete", partner })}><Trash2Icon aria-hidden="true" /></Button>
            </div>
          </div>
        ))}
      </Card>

      {editing?.kind === "create" && <PartnerDialog onClose={() => setEditing(null)} />}
      {editing?.kind === "edit" && <PartnerDialog partner={editing.partner} onClose={() => setEditing(null)} />}
      {editing?.kind === "delete" && (
        <AlertDialog open onOpenChange={(open) => !open && setEditing(null)}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Retirer « {editing.partner.name} » ?</AlertDialogTitle>
              <AlertDialogDescription>Il disparaît du bandeau de l&apos;accueil. Pour le cacher temporairement, préférez « Masqué ».</AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Annuler</AlertDialogCancel>
              <AlertDialogAction variant="destructive" onClick={(e) => { e.preventDefault(); remove.mutate(editing.partner.id); }}>Retirer</AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      )}
    </section>
  );
}
