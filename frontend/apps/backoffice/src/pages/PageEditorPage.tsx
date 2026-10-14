import type { AdminPage } from "@anaqsup/api-client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeftIcon, Loader2Icon, SaveIcon } from "lucide-react";
import { type FormEvent, useState } from "react";
import { Link, useParams } from "react-router";

import { Button } from "@anaqsup/ui/components/button";
import { Card, CardContent } from "@anaqsup/ui/components/card";
import { Input } from "@anaqsup/ui/components/input";
import { Label } from "@anaqsup/ui/components/label";
import { Skeleton } from "@anaqsup/ui/components/skeleton";
import { toast } from "@anaqsup/ui/components/sonner";

import { ImageField } from "@/components/ImageField";
import { PageTitle } from "@/components/PageTitle";
import { RichTextEditor } from "@/components/RichTextEditor";
import { LANGUAGES, Segmented } from "@/components/Segmented";
import { Textarea } from "@/components/Textarea";
import { api, unwrap } from "@/lib/api";
import { PAGE_INFO } from "@/pages/PagesPage";

const SE = "mot-du-secretaire-executif";

/** Image de la page et ses deux textes : photo + nom/fonction, ou organigramme + titre/sous-titre. */
const IMAGE_TEXTS = {
  photo: {
    image: "Photo (facultative)", hint: "Portrait de préférence (4:5). PNG, JPEG ou WebP, 5 Mo maximum.", aspect: "aspect-[4/5]", fit: "cover",
    name: "Nom", namePlaceholder: "Pr Prénom NOM", nameHint: "Affiché en évidence sous la photo, identique en français et en anglais.",
    role: "Fonction", rolePlaceholder: "Secrétaire exécutif de l'ANAQ-Sup", roleEn: "Fonction (anglais)", roleEnPlaceholder: "Executive Secretary of ANAQ-Sup",
  },
  organigramme: {
    image: "Image de l'organigramme",
    hint: "Grand format (2 000 px de large ou plus), PNG, JPEG ou WebP, 5 Mo maximum. Le site l'affiche en entier, avec « Télécharger » et « Voir en taille réelle ».",
    aspect: "aspect-[16/10]", fit: "contain",
    name: "Titre de l'organigramme", namePlaceholder: "Organigramme 2026", nameHint: "Affiché au-dessus de l'image.",
    role: "Sous-titre", rolePlaceholder: "Structure organisationnelle de l'ANAQ-Sup", roleEn: "Sous-titre (anglais)", roleEnPlaceholder: "Organisational structure of ANAQ-Sup",
  },
} as const;

