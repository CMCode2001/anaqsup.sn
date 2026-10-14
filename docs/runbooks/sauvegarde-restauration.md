# Runbook : sauvegarde et restauration

| Objectif | Valeur |
|---|---|
| RPO (perte de données maximale) | 24 h, et 0 pour chaque déploiement grâce à la sauvegarde `avant-<version>` |
| RTO (remise en service) | 2 h |
| Rétention locale | 14 jours |
| Rétention hors site | 90 jours (règle de cycle de vie du stockage distant) |

## Contenu d'une sauvegarde

`/opt/anaqsup/backups/<horodatage>-<libellé>/` contient :

- `anaqsup.dump`, au format custom de `pg_dump` (contenus, avis, comptes et rôles du back-office) ;
- `documents.tar.gz`, l'archive du volume des PDF officiels ;
- `SHA256SUMS`, vérifié avant toute restauration.

La sauvegarde est quotidienne (cron Ansible, 02:30) et précède chaque déploiement en production. Elle est copiée hors site si `BACKUP_RCLONE_REMOTE` est renseigné.

## Restaurer

```bash
/opt/anaqsup/current/scripts/restore.sh production /opt/anaqsup/backups/20261013T023000Z-quotidienne
```

Le script :

1. vérifie les empreintes ;
2. demande de taper le nom de l'environnement ;
3. arrête les applications. Nginx continue de servir le cache des lectures publiques.
4. restaure les deux bases et les documents ;
5. redémarre la pile.

## Test mensuel obligatoire

Une sauvegarde qu'on n'a jamais restaurée n'est pas une sauvegarde. Chaque mois :

1. Copier la dernière sauvegarde de production sur la recette.
2. `restore.sh staging <dossier>`.
3. Vérifier une fiche, une décision et un document.
4. Consigner la date, la durée mesurée (à comparer au RTO) et l'opérateur dans le registre d'exploitation.
