# ADR-0005 : Recherche avec PostgreSQL (plein texte, unaccent, pg_trgm, sigles)

- **Statut** : proposé
- **Date** : 2026-10-13

## Contexte

Il faut retrouver « Université Cheikh Anta Diop » en tapant `ucad`, `universite cheikh anta` ou `cheick anta diop`. Le volume est d'environ 1 300 fiches et quelques centaines d'articles.

## Décision

- Une colonne générée `search_document tsvector`, construite avec la configuration `french` après `unaccent` (via un wrapper `IMMUTABLE`), et indexée en GIN.
- Un index trigramme (`gin_trgm_ops`) sur le nom normalisé pour tolérer les fautes de frappe (`similarity > 0.3`).
- Une table `establishment_alias` pour les sigles et les anciens noms, consultée en priorité : une correspondance exacte de sigle passe en tête.
- Un score combiné : `ts_rank_cd` + `similarity` + un bonus de correspondance de sigle.
- L'autocomplétion est limitée à 8 résultats, avec un *debounce* de 250 ms côté client et un rate-limit Nginx dédié.

## Conséquences

- Aucun composant supplémentaire à exploiter, sauvegarder ou sécuriser.
- Si le volume ou les besoins (facettes complexes, multilingue) dépassent PostgreSQL, le module `search` isole déjà l'implémentation et peut être branché sur OpenSearch.
