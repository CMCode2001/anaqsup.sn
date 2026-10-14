import type { AdminAvis, AdminEstablishment, AdminProgram, AdminProgramLevel } from "@anaqsup/api-client";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckIcon, Loader2Icon, PencilIcon, PlusIcon, SearchIcon, Trash2Icon, XIcon } from "lucide-react";
import { type FormEvent, useDeferredValue, useState } from "react";

import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter,
  AlertDialogHeader, AlertDialogTitle,
} from "@anaqsup/ui/components/alert-dialog";
import { AvisBadge } from "@anaqsup/ui/components/avis-badge";
import { Badge } from "@anaqsup/ui/components/badge";
import { Button } from "@anaqsup/ui/components/button";
import { Card } from "@anaqsup/ui/components/card";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@anaqsup/ui/components/dialog";
import { Input } from "@anaqsup/ui/components/input";
import { Label } from "@anaqsup/ui/components/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@anaqsup/ui/components/select";
import { Skeleton } from "@anaqsup/ui/components/skeleton";
import { toast } from "@anaqsup/ui/components/sonner";

import { PageTitle } from "@/components/PageTitle";
import { Pagination } from "@/components/Pagination";
import { Segmented } from "@/components/Segmented";
import { api, unwrap } from "@/lib/api";

const PAGE_SIZE = 20;
const establishmentsKey = ["registry", "establishments"] as const;
const programsKey = ["registry", "programs"] as const;

type AvisFilter = AdminAvis | "AUCUN" | "ALL";
const AVIS_FILTERS: { value: AvisFilter; label: string }[] = [
  { value: "ALL", label: "Tous les avis" }, { value: "FAVORABLE", label: "Avis favorable" },
  { value: "DEFAVORABLE", label: "Avis défavorable" }, { value: "AUCUN", label: "Sans avis publié" },
];
const LEVELS: { value: AdminProgramLevel; label: string }[] = [
  { value: "LICENCE", label: "Licence" }, { value: "MASTER", label: "Master" }, { value: "DOCTORAT", label: "Doctorat" },
  { value: "DUT", label: "DUT" }, { value: "BTS", label: "BTS" }, { value: "AUTRE", label: "Autre" },
];
const levelLabel = (level: AdminProgramLevel) => LEVELS.find((l) => l.value === level)?.label ?? level;
const thisYear = new Date().getFullYear();

/** Avis et année, qui vont ensemble : sans avis, l'année est vidée ; avec avis, elle est obligatoire. */
function AvisFields({ id, avis, year, onChange }: {
  id: string;
  avis: AdminAvis | null;
  year: string;
  onChange: (avis: AdminAvis | null, year: string) => void;
}) {
  return (
    <fieldset className="grid gap-5 rounded-2xl border p-4 sm:grid-cols-[minmax(0,1fr)_9rem]">
      <legend className="px-1 text-sm font-medium">Avis publié</legend>
      <div className="flex flex-col gap-2">
        <Label htmlFor={`${id}-avis`}>Avis</Label>
        <Select value={avis ?? "AUCUN"} onValueChange={(value) => {
          const next = value === "AUCUN" ? null : (value as AdminAvis);
          onChange(next, next ? year || String(thisYear) : "");
        }}>
          <SelectTrigger id={`${id}-avis`} className="w-full rounded-full"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="AUCUN">Aucun avis publié</SelectItem>
            <SelectItem value="FAVORABLE">Favorable</SelectItem>
            <SelectItem value="DEFAVORABLE">Défavorable</SelectItem>
          </SelectContent>
        </Select>
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor={`${id}-year`}>Année</Label>
        <Input id={`${id}-year`} type="number" inputMode="numeric" min={2000} max={2100} required={avis !== null} disabled={avis === null}
               value={year} onChange={(e) => onChange(avis, e.target.value)} />
      </div>
      <p className="text-xs text-muted-foreground sm:col-span-2">Saisi tel quel, tel que rendu par l&apos;ANAQ-Sup. Il s&apos;affiche sur le site dès l&apos;enregistrement.</p>
    </fieldset>
  );
}

// ---------------------------------------------------------------------------------------------- établissements