function Editor({ page }: { page: AdminPage }) {
  const queryClient = useQueryClient();
  const [lang, setLang] = useState<"fr" | "en">("fr");
  const [form, setForm] = useState({
    title: page.title, summary: page.summary ?? "", bodyHtml: page.bodyHtml,
    titleEn: page.titleEn ?? "", summaryEn: page.summaryEn ?? "", bodyHtmlEn: page.bodyHtmlEn ?? "",
    imageKey: page.imageKey ?? null, imageUrl: page.imageUrl ?? null,
    imageName: page.imageName ?? "", imageRole: page.imageRole ?? "", imageRoleEn: page.imageRoleEn ?? "",
  });
  const set = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) => setForm((current) => ({ ...current, [key]: value }));
  const isSE = page.slug === SE;
  const isOrgChart = page.slug === "organigramme";
  // Fiches procédures : texte seul ; chaque intertitre devient une entrée du sommaire sur le site.
  const isProcedure = page.slug.startsWith("procedure-");
  const texts = IMAGE_TEXTS[isOrgChart ? "organigramme" : "photo"];

  const save = useMutation({
    mutationFn: () =>
      unwrap(api.PUT("/admin/pages/{slug}", {
        params: { path: { slug: page.slug } },
        body: {
          title: form.title, summary: form.summary || null, bodyHtml: form.bodyHtml,
          titleEn: form.titleEn || null, summaryEn: form.summaryEn || null,
          bodyHtmlEn: form.bodyHtmlEn && form.bodyHtmlEn !== "<p></p>" ? form.bodyHtmlEn : null,
          imageKey: form.imageKey, imageName: form.imageName.trim() || null, imageRole: form.imageRole.trim() || null, imageRoleEn: form.imageRoleEn.trim() || null,
        },
      })),
    onSuccess: (saved) => {
      queryClient.setQueryData(["page", page.slug], saved);
      void queryClient.invalidateQueries({ queryKey: ["pages"] });
      toast.success("Page enregistrée et publiée.", { description: "Le site affiche déjà la nouvelle version." });
    },
    onError: (error) => toast.error(error.message),
  });
  const submit = (event: FormEvent) => { event.preventDefault(); save.mutate(); };

  return (
    <form onSubmit={submit} className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_20rem]">
      <Card>
        <CardContent className="flex flex-col gap-6">
          <Segmented label="Langue" value={lang} options={LANGUAGES} onChange={setLang} />

          {lang === "fr" ? (
            <div key="fr" className="flex flex-col gap-6">
              <div className="flex flex-col gap-2">
                <Label htmlFor="title">Titre</Label>
                <Input id="title" required value={form.title} onChange={(e) => set("title", e.target.value)} />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="summary">{isSE ? "Texte affiché sur l'accueil (un paragraphe)" : "Chapeau (sous le titre)"}</Label>
                <Textarea id="summary" maxLength={1000} value={form.summary} onChange={(e) => set("summary", e.target.value)} />
              </div>
              <div className="flex flex-col gap-2">
                <span className="text-sm font-medium">Texte de la page</span>
                {isOrgChart && <p className="text-xs text-muted-foreground">Affiché seulement tant qu&apos;aucune image d&apos;organigramme n&apos;est téléversée.</p>}
                {isProcedure && <p className="text-xs text-muted-foreground">Chaque intertitre devient une entrée du sommaire de la fiche, dans cet ordre.</p>}
                <RichTextEditor label="Texte de la page (français)" value={form.bodyHtml} onChange={(html) => set("bodyHtml", html)} />
              </div>
            </div>
          ) : (
            <div key="en" className="flex flex-col gap-6">
              <p className="rounded-2xl bg-muted px-4 py-3 text-sm text-muted-foreground">
                {page.englishAuto
                  ? "Traduction automatique (DeepL), refaite à chaque modification du français. Relisez-la : dès que vous la corrigez, elle devient définitive."
                  : "Laissez le titre et le texte vides : la traduction automatique (DeepL) les remplira juste après l'enregistrement. Une version saisie ou corrigée ici n'est jamais écrasée."}
              </p>
              <div className="flex flex-col gap-2">
                <Label htmlFor="title-en">Title</Label>
                <Input id="title-en" lang="en" value={form.titleEn} onChange={(e) => set("titleEn", e.target.value)} />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="summary-en">{isSE ? "Home page text (one paragraph)" : "Lead"}</Label>
                <Textarea id="summary-en" lang="en" maxLength={1000} value={form.summaryEn} onChange={(e) => set("summaryEn", e.target.value)} />
              </div>
              <div className="flex flex-col gap-2">
                <span className="text-sm font-medium">Page text</span>
                <RichTextEditor label="Texte de la page (anglais)" value={form.bodyHtmlEn} onChange={(html) => set("bodyHtmlEn", html)} />
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <div className="flex flex-col gap-6">
        {!isProcedure && <Card>
          <CardContent className="flex flex-col gap-5">
            <ImageField
              label={isSE ? "Photo du Secrétaire exécutif" : texts.image}
              value={form.imageUrl}
              onChange={(image) => setForm((current) => ({ ...current, imageKey: image?.key ?? null, imageUrl: image?.url ?? null }))}
              hint={texts.hint}
              aspect={texts.aspect}
              fit={texts.fit}
            />
            <div className="flex flex-col gap-2">
              <Label htmlFor="image-name">{texts.name}</Label>
              <Input id="image-name" maxLength={120} placeholder={texts.namePlaceholder} value={form.imageName} onChange={(e) => set("imageName", e.target.value)} />
              <p className="text-xs text-muted-foreground">{texts.nameHint}</p>
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="image-role">{texts.role}</Label>
              <Input id="image-role" maxLength={200} placeholder={texts.rolePlaceholder} value={form.imageRole} onChange={(e) => set("imageRole", e.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="image-role-en">{texts.roleEn}</Label>
              <Input id="image-role-en" lang="en" maxLength={200} placeholder={texts.roleEnPlaceholder} value={form.imageRoleEn} onChange={(e) => set("imageRoleEn", e.target.value)} />
            </div>
          </CardContent>
        </Card>}
        <Button type="submit" size="lg" disabled={save.isPending || !form.title.trim()} className="xl:sticky xl:top-6">
          {save.isPending ? <Loader2Icon className="animate-spin" aria-hidden="true" /> : <SaveIcon data-icon="inline-start" aria-hidden="true" />}
          Enregistrer et publier
        </Button>
      </div>
    </form>
  );
}

export function PageEditorPage() {
  const { slug = "" } = useParams();
  const page = useQuery({ queryKey: ["page", slug], queryFn: () => unwrap(api.GET("/admin/pages/{slug}", { params: { path: { slug } } })) });

  return (
    <section className="space-y-6">
      <Button asChild variant="ghost" size="sm"><Link to="/pages"><ArrowLeftIcon data-icon="inline-start" aria-hidden="true" />Pages institutionnelles</Link></Button>
      <PageTitle title={PAGE_INFO[slug]?.label ?? page.data?.title ?? "Page"} description={PAGE_INFO[slug]?.where} />
      {page.isPending && <Skeleton className="h-96 rounded-3xl" />}
      {page.isError && <p className="text-sm text-destructive">{page.error.message}</p>}
      {/* `key` : formulaire réinitialisé si l'on passe d'une page à l'autre. */}
      {page.data && <Editor key={page.data.slug + page.data.updatedAt} page={page.data} />}
    </section>
  );
}
