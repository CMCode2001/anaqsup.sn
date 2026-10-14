import type { AdminPublication, AdminPublicationKind } from "@anaqsup/api-client";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ChevronLeftIcon, ChevronRightIcon, ExternalLinkIcon, FileTextIcon, FileUpIcon, ImageIcon, Loader2Icon, PencilIcon,
  PlusIcon, SearchIcon, Trash2Icon,
} from "lucide-react";
import { type FormEvent, useDeferredValue, useRef, useState } from "react";

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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@anaqsup/ui/components/select";
import { Skeleton } from "@anaqsup/ui/components/skeleton";
import { toast } from "@anaqsup/ui/components/sonner";
import { Switch } from "@anaqsup/ui/components/switch";

import { ImageField } from "@/components/ImageField";
import { PageTitle } from "@/components/PageTitle";
import { Textarea } from "@/components/Textarea";
import { api, fileSize, previewUrl, unwrap, uploadDocument } from "@/lib/api";

/** Types de document et rubrique du site où ils apparaissent (en plus de « Publications », qui montre tout). */
const KINDS: { value: AdminPublicationKind; label: string; section?: string }[] = [
  { value: "TEXTE_REGLEMENTAIRE", label: "Texte réglementaire", section: "Textes réglementaires" },
  { value: "REFERENTIEL", label: "Référentiel", section: "Référentiels" },
  { value: "GUIDE", label: "Guide", section: "Guide d'auto-évaluation" },
  { value: "RAPPORT_ANNUEL", label: "Rapport" },
  { value: "ETUDE", label: "Étude" },
  { value: "DISCOURS", label: "Discours" },
  { value: "AUTRE", label: "Autre" },
];
const kindLabel = (kind: AdminPublicationKind) => KINDS.find((k) => k.value === kind)?.label ?? kind;

const PAGE_SIZE = 20;
const listKey = ["publications"] as const;
const shortDate = new Intl.DateTimeFormat("fr-SN", { dateStyle: "medium", timeZone: "Africa/Dakar" });
const today = () => new Date().toLocaleDateString("sv-SE", { timeZone: "Africa/Dakar" }); // AAAA-MM-JJ

type Image = { key: string; url: string };

/** Fichier PDF du document : téléversement, nom et taille, ouverture dans un nouvel onglet. */
function PdfField({ file, onUploaded }: {
  file: { url: string | null; name: string | null; size: number | null };
  onUploaded: (file: Image & { name: string | null; size: number }, thumbnail: Image | null) => void;
}) {
  const input = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const preview = previewUrl(file.url);

  const upload = async (picked: File | undefined) => {
    if (!picked) return;
    setUploading(true);
    try {
      const uploaded = await uploadDocument(picked);
      onUploaded(
        { key: uploaded.file.key, url: uploaded.file.url, name: uploaded.file.originalName ?? picked.name, size: uploaded.file.sizeBytes },
        uploaded.thumbnail ? { key: uploaded.thumbnail.key, url: uploaded.thumbnail.url } : null,
      );
      toast.success("PDF téléversé.", { description: uploaded.thumbnail ? "La vignette reprend sa première page." : "Vignette impossible à générer : choisissez une image." });
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Téléversement impossible.");
    } finally {
      setUploading(false);
      if (input.current) input.current.value = "";
    }
  };

  return (
    <div className="flex flex-col gap-3">
      <span className="text-sm font-medium">Fichier PDF</span>
      <div className="flex items-center gap-3 rounded-2xl border bg-background p-3">
        <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary/15 text-brand-ink">
          <FileTextIcon className="size-5" aria-hidden="true" />
        </span>
        <div className="min-w-0 flex-1 text-sm">
          {file.url ? (
            <>
              <p className="truncate font-semibold" title={file.name ?? undefined}>{file.name ?? "Document PDF"}</p>
              <p className="text-xs text-muted-foreground">{file.size ? `PDF · ${fileSize(file.size)}` : "Lien externe"}</p>
            </>
          ) : (
            <p className="text-muted-foreground">Aucun fichier : obligatoire pour publier.</p>
          )}
        </div>
        {preview && (
          <Button asChild variant="ghost" size="icon-sm" aria-label="Ouvrir le PDF dans un nouvel onglet">
            <a href={preview} target="_blank" rel="noopener noreferrer"><ExternalLinkIcon aria-hidden="true" /></a>
          </Button>
        )}
      </div>
      <input ref={input} type="file" accept="application/pdf,.pdf" className="sr-only" tabIndex={-1} onChange={(e) => void upload(e.target.files?.[0])} />
      <Button type="button" variant="outline" size="sm" className="w-fit" disabled={uploading} onClick={() => input.current?.click()}>
        {uploading ? <Loader2Icon className="animate-spin" aria-hidden="true" /> : <FileUpIcon data-icon="inline-start" aria-hidden="true" />}
        {uploading ? "Téléversement…" : file.url ? "Remplacer le PDF" : "Choisir un PDF"}
      </Button>
      <p className="text-xs text-muted-foreground">PDF de 30 Mo au plus, sans mot de passe.</p>
    </div>
  );
}