function EstablishmentDialog({ establishment, onClose }: { establishment?: AdminEstablishment; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({
    name: establishment?.name ?? "", acronym: establishment?.acronym ?? "", kind: establishment?.kind ?? "PUBLIC",
    city: establishment?.city ?? "", region: establishment?.region ?? "", website: establishment?.website ?? "",
    avis: establishment?.avis ?? null, avisYear: establishment?.avisYear ? String(establishment.avisYear) : "",
  });
  const set = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) => setForm((current) => ({ ...current, [key]: value }));
  const save = useMutation({
    mutationFn: () => {
      const body = {
        name: form.name, acronym: form.acronym.trim() || null, kind: form.kind, city: form.city.trim() || null,
        region: form.region.trim() || null, website: form.website.trim() || null,
        avis: form.avis, avisYear: form.avis ? Number(form.avisYear) : null,
      };
      return establishment
        ? unwrap(api.PUT("/admin/registry/establishments/{id}", { params: { path: { id: establishment.id } }, body }))
        : unwrap(api.POST("/admin/registry/establishments", { body }));
    },
    onSuccess: (saved) => {
      toast.success(`« ${saved.acronym ?? saved.name} » enregistré.`, { description: "Le site affiche déjà la mise à jour." });
      void queryClient.invalidateQueries({ queryKey: ["registry"] });
      onClose();
    },
    onError: (error) => toast.error(error.message),
  });
  const submit = (event: FormEvent) => { event.preventDefault(); save.mutate(); };

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[92dvh] overflow-y-auto sm:max-w-2xl">
        <form onSubmit={submit} className="flex flex-col gap-5">
          <DialogHeader>
            <DialogTitle>{establishment ? "Modifier l'établissement" : "Ajouter un établissement"}</DialogTitle>
            <DialogDescription>Il apparaît dans « Établissements habilités », avec son avis d&apos;habilitation et ses formations.</DialogDescription>
          </DialogHeader>
          <div className="grid gap-5 sm:grid-cols-[minmax(0,1fr)_10rem]">
            <div className="flex flex-col gap-2">
              <Label htmlFor="est-name">Nom</Label>
              <Input id="est-name" required maxLength={300} value={form.name} onChange={(e) => set("name", e.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="est-acronym">Sigle</Label>
              <Input id="est-acronym" maxLength={40} placeholder="UCAD" value={form.acronym} onChange={(e) => set("acronym", e.target.value)} />
            </div>
          </div>
          <div className="grid gap-5 sm:grid-cols-3">
            <div className="flex flex-col gap-2">
              <Label htmlFor="est-kind">Statut</Label>
              <Select value={form.kind} onValueChange={(value) => set("kind", value as typeof form.kind)}>
                <SelectTrigger id="est-kind" className="w-full rounded-full"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="PUBLIC">Public</SelectItem>
                  <SelectItem value="PRIVE">Privé</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="est-city">Ville</Label>
              <Input id="est-city" maxLength={120} value={form.city} onChange={(e) => set("city", e.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="est-region">Région</Label>
              <Input id="est-region" maxLength={120} value={form.region} onChange={(e) => set("region", e.target.value)} />
            </div>
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="est-website">Site web</Label>
            <Input id="est-website" type="url" maxLength={300} placeholder="https://" value={form.website} onChange={(e) => set("website", e.target.value)} />
          </div>
          <AvisFields id="est" avis={form.avis} year={form.avisYear} onChange={(avis, year) => setForm((c) => ({ ...c, avis, avisYear: year }))} />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Annuler</Button>
            <Button type="submit" disabled={save.isPending || !form.name.trim()}>
              {save.isPending && <Loader2Icon className="animate-spin" aria-hidden="true" />}Enregistrer et publier
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function EstablishmentsTab() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const q = useDeferredValue(search.trim());
  const [avis, setAvis] = useState<AvisFilter>("ALL");
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<{ kind: "create" } | { kind: "edit" | "delete"; establishment: AdminEstablishment } | null>(null);
  const list = useQuery({
    queryKey: [...establishmentsKey, { q, avis, page }],
    queryFn: () => unwrap(api.GET("/admin/registry/establishments", {
      params: { query: { q: q || undefined, avis: avis === "ALL" ? undefined : avis, page, size: PAGE_SIZE } },
    })),
    placeholderData: keepPreviousData,
  });
  const remove = useMutation({
    mutationFn: (id: string) => unwrap(api.DELETE("/admin/registry/establishments/{id}", { params: { path: { id } } })),
    onSuccess: () => { toast.success("Établissement retiré du site."); void queryClient.invalidateQueries({ queryKey: ["registry"] }); setEditing(null); },
    onError: (error) => toast.error(error.message),
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <SearchIcon className="pointer-events-none absolute top-1/2 left-4 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
          <Input type="search" aria-label="Rechercher un établissement" placeholder="Nom, sigle ou ville…" className="pl-10"
                 value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
        </div>
        <Select value={avis} onValueChange={(value) => { setAvis(value as AvisFilter); setPage(0); }}>
          <SelectTrigger aria-label="Avis" className="w-full rounded-full sm:w-56"><SelectValue /></SelectTrigger>
          <SelectContent>{AVIS_FILTERS.map((a) => <SelectItem key={a.value} value={a.value}>{a.label}</SelectItem>)}</SelectContent>
        </Select>
        <Button onClick={() => setEditing({ kind: "create" })}><PlusIcon data-icon="inline-start" aria-hidden="true" />Ajouter un établissement</Button>
      </div>

      <Card className="divide-y py-0">
        {list.isPending && Array.from({ length: 3 }, (_, i) => <div key={i} className="p-4"><Skeleton className="h-12" /></div>)}
        {list.isError && <p className="p-6 text-sm text-destructive">{list.error.message}</p>}
        {list.data?.content.length === 0 && <p className="p-6 text-sm text-muted-foreground">Aucun établissement ne correspond.</p>}
        {list.data?.content.map((establishment) => (
          <div key={establishment.id} className="flex flex-wrap items-center gap-4 px-6 py-4">
            <div className="min-w-56 flex-1">
              <p className="font-semibold">
                {establishment.acronym && <span className="text-brand-ink">{establishment.acronym} · </span>}{establishment.name}
              </p>
              <p className="text-xs text-muted-foreground">
                {establishment.kind === "PUBLIC" ? "Public" : "Privé"}{establishment.city && ` · ${establishment.city}`}
                {` · ${establishment.programs} formation${establishment.programs > 1 ? "s" : ""}`}
              </p>
            </div>
            <AvisBadge avis={establishment.avis} year={establishment.avisYear} />
            <div className="flex gap-1">
              <Button variant="ghost" size="icon" aria-label={`Modifier ${establishment.name}`} onClick={() => setEditing({ kind: "edit", establishment })}><PencilIcon aria-hidden="true" /></Button>
              <Button variant="ghost" size="icon" aria-label={`Supprimer ${establishment.name}`}
                      title={establishment.programs > 0 ? "Supprimez d'abord ses formations." : undefined}
                      disabled={establishment.programs > 0} onClick={() => setEditing({ kind: "delete", establishment })}>
                <Trash2Icon aria-hidden="true" />
              </Button>
            </div>
          </div>
        ))}
      </Card>
      <Pagination page={page} totalPages={list.data?.page.totalPages ?? 0} total={list.data?.page.totalElements} noun="établissements" onChange={setPage} />

      {editing?.kind === "create" && <EstablishmentDialog onClose={() => setEditing(null)} />}
      {editing?.kind === "edit" && <EstablishmentDialog key={editing.establishment.id} establishment={editing.establishment} onClose={() => setEditing(null)} />}
      {editing?.kind === "delete" && (
        <AlertDialog open onOpenChange={(open) => !open && setEditing(null)}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Supprimer « {editing.establishment.name} » ?</AlertDialogTitle>
              <AlertDialogDescription>L&apos;établissement et son avis disparaissent du site.</AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Annuler</AlertDialogCancel>
              <AlertDialogAction variant="destructive" onClick={(e) => { e.preventDefault(); remove.mutate(editing.establishment.id); }}>Supprimer</AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------------------------- formations

type EstablishmentRef = { id: string; name: string; acronym: string | null };

/** Choix de l'établissement d'une formation, par recherche (la liste peut compter des centaines d'établissements). */
function EstablishmentPicker({ value, onChange }: { value: EstablishmentRef | null; onChange: (value: EstablishmentRef | null) => void }) {
  const [search, setSearch] = useState("");
  const q = useDeferredValue(search.trim());
  const results = useQuery({
    queryKey: [...establishmentsKey, "picker", q],
    queryFn: () => unwrap(api.GET("/admin/registry/establishments", { params: { query: { q: q || undefined, size: 8 } } })),
    enabled: value === null,
    placeholderData: keepPreviousData,
  });

  if (value) {
    return (
      <div className="flex items-center gap-3 rounded-2xl border bg-background px-4 py-3">
        <CheckIcon className="size-4 shrink-0 text-status-ok" aria-hidden="true" />
        <span className="min-w-0 flex-1 truncate text-sm font-semibold">{value.acronym ? `${value.acronym} · ` : ""}{value.name}</span>
        <Button type="button" variant="ghost" size="sm" onClick={() => onChange(null)}><XIcon data-icon="inline-start" aria-hidden="true" />Changer</Button>
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-2 rounded-2xl border p-3">
      <div className="relative">
        <SearchIcon className="pointer-events-none absolute top-1/2 left-4 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
        <Input id="program-establishment" type="search" placeholder="Rechercher l'établissement (nom, sigle, ville)…" className="pl-10"
               value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>
      <ul className="flex flex-col" aria-label="Établissements trouvés">
        {results.data?.content.map((e) => (
          <li key={e.id}>
            <button type="button" onClick={() => onChange({ id: e.id, name: e.name, acronym: e.acronym ?? null })}
                    className="w-full rounded-xl px-3 py-2 text-left text-sm hover:bg-muted focus-visible:bg-muted focus-visible:outline-none">
              {e.acronym && <span className="font-semibold text-brand-ink">{e.acronym} · </span>}{e.name}
              {e.city && <span className="text-muted-foreground"> — {e.city}</span>}
            </button>
          </li>
        ))}
        {results.data?.content.length === 0 && <li className="px-3 py-2 text-sm text-muted-foreground">Aucun établissement : ajoutez-le d&apos;abord dans l&apos;onglet Établissements.</li>}
      </ul>
    </div>
  );
}

function ProgramDialog({ program, onClose }: { program?: AdminProgram; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({
    title: program?.title ?? "", level: program?.level ?? ("LICENCE" as AdminProgramLevel), domain: program?.domain ?? "",
    establishment: program ? { id: program.establishmentId, name: program.establishmentName, acronym: program.establishmentAcronym ?? null } : null as EstablishmentRef | null,
    avis: program?.avis ?? null, avisYear: program?.avisYear ? String(program.avisYear) : "",
  });
  const set = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) => setForm((current) => ({ ...current, [key]: value }));
  const save = useMutation({
    mutationFn: () => {
      const body = {
        title: form.title, level: form.level, domain: form.domain.trim() || null, establishmentId: form.establishment?.id ?? "",
        avis: form.avis, avisYear: form.avis ? Number(form.avisYear) : null,
      };
      return program
        ? unwrap(api.PUT("/admin/registry/programs/{id}", { params: { path: { id: program.id } }, body }))
        : unwrap(api.POST("/admin/registry/programs", { body }));
    },
    onSuccess: (saved) => {
      toast.success(`« ${saved.title} » enregistrée.`, { description: "Le site affiche déjà la mise à jour." });
      void queryClient.invalidateQueries({ queryKey: ["registry"] });
      onClose();
    },
    onError: (error) => toast.error(error.message),
  });
  const submit = (event: FormEvent) => { event.preventDefault(); save.mutate(); };

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[92dvh] overflow-y-auto sm:max-w-2xl">
        <form onSubmit={submit} className="flex flex-col gap-5">
          <DialogHeader>
            <DialogTitle>{program ? "Modifier la formation" : "Ajouter une formation"}</DialogTitle>
            <DialogDescription>Elle apparaît dans « Formations accréditées » et sur la fiche de son établissement.</DialogDescription>
          </DialogHeader>
          <div className="flex flex-col gap-2">
            <Label htmlFor="program-title">Intitulé</Label>
            <Input id="program-title" required maxLength={300} placeholder="Master Informatique" value={form.title} onChange={(e) => set("title", e.target.value)} />
          </div>
          <div className="grid gap-5 sm:grid-cols-[10rem_minmax(0,1fr)]">
            <div className="flex flex-col gap-2">
              <Label htmlFor="program-level">Niveau</Label>
              <Select value={form.level} onValueChange={(value) => set("level", value as AdminProgramLevel)}>
                <SelectTrigger id="program-level" className="w-full rounded-full"><SelectValue /></SelectTrigger>
                <SelectContent>{LEVELS.map((l) => <SelectItem key={l.value} value={l.value}>{l.label}</SelectItem>)}</SelectContent>
              </Select>
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="program-domain">Domaine</Label>
              <Input id="program-domain" maxLength={200} placeholder="Sciences et technologies" value={form.domain} onChange={(e) => set("domain", e.target.value)} />
            </div>
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="program-establishment">Établissement</Label>
            <EstablishmentPicker value={form.establishment} onChange={(value) => set("establishment", value)} />
          </div>
          <AvisFields id="program" avis={form.avis} year={form.avisYear} onChange={(avis, year) => setForm((c) => ({ ...c, avis, avisYear: year }))} />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Annuler</Button>
            <Button type="submit" disabled={save.isPending || !form.title.trim() || !form.establishment}>
              {save.isPending && <Loader2Icon className="animate-spin" aria-hidden="true" />}Enregistrer et publier
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function ProgramsTab() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const q = useDeferredValue(search.trim());
  const [avis, setAvis] = useState<AvisFilter>("ALL");
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<{ kind: "create" } | { kind: "edit" | "delete"; program: AdminProgram } | null>(null);
  const list = useQuery({
    queryKey: [...programsKey, { q, avis, page }],
    queryFn: () => unwrap(api.GET("/admin/registry/programs", {
      params: { query: { q: q || undefined, avis: avis === "ALL" ? undefined : avis, page, size: PAGE_SIZE } },
    })),
    placeholderData: keepPreviousData,
  });
  const remove = useMutation({
    mutationFn: (id: string) => unwrap(api.DELETE("/admin/registry/programs/{id}", { params: { path: { id } } })),
    onSuccess: () => { toast.success("Formation retirée du site."); void queryClient.invalidateQueries({ queryKey: ["registry"] }); setEditing(null); },
    onError: (error) => toast.error(error.message),
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <SearchIcon className="pointer-events-none absolute top-1/2 left-4 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
          <Input type="search" aria-label="Rechercher une formation" placeholder="Intitulé, domaine ou établissement…" className="pl-10"
                 value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
        </div>
        <Select value={avis} onValueChange={(value) => { setAvis(value as AvisFilter); setPage(0); }}>
          <SelectTrigger aria-label="Avis" className="w-full rounded-full sm:w-56"><SelectValue /></SelectTrigger>
          <SelectContent>{AVIS_FILTERS.map((a) => <SelectItem key={a.value} value={a.value}>{a.label}</SelectItem>)}</SelectContent>
        </Select>
        <Button onClick={() => setEditing({ kind: "create" })}><PlusIcon data-icon="inline-start" aria-hidden="true" />Ajouter une formation</Button>
      </div>

      <Card className="divide-y py-0">
        {list.isPending && Array.from({ length: 3 }, (_, i) => <div key={i} className="p-4"><Skeleton className="h-12" /></div>)}
        {list.isError && <p className="p-6 text-sm text-destructive">{list.error.message}</p>}
        {list.data?.content.length === 0 && <p className="p-6 text-sm text-muted-foreground">Aucune formation ne correspond.</p>}
        {list.data?.content.map((program) => (
          <div key={program.id} className="flex flex-wrap items-center gap-4 px-6 py-4">
            <div className="min-w-56 flex-1">
              <p className="font-semibold">{program.title}</p>
              <p className="text-xs text-muted-foreground">
                {program.establishmentAcronym ?? program.establishmentName}{program.domain && ` · ${program.domain}`}
              </p>
            </div>
            <Badge variant="outline">{levelLabel(program.level)}</Badge>
            <AvisBadge avis={program.avis} year={program.avisYear} />
            <div className="flex gap-1">
              <Button variant="ghost" size="icon" aria-label={`Modifier ${program.title}`} onClick={() => setEditing({ kind: "edit", program })}><PencilIcon aria-hidden="true" /></Button>
              <Button variant="ghost" size="icon" aria-label={`Supprimer ${program.title}`} onClick={() => setEditing({ kind: "delete", program })}><Trash2Icon aria-hidden="true" /></Button>
            </div>
          </div>
        ))}
      </Card>
      <Pagination page={page} totalPages={list.data?.page.totalPages ?? 0} total={list.data?.page.totalElements} noun="formations" onChange={setPage} />

      {editing?.kind === "create" && <ProgramDialog onClose={() => setEditing(null)} />}
      {editing?.kind === "edit" && <ProgramDialog key={editing.program.id} program={editing.program} onClose={() => setEditing(null)} />}
      {editing?.kind === "delete" && (
        <AlertDialog open onOpenChange={(open) => !open && setEditing(null)}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Supprimer « {editing.program.title} » ?</AlertDialogTitle>
              <AlertDialogDescription>La formation et son avis disparaissent du site.</AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Annuler</AlertDialogCancel>
              <AlertDialogAction variant="destructive" onClick={(e) => { e.preventDefault(); remove.mutate(editing.program.id); }}>Supprimer</AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      )}
    </div>
  );
}

const TABS = [{ value: "etablissements" as const, label: "Établissements" }, { value: "formations" as const, label: "Formations" }];

export function RegistryPage() {
  const [tab, setTab] = useState<(typeof TABS)[number]["value"]>("etablissements");
  return (
    <section className="space-y-6">
      <PageTitle
        title="Habilitations & accréditations"
        description="Établissements (habilitation) et formations (accréditation), avec l'avis rendu par l'ANAQ-Sup et son année. Publication directe."
      />
      <Segmented label="Liste" value={tab} options={TABS} onChange={setTab} />
      {tab === "etablissements" ? <EstablishmentsTab /> : <ProgramsTab />}
    </section>
  );
}
