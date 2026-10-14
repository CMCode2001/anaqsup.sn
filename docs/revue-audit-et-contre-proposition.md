# Revue critique de l'audit et contre-proposition

| | |
|---|---|
| **Objet** | Avis technique sur la proposition « Audit du site ANAQ-Sup et proposition de refonte premium » |
| **Date** | 13 octobre 2026 |
| **Statut** | Proposition à valider en comité de pilotage |
| **Périmètre** | Portail public `anaqsup.sn` **uniquement** : site, registre, API publique, back-office, socle d'exploitation. e-ANAQ est un système distinct, **hors périmètre** : le portail n'en dépend pas. |

---

> ## Décisions de l'ANAQ-Sup (13/10/2026) — elles priment sur les propositions ci-dessous
>
> | Sujet | Décision |
> |---|---|
> | Nature du site | **Site institutionnel** : présentation, missions, actualités, événements, publications, mot du Secrétaire exécutif, actions. Il inclut une **publication structurée** des habilitations et accréditations. |
> | Périmètre | **anaqsup.sn uniquement**. e-ANAQ est un système distinct, hors périmètre. |
> | Menu | L'ANAQ-Sup ▾ (Présentation, Missions, Gouvernance, Organigramme) · Actualités ▾ (Toutes les actualités, Coopération & partenariats, Communiqués, Événements) · Habilitations & accréditations · Ressources documentaires ▾ (Publications, Textes réglementaires, Référentiels, Guide d'auto-évaluation) · Contact. Sous-menus en carte au survol ; **chaque sous-élément a sa propre page**. |
> | Bandeau supérieur | Téléphone et e-mail ; Facebook, X, LinkedIn, YouTube (nouvel onglet) ; sélecteur **Français / English**. |
> | Langues | **Bilingue au cas par cas**, adresses anglaises traduites (`/en/news`), traduction des contenus par **DeepL relue par l'éditeur** ([ADR-0008](architecture/adr/0008-site-bilingue.md)). |
> | Pied de page | Bande drapeau, liens utiles MESRI, CAMES, CAMPUSEN, UNESCO. Fond anthracite uni. |
> | Accueil | Article **à la une** (seul, avec son image) → **mot du Secrétaire exécutif** (photo, nom et fonction, accroche, géré dans le back-office) → **agenda** des événements → publications → habilitations et accréditations → **partenaires** (bandeau qui défile, géré dans le back-office) |
> | Habilitations et accréditations | **Listes structurées**. Avis **favorable ou défavorable** et année, saisis tels quels, sans calcul de statut ni historique de décisions ([ADR-0004](architecture/adr/0004-registre-public-gere-dans-le-portail.md)). |
> | Publication | **Directe**, sans relecture obligatoire. |
> | Connexion au back-office | **Comptes simples** gérés par Spring Boot, sans Keycloak. Rôles **Administrateur** et **Chargé de communication**, l'administrateur pouvant créer d'autres rôles ; mot de passe provisoire à changer, 8 caractères minimum ; adresse d'accès non devinable ([ADR-0006](architecture/adr/0006-comptes-simples-backoffice.md)). |
> | Actualités | Article **à la une** (épinglé, sinon le plus récent) en grand avec son image, puis les autres en grille ; numéros de pages. |
> | Charte | **Couleurs du logotype uniquement** : or #C7B62E en couleur principale, titres et liens en or assombri #6E6414, fonds sombres **anthracite**. **Poppins** partout. Logo recadré sur ses marges blanches (accepté). |
> | UI | shadcn/ui, Magic UI, Framer Motion (`motion`), Tailwind. Boutons et champs en `rounded-full`, cartes arrondies avec **zoom au survol**, responsive. |
> | Retiré | Vérification d'authenticité des documents (QR code), registre à statut calculé, relecture par une seconde personne, Keycloak. |

---

## 1. Verdict

L'audit est **solide sur les principes** : séparation public/métier, DTO dédiés, API versionnée, priorisation P0/P1/P2 et honnêteté sur ses propres limites. Il reste cependant **déclaratif**. Il laisse ouvertes les trois décisions dont tout le reste dépend :

1. **le mode de rendu du front** (SEO) : la stack front en dépend ;
2. **la source de vérité des décisions** : la fiabilité du portail en dépend ;
3. **le périmètre réel de la reprise des données** : le planning en dépend.

