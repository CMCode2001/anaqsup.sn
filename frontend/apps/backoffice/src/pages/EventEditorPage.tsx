import type { AdminEvent } from "@anaqsup/api-client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeftIcon, Loader2Icon, SaveIcon } from "lucide-react";
import { type FormEvent, useState } from "react";
import { Link, useNavigate, useParams } from "react-router";

import { Button } from "@anaqsup/ui/components/button";
import { Card, CardContent } from "@anaqsup/ui/components/card";
import { Input } from "@anaqsup/ui/components/input";
import { Label } from "@anaqsup/ui/components/label";
import { Skeleton } from "@anaqsup/ui/components/skeleton";
import { toast } from "@anaqsup/ui/components/sonner";
import { Switch } from "@anaqsup/ui/components/switch";

import { EnglishNotice, blankHtml } from "@/components/EnglishNotice";
import { PageTitle } from "@/components/PageTitle";
import { RichTextEditor } from "@/components/RichTextEditor";
import { LANGUAGES, Segmented } from "@/components/Segmented";
import { Textarea } from "@/components/Textarea";
import { api, unwrap } from "@/lib/api";
import { fromLocalInput, toLocalInput } from "@/lib/dates";
import { eventsListKey } from "@/pages/EventsPage";

const NEW = "nouveau";

