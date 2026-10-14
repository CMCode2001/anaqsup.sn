# Tester en local (projet Docker `anaqsup-sn`)

## Démarrer

```bash
make dev-stack
```

La commande copie `devops/env/dev.env.example` vers `devops/env/dev.env` si besoin, construit les images, démarre la pile et attend que tout soit *healthy*.

| Service | URL |
|---|---|
| Site public | http://localhost:3000 |
| API et documentation (Swagger) | http://localhost:8080/docs |
| Back-office | http://localhost:5174/`ADMIN_PATH`/ (valeur dans `devops/env/dev.env`). Toute autre adresse répond 404 |
| PostgreSQL | `docker exec -it anaqsup-sn-postgres-1 psql -U anaqsup -d anaqsup` |

Arrêter la pile en conservant les données :

```bash
make dev-down
```

Repartir de zéro en supprimant les volumes :

```bash
docker compose --project-name anaqsup-sn --env-file devops/env/dev.env -f devops/compose/compose.yaml -f devops/compose/compose.dev.yaml down --volumes
```

Reconstruire un seul service après une modification (ici le portail) :

```bash
docker compose --project-name anaqsup-sn --env-file devops/env/dev.env -f devops/compose/compose.yaml -f devops/compose/compose.dev.yaml up -d --build --wait portail
```

## Se connecter au back-office

1. Ouvrir http://localhost:5174/`ADMIN_PATH`/, avec la valeur de `ADMIN_PATH` dans `devops/env/dev.env`.
2. Se connecter avec `ADMIN_BOOTSTRAP_EMAIL` et `ADMIN_BOOTSTRAP_PASSWORD` du même fichier.
3. Ce mot de passe est **provisoire** : le back-office demande d'en choisir un nouveau, d'au moins 8 caractères.

Le premier administrateur n'est créé que si la base n'a **aucun** compte. Pour repartir de ce premier compte (mot de passe oublié en local, par exemple) :

```bash
docker exec anaqsup-sn-postgres-1 psql -U anaqsup -d anaqsup -c "DELETE FROM admin_account"
```

```bash
docker restart anaqsup-sn-backend-1
```

Rôles livrés :
- **Administrateur** : tous les droits, non modifiable ;
- **Chargé de communication** : actualités, événements, ressources documentaires, pages.

L'administrateur peut créer d'autres rôles dans « Rôles et permissions ».

**Ce que l'on gère déjà dans le back-office** :
- les pages institutionnelles, dont le mot du Secrétaire exécutif (photo, nom et fonction, texte d'accueil, texte complet, version anglaise) ;
- les partenaires du bandeau de l'accueil.

Une modification est en ligne **immédiatement** : le backend prévient le portail par un webhook signé (`PORTAL_REVALIDATE_URL`).
- En local, l'adresse par défaut vise le portail lancé par `pnpm dev` sur la machine.
- Avec le conteneur `portail`, mettre `PORTAL_REVALIDATE_URL=http://portail:3000/api/revalidate` dans `dev.env`.

Les images téléversées sont dans le volume `documents` (sous-dossier `media/`), déjà couvert par les sauvegardes.

Pour travailler sur le back-office à chaud, lancer `pnpm dev:backoffice` depuis `frontend/` : il s'ouvre sur http://localhost:5173, sans adresse secrète en local, et s'appuie sur l'API de Docker.

## Modifier le site et voir le résultat à chaud

Le site dans Docker est une version compilée : il ne change pas tant qu'on ne le reconstruit pas. Pour voir chaque modification aussitôt, lancez le portail en mode développement **à la place** du conteneur `portail`. Les deux utilisent le port 3000.

Arrêter le conteneur du site, en gardant la base et l'API :

```bash
docker stop anaqsup-sn-portail-1
```

Lancer le mode développement. pnpm 12 doit être installé une fois (`npm install -g pnpm@12.11.2`) :

```bash
cd frontend && pnpm install && pnpm dev
```

- Français : http://localhost:3000 ; anglais : http://localhost:3000/en.
- Le site lit l'API de Docker sur http://localhost:8080.

## Données de démonstration

Le profil Spring `demo`, activé **uniquement** par `compose.dev.yaml`, charge `backend/src/main/resources/db/demo/`. Tout y est **fictif** :

- 6 actualités, 4 événements (2 à venir et 2 passés) et 6 publications ;
- 5 pages institutionnelles : présentation, missions, gouvernance, organigramme et mot du Secrétaire exécutif ;
- des versions anglaises **partielles**, pour voir sur `/en` à la fois les contenus traduits et la mention « This content is only available in French » (la page Gouvernance, par exemple) ;
- 5 établissements et 11 formations, chacun avec la mention « Démo » dans son nom, dont les avis sont favorables, défavorables ou non renseignés.

Ces données sont remplacées par la reprise des contenus officiels (docs/migration).

## À essayer

- L'accueil : actualités à la une, événements, mot du Secrétaire exécutif, publications, puis le bloc habilitations et accréditations.
- `/recherche?q=universite demo` : la recherche ignore les accents.
- `/etablissements?avis=FAVORABLE` : filtre par avis.
- `/formations?niveau=MASTER` : filtre par niveau.
- `/actualites?categorie=assurance-qualite` : filtre par catégorie.

Tout ce que montre le site se modifie dans le back-office (publication directe, le portail est prévenu aussitôt) :

| Écran du back-office | Ce qu'il modifie sur le site |
|---|---|
| Actualités (et « Rubriques ») | Articles, article « À la une » du carrousel, rubriques et leur ordre |
| Événements | Agenda |
| Ressources documentaires | Documents PDF et leurs vignettes |
| Pages institutionnelles | Présentation, missions, gouvernance, organigramme, mot du SE, fiches procédures |
| Habilitations & accréditations | Établissements et formations, avis et année |
| Partenaires | Bandeau de l'accueil |
| Paramètres du site | Coordonnées, réseaux sociaux, liens utiles, textes d'introduction des rubriques |

Sans le webhook de revalidation, le cache de l'API est de 5 minutes et celui du portail de 2 à 10 minutes : appeler
directement l'API pour voir un changement tout de suite, par exemple `http://localhost:8080/api/v1/public/settings`.
