import type { AdminSiteSettings } from "@anaqsup/api-client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowDownIcon, ArrowUpIcon, Loader2Icon, PlusIcon, RotateCcwIcon, SaveIcon, Trash2Icon } from "lucide-react";
import { type FormEvent, useState } from "react";

import { Button } from "@anaqsup/ui/components/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@anaqsup/ui/components/card";
import { Input } from "@anaqsup/ui/components/input";
import { Label } from "@anaqsup/ui/components/label";
import { Skeleton } from "@anaqsup/ui/components/skeleton";
import { toast } from "@anaqsup/ui/components/sonner";

import { PageTitle } from "@/components/PageTitle";
import { Textarea } from "@/components/Textarea";
import { api, unwrap } from "@/lib/api";

// Textes d'origine du portail : affichés en exemple, ils restent en place tant qu'aucun texte n'est saisi ici.
import enMessages from "../../../portail/messages/en.json";
import frMessages from "../../../portail/messages/fr.json";

type Messages = Record<string, Record<string, unknown>>;
const original = (messages: Messages, key: string) => {
  const [namespace = "", name = ""] = key.split(".");
  const value = messages[namespace]?.[name];
  return typeof value === "string" ? value : "";
};

const settingsKey = ["settings"] as const;

const NETWORKS = [
  { value: "facebook", label: "Facebook", placeholder: "https://www.facebook.com/…" },
  { value: "x", label: "X (Twitter)", placeholder: "https://x.com/…" },
  { value: "linkedin", label: "LinkedIn", placeholder: "https://www.linkedin.com/company/…" },
  { value: "youtube", label: "YouTube", placeholder: "https://www.youtube.com/…" },
] as const;
type Network = (typeof NETWORKS)[number]["value"];

/** Où chaque texte d'introduction s'affiche sur le site. */
const INTRO_WHERE: Record<string, string> = {
  "Home.approvalsLead": "Accueil — bloc « Habilitations & accréditations »",
  "Home.partnersLead": "Accueil — bloc « Partenaires »",
  "Events.lead": "Événements",
  "Resources.publicationsLead": "Ressources → Publications",
  "Resources.regulationsLead": "Ressources → Textes réglementaires",
  "Resources.standardsLead": "Ressources → Référentiels",
  "Resources.selfAssessmentLead": "Ressources → Guide d'auto-évaluation",
  "Approvals.lead": "Habilitations & accréditations",
  "Institutions.lead": "Habilitations → Établissements habilités",
  "Programmes.lead": "Accréditations → Formations accréditées",
};

type Link = { id: number; label: string; labelEn: string; description: string; descriptionEn: string; url: string };
let nextId = 0;