Il ne s'appuie enfin sur aucune mesure du site actuel. Les constats ci-dessous ont été relevés le 13/10/2026 et changent sensiblement l'estimation : **16 à 25 jours ouvrés n'est pas tenable** pour le périmètre décrit (voir § 6).

---

## 2. Ce que l'audit fait bien, et qu'on conserve

| Point | Pourquoi c'est juste |
|---|---|
| Séparer la consultation publique de la gestion | Deux publics, deux niveaux de risque : l'API publique ne fait que lire, l'administration est isolée sur son propre hôte. |
| Ne jamais exposer les entités JPA | Le contrat d'API doit survivre aux évolutions du modèle interne. |
| Monolithe modulaire plutôt que microservices | C'est le bon dimensionnement pour une équipe réduite. |
| Ne pas afficher de chiffres inventés | La crédibilité d'un régulateur repose sur l'exactitude. |
| Ne pas signaler un statut par la seule couleur | Exigence d'accessibilité (WCAG 1.4.1). |
| Distinguer avis, décision et document | Il distingue bien les concepts. Nous allons plus loin en les modélisant (§ 4.3). |
| Règle « aucun P2 ne retarde un P0 » | C'est la bonne règle de pilotage. |

---

## 3. Constats mesurés sur le site actuel (13/10/2026)

L'audit n'a examiné que le contenu visible. Une inspection technique non intrusive (en-têtes HTTP, sitemaps, balises `generator`) révèle les points suivants.

| # | Constat | Preuve | Impact |
|---|---|---|---|
| C1 | Socle **WordPress + Elementor 3.14.1 + All in One SEO 4.4.6**, des versions d'extensions datant de 2023 | balises `<meta name="generator">` | Les extensions obsolètes sont la première cause de compromission des sites WordPress. |
| C2 | **XML-RPC actif** | `POST /xmlrpc.php` → `200` | Permet le brute-force amplifié (`system.multicall`) et la réflexion de pingbacks. |
| C3 | **Inscription publique** exposée et indexée | `/login/`, `/register/`, `/edit-profile/` dans le sitemap | Des comptes n'ont rien à faire sur un site institutionnel : c'est une surface d'attaque inutile. |
| C4 | **En-têtes de sécurité absents** | ni `Strict-Transport-Security`, ni `Content-Security-Policy`, ni `X-Frame-Options` | Le site est exposé au clickjacking et au downgrade HTTPS. |
| C5 | **2 157 URLs indexées**, dont **1 052 fiches « accréditations »** et **229 fiches « habilitation »** | sitemaps AIOSEO | C'est le capital SEO du site. Sans plan de redirections 301, il disparaît le jour de la bascule. |
| C6 | **Données hétérogènes** : doublons probables (`isae-ucao-…`, `isaet-…`, `isaet-ucao-…`), un *rapport d'évaluation* stocké comme une *accréditation*, des habilitations identifiées par sigle seul (`uasz`, `iseg`) | URLs des fiches | La reprise est un **chantier de qualité de données**, pas une simple copie. |
| C7 | **Pages parasites indexées** : `/test/`, `/accreditations-copy/`, `/acreditations/`, `/7320-2/`, `/company-overview/`, `/our-mission/` (restes du thème) | `page-sitemap.xml` | Ces pages donnent une image peu professionnelle dans les résultats Google. |
| C8 | **Versions linguistiques partielles** : EN, PT, AR | `/accueil-english/`, `/accueil-portugues/`, `/accueil-العربية/` | Le multilinguisme n'est pas un simple « P2 à définir » : un public international existe déjà, il faut décider ce qu'on garde. |
| C9 | **Taxonomie incohérente** : `partanariat`, `uncategorized` doublonnant `non-classe-fr` | `category-sitemap.xml` | La refonte éditoriale doit inclure une taxonomie. |
| C10 | API REST WordPress fermée (`401`) | `/wp-json/wp/v2/*` | C'est un bon point de sécurité. La migration passera par un export WXR et un accès administrateur. |
| C11 | **Métadonnées SEO non rédigées** : titre « ACCUEIL - ANAQSUP », et une `og:description` qui reprend tout le texte de la page (adresse, menus…) | balises `<title>` et `og:*` | Les aperçus Google et WhatsApp sont illisibles. Chaque page doit avoir un titre et une description rédigés. |

L'inventaire complet (2 153 URLs uniques, car le sitemap contient 4 doublons), avec une cible proposée pour chacune, est dans [`migration/inventaire-urls-legacy.csv`](migration/inventaire-urls-legacy.csv).

