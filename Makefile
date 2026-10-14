# Raccourcis de développement. `make help` pour la liste.
SHELL := /bin/bash
COMPOSE_DEV := docker compose --project-name anaqsup-sn --env-file devops/env/dev.env \
	-f devops/compose/compose.yaml -f devops/compose/compose.dev.yaml

.PHONY: help
help: ## Affiche cette aide
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

devops/env/dev.env:
	cp devops/env/dev.env.example $@

.PHONY: dev-infra
dev-infra: devops/env/dev.env ## PostgreSQL en local (applis lancées depuis l'IDE)
	$(COMPOSE_DEV) up -d --wait postgres

.PHONY: dev-stack
dev-stack: devops/env/dev.env ## Pile complète « anaqsup-sn » construite localement (sans Nginx/TLS)
	$(COMPOSE_DEV) up -d --build --wait --remove-orphans postgres backend portail backoffice

.PHONY: dev-down
dev-down: ## Arrête la pile locale (les volumes sont conservés)
	$(COMPOSE_DEV) down

.PHONY: backend
backend: ## Lance le backend (profil dev) sur :8080
	cd backend && SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

.PHONY: portail
portail: ## Lance le portail Next.js sur :3000
	cd frontend && pnpm dev

.PHONY: backoffice
backoffice: ## Lance le back-office Vite sur :5173
	cd frontend && pnpm dev:backoffice

.PHONY: api
api: ## Régénère le client TypeScript depuis contracts/openapi
	cd frontend && pnpm gen:api

.PHONY: test
test: ## Tous les contrôles de la CI, en local
	npx -y @redocly/cli@2.62.2 lint --config contracts/redocly.yaml
	cd backend && ./mvnw -B -ntp verify
	cd frontend && pnpm lint && pnpm typecheck && pnpm build && pnpm --filter @anaqsup/portail budget

.PHONY: redirects
redirects: ## Recompile les redirections 301 depuis l'inventaire validé
	python3 -I devops/scripts/legacy-redirects.py compile --csv docs/migration/inventaire-urls-legacy.csv --out devops/nginx/redirects
