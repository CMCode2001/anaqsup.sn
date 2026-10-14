import type { AdminNewsCategory } from "@anaqsup/api-client";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ArrowDownIcon, ArrowUpIcon, CheckIcon, ImageIcon, Loader2Icon, PencilIcon, PinIcon, PlusIcon, SearchIcon, TagsIcon,
  ThumbsDownIcon, ThumbsUpIcon, Trash2Icon, XIcon,
} from "lucide-react";
import { type FormEvent, useDeferredValue, useState } from "react";
import { Link } from "react-router";

import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter,
  AlertDialogHeader, AlertDialogTitle,
} from "@anaqsup/ui/components/alert-dialog";
import { Badge } from "@anaqsup/ui/components/badge";
import { Button } from "@anaqsup/ui/components/button";
import { Card } from "@anaqsup/ui/components/card";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@anaqsup/ui/components/dialog";
import { Input } from "@anaqsup/ui/components/input";
import { Label } from "@anaqsup/ui/components/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@anaqsup/ui/components/select";
import { Skeleton } from "@anaqsup/ui/components/skeleton";
import { toast } from "@anaqsup/ui/components/sonner";

import { EnglishBadge } from "@/components/EnglishNotice";
import { PageTitle } from "@/components/PageTitle";
import { Pagination } from "@/components/Pagination";
import { api, previewUrl, unwrap } from "@/lib/api";
import { shortDate } from "@/lib/dates";

const PAGE_SIZE = 20;
export const newsListKey = ["news", "list"] as const;
export const categoriesKey = ["news-categories"] as const;

export function useNewsCategories() {
  return useQuery({ queryKey: categoriesKey, queryFn: () => unwrap(api.GET("/admin/news/categories")) });
}

/** Une rubrique : renommer (français et anglais), monter, descendre, supprimer si vide. */
function CategoryRow({ category, first, last, busy, onMove, onDelete }: {
  category: AdminNewsCategory;
  first: boolean;
  last: boolean;
  busy: boolean;
  onMove: (offset: -1 | 1) => void;
  onDelete: () => void;
}) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [label, setLabel] = useState(category.label);
  const [labelEn, setLabelEn] = useState(category.labelEn ?? "");
  const rename = useMutation({
    mutationFn: () => unwrap(api.PUT("/admin/news/categories/{id}", {
      params: { path: { id: category.id } }, body: { label, labelEn: labelEn.trim() || null },
    })),
    onSuccess: () => {
      toast.success("Rubrique renommée.");
      void queryClient.invalidateQueries({ queryKey: categoriesKey });
      void queryClient.invalidateQueries({ queryKey: newsListKey });
      setEditing(false);
    },
    onError: (error) => toast.error(error.message),
  });
  const submit = (event: FormEvent) => { event.preventDefault(); rename.mutate(); };
  const locked = category.menu
    ? "Rubrique du menu du site : elle ne peut pas être supprimée."
    : category.articles > 0 ? "Déplacez ou supprimez d'abord ses articles." : undefined;

  if (editing) {
    return (
      <form onSubmit={submit} className="flex flex-wrap items-end gap-3 px-4 py-3">
        <div className="flex min-w-44 flex-1 flex-col gap-1.5">
          <Label htmlFor={`cat-${category.id}`} className="text-xs">Libellé</Label>
          <Input id={`cat-${category.id}`} required maxLength={80} value={label} onChange={(e) => setLabel(e.target.value)} />
        </div>
        <div className="flex min-w-44 flex-1 flex-col gap-1.5">
          <Label htmlFor={`cat-en-${category.id}`} className="text-xs">Label (English)</Label>
          <Input id={`cat-en-${category.id}`} lang="en" maxLength={80} value={labelEn} onChange={(e) => setLabelEn(e.target.value)} />
        </div>
        <div className="flex gap-1">
          <Button type="submit" size="icon" aria-label="Enregistrer" disabled={rename.isPending || !label.trim()}>
            {rename.isPending ? <Loader2Icon className="animate-spin" aria-hidden="true" /> : <CheckIcon aria-hidden="true" />}
          </Button>
          <Button type="button" variant="ghost" size="icon" aria-label="Annuler" onClick={() => setEditing(false)}><XIcon aria-hidden="true" /></Button>
        </div>
      </form>
    );
  }
  return (
    <div className="flex items-center gap-3 px-4 py-3">
      <div className="flex flex-col">
        <Button variant="ghost" size="icon-sm" aria-label={`Monter ${category.label}`} disabled={first || busy} onClick={() => onMove(-1)}><ArrowUpIcon aria-hidden="true" /></Button>
        <Button variant="ghost" size="icon-sm" aria-label={`Descendre ${category.label}`} disabled={last || busy} onClick={() => onMove(1)}><ArrowDownIcon aria-hidden="true" /></Button>
      </div>
      <div className="min-w-0 flex-1">
        <p className="font-semibold">{category.label}</p>
        <p className="text-xs text-muted-foreground">
          {category.labelEn ?? "Anglais à saisir"} · {category.articles} article{category.articles > 1 ? "s" : ""}
        </p>
      </div>
      {category.menu && <Badge variant="outline">Menu du site</Badge>}
      <Button variant="ghost" size="icon" aria-label={`Renommer ${category.label}`} onClick={() => setEditing(true)}><PencilIcon aria-hidden="true" /></Button>
      <Button variant="ghost" size="icon" aria-label={`Supprimer ${category.label}`} title={locked} disabled={locked !== undefined} onClick={onDelete}>
        <Trash2Icon aria-hidden="true" />
      </Button>
    </div>
  );
}

