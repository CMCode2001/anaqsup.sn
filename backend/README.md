# Backend : Spring Boot 4.1, Java 25, Spring Modulith

## Modules

Chaque sous-package de `sn.anaqsup.portail` est un module. Son package racine constitue son API publique (services, événements) ; ses sous-packages `domain`, `persistence` et `web` sont internes. Les frontières sont vérifiées par `ModularityTests`.

Modules :
- `content` : site institutionnel, avec les pages et les partenaires gérés dans le back-office ;
- `referentiel` : habilitations et accréditations ;
- `comptes` : comptes et rôles du back-office ([ADR-0006](../docs/architecture/adr/0006-comptes-simples-backoffice.md)) ;
- `medias` : images téléversées, stockées dans `documents/media` et servies au public avec un cache d'un an ;
- `integration` : webhook signé qui met le portail à jour après chaque modification.

Ils suivent les conventions suivantes :

- un DTO de contrat, jamais la ligne SQL ;
- `JdbcClient` et du SQL explicite ;
- les erreurs au format RFC 9457 ;
- `Cache-Control` et `ETag` sur les réponses publiques.

## Commandes

Lancer l'application en local (PostgreSQL via `make dev-infra`) :

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Lancer les tests unitaires, les frontières de modules et les tests d'intégration sur un vrai PostgreSQL 18 (Testcontainers) :

```bash
./mvnw verify
```

## Règles de schéma

- Flyway *forward-only*, migrations compatibles N-1 (voir [rollback](../docs/runbooks/rollback.md)).
- Un contenu publié a toujours une date de publication (contrainte `CHECK`).
- L'avis d'habilitation ou d'accréditation et son année vont toujours ensemble (contrainte `CHECK`).
- Une version anglaise (`*_en`) est complète ou absente : titre et corps vont ensemble (contrainte `CHECK`). L'API sert l'anglais avec `lang=en`, avec repli sur le français contenu par contenu ([ADR-0008](../docs/architecture/adr/0008-site-bilingue.md)).