---

## 4. Les angles morts de l'audit

> **Dénomination.** L'audit parle d'« agence ». La dénomination officielle affichée par l'institution est **Autorité nationale d'Assurance Qualité de l'Enseignement supérieur, de la Recherche et de l'Innovation**. Le portail doit la reprendre à l'identique.

### 4.1 Le SEO conditionne la stack : il faut trancher maintenant

L'audit écrit que « React avec Vite peut convenir » et renvoie la question à plus tard. Pour ce portail, **une SPA Vite est un mauvais choix** :

- les fiches établissements et formations sont la principale valeur du site, et elles doivent être indexables au premier octet ;
- au Sénégal, **WhatsApp est le premier canal de diffusion** des liens. L'aperçu d'un lien (titre, image) exige des balises Open Graph rendues côté serveur. Avec une SPA, un parent qui partage « la fiche de la licence de mon fils » obtient un aperçu vide ;
- sur un mobile d'entrée de gamme en 3G/4G, attendre le JavaScript avant d'afficher un statut officiel dégrade l'expérience.

**Décision proposée : Next.js (React) avec rendu serveur et régénération incrémentale (ISR)**, invalidée à la demande quand une décision est publiée ([ADR-0002](architecture/adr/0002-nextjs-rendu-serveur-isr.md)). Cela reste du React, conformément à la stack imposée.

### 4.2 La source de vérité est le risque n° 1, pas un point à identifier

Aujourd'hui, les « accréditations » sont des articles WordPress saisis à la main, sans circuit de validation (C6). L'audit laisse ouverte la question « d'où viennent les décisions et qui les publie ». C'est pourtant ce qui fait la fiabilité d'un registre officiel.

**Proposition :** le portail anaqsup.sn est **la source de vérité du registre public** ([ADR-0004](architecture/adr/0004-registre-public-gere-dans-le-portail.md)). Il ne dépend d'aucun système externe.

- **Saisie** : un gestionnaire du registre saisit la décision dans le back-office. Elle reste un brouillon, invisible du public.
- **Publication** : un valideur, **forcément une autre personne**, la publie. La base de données refuse qu'on publie sa propre saisie : c'est la contrainte des quatre yeux.
- **Isolation** : l'administration est séparée sur `admin.anaqsup.sn`, accessible par liste d'IP ou VPN et protégée par la MFA. L'API publique, elle, ne fait que lire.

### 4.3 Un statut se calcule, il ne se saisit pas

Une décision publiée est un **fait juridique immuable**. Une erreur se corrige par un **rectificatif** qui référence la décision corrigée, jamais par une modification silencieuse. Cette règle est appliquée par un trigger PostgreSQL, pas seulement par convention.

Le statut affiché (« en vigueur », « suspendu », « expiré », « retiré », « non accordé », « aucune décision publiée ») est **calculé à une date donnée** à partir de l'historique des décisions, par un composant unique et testé (`CurrentStatusResolver`). Le portail affiche toujours *sur quelle décision* repose le statut.

### 4.4 L'authenticité des documents : le service public qui manque

La fraude aux attestations et la revendication abusive d'accréditations sont un problème réel de l'enseignement supérieur privé. L'audit n'en parle pas. Nous proposons :

- un **code de vérification** et un **QR code** apposés sur chaque document officiel publié, qui pointent vers `anaqsup.sn/verifier?code=…` ;
- l'**empreinte SHA-256** de chaque PDF, publiée sur sa page. Le visiteur peut glisser son fichier pour le comparer : le calcul se fait **dans le navigateur** (Web Crypto), sans téléversement, donc sans collecte de données ;
- à terme, une signature électronique PAdES si un prestataire de confiance national est disponible.

Un employeur, une ambassade ou un parent peut alors vérifier en dix secondes qu'une décision est authentique et en vigueur.

### 4.5 La recherche : PostgreSQL suffit

Les besoins de l'audit (tolérance aux fautes, variantes de noms) se traitent avec **PostgreSQL** : recherche plein texte en français, `unaccent` et similarité trigramme (`pg_trgm`), plus une **table de sigles** (UCAD, UGB, UASZ, ISM…), car les visiteurs cherchent par sigle. Le volume est d'environ 1 300 fiches : un moteur dédié (Elasticsearch, OpenSearch) serait un composant de plus à exploiter, sans gain mesurable ([ADR-0005](architecture/adr/0005-recherche-postgresql.md)).

