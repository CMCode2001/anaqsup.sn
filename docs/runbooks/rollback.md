# Runbook : retour arrière

## Automatique

`deploy.sh` revient seul à la release précédente si les healthchecks échouent dans les 5 minutes.

## Manuel

```bash
/opt/anaqsup/current/scripts/rollback.sh production
```

Pour revenir à une version précise :

```bash
/opt/anaqsup/current/scripts/rollback.sh production v1.3.2
```

Les 5 dernières releases sont conservées sur le serveur, et les images restent disponibles dans GHCR.

## La règle qui rend le rollback possible : expand/contract

Le rollback ramène le **code** en N-1, mais le **schéma** reste en N, car Flyway ne revient jamais en arrière. Toute migration doit donc être compatible avec la version précédente de l'application.

| Changement | Release N (expand) | Release N+1 (contract) |
|---|---|---|
| Renommer une colonne | Ajouter la nouvelle colonne, écrire dans les deux, migrer les données | Supprimer l'ancienne |
| Rendre une colonne obligatoire | Ajouter la colonne en nullable avec une valeur par défaut | Poser `NOT NULL` |
| Supprimer une table | Cesser de l'utiliser | La supprimer |

Une PR qui viole cette règle est refusée en revue (voir la case du modèle de PR).

## Si les données sont corrompues

Restaurer la sauvegarde `avant-<version>` créée automatiquement : voir [sauvegarde-restauration.md](sauvegarde-restauration.md).