function Form({ settings, introKeys }: { settings: AdminSiteSettings; introKeys: string[] }) {
  const queryClient = useQueryClient();
  const [contact, setContact] = useState({
    phone: settings.contact.phone, email: settings.contact.email, address: settings.contact.address, addressEn: settings.contact.addressEn ?? "",
  });
  const [social, setSocial] = useState<Record<Network, string>>(() => {
    const urls = { facebook: "", x: "", linkedin: "", youtube: "" };
    for (const link of settings.social) urls[link.network] = link.url;
    return urls;
  });
  const [links, setLinks] = useState<Link[]>(() => settings.usefulLinks.map((l) => ({
    id: nextId++, label: l.label, labelEn: l.labelEn ?? "", description: l.description ?? "", descriptionEn: l.descriptionEn ?? "", url: l.url,
  })));
  const [intros, setIntros] = useState<Record<string, { fr: string; en: string }>>(() =>
    Object.fromEntries(introKeys.map((key) => [key, { fr: settings.intros[key]?.fr ?? "", en: settings.intros[key]?.en ?? "" }])));
  const setLink = (id: number, field: keyof Omit<Link, "id">, value: string) =>
    setLinks((current) => current.map((l) => (l.id === id ? { ...l, [field]: value } : l)));
  const moveLink = (index: number, offset: -1 | 1) => setLinks((current) => {
    const next = [...current];
    [next[index], next[index + offset]] = [next[index + offset]!, next[index]!];
    return next;
  });

  const save = useMutation({
    mutationFn: () => unwrap(api.PUT("/admin/settings", {
      body: {
        contact: { phone: contact.phone, email: contact.email, address: contact.address, addressEn: contact.addressEn.trim() || null },
        social: NETWORKS.filter((n) => social[n.value].trim()).map((n) => ({ network: n.value, url: social[n.value].trim() })),
        usefulLinks: links.map((l) => ({
          label: l.label, labelEn: l.labelEn.trim() || null, description: l.description.trim() || null,
          descriptionEn: l.descriptionEn.trim() || null, url: l.url.trim(),
        })),
        intros: Object.fromEntries(Object.entries(intros)
          .filter(([, text]) => text.fr.trim() || text.en.trim())
          .map(([key, text]) => [key, { fr: text.fr.trim() || null, en: text.en.trim() || null }])),
      },
    })),
    onSuccess: (saved) => {
      queryClient.setQueryData(settingsKey, saved);
      toast.success("Paramètres enregistrés.", { description: "Le site affiche déjà les nouvelles valeurs." });
    },
    onError: (error) => toast.error(error.message),
  });
  const submit = (event: FormEvent) => { event.preventDefault(); save.mutate(); };
  const order = Object.keys(INTRO_WHERE);
  const sortedKeys = [...introKeys].sort((a, b) => order.indexOf(a) - order.indexOf(b));

  return (
    <form onSubmit={submit} className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-brand-ink">Coordonnées</CardTitle>
          <CardDescription>Bandeau du haut, pied de page et page Contact.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-5 md:grid-cols-2">
          <div className="flex flex-col gap-2">
            <Label htmlFor="phone">Téléphone</Label>
            <Input id="phone" type="tel" required maxLength={40} value={contact.phone} onChange={(e) => setContact({ ...contact, phone: e.target.value })} />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="email">Adresse électronique</Label>
            <Input id="email" type="email" required maxLength={120} value={contact.email} onChange={(e) => setContact({ ...contact, email: e.target.value })} />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="address">Adresse postale</Label>
            <Textarea id="address" required maxLength={300} value={contact.address} onChange={(e) => setContact({ ...contact, address: e.target.value })} />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="address-en">Adresse (anglais, facultative)</Label>
            <Textarea id="address-en" lang="en" maxLength={300} placeholder="Vide : l'adresse française s'affiche." value={contact.addressEn} onChange={(e) => setContact({ ...contact, addressEn: e.target.value })} />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-brand-ink">Réseaux sociaux</CardTitle>
          <CardDescription>Icônes du bandeau et du pied de page. Laissez une adresse vide pour masquer le réseau.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-5 md:grid-cols-2">
          {NETWORKS.map((network) => (
            <div key={network.value} className="flex flex-col gap-2">
              <Label htmlFor={`social-${network.value}`}>{network.label}</Label>
              <Input id={`social-${network.value}`} type="url" pattern="https://\S+" maxLength={300} placeholder={network.placeholder}
                     value={social[network.value]} onChange={(e) => setSocial({ ...social, [network.value]: e.target.value })} />
            </div>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-brand-ink">Liens utiles</CardTitle>
          <CardDescription>Colonne « Liens utiles » du pied de page, dans cet ordre (12 au plus).</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {links.map((link, index) => (
            <fieldset key={link.id} className="grid gap-4 rounded-2xl border p-4 md:grid-cols-[auto_minmax(0,1fr)_minmax(0,1fr)_auto]">
              <legend className="sr-only">Lien {index + 1}</legend>
              <div className="flex gap-1 md:flex-col">
                <Button type="button" variant="ghost" size="icon-sm" aria-label={`Monter ${link.label || "le lien"}`} disabled={index === 0} onClick={() => moveLink(index, -1)}><ArrowUpIcon aria-hidden="true" /></Button>
                <Button type="button" variant="ghost" size="icon-sm" aria-label={`Descendre ${link.label || "le lien"}`} disabled={index === links.length - 1} onClick={() => moveLink(index, 1)}><ArrowDownIcon aria-hidden="true" /></Button>
              </div>
              <div className="flex flex-col gap-3">
                <Input aria-label="Libellé" placeholder="Libellé (MESRI)" required maxLength={80} value={link.label} onChange={(e) => setLink(link.id, "label", e.target.value)} />
                <Input aria-label="Description" placeholder="Description (facultative)" maxLength={200} value={link.description} onChange={(e) => setLink(link.id, "description", e.target.value)} />
                <Input aria-label="Adresse du lien" type="url" placeholder="https://" required maxLength={300} value={link.url} onChange={(e) => setLink(link.id, "url", e.target.value)} />
              </div>
              <div className="flex flex-col gap-3">
                <Input aria-label="Libellé (anglais)" lang="en" placeholder="Label (English, optional)" maxLength={80} value={link.labelEn} onChange={(e) => setLink(link.id, "labelEn", e.target.value)} />
                <Input aria-label="Description (anglais)" lang="en" placeholder="Description (English, optional)" maxLength={200} value={link.descriptionEn} onChange={(e) => setLink(link.id, "descriptionEn", e.target.value)} />
              </div>
              <Button type="button" variant="ghost" size="icon" aria-label={`Retirer ${link.label || "le lien"}`} onClick={() => setLinks(links.filter((l) => l.id !== link.id))}>
                <Trash2Icon aria-hidden="true" />
              </Button>
            </fieldset>
          ))}
          <Button type="button" variant="outline" size="sm" className="w-fit" disabled={links.length >= 12}
                  onClick={() => setLinks([...links, { id: nextId++, label: "", labelEn: "", description: "", descriptionEn: "", url: "" }])}>
            <PlusIcon data-icon="inline-start" aria-hidden="true" />Ajouter un lien
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-brand-ink">Textes d&apos;introduction</CardTitle>
          <CardDescription>
            Phrase affichée sous le titre de chaque rubrique. Vide : le texte d&apos;origine du site (en grisé) reste affiché.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col divide-y">
          {sortedKeys.map((key) => {
            const text = intros[key] ?? { fr: "", en: "" };
            const changed = text.fr.trim() !== "" || text.en.trim() !== "";
            return (
              <div key={key} className="grid gap-3 py-5 first:pt-0 last:pb-0 md:grid-cols-2">
                <div className="flex items-center justify-between gap-2 md:col-span-2">
                  <span className="text-sm font-semibold">{INTRO_WHERE[key] ?? key}</span>
                  {changed && (
                    <Button type="button" variant="ghost" size="sm" onClick={() => setIntros({ ...intros, [key]: { fr: "", en: "" } })}>
                      <RotateCcwIcon data-icon="inline-start" aria-hidden="true" />Texte d&apos;origine
                    </Button>
                  )}
                </div>
                <Textarea aria-label={`${INTRO_WHERE[key] ?? key} (français)`} maxLength={600} placeholder={original(frMessages as Messages, key)}
                          value={text.fr} onChange={(e) => setIntros({ ...intros, [key]: { ...text, fr: e.target.value } })} />
                <Textarea aria-label={`${INTRO_WHERE[key] ?? key} (anglais)`} lang="en" maxLength={600} placeholder={original(enMessages as Messages, key)}
                          value={text.en} onChange={(e) => setIntros({ ...intros, [key]: { ...text, en: e.target.value } })} />
              </div>
            );
          })}
        </CardContent>
      </Card>

      <Button type="submit" size="lg" disabled={save.isPending} className="sticky bottom-6 w-fit self-end shadow-lg">
        {save.isPending ? <Loader2Icon className="animate-spin" aria-hidden="true" /> : <SaveIcon data-icon="inline-start" aria-hidden="true" />}
        Enregistrer et publier
      </Button>
    </form>
  );
}

export function SettingsPage() {
  const settings = useQuery({ queryKey: settingsKey, queryFn: () => unwrap(api.GET("/admin/settings")) });
  return (
    <section className="space-y-6">
      <PageTitle
        title="Paramètres du site"
        description="Coordonnées, réseaux sociaux, liens utiles et textes d'introduction des rubriques. Tout est publié dès l'enregistrement."
      />
      {settings.isPending && <Skeleton className="h-96 rounded-3xl" />}
      {settings.isError && <p className="text-sm text-destructive">{settings.error.message}</p>}
      {settings.data && <Form key={JSON.stringify(settings.data.settings)} settings={settings.data.settings} introKeys={settings.data.introKeys} />}
    </section>
  );
}