### 4.6 Identité et accès : rien n'est spécifié

- *(proposition non retenue : Keycloak. Décision : comptes simples, voir [ADR-0006](architecture/adr/0006-comptes-simples-backoffice.md))* ;
- des rôles séparés : rédaction et validation éditoriales, saisie et validation du registre, administration technique sans droit sur les contenus ;
- des clients *client credentials* à portée minimale pour les futurs partenaires de l'API ;
- le back-office accessible uniquement sur `admin.anaqsup.sn`, avec une liste d'IP ou un VPN autorisés.

### 4.7 Conformité : le cadre sénégalais est absent

- **Loi n° 2008-12 sur la protection des données à caractère personnel** : déclarations auprès de la **CDP** (formulaire de contact, comptes du back-office, journaux) et mentions d'information ;
- l'**accessibilité** cible **WCAG 2.2 niveau AA**, avec une déclaration d'accessibilité publiée ;
- une **mesure d'audience auto-hébergée et sans cookie**, donc sans bandeau de consentement ni transfert de données vers des tiers ;
- un **hébergement souverain** au Sénégal à privilégier, par exemple au datacenter national ou chez un hébergeur local ([ADR-0007](architecture/adr/0007-docker-compose-nginx-vm.md)).

### 4.8 Le contexte réseau impose un budget de performance chiffré

Les seuils Core Web Vitals de l'audit sont justes mais insuffisants. On y ajoute :

- **JavaScript initial ≤ 150 Ko compressés** sur les fiches et ≤ 160 Ko sur l'accueil (+10 Ko arbitrés pour les animations Motion). C'est **contrôlé en CI** ; mesures actuelles : 136,6 Ko et 152 Ko ;
- des images en AVIF/WebP avec dimensions explicites (pour le CLS) et des polices auto-hébergées en sous-ensemble latin ;
- une **recherche qui fonctionne sans JavaScript** (formulaire `GET`) ; l'autocomplétion n'est qu'une amélioration progressive ;
- des réponses API publiques **cachables** (`Cache-Control`, `ETag`) et servies par le reverse proxy même si le backend est indisponible (`proxy_cache_use_stale`).

### 4.8 bis La palette de l'audit ne passe pas les contrastes

L'**or #C5A15A** proposé n'atteint que **2,44:1 sur fond blanc**, sous le minimum WCAG de 3:1, même pour un élément non textuel. Il est donc réservé aux fonds bleu nuit, où il atteint 6,01:1. Sur fond clair, on utilise une variante #8A6A2B (5,03:1). L'anneau de focus est en bleu #1F4E79 (8,66:1). Tous les couples sont vérifiés dans `packages/ui/src/styles/globals.css`.

### 4.9 L'exploitation est réduite à une liste de souhaits

L'audit cite « sauvegardes vérifiées » et « retour à une version précédente » sans dispositif. Ce dépôt fournit :

- **RPO 24 h et RTO 2 h**, avec une sauvegarde quotidienne, une sauvegarde avant chaque déploiement et un test de restauration mensuel ;
- un déploiement par *release* immuable (`releases/<version>` et un lien symbolique `current`) avec **rollback automatique** si les healthchecks échouent ;
- des migrations de schéma **expand/contract** : une version N-1 de l'application doit fonctionner sur le schéma N, sinon le rollback est impossible ;
- les environnements `dev`, `staging` (recette) et `production`, avec une approbation manuelle pour la production.

### 4.10 Le site actuel reste en ligne pendant la refonte

La refonte prendra plusieurs semaines. Pendant ce temps, les constats C1 à C4 restent exploitables. Il faut un **lot 0 de durcissement immédiat** du WordPress actuel (§ 6).

---

## 5. Contre-proposition : un registre national, pas un site vitrine

Le repositionnement consiste à passer **d'un site institutionnel** à un **registre officiel de l'assurance qualité**, doté d'un portail éditorial et d'une API ouverte.

| Produit | Hôte | Rôle | Priorité |
|---|---|---|---|
| **Registre** | `anaqsup.sn/etablissements`, `/formations`, `/decisions`, `/verifier` | Statuts officiels, historique des décisions, documents vérifiables | **P0** |
| **Portail éditorial** | `anaqsup.sn` | Missions, procédures, actualités, publications, textes | **P0** (pages), **P1** (actualités) |
| **API publique et données ouvertes** | `api.anaqsup.sn` | Contrat OpenAPI, flux de modifications pour les partenaires, export CSV/JSON | **P0** (lecture), **P2** (partenaires) |
| **Back-office** | `admin.anaqsup.sn` (accès restreint) | Rédaction, validation à deux personnes, documents | **P1** |
| **Identité** | `auth.anaqsup.sn` | SSO, MFA, clients machine | **P0** |

