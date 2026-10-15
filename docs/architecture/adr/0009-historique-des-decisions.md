# ADR-0009 : Historique des décisions derrière les listes d'habilitations et d'accréditations

- **Statut** : proposé, à valider par l'ANAQ-Sup (il complète l'[ADR-0004](0004-registre-public-gere-dans-le-portail.md))
- **Date** : 2026-10-15

## Contexte

L'ADR-0004 (13/10/2026) a retenu des **listes structurées** : un avis (favorable ou défavorable) et une année par établissement
et par formation, saisis tels quels, sans historique ni statut calculé.

La reprise de l'ancien site (1 281 lignes) montre ce que cette simplicité coûte :

- un programme refusé en 2019 puis accepté en 2025 existe en **deux lignes** sur l'ancien site, et il faut savoir laquelle fait
  foi ;
- chaque ligne porte un **rapport d'évaluation** (PDF) que la fiche doit pouvoir montrer ;
- les écoles doctorales sont jugées en bloc (un rapport, plusieurs doctorats, avis parfois différents) ;
- la **durée de validité** (5, 6 ou 10 ans) fait dire si une habilitation est encore en cours ;
- 380 lignes sont douteuses et ne doivent pas être publiées, sans pour autant être perdues.

## Décision

1. Les listes de l'ADR-0004 **ne changent pas pour le public** : `habilitationAvis`, `habilitationAvisYear`,
   `accreditationAvis` et `accreditationAvisYear` restent lus par le portail, la recherche et les statistiques, avec les mêmes
   valeurs possibles (`FAVORABLE`, `DEFAVORABLE`).
2. Ces quatre champs ne sont plus saisis : ils **reflètent la décision validée la plus récente**. Des déclencheurs de la base les
   recopient à chaque validation ou archivage (migration `V17`).
3. Chaque décision du Conseil scientifique est conservée (`decision`, `decision_program`) avec son rapport (`report`), sa
   durée et sa fin de validité (date du Conseil + durée − 1 jour, estimée à partir de l'année quand la date est inconnue).
4. Une décision naît en **brouillon**, se **valide** (publication directe, sans relecture obligatoire : l'ADR-0004 est
   respecté) ou se supprime. Une décision validée ne se modifie plus : on l'**archive** et on en crée une nouvelle.
5. UFR, facultés et écoles doctorales sont des **composantes** (`parent_id`) d'un établissement. La fiche d'un établissement
   donne ses composantes, et `/public/establishments/{slug}/programs` donne **toutes ses formations**, composantes comprises.
6. Les lignes de l'ancien site qui ont une anomalie sont gardées dans `import_line` (statut `A_VERIFIER`) et ne sont pas
   publiées.
7. La **saisie rapide** de la fiche (avis et année) continue de fonctionner : elle crée une décision validée.

## Conséquences

- Les contrats publics et d'administration ne reçoivent que des champs et des routes **en plus** ; les énumérations `Avis` et
  `ProgramLevel` sont inchangées, pour que les types du frontend restent valides. Les types générés du client sont régénérés.
- Une accréditation « conditionnelle » s'affiche `FAVORABLE` sur la fiche ; la décision de l'historique porte `CONDITIONNEL`.
  C'est un choix de présentation à confirmer avec l'ANAQ-Sup.
- Le back-office gagne une rubrique « Décisions » (brouillon, validation, archivage, historique). L'écran actuel des
  établissements et des formations n'a pas à changer pour que le site reste à jour.
- Depuis l'écran actuel des établissements et des formations, enregistrer un avis et une année crée bien une décision validée,
  mais **retirer un avis** (« Aucun avis publié ») ou l'antidater avant la dernière décision est refusé avec un message : il faut
  archiver la décision, pour ne pas perdre l'historique. L'écran devra le proposer.
- L'ADR-0004 dit « pas d'historique des avis » : cet ADR l'amende. Si l'ANAQ-Sup préfère s'en tenir à la liste simple, le
  module de décisions peut être retiré par une migration inverse, sans toucher aux champs publics.
