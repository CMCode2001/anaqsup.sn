# Reprise des habilitations et accréditations de l'ancien site

Le site WordPress actuel publie **1 281 lignes** : 1 052 accréditations (formations) et 229 habilitations (établissements).
Une copie à l'identique importerait ses erreurs (voir [le plan de migration](../plan-migration-wordpress.md)) : les lignes
ont donc été lues, contrôlées une à une, puis réparties en deux groupes.

| Groupe | Lignes | Où elles vont |
|---|---|---|
| **Importées** (aucune anomalie) | 901 | `decision` (statut `VALIDE`), avec leurs formations, leurs rapports et leurs établissements |
| **À vérifier** (au moins une anomalie) | 380 | `import_line` seulement (statut `A_VERIFIER`) : elles ne sont **pas** des décisions et n'apparaissent pas sur le site |

Toutes les lignes lues sont gardées dans `import_line`, telles qu'elles sont sur l'ancien site, avec leur résultat : on peut
toujours savoir ce qui est dans la base et ce qui n'y est pas.

## Fichiers

| Fichier | Rôle |
|---|---|
| `registre-import.sql` | Reprise des 901 lignes (160 établissements dont 12 composantes, 686 formations, 489 rapports, 877 décisions) et des 1 281 lignes de l'ancien site. |
| `registre-a-verifier.csv` | Les 380 lignes à vérifier, avec le motif (UTF-8, séparateur « ; », s'ouvre dans Excel). |

## Lancer la reprise

Prérequis : la migration `V17__registre_decisions_rapports.sql` est appliquée (au démarrage du backend).

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f docs/migration/registre/registre-import.sql
```

- Le script est **rejouable** : les identifiants sont déterministes et chaque insertion ignore les doublons.
- Il s'exécute dans une seule transaction : en cas d'erreur, rien n'est écrit.
- Les avis affichés sur les fiches (`habilitation_avis`, `accreditation_avis`) sont recalculés par des déclencheurs de la base à
  partir des décisions : il n'y a rien d'autre à faire pour que le site les affiche.
- Le test `RegistryImportIT` rejoue ce script sur une base PostgreSQL 18 vide et vérifie les totaux, l'absence de doublon
  au second passage, et la lecture par l'API publique.

## Règles appliquées

- **Historique conservé** : un programme refusé en 2019 puis accepté en 2025 a deux décisions ; la plus récente fait le statut
  courant. Une décision n'est jamais écrasée.
- **Établissement et composantes** : UFR, facultés et écoles doctorales sont des établissements enfants (`parent_id`),
  du même type (public ou privé) que leur établissement. Les anciennes écritures d'un nom (fautes, majuscules, sigle collé au
  nom) sont des alias (`establishment_alias`).
- **Date du Conseil scientifique** : l'ancien site ne donne que l'année. `decided_on` reste vide et la fin de validité est
  **estimée** à partir du 31 décembre de l'année (`validUntilEstimated = true` dans l'API).
- **Fin de validité** = date du Conseil scientifique + durée − 1 jour. Durée : celle du rapport quand elle y est écrite (6 ans
  pour les écoles doctorales), sinon 5 ans (accréditation) ou 10 ans (habilitation).
- **Niveaux** : `LICENCE`, `MASTER`, `DOCTORAT`, `DUT`, `BTS` ou `AUTRE`, comme le contrat public. Une licence professionnelle est
  rangée dans `LICENCE`, un DES, un DU ou un diplôme d'ingénieur dans `AUTRE` ; l'intitulé garde la précision.
- **Rapports** : identifiés par leur adresse, avec la date de signature lue sur la page de garde du PDF. Un rapport absent du
  site reste privé.
- **Provisoire** : quand une ligne plus récente d'un établissement ou d'une formation est à vérifier, sa fiche porte
  `verification_en_cours` et l'API renvoie `provisional: true`.

## Motifs des 380 lignes à vérifier

Les plus fréquents (une ligne peut en cumuler plusieurs) : établissement ressemblant à un autre (114), rapport rattaché à un
autre programme (103), lien mort (84), type ou lieu contradictoires (49), avis contredit par le rapport (16), année antérieure
à la signature du rapport (15) ou à 2014 (12).
Le détail de chaque ligne est dans `registre-a-verifier.csv` ; une ligne corrigée par le bureau se saisit ensuite comme
une décision, dans le back-office.

## Régénérer le script

Le script est produit par l'outil de contrôle des données (`importer.export_portail`), qui relit le site actuel :

```bash
python -m importer.export_portail --cache <dossier de travail> --sortie docs/migration/registre
```

Il ne faut le régénérer que si l'ancien site a changé ; sinon le fichier versionné fait foi.
