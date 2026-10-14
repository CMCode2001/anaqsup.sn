# ADR-0001 : Monorepo `frontend/` · `backend/` · `devops/` · `contracts/`

- **Statut** : proposé
- **Date** : 2026-10-13

## Contexte

La refonte réunit un portail Next.js, un back-office React, un backend Spring Boot, un contrat d'API et une infrastructure. Si ces éléments vivent dans des dépôts séparés, le contrat d'API et l'implémentation dérivent, et une *release* devient une coordination entre plusieurs dépôts.

## Décision

Un seul dépôt, organisé en quatre racines :

| Dossier | Contenu | Outillage |
|---|---|---|
| `contracts/` | Contrat OpenAPI 3.1, **source de vérité** de l'API publique | Redocly (lint) |
| `backend/` | Application Spring Boot unique (monolithe modulaire) | Maven wrapper |
| `frontend/` | Workspace pnpm : `apps/portail`, `apps/backoffice`, `packages/ui`, `packages/api-client` | pnpm |
| `devops/` | Compose, Nginx, Keycloak, scripts, Ansible, supervision | Docker, Ansible |

Le contrat est **contract-first** :

- `packages/api-client` est **généré** depuis `contracts/` (`pnpm gen:api`), et la CI échoue si le fichier généré n'est pas à jour ;
- le backend embarque le contrat et le sert dans Swagger UI ;
- les tests d'intégration valident les réponses contre ce contrat.

Les images Docker sont versionnées ensemble (même tag `vX.Y.Z`).

## Conséquences

- Une PR peut modifier le contrat, le backend et le front de façon atomique.
- La CI est découpée par chemin (`paths:`) pour ne pas tout reconstruire à chaque commit.
- `CODEOWNERS` attribue la revue par dossier.
