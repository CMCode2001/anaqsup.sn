# Plan de migration depuis WordPress

## Reprise automatique des actualités et des documents (14/10/2026)

Décisions de l'ANAQ-Sup du 14/10/2026 :

- **Actualités** : les 548 articles sont repris depuis le **flux RSS public**, qui est paginé et donne le texte complet, la date et les catégories. L'API REST reste fermée. L'image principale est lue sur la page de chaque article (`img.wp-post-image`, variante de 1 600 pixels au plus) puis copiée dans la médiathèque.
- **Documents** : les PDF des pages Textes réglementaires, Référentiels et Publications sont copiés dans la médiathèque, avec leur vignette (première page).
  - Le titre est le texte du lien.
  - La date est celle écrite dans le titre ou le nom du fichier, sinon le mois de dépôt.
  - Le type dépend de la page et du titre : un guide reste un guide.
- **Catégories** :
  - Partenariat, Accord, Visite → Coopération & partenariats ;
  - Publication (rapports annuels, discours du SE…) → Communiqués ;
  - Actualité, Événements, Non classée → Vie institutionnelle.
- **Traduction** : seules les actualités des 12 derniers mois sont traduites automatiquement (`anaqsup.translation.news-max-age-months`). Les archives restent en français, car le quota DeepL ne les couvrirait pas.
- Les doublons de l'ancien site (même titre et même date, ou même document déposé deux fois) ne sont repris qu'une fois.
- L'import est **relançable** : un article déjà repris (identifiant WordPress) ou un document déjà repris (adresse du PDF) est ignoré.
- Les adresses d'origine des articles sont conservées, pour les redirections.

Lancement ponctuel (le programme affiche un bilan puis s'arrête) :

```bash
docker compose -p anaqsup-sn --env-file devops/env/dev.env -f devops/compose/compose.yaml -f devops/compose/compose.dev.yaml run --rm --no-deps backend --anaqsup.import.wordpress-url=https://anaqsup.sn
```

Code : `backend/…/integration/wordpress/` (lecture de l'ancien site) et `content/LegacyContentImport` (enregistrement).

Les points suivants restent à faire :

- Les images placées **dans** le texte des articles ne sont pas reprises : la liste blanche du HTML n'accepte pas les images.
- Les liens internes vers l'ancien site (`anaqsup.sn/wp-content/…`) restent tels quels jusqu'aux redirections.
- Les pages institutionnelles et les habilitations et accréditations ne sont pas concernées (voir ci-dessous).

## Inventaire (13/10/2026)

Source : les sitemaps All in One SEO du site actuel. Le détail ligne par ligne est dans [`inventaire-urls-legacy.csv`](inventaire-urls-legacy.csv), généré par `devops/scripts/legacy-redirects.py inventory`.

| Type | URLs | Cible | Statut |
|---|---|---|---|
| Articles (`post`) | 548 | `/actualites/{slug}` (slug conservé à l'import) | AUTO |
| Étiquettes | 286 | `/actualites` | AUTO |
| Catégories | 11 | `/actualites?categorie=…` | AUTO |
| Pages | 25 | correspondance éditoriale explicite | 14 AUTO, 5 À VALIDER, 6 GONE (410) |
| **Accréditations** | **1 052** | `/formations/{slug}` | **À VALIDER après curation** |
| **Habilitations** | **229** | `/etablissements/{slug}` | **À VALIDER après curation** |
| Divers | 2 | — | À VALIDER |
| **Total** | **2 153 uniques** (2 157 entrées : le sitemap WordPress contient 4 doublons) | | |

## Pourquoi 1 281 fiches demandent une curation

Les URLs montrent des données hétérogènes :

- des doublons probables : `isae-ucao-…`, `isaet-…`, `isaet-ucao-…` ;
- des **rapports d'évaluation** saisis comme des « accréditations » ;
- des établissements identifiés par leur seul sigle (`uasz`, `iseg`).

Une copie à l'identique importerait ces erreurs dans un registre qui se veut **opposable**.

## Démarche

1. **Export** : export WXR (Outils → Exporter), puisque l'API REST WordPress est fermée (401). Les PDF de `wp-content/uploads` sont récupérés par SFTP.
2. **Extraction** : un script (module `integration.wordpress`) produit un tableur de curation par fiche, avec l'ancien slug, le titre, le type présumé, l'établissement présumé et les documents liés.
3. **Curation** par le référent données ANAQ-Sup :
   - fusionner les doublons ;
   - qualifier chaque fiche (établissement, formation ou rapport) ;
   - rattacher les décisions et leurs dates ;
   - attribuer le slug définitif.
4. **Import** dans le registre du portail (`origin = REPRISE_WORDPRESS`) : chaque décision reprise est saisie par l'outil d'import, puis **publiée par le référent données** (quatre yeux). Les anciens slugs vont dans `establishment_alias (kind = ANCIEN_SLUG)`.
5. **Redirections** : le tableur de curation met à jour la colonne `cible` de l'inventaire et passe le statut à `VALIDE`. On lance ensuite `make redirects`.
6. **Contrôle** : la compilation `--strict` doit réussir, ce qui garantit 0 URL sans cible, avant la bascule DNS.