/** Ajout ou modification d'un document. */
function DocumentDialog({ publication, onClose }: { publication?: AdminPublication; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({
    title: publication?.title ?? "",
    summary: publication?.summary ?? "",
    kind: publication?.kind ?? ("TEXTE_REGLEMENTAIRE" as AdminPublicationKind),
    publishedOn: publication?.publishedOn ?? today(),
    published: publication?.published ?? true,
    titleEn: publication?.titleEn ?? "",
    summaryEn: publication?.summaryEn ?? "",
  });
  const set = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) => setForm((current) => ({ ...current, [key]: value }));
  const [file, setFile] = useState({
    key: publication?.fileKey ?? null, url: publication?.fileUrl ?? null,
    name: publication?.fileName ?? null, size: publication?.fileSizeBytes ?? null,
  });
  const [cover, setCover] = useState<Image | null>(
    publication?.coverKey && publication.coverUrl ? { key: publication.coverKey, url: publication.coverUrl } : null);
  // Vignette tirée du PDF actuel : la carte la reprend tant qu'aucune image n'a été choisie à la place.
  const [pdfThumbnail, setPdfThumbnail] = useState<Image | null>(publication?.coverGenerated ? cover : null);
  const coverIsFromPdf = cover !== null && cover.key === pdfThumbnail?.key;

  const save = useMutation({
    mutationFn: () => {
      const body = {
        title: form.title, summary: form.summary.trim() || null, kind: form.kind, publishedOn: form.publishedOn,
        published: form.published, fileKey: file.key, coverKey: cover?.key ?? null,
        titleEn: form.titleEn.trim() || null, summaryEn: form.summaryEn.trim() || null,
      };
      return publication
        ? unwrap(api.PUT("/admin/publications/{id}", { params: { path: { id: publication.id } }, body }))
        : unwrap(api.POST("/admin/publications", { body }));
    },
    onSuccess: (saved) => {
      toast.success(saved.published ? `« ${saved.title} » est en ligne.` : `« ${saved.title} » enregistré en brouillon.`);
      void queryClient.invalidateQueries({ queryKey: listKey });
      onClose();
    },
    onError: (error) => toast.error(error.message),
  });
  const submit = (event: FormEvent) => { event.preventDefault(); save.mutate(); };
  const section = KINDS.find((k) => k.value === form.kind)?.section;

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[92dvh] overflow-y-auto sm:max-w-4xl">
        <form onSubmit={submit} className="flex flex-col gap-6">
          <DialogHeader>
            <DialogTitle>{publication ? "Modifier le document" : "Ajouter un document"}</DialogTitle>
            <DialogDescription>
              Publié, il apparaît dans « Publications »{section ? ` et dans « ${section} »` : ""}, avec sa vignette et le bouton « Consulter ».
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-6 md:grid-cols-[16rem_minmax(0,1fr)]">
            <div className="flex flex-col gap-6">
              <PdfField
                file={file}
                onUploaded={(uploaded, thumbnail) => {
                  setFile({ key: uploaded.key, url: uploaded.url, name: uploaded.name, size: uploaded.size });
                  // Nouvelle première page, sauf si une image a été choisie à la place de la vignette.
                  if (cover === null || coverIsFromPdf) setCover(thumbnail);
                  setPdfThumbnail(thumbnail);
                  if (!form.title.trim() && uploaded.name) set("title", uploaded.name.replace(/\.pdf$/i, "").replace(/[_-]+/g, " ").trim());
                }}
              />
              <ImageField
                label="Vignette"
                value={cover?.url}
                aspect="aspect-[3/4]"
                hint={coverIsFromPdf ? "Première page du PDF. Vous pouvez la remplacer par une image (PNG, JPEG ou WebP)." : "Image choisie (PNG, JPEG ou WebP, 5 Mo maximum)."}
                onChange={(image) => setCover(image)}
              />
              {pdfThumbnail && !coverIsFromPdf && (
                <Button type="button" variant="link" size="sm" className="-mt-4 w-fit px-0" onClick={() => setCover(pdfThumbnail)}>
                  <ImageIcon data-icon="inline-start" aria-hidden="true" />Reprendre la première page du PDF
                </Button>
              )}
            </div>

            <div className="flex flex-col gap-5">
              <div className="flex flex-col gap-2">
                <Label htmlFor="doc-title">Titre</Label>
                <Input id="doc-title" required maxLength={300} value={form.title} onChange={(e) => set("title", e.target.value)} />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="doc-summary">Résumé (facultatif)</Label>
                <Textarea id="doc-summary" maxLength={1000} value={form.summary} onChange={(e) => set("summary", e.target.value)} />
              </div>
              <div className="grid gap-5 sm:grid-cols-2">
                <div className="flex flex-col gap-2">
                  <Label htmlFor="doc-kind">Type</Label>
                  <Select value={form.kind} onValueChange={(value) => set("kind", value as AdminPublicationKind)}>
                    <SelectTrigger id="doc-kind" className="w-full rounded-full"><SelectValue /></SelectTrigger>
                    <SelectContent>
                      {KINDS.map((kind) => <SelectItem key={kind.value} value={kind.value}>{kind.label}</SelectItem>)}
                    </SelectContent>
                  </Select>
                </div>
                <div className="flex flex-col gap-2">
                  <Label htmlFor="doc-date">Date du document</Label>
                  <Input id="doc-date" type="date" required value={form.publishedOn} onChange={(e) => set("publishedOn", e.target.value)} />
                </div>
              </div>
              <div className="flex items-center justify-between gap-4 rounded-2xl border p-4">
                <div>
                  <Label htmlFor="doc-published">Publié sur le site</Label>
                  <p className="text-xs text-muted-foreground">Décochez pour l&apos;enregistrer en brouillon.</p>
                </div>
                <Switch id="doc-published" checked={form.published} onCheckedChange={(value) => set("published", value)} />
              </div>

              <fieldset className="flex flex-col gap-4 rounded-3xl bg-muted/60 p-5">
                <legend className="sr-only">Version anglaise</legend>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="text-sm font-semibold">English version</span>
                  {publication?.titleEn && (publication.englishAuto
                    ? <Badge variant="outline">Traduction automatique</Badge>
                    : <Badge className="bg-status-ok-surface text-status-ok">Relue</Badge>)}
                </div>
                <p className="text-xs text-muted-foreground">
                  {publication?.englishAuto
                    ? "Traduite par DeepL et refaite à chaque modification du français. Dès que vous la corrigez, elle devient définitive."
                    : "Laissez vide : DeepL la traduira juste après l'enregistrement. Une version saisie ici n'est jamais écrasée."}
                </p>
                <div className="flex flex-col gap-2">
                  <Label htmlFor="doc-title-en">Title</Label>
                  <Input id="doc-title-en" lang="en" maxLength={300} value={form.titleEn} onChange={(e) => set("titleEn", e.target.value)} />
                </div>
                <div className="flex flex-col gap-2">
                  <Label htmlFor="doc-summary-en">Summary</Label>
                  <Textarea id="doc-summary-en" lang="en" maxLength={1000} value={form.summaryEn} onChange={(e) => set("summaryEn", e.target.value)} />
                </div>
              </fieldset>
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Annuler</Button>
            <Button type="submit" disabled={save.isPending || !form.title.trim() || !form.publishedOn}>
              {save.isPending && <Loader2Icon className="animate-spin" aria-hidden="true" />}
              {form.published ? "Enregistrer et publier" : "Enregistrer le brouillon"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

type Editing = { kind: "create" } | { kind: "edit" | "delete"; publication: AdminPublication } | null;

export function DocumentsPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const q = useDeferredValue(search.trim());
  const [kind, setKind] = useState<AdminPublicationKind | "ALL">("ALL");
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<Editing>(null);

  const list = useQuery({
    queryKey: [...listKey, { q, kind, page }],
    queryFn: () => unwrap(api.GET("/admin/publications", {
      params: { query: { q: q || undefined, kind: kind === "ALL" ? undefined : kind, page, size: PAGE_SIZE } },
    })),
    placeholderData: keepPreviousData,
  });
  const remove = useMutation({
    mutationFn: (id: string) => unwrap(api.DELETE("/admin/publications/{id}", { params: { path: { id } } })),
    onSuccess: () => { toast.success("Document retiré du site."); void queryClient.invalidateQueries({ queryKey: listKey }); setEditing(null); },
    onError: (error) => toast.error(error.message),
  });
  const totalPages = list.data?.page.totalPages ?? 0;

  return (
    <section className="space-y-6">
      <PageTitle
        title="Ressources documentaires"
        description="Textes réglementaires, référentiels, guides, rapports… Chaque document publié apparaît sur le site avec sa vignette, et peut être consulté ou téléchargé."
        actions={<Button onClick={() => setEditing({ kind: "create" })}><PlusIcon data-icon="inline-start" aria-hidden="true" />Ajouter un document</Button>}
      />

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <SearchIcon className="pointer-events-none absolute top-1/2 left-4 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
          <Input type="search" aria-label="Rechercher un document" placeholder="Rechercher un document…" className="pl-10"
                 value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
        </div>
        <Select value={kind} onValueChange={(value) => { setKind(value as typeof kind); setPage(0); }}>
          <SelectTrigger aria-label="Type de document" className="w-full rounded-full sm:w-64"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Tous les types</SelectItem>
            {KINDS.map((k) => <SelectItem key={k.value} value={k.value}>{k.label}</SelectItem>)}
          </SelectContent>
        </Select>
      </div>

      <Card className="divide-y py-0">
        {list.isPending && Array.from({ length: 4 }, (_, i) => <div key={i} className="p-4"><Skeleton className="h-20" /></div>)}
        {list.isError && <p className="p-6 text-sm text-destructive">{list.error.message}</p>}
        {list.data?.content.length === 0 && (
          <p className="p-6 text-sm text-muted-foreground">{q || kind !== "ALL" ? "Aucun document ne correspond." : "Aucun document pour l'instant."}</p>
        )}
        {list.data?.content.map((publication) => (
          <div key={publication.id} className="flex flex-wrap items-center gap-4 px-6 py-4">
            <div className="relative h-20 w-16 shrink-0 overflow-hidden rounded-xl border bg-muted">
              {previewUrl(publication.coverUrl)
                ? <img src={previewUrl(publication.coverUrl)} alt="" className="absolute inset-0 size-full object-cover object-top" />
                : <FileTextIcon className="absolute inset-0 m-auto size-6 text-brand-ink" aria-hidden="true" />}
            </div>
            <div className="min-w-56 flex-1">
              <p className="font-semibold">{publication.title}</p>
              <p className="text-xs text-muted-foreground">
                {kindLabel(publication.kind)} · {shortDate.format(new Date(`${publication.publishedOn}T12:00:00Z`))}
                {publication.fileSizeBytes ? ` · PDF ${fileSize(publication.fileSizeBytes)}` : ""}
              </p>
            </div>
            <div className="flex flex-wrap gap-1.5">
              {publication.published
                ? <Badge className="bg-status-ok-surface text-status-ok">Publié</Badge>
                : <Badge className="bg-status-neutral-surface text-status-neutral">Brouillon</Badge>}
              {!publication.fileUrl && <Badge variant="outline">PDF manquant</Badge>}
              {!publication.titleEn
                ? <Badge variant="outline">Anglais en attente</Badge>
                : publication.englishAuto && <Badge variant="outline">Anglais automatique</Badge>}
            </div>
            <div className="flex gap-1">
              <Button variant="ghost" size="icon" aria-label={`Modifier ${publication.title}`} onClick={() => setEditing({ kind: "edit", publication })}><PencilIcon aria-hidden="true" /></Button>
              <Button variant="ghost" size="icon" aria-label={`Supprimer ${publication.title}`} onClick={() => setEditing({ kind: "delete", publication })}><Trash2Icon aria-hidden="true" /></Button>
            </div>
          </div>
        ))}
      </Card>

      {totalPages > 1 && (
        <div className="flex items-center justify-between gap-4 text-sm text-muted-foreground">
          <span>Page {page + 1} sur {totalPages} · {list.data?.page.totalElements} documents</span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}><ChevronLeftIcon data-icon="inline-start" aria-hidden="true" />Précédente</Button>
            <Button variant="outline" size="sm" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>Suivante<ChevronRightIcon data-icon="inline-end" aria-hidden="true" /></Button>
          </div>
        </div>
      )}

      {editing?.kind === "create" && <DocumentDialog onClose={() => setEditing(null)} />}
      {editing?.kind === "edit" && <DocumentDialog key={editing.publication.id} publication={editing.publication} onClose={() => setEditing(null)} />}
      {editing?.kind === "delete" && (
        <AlertDialog open onOpenChange={(open) => !open && setEditing(null)}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Supprimer « {editing.publication.title} » ?</AlertDialogTitle>
              <AlertDialogDescription>
                Le document disparaît du site. Pour le retirer temporairement, préférez le passer en brouillon.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Annuler</AlertDialogCancel>
              <AlertDialogAction variant="destructive" onClick={(e) => { e.preventDefault(); remove.mutate(editing.publication.id); }}>Supprimer</AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      )}
    </section>
  );
}
