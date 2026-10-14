# ADR-0003 : Monolithe modulaire avec Spring Modulith

- **Statut** : proposé
- **Date** : 2026-10-13

## Contexte

L'audit recommande à juste titre un backend modulaire plutôt que des microservices. Sans outillage, cependant, les « frontières métier explicites » s'érodent dès le troisième sprint.

## Décision

- Une application Spring Boot 4.1 (Java 25 LTS), package racine `sn.anaqsup.portail`.
- Chaque sous-package direct est un **module Spring Modulith** (`referentiel`, `decision`, `document`, `content`, `search`, `statistics`, `integration`, `audit`, et `shared`, qui est ouvert).
- Le package racine d'un module constitue son **API publique** (services, événements). Ses sous-packages (`domain`, `persistence`, `web`) sont **internes**.
- `ModularityTests` vérifie les frontières à chaque build, et une dépendance interdite fait échouer la CI.
- Les modules communiquent par **événements applicatifs** (ex. `DecisionPublished`), persistés par l'outbox JDBC de Spring Modulith. La revalidation du portail et le journal d'audit ne se perdent donc pas en cas de redémarrage.
- La persistance du côté public utilise **JdbcClient et du SQL explicite**, pas JPA. C'est une projection en lecture : le SQL reste lisible, les index sont maîtrisés, et le *lazy loading* ne peut pas provoquer de surprises.
- Le schéma est versionné par **Flyway** en *forward-only*, selon la discipline expand/contract (voir le runbook de rollback).

## Conséquences

- Un module pourra être extrait en service si un besoin réel apparaît. Les événements définissent déjà son contrat.
- La documentation des modules est générée automatiquement par Spring Modulith (`Documenter`).
