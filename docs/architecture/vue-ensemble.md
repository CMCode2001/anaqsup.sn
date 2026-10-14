# Vue d'ensemble de l'architecture

## 1. Contexte (C4, niveau 1)

```mermaid
flowchart LR
    public["Public<br/>étudiants, parents, employeurs"]
    ees["Établissements"]
    staff["Agents ANAQ-Sup<br/>rédaction, saisie et validation du registre"]
    partners["Partenaires<br/>ministère, plateformes (API ouverte)"]

    subgraph anaq["Plateforme anaqsup.sn (périmètre du projet)"]
        portail["Portail public"]
        api["API publique (lecture seule)"]
        bo["Back-office"]
    end

    public --> portail
    ees --> portail
    staff --> bo
    partners --> api
```

> e-ANAQ est un système distinct, hors du périmètre : la plateforme anaqsup.sn n'en dépend pas.

## 2. Conteneurs (C4, niveau 2)

```mermaid
flowchart TB
    internet((Internet))

    subgraph edge["Réseau edge"]
        nginx["Nginx 1.30<br/>TLS, en-têtes, rate-limit,<br/>cache API, redirections 301"]
        portail["portail<br/>Next.js 16<br/>:3000"]
        backoffice["backoffice<br/>SPA statique<br/>:8080"]
        backend["backend<br/>Spring Boot 4.1 / Modulith<br/>:8080 (API), :8081 (management)"]
    end

    subgraph data["Réseau data (interne, sans accès externe)"]
        pg[("PostgreSQL 18<br/>base anaqsup")]
        docs[("Volume documents<br/>PDF des publications")]
    end

    internet --> nginx
    nginx -- "anaqsup.sn" --> portail
    nginx -- "admin.anaqsup.sn" --> backoffice
    nginx -- "api.anaqsup.sn, admin /api" --> backend
    portail -- "fetch serveur (réseau interne)" --> backend
    backend --> pg
    backend --> docs
```

## 3. Publication d'un contenu (publication directe)

```mermaid
sequenceDiagram
    autonumber
    actor A as Agent ANAQ-Sup
    participant BO as back-office
    participant B as backend
    participant DB as PostgreSQL
    participant P as portail (Next.js)

    A->>BO: Saisit une actualité, un événement, une publication ou un avis
    BO->>B: POST /api/v1/admin/… (compte simple, session)
    B->>DB: INSERT / UPDATE (statut PUBLIE, date de publication)
    B->>P: POST /api/revalidate (webhook signé, tags concernés)
    P->>P: La page est régénérée à la prochaine visite
```

## 4. Plan de nommage DNS

| Hôte | Cible | Exposition |
|---|---|---|
| `anaqsup.sn` | portail | Public |
| `www.anaqsup.sn` | redirection 301 vers `anaqsup.sn` | Public |
| `api.anaqsup.sn` | backend `/api/v1/public/**`, documentation OpenAPI | Public, avec limitation de débit |
| `admin.anaqsup.sn` | back-office + `/api/**` | **Restreint** (IP/VPN), comptes simples |
| `staging.anaqsup.sn`, `api-staging.…`, `admin-staging.…` | Recette | Restreint (IP) et `noindex` |

## 5. Environnements

| | dev | staging | production |
|---|---|---|---|
| Déclencheur | poste du développeur | push sur `main` | tag `vX.Y.Z` avec approbation |
| Données | jeu de démonstration | copie anonymisée de la production | réelles |
| Indexation | — | `X-Robots-Tag: noindex` | oui |
| Fichier d'environnement | `devops/env/dev.env.example` | `/opt/anaqsup/shared/staging.env` | `/opt/anaqsup/shared/production.env` |

## 6. Modules du backend (Spring Modulith)

| Module | Responsabilité | Dépend de |
|---|---|---|
| `shared` | Erreurs RFC 9457, pagination, sécurité, configuration web | — (module ouvert) |
| `content` | Actualités et catégories, événements, publications, pages institutionnelles (publication directe) | `shared` |
| `referentiel` | Habilitations (établissements) et accréditations (formations) : avis et année saisis tels quels, recherche | `shared` |
| `statistics` | Nombre d'avis favorables | `referentiel`, `shared` |
| `integration` | Reprise WordPress, webhook de revalidation du portail | — |
| `audit` | Journal des actions sensibles (optionnel, à confirmer) | — |

Les frontières sont **vérifiées par un test** (`ModularityTests`). Une dépendance non déclarée fait échouer la CI.