### 5.1 Stack retenue

| Couche | Choix | Version | ADR |
|---|---|---|---|
| Portail public | Next.js (App Router, SSR + ISR), React, TypeScript | 16.4 / 19.3 | [0002](architecture/adr/0002-nextjs-rendu-serveur-isr.md) |
| Back-office | React SPA (Vite), TanStack Query | 8.3 | [0002](architecture/adr/0002-nextjs-rendu-serveur-isr.md) |
| Design system | `@anaqsup/ui` : Tailwind CSS 4, **shadcn/ui** (Radix, monorepo), **Magic UI**, **Motion** (LazyMotion) | 4.3 / shadcn 4.21 / Motion 14 | — |
| Backend | Java 25 LTS, Spring Boot, Spring Modulith | 4.1 / 2.1 | [0003](architecture/adr/0003-monolithe-modulaire-spring-modulith.md) |
| Base de données | PostgreSQL (FTS, `pg_trgm`, `unaccent`) | 18 | [0005](architecture/adr/0005-recherche-postgresql.md) |
| Comptes du back-office | Comptes simples gérés par Spring Boot (décision ANAQ-Sup) | — | [0006](architecture/adr/0006-comptes-simples-backoffice.md) |
| Contrat d'API | OpenAPI 3.1 *contract-first* et client TypeScript généré | — | [0001](architecture/adr/0001-monorepo.md) |
| Exploitation | Docker Compose, Nginx, Let's Encrypt, GitHub Actions + GHCR | Nginx 1.30 | [0007](architecture/adr/0007-docker-compose-nginx-vm.md) |
| Observabilité | Actuator, Micrometer, Prometheus, Grafana | — | — |

### 5.2 Arborescence publique (URLs en français, stables)

```
/                                   Accueil : recherche, accès rapides, dernières décisions
/etablissements                     Registre des établissements (filtres conservés dans l'URL)
/etablissements/{slug}              Fiche : statut d'habilitation, historique, formations
/formations                         Registre des formations
/formations/{slug}                  Fiche : statut d'accréditation, décision de référence, documents
/decisions                          Journal des décisions publiées
/verifier                           Vérifier l'authenticité d'un document (code, QR ou fichier)
/procedures/habilitation            Fiche pratique : éligibilité, pièces, étapes, délais
/procedures/accreditation
/actualites  ·  /actualites/{slug}
/publications                       Rapports, référentiels, textes réglementaires
/anaq-sup                           Missions, gouvernance, organisation
/donnees-ouvertes                   Jeux de données et documentation de l'API
/contact · /mentions-legales · /confidentialite · /accessibilite
```

Le **slug** d'une fiche est son identifiant public : il ne change jamais. Un renommage crée un alias redirigé.

### 5.3 Ce qui change par rapport à l'API de l'audit

| Audit | Contre-proposition | Raison |
|---|---|---|
| `/api/v1/management/*` exposé à côté de l'API publique | **Remplacé** par `/api/v1/admin/*`, servi uniquement sur `admin.anaqsup.sn` (IP/VPN, MFA) | L'API publique (`api.anaqsup.sn`) reste en lecture seule (§ 4.2). |
| Pagination `page` et `size` | `size ≤ 100`, plus un **flux de modifications** `updatedSince` et `cursor` pour les partenaires | Évite l'aspiration page à page et permet la synchronisation incrémentale. |
| Format d'erreur « illustratif » | **RFC 9457** (`application/problem+json`), natif dans Spring | Le format est standard et outillé. |
| — | `Cache-Control` et `ETag` sur toutes les lectures publiques | Les réponses sont cachables par le proxy, le CDN et les clients. |
| — | `GET /documents/verify/{code}` | Sert l'authenticité des documents (§ 4.4). |
| — | `GET /{establishments,programs}/{slug}/status` | Le statut est calculé et adossé à sa décision (§ 4.3). |
| Contrat défini « avant les composants » | Contrat **versionné dans `contracts/`**, client TypeScript **généré** et validé en CI | Le front et le back ne peuvent pas diverger silencieusement. |

