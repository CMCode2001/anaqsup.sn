# Runbook : déploiement

## Première installation d'un environnement

1. **Serveur** : une VM Ubuntu 24.04 LTS, accessible en SSH par clé.

   ```bash
   cd devops/ansible && ansible-galaxy collection install -r requirements.yml
   ansible-playbook -i inventories/production.ini playbooks/bootstrap.yml
   ```

2. **DNS** : des enregistrements A/AAAA vers la VM pour `anaqsup.sn`, `www`, `api` et `admin`.

3. **Secrets** : copier `devops/env/production.env.example` vers `/opt/anaqsup/shared/production.env`, appliquer `chmod 600`, puis générer chaque secret avec `openssl rand -base64 48`.
   - `ADMIN_PATH` : adresse d'accès du back-office, **non publiée** (`echo "espace-$(openssl rand -hex 8)"`). Le back-office répond seulement sous `https://admin.anaqsup.sn/<ADMIN_PATH>/`, toute autre adresse renvoie 404. À transmettre uniquement aux personnes autorisées.
   - `DEEPL_API_KEY` : clé du compte DeepL API de l'ANAQ-Sup, pour la traduction automatique des contenus en anglais ([ADR-0008](../architecture/adr/0008-site-bilingue.md)). Vide, les contenus non traduits restent en français sur le site anglais.
   - `ADMIN_BOOTSTRAP_EMAIL` et `ADMIN_BOOTSTRAP_PASSWORD` : premier administrateur, avec un mot de passe provisoire de 8 caractères minimum.

4. **Environnement GitHub** `production` (Settings → Environments) :
   - *reviewers* obligatoires ;
   - secrets `SSH_PRIVATE_KEY` et `SSH_KNOWN_HOSTS` (sortie de `ssh-keyscan` vérifiée hors ligne) ;
   - variables `SSH_HOST`, `PUBLIC_URL` et `API_URL`.

5. **Première release** : pousser un tag `v0.1.0`. La CI dépose `releases/v0.1.0` mais le démarrage échoue, faute de certificat. Sur le serveur :

   ```bash
   /opt/anaqsup/releases/v0.1.0/scripts/tls-issue.sh production --test
   ```

   Une fois le résultat validé, relancer sans `--test` :

   ```bash
   /opt/anaqsup/releases/v0.1.0/scripts/tls-issue.sh production
   ```

   Puis démarrer la release :

   ```bash
   /opt/anaqsup/releases/v0.1.0/scripts/deploy.sh production v0.1.0
   ```

6. **Premier administrateur** : ouvrir `https://admin.anaqsup.sn/<ADMIN_PATH>/` et se connecter avec `ADMIN_BOOTSTRAP_EMAIL`. Le mot de passe provisoire doit être changé dès la première connexion.
   - Créer ensuite les comptes nominatifs : Administrateur, Chargé de communication, ou un rôle créé pour l'occasion.
   - **Vider** `ADMIN_BOOTSTRAP_PASSWORD` dans `production.env` : il est ignoré dès qu'un compte existe, mais n'a plus de raison d'y figurer.

## Déploiement courant

C'est automatique : un tag `vX.Y.Z` déclenche les étapes suivantes.

1. Construction des 3 images, scan Trivy, SBOM.
2. Approbation dans l'environnement `production`.
3. `deploy.sh`, qui enchaîne :
   - une sauvegarde ;
   - `pull` ;
   - la bascule du lien `current` ;
   - `up --wait`, avec retour arrière automatique en cas d'échec.
4. Le test de bout en bout (`/api/health` et contrat OpenAPI).

## Bascule depuis WordPress (jour J)

1. Compiler les redirections en mode strict, ce qui échoue s'il reste des URLs à valider :

   ```bash
   python3 -I devops/scripts/legacy-redirects.py compile --strict --csv docs/migration/inventaire-urls-legacy.csv --out devops/nginx/redirects
   ```

2. Abaisser le TTL DNS à 300 s la veille.
3. Geler les publications sur WordPress et faire la dernière synchronisation des contenus.
4. Basculer les enregistrements DNS.
5. Tester les 2 157 anciennes URLs : aucune ne doit renvoyer 404.
6. Soumettre le nouveau `sitemap.xml` à la Search Console.
7. Conserver WordPress en lecture seule, hors ligne publique, pendant 3 mois.
