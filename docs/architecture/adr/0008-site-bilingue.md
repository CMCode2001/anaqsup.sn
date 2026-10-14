# ADR-0008 : Site bilingue français / anglais

- **Statut** : accepté (décisions de l'ANAQ-Sup du 2026-10-13, traduction automatique décidée le 2026-10-14)
- **Date** : 2026-10-13, mise à jour le 2026-10-14

## Contexte

L'ANAQ-Sup veut un sélecteur « Français / English » dans le bandeau supérieur. Le public visé comprend des partenaires internationaux (CAMES, UNESCO, agences homologues). Les contenus sont saisis en français par l'équipe éditoriale.

## Décisions de l'ANAQ-Sup

| Sujet | Décision |
|---|---|
| Portée | **Bilingue au cas par cas** : toute l'interface est traduite ; chaque contenu peut avoir une version anglaise. |
| Contenu non traduit | **Traduit automatiquement par DeepL** juste après l'enregistrement (décision du 14/10). Tant que la traduction n'existe pas (clé absente, service indisponible), la version française est affichée avec la mention « This content is only available in French ». |
| Adresses | **Rubriques traduites** : `/en/news`, `/en/about/missions`, `/en/approvals-and-accreditations`… Le français reste à la racine, sans préfixe. |
| Libellés du sélecteur | « Français / English » : chaque langue est nommée dans sa propre langue. |
| Traduction des contenus | **DeepL, automatiquement** (décision du 14/10, qui remplace le bouton « Traduire en anglais » prévu le 13/10). La traduction reste **modifiable** dans le back-office ; une version corrigée n'est plus jamais écrasée. |

## Décisions techniques

**Portail (Next.js)**

- La bibliothèque **next-intl** (`next-intl/server`) gère les traductions de l'interface et l'aiguillage des langues (`src/proxy.ts`).
- Les textes sont dans `messages/fr.json` et `messages/en.json`. Une vérification de compilation (`src/i18n/messages.check.ts`) échoue si une clé manque dans une langue.
- La table des adresses est `src/i18n/routing.ts`. Ses clés sont les chemins internes, les dossiers de `app/[locale]`, en français.
- **Aucun code de traduction n'est envoyé au navigateur.**
  - Les liens passent par `@/i18n/navigation`. Ce `Link` est résolu sur le serveur et rend un `next/link` ordinaire.
  - Le `Link` de `next-intl/navigation` et l'entrée `next-intl` ne sont pas utilisés : leurs composants client embarqueraient le moteur de messages, soit environ 10 Ko par page.
  - Les deux seuls composants client concernés, le menu et le sélecteur, reçoivent des libellés et des adresses déjà traduits.
- Pas de détection automatique de la langue ni de cookie : tout le monde arrive en français et choisit l'anglais.
- **Référencement**
  - Chaque page déclare ses versions linguistiques (`hreflang`), et le sitemap liste les deux langues.
  - Une page anglaise qui affiche un contenu non traduit déclare la version française comme adresse canonique, pour éviter le contenu dupliqué.

**API et base de données**

- Les contenus éditoriaux (actualités, catégories, événements, publications, pages) ont des colonnes `*_en`, ajoutées par la migration V5.
  - Une contrainte `CHECK` impose qu'une version anglaise soit complète : titre **et** corps.
  - Il n'y a pas de table de traductions : avec deux langues seulement, les lectures restent simples.
- Les lectures publiques acceptent `lang=fr|en`. Le repli sur le français se fait **contenu par contenu**, jamais champ par champ : un titre anglais n'est jamais affiché sur un corps français.
- Chaque élément renvoie `lang`, la langue réellement servie. Une réponse sans `lang`, venant d'une API plus ancienne, est traitée comme du français : le site reste compatible N-1.
- Les noms d'établissements et de formations sont des noms propres. Ils ne sont pas traduits et sont balisés `lang="fr"`.

**Traduction automatique (migration V9)**

- Le module Contenu définit l'interface `MachineTranslator` ; le module Intégrations l'implémente avec l'API DeepL v2 (`DeeplTranslator`, anglais britannique `EN-GB`, HTML traduit avec `tag_handling=html` puis repassé par la liste blanche).
- `AutomaticTranslation` traduit les contenus sans version anglaise après chaque enregistrement (événement `ContentChanged`) et toutes les 15 minutes en rattrapage (service indisponible, quota atteint). Au plus 25 contenus par type et par passage. Le portail est prévenu contenu par contenu (webhook de revalidation).
- Chaque table traduisible a deux colonnes :
  - `en_auto` : la version anglaise vient de DeepL et n'a pas été retouchée ;
  - `en_source` : empreinte (MD5) du texte français traduit.
- Quand le français change, une traduction automatique est refaite. Dès qu'un éditeur modifie la version anglaise, `en_auto` passe à faux et la traduction n'est plus jamais écrasée.
- Clé DeepL côté serveur uniquement (`DEEPL_API_KEY`) ; seul le texte public est envoyé. Sans clé, rien n'est envoyé. Offre gratuite : 500 000 caractères par mois (clé terminée par « :fx »).

## Reste à faire

- Créer le compte DeepL API de l'ANAQ-Sup et renseigner `DEEPL_API_KEY` sur chaque environnement.
- Faire relire par l'ANAQ-Sup les textes anglais de l'interface (`messages/en.json`, orthographe britannique).