---

## 6. Plan de réalisation réaliste

L'estimation de l'audit (16 à 25 jours) suppose que la reprise des données est une copie et que les validations institutionnelles sont instantanées. Les constats C5 et C6 montrent le contraire. Il faut curer environ 1 300 fiches et 549 articles, et réécrire 2 157 URLs.

| Lot | Durée | Contenu | Livrables |
|---|---|---|---|
| **0. Durcissement du site actuel** | Semaine 1, en parallèle | Mise à jour du cœur et des extensions, désactivation de XML-RPC et de l'inscription publique, en-têtes de sécurité, suppression des pages parasites, sauvegarde | Site actuel sécurisé |
| **1. Cadrage** | 2 semaines | Modèle de données public, contrat OpenAPI v1, maquettes (accueil, fiches, recherche), règles de publication et rôles (saisie / validation), choix de l'hébergement | Contrat v1 gelé, maquettes validées, ADR acceptés |
| **2. Registre et socle** | 4 semaines | API publique, recherche, fiches, `/verifier`, saisie et validation des décisions dans le back-office, Keycloak, CI/CD, recette | Registre en recette |
| **3. Portail éditorial et back-office** | 3 semaines (chevauchement avec le lot 2) | Pages, actualités, publications, back-office avec validation, outils de migration | Contenus migrés en recette |
| **4. Recette et bascule** | 2 semaines | Curation des données avec le référent métier, tests WCAG, charge et sécurité (pentest), redirections 301 testées, bascule DNS | Mise en production |
| **Après le lancement** | — | Version anglaise, données ouvertes, API partenaires, signature électronique des documents | — |

**Total : environ 10 à 11 semaines** pour une mise en production. Cela suppose une équipe de 4 à 5 personnes (lead technique, développeur backend, 1 à 2 développeurs front, UX/UI à temps partiel, DevOps à temps partiel) et, **côté ANAQ-Sup**, un *product owner* et un **référent données** disponibles. Sans référent données, le lot 4 ne peut pas se terminer.

---

## 7. Gouvernance de la publication

| Action | Rédacteur | Valideur éditorial | Gestionnaire du registre | Valideur du registre | Admin technique |
|---|---|---|---|---|---|
| Rédiger une actualité | **R** | A | | | |
| Publier une actualité | | **R/A** | | | |
| Saisir une décision | | | **R** | | |
| Publier une décision | | | ✗ (sa propre saisie) | **R/A** | |
| Corriger une décision (rectificatif) | | | **R** | **A** | |
| Gérer les comptes | | | | | **R** |
| Modifier un contenu ou le registre | | | | | ✗ |

R = réalise, A = approuve. Toute publication est tracée dans le journal d'audit (qui, quoi, quand, avant/après).

---

## 8. Critères de réussite mesurables

| Indicateur | Cible |
|---|---|
| URLs historiques en 404 après la bascule | **0** sur 2 157 (test automatisé sur la table de redirections) |
| LCP au 75e percentile, mobile 4G | ≤ 2,5 s |
| JavaScript initial (accueil, fiches) | ≤ 150 Ko compressés |
| Documents officiels publiés avec code de vérification | 100 % |
| Accessibilité | WCAG 2.2 AA : audit manuel et Lighthouse ≥ 95 |
| Délai entre la publication d'une décision dans le back-office et sa visibilité sur le portail | < 1 min (revalidation ISR par tag) |
| Déploiement / rollback | < 15 min / < 5 min |
| Restauration de sauvegarde | testée chaque mois, procès-verbal archivé |

---

## 9. Décisions attendues du comité de pilotage

1. **Valider Next.js** (rendu serveur) pour le portail public.
2. **Désigner les agents** qui saisissent le registre et ceux qui le valident (jamais la même personne pour une même décision).
3. **Arrêter les langues** du lancement : FR et EN ? PT et AR abandonnés ou maintenus ?
4. **Choisir l'hébergement** (datacenter national ou hébergeur local) et valider le plan de sous-domaines.
5. **Nommer** un référent données et un valideur éditorial.
6. **Compléter la charte graphique.** Le logotype est intégré selon sa charte : fichier officiel, sur fond blanc, sans variante. Il faut encore la palette complète et un fichier vectoriel du logo (voir [charte](charte/README.md)).
7. **Autoriser le lot 0** sur le site actuel, dès cette semaine.
