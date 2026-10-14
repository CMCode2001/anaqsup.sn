# syntax=docker/dockerfile:1.7
# Contexte de build : frontend/
#   docker build -f apps/portail/Dockerfile -t anaqsup-portail frontend/

FROM node:26-alpine AS base
RUN npm install -g pnpm@12.11.2
WORKDIR /repo

FROM base AS deps
COPY pnpm-lock.yaml pnpm-workspace.yaml package.json ./
COPY apps/portail/package.json apps/portail/
COPY packages/ui/package.json packages/ui/
COPY packages/api-client/package.json packages/api-client/
RUN --mount=type=cache,id=pnpm,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile --filter @anaqsup/portail...

FROM deps AS build
COPY . .
# Valeurs FIGÉES au build (pages statiques, balises canoniques/Open Graph) : même image en recette
# et en production, la recette pointant volontairement ses canoniques vers la production (noindex).
ARG NEXT_PUBLIC_SITE_URL=https://anaqsup.sn
ENV NEXT_TELEMETRY_DISABLED=1 NEXT_PUBLIC_SITE_URL=${NEXT_PUBLIC_SITE_URL}
RUN pnpm --filter @anaqsup/portail build

FROM node:26-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 PORT=3000 HOSTNAME=0.0.0.0
ARG VERSION=dev
LABEL org.opencontainers.image.title="anaqsup-portail" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.source="https://github.com/CMCode2001/anaqsup.sn"
COPY --from=build --chown=node:node /repo/apps/portail/.next/standalone ./
COPY --from=build --chown=node:node /repo/apps/portail/.next/static ./apps/portail/.next/static
COPY --from=build --chown=node:node /repo/apps/portail/public ./apps/portail/public
# Cache ISR monté en volume (compose) : le répertoire doit appartenir à `node`.
RUN mkdir -p apps/portail/.next/cache && chown node:node apps/portail/.next/cache
USER node
EXPOSE 3000
HEALTHCHECK --interval=15s --timeout=3s --start-period=20s --retries=5 \
  CMD wget -qO- http://127.0.0.1:3000/api/health >/dev/null || exit 1
CMD ["node", "apps/portail/server.js"]