/** Rubriques d'actualités : ordre d'affichage (filtres du site), libellés, ajout et suppression. */
function CategoriesDialog({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient();
  const categories = useNewsCategories();
  const [label, setLabel] = useState("");
  const [labelEn, setLabelEn] = useState("");
  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: categoriesKey });
    void queryClient.invalidateQueries({ queryKey: newsListKey });
  };
  const create = useMutation({
    mutationFn: () => unwrap(api.POST("/admin/news/categories", { body: { label, labelEn: labelEn.trim() || null } })),
    onSuccess: (created) => { toast.success(`Rubrique « ${created.label} » ajoutée.`); setLabel(""); setLabelEn(""); refresh(); },
    onError: (error) => toast.error(error.message),
  });
  const reorder = useMutation({
    mutationFn: (ids: string[]) => unwrap(api.PUT("/admin/news/categories/order", { body: { ids } })),
    onSuccess: (sorted) => queryClient.setQueryData(categoriesKey, sorted),
    onError: (error) => toast.error(error.message),
  });
  const remove = useMutation({
    mutationFn: (id: string) => unwrap(api.DELETE("/admin/news/categories/{id}", { params: { path: { id } } })),
    onSuccess: () => { toast.success("Rubrique supprimée."); refresh(); },
    onError: (error) => toast.error(error.message),
  });
  const list = categories.data ?? [];
  const move = (index: number, offset: -1 | 1) => {
    const ids = list.map((c) => c.id);
    [ids[index], ids[index + offset]] = [ids[index + offset]!, ids[index]!];
    reorder.mutate(ids);
  };
  const submit = (event: FormEvent) => { event.preventDefault(); create.mutate(); };

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[92dvh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Rubriques d&apos;actualités</DialogTitle>
          <DialogDescription>
            Elles classent les articles et servent de filtres sur la page Actualités, dans cet ordre. « Coopération &amp; partenariats »
            et « Communiqués » ont leur propre page dans le menu : elles se renomment mais ne se suppriment pas.
          </DialogDescription>
        </DialogHeader>
        <div className="divide-y rounded-2xl border">
          {categories.isPending && <Skeleton className="m-4 h-24" />}
          {list.map((category, index) => (
            <CategoryRow
              key={category.id + category.label + (category.labelEn ?? "")}
              category={category}
              first={index === 0}
              last={index === list.length - 1}
              busy={reorder.isPending}
              onMove={(offset) => move(index, offset)}
              onDelete={() => remove.mutate(category.id)}
            />
          ))}
        </div>
        <form onSubmit={submit} className="flex flex-col gap-3 rounded-2xl bg-muted/60 p-4">
          <span className="text-sm font-semibold">Nouvelle rubrique</span>
          <div className="grid gap-3 sm:grid-cols-2">
            <Input aria-label="Libellé de la nouvelle rubrique" placeholder="Libellé" required maxLength={80} value={label} onChange={(e) => setLabel(e.target.value)} />
            <Input aria-label="Libellé anglais de la nouvelle rubrique" lang="en" placeholder="Label (English)" maxLength={80} value={labelEn} onChange={(e) => setLabelEn(e.target.value)} />
          </div>
          <Button type="submit" size="sm" className="w-fit" disabled={create.isPending || !label.trim()}>
            {create.isPending ? <Loader2Icon className="animate-spin" aria-hidden="true" /> : <PlusIcon data-icon="inline-start" aria-hidden="true" />}
            Ajouter la rubrique
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function NewsPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const q = useDeferredValue(search.trim());
  const [category, setCategory] = useState("ALL");
  const [page, setPage] = useState(0);
  const [categoriesOpen, setCategoriesOpen] = useState(false);
  const [deleting, setDeleting] = useState<{ id: string; title: string } | null>(null);
  const categories = useNewsCategories();

  const list = useQuery({
    queryKey: [...newsListKey, { q, category, page }],
    queryFn: () => unwrap(api.GET("/admin/news", {
      params: { query: { q: q || undefined, category: category === "ALL" ? undefined : category, page, size: PAGE_SIZE } },
    })),
    placeholderData: keepPreviousData,
  });
  const remove = useMutation({
    mutationFn: (id: string) => unwrap(api.DELETE("/admin/news/{id}", { params: { path: { id } } })),
    onSuccess: () => {
      toast.success("Actualité retirée du site.");
      void queryClient.invalidateQueries({ queryKey: newsListKey });
      void queryClient.invalidateQueries({ queryKey: categoriesKey });
      setDeleting(null);
    },
    onError: (error) => toast.error(error.message),
  });

  return (
    <section className="space-y-6">
      <PageTitle
        title="Actualités"
        description="Articles du site, rubriques et article « À la une » (épinglé en tête du carrousel de l'accueil). Publication directe : un article publié est en ligne dès l'enregistrement."
        actions={(
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" onClick={() => setCategoriesOpen(true)}><TagsIcon data-icon="inline-start" aria-hidden="true" />Rubriques</Button>
            <Button asChild><Link to="/actualites/nouvelle"><PlusIcon data-icon="inline-start" aria-hidden="true" />Nouvelle actualité</Link></Button>
          </div>
        )}
      />

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <SearchIcon className="pointer-events-none absolute top-1/2 left-4 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
          <Input type="search" aria-label="Rechercher une actualité" placeholder="Rechercher une actualité…" className="pl-10"
                 value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
        </div>
        <Select value={category} onValueChange={(value) => { setCategory(value); setPage(0); }}>
          <SelectTrigger aria-label="Rubrique" className="w-full rounded-full sm:w-64"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Toutes les rubriques</SelectItem>
            {categories.data?.map((c) => <SelectItem key={c.slug} value={c.slug}>{c.label}</SelectItem>)}
          </SelectContent>
        </Select>
      </div>

      <Card className="divide-y py-0">
        {list.isPending && Array.from({ length: 4 }, (_, i) => <div key={i} className="p-4"><Skeleton className="h-16" /></div>)}
        {list.isError && <p className="p-6 text-sm text-destructive">{list.error.message}</p>}
        {list.data?.content.length === 0 && (
          <p className="p-6 text-sm text-muted-foreground">{q || category !== "ALL" ? "Aucune actualité ne correspond." : "Aucune actualité pour l'instant."}</p>
        )}
        {list.data?.content.map((news) => (
          <div key={news.id} className="flex flex-wrap items-center gap-4 px-6 py-4">
            <div className="relative aspect-[16/10] w-28 shrink-0 overflow-hidden rounded-xl border bg-muted">
              {previewUrl(news.coverUrl)
                ? <img src={previewUrl(news.coverUrl)} alt="" loading="lazy" className="absolute inset-0 size-full object-cover" />
                : <ImageIcon className="absolute inset-0 m-auto size-5 text-muted-foreground" aria-hidden="true" />}
            </div>
            <div className="min-w-56 flex-1">
              <Link to={`/actualites/${news.id}`} className="font-semibold hover:text-brand-ink hover:underline">{news.title}</Link>
              <p className="flex flex-wrap items-center gap-x-1 text-xs text-muted-foreground">
                {news.categoryLabel} · {news.publishedAt ? shortDate.format(new Date(news.publishedAt)) : "non datée"}
                {/* Avis des lecteurs : « J'aime » (public sur le site) et « Je n'aime pas » (visible ici seulement). */}
                {(news.likes > 0 || news.dislikes > 0) && (
                  <>
                    <span aria-hidden="true">·</span>
                    <span className="inline-flex items-center gap-1" title="J'aime (affiché sur le site)">
                      <ThumbsUpIcon className="size-3.5 text-status-ok" aria-hidden="true" /><span className="sr-only">J'aime : </span>{news.likes}
                    </span>
                    <span className="ml-1.5 inline-flex items-center gap-1" title="Je n'aime pas (visible dans le back-office seulement)">
                      <ThumbsDownIcon className="size-3.5 text-status-stop" aria-hidden="true" /><span className="sr-only">Je n'aime pas : </span>{news.dislikes}
                    </span>
                  </>
                )}
              </p>
            </div>
            <div className="flex flex-wrap gap-1.5">
              {news.featured && <Badge className="gap-1 bg-primary text-primary-foreground"><PinIcon data-icon="inline-start" aria-hidden="true" />À la une</Badge>}
              {news.published
                ? <Badge className="bg-status-ok-surface text-status-ok">Publiée</Badge>
                : <Badge className="bg-status-neutral-surface text-status-neutral">Brouillon</Badge>}
              <EnglishBadge hasEnglish={!!news.titleEn} auto={news.englishAuto} />
            </div>
            <div className="flex gap-1">
              <Button asChild variant="ghost" size="icon" aria-label={`Modifier ${news.title}`}>
                <Link to={`/actualites/${news.id}`}><PencilIcon aria-hidden="true" /></Link>
              </Button>
              <Button variant="ghost" size="icon" aria-label={`Supprimer ${news.title}`} onClick={() => setDeleting(news)}><Trash2Icon aria-hidden="true" /></Button>
            </div>
          </div>
        ))}
      </Card>

      <Pagination page={page} totalPages={list.data?.page.totalPages ?? 0} total={list.data?.page.totalElements} noun="actualités" onChange={setPage} />

      {categoriesOpen && <CategoriesDialog onClose={() => setCategoriesOpen(false)} />}
      {deleting && (
        <AlertDialog open onOpenChange={(open) => !open && setDeleting(null)}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Supprimer « {deleting.title} » ?</AlertDialogTitle>
              <AlertDialogDescription>
                L&apos;article disparaît du site. Pour le retirer temporairement, préférez le passer en brouillon.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Annuler</AlertDialogCancel>
              <AlertDialogAction variant="destructive" onClick={(e) => { e.preventDefault(); remove.mutate(deleting.id); }}>Supprimer</AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      )}
    </section>
  );
}
