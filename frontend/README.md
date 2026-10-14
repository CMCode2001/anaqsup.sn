# Frontend : workspace pnpm

| Paquet | Rôle |
|---|---|
| `apps/portail` | Site public (Next.js 16, rendu serveur + ISR) |
| `apps/backoffice` | Back-office éditorial (Vite + React) |
| `packages/ui` | Design system : Tailwind CSS 4, **shadcn/ui** (Radix), **Magic UI**, **Motion** |
| `packages/api-client` | Types et client **générés** depuis `contracts/openapi` (ne pas éditer `schema.d.ts`) |

## Ajouter un composant

shadcn est configuré en mode monorepo : les composants sont installés dans `packages/ui`.

```bash
cd apps/portail && pnpm dlx shadcn@latest add dialog
```

Pour Magic UI :

```bash
cd packages/ui && pnpm dlx shadcn@latest add "https://magicui.design/r/<composant>.json" -p src/components/magicui
```

## Règles de performance du portail

Le budget de JavaScript initial est contrôlé en CI par `pnpm --filter @anaqsup/portail budget`.

| Règle | Pourquoi |
|---|---|
| Composants serveur par défaut | Zéro JavaScript : `Button`, `Card`, `Badge` et `AvisBadge` s'y rendent tels quels. |
| `Label`, `Separator` (Radix, `"use client"`) | À éviter sur le portail : un `<label>` ou un `<hr>` natif fait la même chose sans JS. |
| Magic UI côté portail | **CSS uniquement** : `grid-pattern`, `animated-shiny-text`, `shimmer-button`. |
| Magic UI à base de `motion` | Back-office seulement : `number-ticker`, `blur-fade`, `border-beam`. |
| Motion côté portail | Uniquement via `MotionProvider`, `Reveal` et `FadeIn` de `@anaqsup/ui/components/motion/motion` (LazyMotion `strict` + `m`, `reducedMotion="user"`), monté **au plus près** de la zone animée. |
| `Reveal` | Jamais au-dessus de la ligne de flottaison : cela retarderait le LCP. |

## Site bilingue (portail)

Le français est servi à la racine, l'anglais sous `/en`, avec des adresses traduites. Voir l'[ADR-0008](../docs/architecture/adr/0008-site-bilingue.md).

| À faire | Pourquoi |
|---|---|
| Textes dans `apps/portail/messages/fr.json` **et** `en.json` | `tsc` échoue si une clé manque dans une langue (`src/i18n/messages.check.ts`). |
| Nouvelle page : dossier sous `app/[locale]/`, puis ajout dans `pathnames` (`src/i18n/routing.ts`) | La table donne l'adresse anglaise et alimente le sitemap. |
| Liens : `Link` de `@/i18n/navigation`, avec le chemin interne (`<Link href="/actualites">`) | Rend `/actualites` ou `/en/news` selon la langue de la page. |
| Langue et traductions côté serveur : `getLocale`, `getTranslations` de `@/i18n/server` | Typés `"fr" \| "en"` et vérifiés à la compilation. |
| **Ne pas importer** `next-intl` ni `next-intl/navigation` dans le portail | Leurs composants client ajouteraient environ 10 Ko de JavaScript à chaque page. |
| Composant client : recevoir des libellés et des adresses déjà traduits | Aucune bibliothèque de traduction dans le navigateur. |
| Page dépendant de la requête (`searchParams`, contenus vivants) : `export const dynamic = "force-dynamic"` | Les autres pages sont générées à la première visite puis mises en cache (ISR). |
