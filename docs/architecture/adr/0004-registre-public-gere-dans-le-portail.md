# ADR-0004 : Habilitations et accréditations en listes structurées, publication directe

- **Statut** : accepté (décisions de l'ANAQ-Sup du 2026-10-13)
- **Date** : 2026-10-13

## Contexte

anaqsup.sn est un **site institutionnel**. Il présente l'ANAQ-Sup, ses missions, ses actualités, ses événements, ses publications et le mot du Secrétaire exécutif. Il publie aussi, de façon **structurée et pédagogique**, les habilitations et les accréditations.

Le périmètre est limité à anaqsup.sn : e-ANAQ est un système distinct, et le site n'en dépend pas.

Une première version proposait un registre avec statut calculé et décisions immuables. L'ANAQ-Sup ne l'a pas retenue.

## Décision

1. **Listes structurées** : chaque établissement porte un *avis d'habilitation* et chaque formation un *avis d'accréditation*.
   - Valeurs possibles : `FAVORABLE`, `DEFAVORABLE`, ou non renseigné.
   - L'**année** de l'avis est saisie avec lui.
   - Les deux sont saisis **tels quels** dans le back-office ; aucun statut n'est calculé.
2. Le public peut **rechercher et filtrer** : texte sans accents et tolérant aux fautes, sigle, avis, type d'établissement, niveau de formation.
3. Une rubrique explique **comment lire un avis**, avec la différence entre habilitation et accréditation et les procédures.
4. **Publication directe** : la personne qui saisit un contenu ou un avis le publie elle-même, sans relecture obligatoire.
5. Le modèle est appliqué par la migration `V4__listes_avis_publication_directe.sql`. L'avis et l'année vont toujours ensemble (contrainte `CHECK`).

## Conséquences

- Modèle simple, conforme au site actuel (« avis d'habilitation »).
- Il n'y a pas d'historique des avis. Un nouvel avis remplace l'ancien sur la fiche.
- La reprise WordPress alimente directement ces champs (docs/migration).
