# syntax=docker/dockerfile:1.7
# Contexte de build : frontend/
#   docker build -f apps/backoffice/Dockerfile -t anaqsup-backoffice frontend/

FROM node:25-alpine AS build
RUN npm install -g pnpm@12.11.2
WORKDIR /repo
COPY pnpm-lock.yaml pnpm-workspace.yaml package.json ./
COPY apps/backoffice/package.json apps/backoffice/
COPY packages/ui/package.json packages/ui/
COPY packages/api-client/package.json packages/api-client/
RUN --mount=type=cache,id=pnpm,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile --filter @anaqsup/backoffice...
COPY . .
RUN pnpm --filter @anaqsup/backoffice build

FROM nginxinc/nginx-unprivileged:1.30-alpine
ARG VERSION=dev
LABEL org.opencontainers.image.title="anaqsup-backoffice" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.source="https://github.com/CMCode2001/anaqsup.sn"
# Gabarit traité au démarrage par l'image (envsubst) : ADMIN_PATH est obligatoire.
COPY apps/backoffice/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /repo/apps/backoffice/dist /usr/share/nginx/html
# Utilisateur non root (nginx, uid 101) : déjà celui de l'image nginx-unprivileged, rendu explicite ici.
USER 101
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --retries=5 CMD wget -qO- http://127.0.0.1:8080/healthz >/dev/null || exit 1
