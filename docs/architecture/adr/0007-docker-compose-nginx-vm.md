# ADR-0007 : Docker Compose et Nginx sur machine virtuelle souveraine

- **Statut** : proposé. L'hébergeur reste à choisir par le comité.
- **Date** : 2026-10-13

## Contexte

Le trafic d'un portail institutionnel est modéré. L'équipe d'exploitation est réduite. La souveraineté des données publiques est un enjeu, et l'hébergement doit pouvoir se faire au Sénégal (datacenter national ou hébergeur local).

## Décision

- **Une VM Linux (Ubuntu 24.04 LTS) par environnement**. Dimensionnement de départ pour la production : 4 vCPU, 8 Go de RAM, 100 Go de SSD, plus un stockage externe pour les sauvegardes.
- Le serveur est provisionné par **Ansible** (`devops/ansible`) : durcissement SSH, pare-feu, fail2ban, mises à jour automatiques, Docker.
- **Docker Compose** orchestre la pile : `nginx`, `portail`, `backoffice`, `backend`, `keycloak`, `postgres`.
- **Nginx** est le seul point d'entrée (80/443). Il porte le TLS (Let's Encrypt), les en-têtes de sécurité, la limitation de débit, le cache des réponses API publiques (`proxy_cache_use_stale`) et les **redirections 301** des 2 157 URLs historiques.
- Deux réseaux Docker : `edge` et `data`. Le réseau `data` est `internal` : PostgreSQL n'est joignable ni depuis Internet ni depuis Nginx.
- Les images sont construites par GitHub Actions et publiées sur GHCR, avec des tags immuables (`vX.Y.Z`, `sha-xxxxxxx`).
- Le déploiement est un **répertoire de release immuable** (`/opt/anaqsup/releases/<version>`), activé par un lien symbolique `current`. Le rollback est automatique si les healthchecks échouent.

## Conséquences

- La VM est un point unique de défaillance, acceptable pour un RTO de 2 h. Une haute disponibilité (deux VM, PostgreSQL répliqué) est le palier suivant si le SLA l'exige.
- La migration vers Kubernetes reste possible : les images, healthchecks et variables d'environnement sont déjà compatibles.