function Editor({ event }: { event?: AdminEvent }) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [lang, setLang] = useState<"fr" | "en">("fr");
  const [form, setForm] = useState({
    title: event?.title ?? "", summary: event?.summary ?? "", bodyHtml: event?.bodyHtml ?? "",
    titleEn: event?.titleEn ?? "", summaryEn: event?.summaryEn ?? "", bodyHtmlEn: event?.bodyHtmlEn ?? "",
    location: event?.location ?? "", startsAt: toLocalInput(event?.startsAt), endsAt: toLocalInput(event?.endsAt),
    published: event?.published ?? true,
  });
  const set = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) => setForm((current) => ({ ...current, [key]: value }));
  const endBeforeStart = form.endsAt !== "" && form.startsAt !== "" && form.endsAt < form.startsAt;

  const save = useMutation({
    mutationFn: () => {
      const body = {
        title: form.title, summary: form.summary.trim() || null, bodyHtml: blankHtml(form.bodyHtml) ? "" : form.bodyHtml,
        location: form.location.trim() || null, startsAt: fromLocalInput(form.startsAt) ?? "", endsAt: fromLocalInput(form.endsAt),
        published: form.published,
        titleEn: form.titleEn.trim() || null, summaryEn: form.summaryEn.trim() || null,
        bodyHtmlEn: blankHtml(form.bodyHtmlEn) ? null : form.bodyHtmlEn,
      };
      return event
        ? unwrap(api.PUT("/admin/events/{id}", { params: { path: { id: event.id } }, body }))
        : unwrap(api.POST("/admin/events", { body }));
    },
    onSuccess: (saved) => {
      toast.success(saved.published ? "Événement enregistré et publié." : "Brouillon enregistré.");
      queryClient.setQueryData(["events", "item", saved.id], saved);
      void queryClient.invalidateQueries({ queryKey: eventsListKey });
      if (!event) void navigate(`/evenements/${saved.id}`, { replace: true });
    },
    onError: (error) => toast.error(error.message),
  });
  const submit = (e: FormEvent) => { e.preventDefault(); save.mutate(); };

  return (
    <form onSubmit={submit} className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_22rem]">
      <Card>
        <CardContent className="flex flex-col gap-6">
          <Segmented label="Langue" value={lang} options={LANGUAGES} onChange={setLang} />
          {lang === "fr" ? (
            <div key="fr" className="flex flex-col gap-6">
              <div className="flex flex-col gap-2">
                <Label htmlFor="title">Titre</Label>
                <Input id="title" required maxLength={300} value={form.title} onChange={(e) => set("title", e.target.value)} />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="summary">Résumé (facultatif)</Label>
                <Textarea id="summary" maxLength={1000} value={form.summary} onChange={(e) => set("summary", e.target.value)} />
              </div>
              <div className="flex flex-col gap-2">
                <span className="text-sm font-medium">Programme et informations pratiques</span>
                <RichTextEditor label="Texte de l'événement (français)" value={form.bodyHtml} onChange={(html) => set("bodyHtml", html)} />
              </div>
            </div>
          ) : (
            <div key="en" className="flex flex-col gap-6">
              <EnglishNotice auto={event?.englishAuto ?? false} />
              <div className="flex flex-col gap-2">
                <Label htmlFor="title-en">Title</Label>
                <Input id="title-en" lang="en" maxLength={300} value={form.titleEn} onChange={(e) => set("titleEn", e.target.value)} />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="summary-en">Summary</Label>
                <Textarea id="summary-en" lang="en" maxLength={1000} value={form.summaryEn} onChange={(e) => set("summaryEn", e.target.value)} />
              </div>
              <div className="flex flex-col gap-2">
                <span className="text-sm font-medium">Programme and practical information</span>
                <RichTextEditor label="Texte de l'événement (anglais)" value={form.bodyHtmlEn} onChange={(html) => set("bodyHtmlEn", html)} />
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <div className="flex flex-col gap-6">
        <Card>
          <CardContent className="flex flex-col gap-5">
            <div className="flex flex-col gap-2">
              <Label htmlFor="starts-at">Début</Label>
              <Input id="starts-at" type="datetime-local" required value={form.startsAt} onChange={(e) => set("startsAt", e.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="ends-at">Fin (facultative)</Label>
              <Input id="ends-at" type="datetime-local" min={form.startsAt || undefined} aria-invalid={endBeforeStart || undefined}
                     value={form.endsAt} onChange={(e) => set("endsAt", e.target.value)} />
              <p className={endBeforeStart ? "text-xs text-destructive" : "text-xs text-muted-foreground"}>
                {endBeforeStart ? "La fin doit suivre le début." : "Heure de Dakar. Pour un événement sur plusieurs jours, indiquez la fin."}
              </p>
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="location">Lieu</Label>
              <Input id="location" maxLength={200} placeholder="Dakar, siège de l'ANAQ-Sup" value={form.location} onChange={(e) => set("location", e.target.value)} />
            </div>
            <div className="flex items-center justify-between gap-4 rounded-2xl border p-4">
              <div>
                <Label htmlFor="published">Publié sur le site</Label>
                <p className="text-xs text-muted-foreground">Décochez pour l&apos;enregistrer en brouillon.</p>
              </div>
              <Switch id="published" checked={form.published} onCheckedChange={(value) => set("published", value)} />
            </div>
          </CardContent>
        </Card>
        <Button type="submit" size="lg" disabled={save.isPending || !form.title.trim() || !form.startsAt || endBeforeStart} className="xl:sticky xl:bottom-6">
          {save.isPending ? <Loader2Icon className="animate-spin" aria-hidden="true" /> : <SaveIcon data-icon="inline-start" aria-hidden="true" />}
          {form.published ? "Enregistrer et publier" : "Enregistrer le brouillon"}
        </Button>
      </div>
    </form>
  );
}

export function EventEditorPage() {
  const { id = NEW } = useParams();
  const isNew = id === NEW;
  const event = useQuery({
    queryKey: ["events", "item", id],
    queryFn: () => unwrap(api.GET("/admin/events/{id}", { params: { path: { id } } })),
    enabled: !isNew,
  });

  return (
    <section className="space-y-6">
      <Button asChild variant="ghost" size="sm"><Link to="/evenements"><ArrowLeftIcon data-icon="inline-start" aria-hidden="true" />Événements</Link></Button>
      <PageTitle title={isNew ? "Nouvel événement" : "Modifier l'événement"} />
      {isNew && <Editor />}
      {!isNew && event.isPending && <Skeleton className="h-96 rounded-3xl" />}
      {event.isError && <p className="text-sm text-destructive">{event.error.message}</p>}
      {!isNew && event.data && <Editor key={event.data.id} event={event.data} />}
    </section>
  );
}
