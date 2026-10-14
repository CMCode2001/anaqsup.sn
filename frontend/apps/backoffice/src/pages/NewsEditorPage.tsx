import type { AdminNews } from "@anaqsup/api-client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeftIcon, Loader2Icon, SaveIcon } from "lucide-react";
import { type FormEvent, useState } from "react";
import { Link, useNavigate, useParams } from "react-router";

import { Button } from "@anaqsup/ui/components/button";
import { Card, CardContent } from "@anaqsup/ui/components/card";
import { Input } from "@anaqsup/ui/components/input";
import { Label } from "@anaqsup/ui/components/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@anaqsup/ui/components/select";
import { Skeleton } from "@anaqsup/ui/components/skeleton";
import { toast } from "@anaqsup/ui/components/sonner";
import { Switch } from "@anaqsup/ui/components/switch";

import { EnglishNotice, blankHtml } from "@/components/EnglishNotice";
import { ImageField } from "@/components/ImageField";
import { PageTitle } from "@/components/PageTitle";
import { RichTextEditor } from "@/components/RichTextEditor";
import { LANGUAGES, Segmented } from "@/components/Segmented";
import { Textarea } from "@/components/Textarea";
import { api, unwrap } from "@/lib/api";
import { fromLocalInput, toLocalInput } from "@/lib/dates";
import { categoriesKey, newsListKey, useNewsCategories } from "@/pages/NewsPage";

const NEW = "nouvelle";

