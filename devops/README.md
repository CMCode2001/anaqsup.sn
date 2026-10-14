# DevOps

Ce dossier est déposé **tel quel** sur le serveur à chaque release (`/opt/anaqsup/releases/<version>/`). Il ne doit donc contenir **aucun secret**.

| Dossier | Contenu |
|---|---|
| `compose/` | `compose.yaml` (staging et production), `compose.dev.yaml`, `compose.monitoring.yaml` |
| `env/` | Modèles de fichiers d'environnement (les vrais fichiers vivent dans `/opt/anaqsup/shared/`) |
| `nginx/` | `nginx.conf`, *templates* par hôte (envsubst), *snippets* (TLS, en-têtes, proxy), redirections 301 générées |
| `postgres/initdb/` | Création des rôles et bases au premier démarrage, avec le moindre privilège |
| `monitoring/` | Prometheus et Grafana (provisionnés) |
| `ansible/` | Provisionnement et durcissement de la VM |
| `scripts/` | `deploy`, `rollback`, `backup`, `restore`, `tls-issue`, `tls-renew`, `legacy-redirects.py` |

## Topologie réseau

```
Internet ──443──▶ nginx ─┬─▶ portail:3000       (réseau edge)
                         ├─▶ backoffice:8080
                         └─▶ backend:8080 ───▶ postgres:5432 (réseau data, internal: true)
```

PostgreSQL n'est joignable ni depuis Internet ni depuis Nginx. L'actuator (`:8081`) n'est exposé qu'à Prometheus.
