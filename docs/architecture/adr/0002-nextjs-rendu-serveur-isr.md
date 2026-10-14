# ADR-0002 : Next.js (SSR + ISR) pour le portail, SPA Vite pour le back-office

- **Statut** : proposé
- **Date** : 2026-10-13

## Contexte

L'audit hésite entre React + Vite (SPA) et un rendu serveur. Or :

- 1 281 fiches (accréditations et habilitations) sont aujourd'hui indexées, et la valeur du portail réside dans leur trouvabilité ;
- les liens circulent surtout par WhatsApp, dont l'aperçu exige des balises Open Graph présentes dans le HTML initial ;
- une part importante de l'audience utilise un mobile sur un réseau 3G/4G.

Le back-office, lui, est authentifié, non indexé et très interactif.

## Décision

- **Portail** : Next.js 16 (App Router), React Server Components, `output: "standalone"` pour Docker.
  - Les fiches sont rendues à la première visite puis mises en cache (ISR, `revalidate = 3600`).
  - Le backend invalide les pages concernées (`revalidateTag`) dès qu'une décision est publiée, via un webhook signé sur `/api/revalidate`.
  - Les listes filtrées (`/formations?niveau=master`) sont rendues côté serveur, avec les filtres dans l'URL.
  - La recherche fonctionne sans JavaScript (formulaire `GET`), et l'autocomplétion s'y ajoute en amélioration progressive.
- **Back-office** : SPA React + Vite, TanStack Query, OIDC Keycloak. Elle est servie en fichiers statiques sur `admin.anaqsup.sn`.
- **Design system partagé** : `@anaqsup/ui`, qui regroupe les jetons CSS et les composants accessibles, dont `StatusBadge` (libellé et icône, jamais la couleur seule).

## Conséquences

- Le portail nécessite un runtime Node.js en production (un conteneur de plus qu'une SPA statique).
- Les Server Components réduisent le JavaScript envoyé : TanStack Query n'est **pas** utile sur le portail, seulement dans le back-office.
- Le build du portail ne dépend pas de l'API : `generateStaticParams` renvoie une liste vide, et les pages sont générées à la demande.