function Editor({ news }: { news?: AdminNews }) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const categories = useNewsCategories();
  const [lang, setLang] = useState<"fr" | "en">("fr");
  const [form, setForm] = useState({
    title: news?.title ?? "", summary: news?.summary ?? "", bodyHtml: news?.bodyHtml ?? "",
    titleEn: news?.titleEn ?? "", summaryEn: news?.summaryEn ?? "", bodyHtmlEn: news?.bodyHtmlEn ?? "",
    category: news?.category ?? "", publishedAt: toLocalInput(news?.publishedAt),
    published: news?.published ?? true, featured: news?.featured ?? false,
    cover: news?.coverKey ? { key: news.coverKey, url: news.coverUrl ?? "" } : null as { key: string; url: string } | null,
  });
  const set = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) => setForm((current) => ({ ...current, [key]: value }));

  const save = useMutation({
    mutationFn: () => {
      const body = {
        title: form.title, summary: form.summary.trim() || null, bodyHtml: blankHtml(form.bodyHtml) ? "" : form.bodyHtml,
        category: form.category, coverKey: form.cover?.key ?? null, published: form.published,
        publishedAt: fromLocalInput(form.publishedAt), featured: form.published && form.featured,
        titleEn: form.titleEn.trim() || null, summaryEn: form.summaryEn.trim() || null,
        bodyHtmlEn: blankHtml(form.bodyHtmlEn) ? null : form.bodyHtmlEn,
      };
      return news
        ? unwrap(api.PUT("/admin/news/{id}", { params: { path: { id: news.id } }, body }))
        : unwrap(api.POST("/admin/news", { body }));
    },
    onSuccess: (saved) => {
      toast.success(saved.published ? "Actualité enregistrée et publiée." : "Brouillon enregistré.", {
        description: saved.featured ? "Elle est épinglée en tête du carrousel de l'accueil." : undefined,
      });
      queryClient.setQueryData(["news", "item", saved.id], saved);
      void queryClient.invalidateQueries({ queryKey: newsListKey });
      void queryClient.invalidateQueries({ queryKey: categoriesKey });
      if (!news) void navigate(`/actualites/${saved.id}`, { replace: true });
    },
    onError: (error) => toast.error(error.message),
  });
  const submit = (event: FormEvent) => { event.preventDefault(); save.mutate(); };

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
                <Label htmlFor="summary">Chapeau (facultatif)</Label>
                <Textarea id="summary" maxLength={1000} value={form.summary} onChange={(e) => set("summary", e.target.value)} />
                <p className="text-xs text-muted-foreground">Deux ou trois phrases, reprises sur les cartes et dans le carrousel.</p>
              </div>
              <div className="flex flex-col gap-2">
                <span className="text-sm font-medium">Texte de l&apos;article</span>
                <RichTextEditor label="Texte de l'article (français)" value={form.bodyHtml} onChange={(html) => set("bodyHtml", html)} />
              </div>
            </div>
          ) : (
            <div key="en" className="flex flex-col gap-6">
              <EnglishNotice auto={news?.englishAuto ?? false} note="La traduction automatique concerne les actualités des 12 derniers mois." />
              <div className="flex flex-col gap-2">
                <Label htmlFor="title-en">Title</Label>
                <Input id="title-en" lang="en" maxLength={300} value={form.titleEn} onChange={(e) => set("titleEn", e.target.value)} />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="summary-en">Lead</Label>
                <Textarea id="summary-en" lang="en" maxLength={1000} value={form.summaryEn} onChange={(e) => set("summaryEn", e.target.value)} />
              </div>
              <div className="flex flex-col gap-2">
                <span className="text-sm font-medium">Article text</span>
                <RichTextEditor label="Texte de l'article (anglais)" value={form.bodyHtmlEn} onChange={(html) => set("bodyHtmlEn", html)} />
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <div className="flex flex-col gap-6">
        <Card>
          <CardContent className="flex flex-col gap-5">
            <div className="flex flex-col gap-2">
              <Label htmlFor="category">Rubrique</Label>
              <Select value={form.category} onValueChange={(value) => set("category", value)} required>
                <SelectTrigger id="category" className="w-full rounded-full"><SelectValue placeholder="Choisir une rubrique" /></SelectTrigger>
                <SelectContent>
                  {categories.data?.map((c) => <SelectItem key={c.slug} value={c.slug}>{c.label}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="published-at">Date de publication</Label>
              <Input id="published-at" type="datetime-local" value={form.publishedAt} onChange={(e) => set("publishedAt", e.target.value)} />
              <p className="text-xs text-muted-foreground">Heure de Dakar. Vide : la date et l&apos;heure de l&apos;enregistrement.</p>
            </div>
            <div className="flex items-center justify-between gap-4 rounded-2xl border p-4">
              <div>
                <Label htmlFor="published">Publiée sur le site</Label>
                <p className="text-xs text-muted-foreground">Décochez pour l&apos;enregistrer en brouillon.</p>
              </div>
              <Switch id="published" checked={form.published} onCheckedChange={(value) => set("published", value)} />
            </div>
            <div className="flex items-center justify-between gap-4 rounded-2xl border p-4">
              <div>
                <Label htmlFor="featured">À la une</Label>
                <p className="text-xs text-muted-foreground">
                  En tête du carrousel de l&apos;accueil. Un seul article à la fois : épingler celui-ci retire l&apos;autre.
                </p>
              </div>
              <Switch id="featured" disabled={!form.published} checked={form.published && form.featured} onCheckedChange={(value) => set("featured", value)} />
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent>
            <ImageField
              label="Image de couverture"
              value={form.cover?.url}
              aspect="aspect-[16/10]"
              hint="Format paysage, 1 600 px de large conseillé. PNG, JPEG ou WebP, 5 Mo maximum. Affichée en grand dans le carrousel de l'accueil."
              onChange={(image) => set("cover", image)}
            />
          </CardContent>
        </Card>
        <Button type="submit" size="lg" disabled={save.isPending || !form.title.trim() || !form.category} className="xl:sticky xl:bottom-6">
          {save.isPending ? <Loader2Icon className="animate-spin" aria-hidden="true" /> : <SaveIcon data-icon="inline-start" aria-hidden="true" />}
          {form.published ? "Enregistrer et publier" : "Enregistrer le brouillon"}
        </Button>
      </div>
    </form>
  );
}

export function NewsEditorPage() {
  const { id = NEW } = useParams();
  const isNew = id === NEW;
  const news = useQuery({
    queryKey: ["news", "item", id],
    queryFn: () => unwrap(api.GET("/admin/news/{id}", { params: { path: { id } } })),
    enabled: !isNew,
  });

  return (
    <section className="space-y-6">
      <Button asChild variant="ghost" size="sm"><Link to="/actualites"><ArrowLeftIcon data-icon="inline-start" aria-hidden="true" />Actualités</Link></Button>
      <PageTitle
        title={isNew ? "Nouvelle actualité" : "Modifier l'actualité"}
        description={news.data?.legacy ? "Article repris de l'ancien site anaqsup.sn." : undefined}
      />
      {isNew && <Editor />}
      {!isNew && news.isPending && <Skeleton className="h-96 rounded-3xl" />}
      {news.isError && <p className="text-sm text-destructive">{news.error.message}</p>}
      {/* `key` : formulaire réinitialisé si l'on passe d'un article à l'autre. */}
      {!isNew && news.data && <Editor key={news.data.id} news={news.data} />}
    </section>
  );
}
