import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { MapPinIcon, PencilIcon, PlusIcon, SearchIcon, Trash2Icon } from "lucide-react";
import { useDeferredValue, useState } from "react";
import { Link } from "react-router";

import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter,
  AlertDialogHeader, AlertDialogTitle,
} from "@anaqsup/ui/components/alert-dialog";
import { Badge } from "@anaqsup/ui/components/badge";
import { Button } from "@anaqsup/ui/components/button";
import { Card } from "@anaqsup/ui/components/card";
import { Input } from "@anaqsup/ui/components/input";
import { Skeleton } from "@anaqsup/ui/components/skeleton";
import { toast } from "@anaqsup/ui/components/sonner";

import { EnglishBadge } from "@/components/EnglishNotice";
import { PageTitle } from "@/components/PageTitle";
import { Pagination } from "@/components/Pagination";
import { Segmented } from "@/components/Segmented";
import { api, unwrap } from "@/lib/api";
import { dayMonth, hourMinute, shortDate } from "@/lib/dates";

const PAGE_SIZE = 20;
export const eventsListKey = ["events", "list"] as const;
type When = "upcoming" | "past" | "all";
const WHEN: { value: When; label: string }[] = [
  { value: "upcoming", label: "À venir" }, { value: "past", label: "Passés" }, { value: "all", label: "Tous" },
];

export function EventsPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const q = useDeferredValue(search.trim());
  const [when, setWhen] = useState<When>("upcoming");
  const [page, setPage] = useState(0);
  const [deleting, setDeleting] = useState<{ id: string; title: string } | null>(null);

  const list = useQuery({
    queryKey: [...eventsListKey, { q, when, page }],
    queryFn: () => unwrap(api.GET("/admin/events", {
      params: { query: { q: q || undefined, upcoming: when === "all" ? undefined : when === "upcoming", page, size: PAGE_SIZE } },
    })),
    placeholderData: keepPreviousData,
  });
  const remove = useMutation({
    mutationFn: (id: string) => unwrap(api.DELETE("/admin/events/{id}", { params: { path: { id } } })),
    onSuccess: () => { toast.success("Événement retiré du site."); void queryClient.invalidateQueries({ queryKey: eventsListKey }); setDeleting(null); },
    onError: (error) => toast.error(error.message),
  });

  return (
    <section className="space-y-6">
      <PageTitle
        title="Événements"
        description="Ateliers, séminaires, colloques… Publication directe : un événement publié apparaît dans l'agenda du site dès l'enregistrement."
        actions={<Button asChild><Link to="/evenements/nouveau"><PlusIcon data-icon="inline-start" aria-hidden="true" />Nouvel événement</Link></Button>}
      />

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <SearchIcon className="pointer-events-none absolute top-1/2 left-4 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
          <Input type="search" aria-label="Rechercher un événement" placeholder="Titre ou lieu…" className="pl-10"
                 value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
        </div>
        <Segmented label="Période" value={when} options={WHEN} onChange={(value) => { setWhen(value); setPage(0); }} />
      </div>

      <Card className="divide-y py-0">
        {list.isPending && Array.from({ length: 3 }, (_, i) => <div key={i} className="p-4"><Skeleton className="h-16" /></div>)}
        {list.isError && <p className="p-6 text-sm text-destructive">{list.error.message}</p>}
        {list.data?.content.length === 0 && (
          <p className="p-6 text-sm text-muted-foreground">
            {q ? "Aucun événement ne correspond." : when === "upcoming" ? "Aucun événement à venir." : "Aucun événement."}
          </p>
        )}
        {list.data?.content.map((event) => {
          const start = new Date(event.startsAt);
          const [day, month] = dayMonth.format(start).split(" ");
          return (
            <div key={event.id} className="flex flex-wrap items-center gap-4 px-6 py-4">
              <div className="flex size-16 shrink-0 flex-col items-center justify-center rounded-2xl bg-primary/15 text-brand-ink">
                <span className="text-xl leading-none font-extrabold">{day}</span>
                <span className="text-xs font-semibold uppercase">{month}</span>
              </div>
              <div className="min-w-56 flex-1">
                <Link to={`/evenements/${event.id}`} className="font-semibold hover:text-brand-ink hover:underline">{event.title}</Link>
                <p className="flex flex-wrap items-center gap-x-1 text-xs text-muted-foreground">
                  {shortDate.format(start)} à {hourMinute.format(start)}
                  {event.endsAt && <> → {shortDate.format(new Date(event.endsAt))}</>}
                  {event.location && <><MapPinIcon className="ml-2 size-3" aria-hidden="true" />{event.location}</>}
                </p>
              </div>
              <div className="flex flex-wrap gap-1.5">
                {event.published
                  ? <Badge className="bg-status-ok-surface text-status-ok">Publié</Badge>
                  : <Badge className="bg-status-neutral-surface text-status-neutral">Brouillon</Badge>}
                <EnglishBadge hasEnglish={!!event.titleEn} auto={event.englishAuto} />
              </div>
              <div className="flex gap-1">
                <Button asChild variant="ghost" size="icon" aria-label={`Modifier ${event.title}`}>
                  <Link to={`/evenements/${event.id}`}><PencilIcon aria-hidden="true" /></Link>
                </Button>
                <Button variant="ghost" size="icon" aria-label={`Supprimer ${event.title}`} onClick={() => setDeleting(event)}><Trash2Icon aria-hidden="true" /></Button>
              </div>
            </div>
          );
        })}
      </Card>

      <Pagination page={page} totalPages={list.data?.page.totalPages ?? 0} total={list.data?.page.totalElements} noun="événements" onChange={setPage} />

      {deleting && (
        <AlertDialog open onOpenChange={(open) => !open && setDeleting(null)}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Supprimer « {deleting.title} » ?</AlertDialogTitle>
              <AlertDialogDescription>L&apos;événement disparaît de l&apos;agenda du site. Pour le masquer temporairement, passez-le en brouillon.</AlertDialogDescription>
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
