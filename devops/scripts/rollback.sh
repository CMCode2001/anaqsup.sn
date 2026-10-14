#!/usr/bin/env bash
# Retour à une release précédente (images et configuration ; le schéma reste en N, cf. expand/contract).
#   ./rollback.sh <staging|production> [version]   (par défaut : la release précédente)
set -Eeuo pipefail
# shellcheck source-path=SCRIPTDIR source=common.sh
source "$(dirname "$(readlink -f "$0")")/common.sh"

ENV_NAME=${1:-}
require_env "$ENV_NAME"
ENV_FILE=$(env_file "$ENV_NAME")
CURRENT=$(current_release)

if [[ -n ${2:-} ]]; then
  TARGET="$BASE/releases/$2"
else
  TARGET=$(find "$BASE/releases" -mindepth 1 -maxdepth 1 -type d -printf '%T@ %p\n' \
    | sort -rn | cut -d' ' -f2- | grep -vxF "$CURRENT" | head -n1 || true)
fi
[[ -n $TARGET && -d $TARGET ]] || die "aucune release cible trouvée"

exec 9>"$BASE/.deploy.lock"
flock -n 9 || die "un déploiement est en cours"

log "Retour arrière : $(basename "$CURRENT") → $(basename "$TARGET")"
ln -sfn "$TARGET" "$BASE/current"
ANAQSUP_VERSION=$(basename "$TARGET") compose "$TARGET" up -d --remove-orphans --wait --wait-timeout 300
log "✓ $(basename "$TARGET") en service"
