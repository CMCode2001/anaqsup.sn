#!/usr/bin/env bash
# Déploie une release déjà déposée par la CI, avec retour arrière automatique.
#   ./deploy.sh <staging|production> <version>
set -Eeuo pipefail
# shellcheck source-path=SCRIPTDIR source=common.sh
source "$(dirname "$(readlink -f "$0")")/common.sh"

ENV_NAME=${1:-}
VERSION=${2:?version requise (ex. v1.2.0)}
require_env "$ENV_NAME"
ENV_FILE=$(env_file "$ENV_NAME")
RELEASE="$BASE/releases/$VERSION"

[[ -d $RELEASE ]] || die "release absente : $RELEASE"
[[ -f $ENV_FILE ]] || die "fichier d'environnement absent : $ENV_FILE"

exec 9>"$BASE/.deploy.lock"
flock -n 9 || die "un déploiement est déjà en cours"

PREVIOUS=$(current_release)

if [[ $ENV_NAME == production && -n $PREVIOUS ]]; then
  log "Sauvegarde avant déploiement"
  "$PREVIOUS/scripts/backup.sh" "$ENV_NAME" "avant-$VERSION"
fi

export ANAQSUP_VERSION=$VERSION
log "Téléchargement des images $VERSION"
compose "$RELEASE" pull --quiet

ln -sfn "$RELEASE" "$BASE/current"
log "Démarrage de $VERSION (attente des healthchecks, 5 min max)"
if compose "$RELEASE" up -d --remove-orphans --wait --wait-timeout 300; then
  log "✓ $VERSION en service sur $ENV_NAME"
  # Conserver les 5 releases les plus récentes (dont la courante).
  find "$BASE/releases" -mindepth 1 -maxdepth 1 -type d -printf '%T@ %p\n' \
    | sort -rn | tail -n +6 | cut -d' ' -f2- | xargs -r rm -rf --
  docker image prune -f --filter "until=168h" >/dev/null
  exit 0
fi

log "✗ Healthchecks en échec : retour arrière"
[[ -n $PREVIOUS ]] || die "aucune release précédente : intervention manuelle requise"
ln -sfn "$PREVIOUS" "$BASE/current"
ANAQSUP_VERSION=$(basename "$PREVIOUS") compose "$PREVIOUS" up -d --remove-orphans --wait --wait-timeout 300
die "$VERSION annulée ; $(basename "$PREVIOUS") restaurée (voir docs/runbooks/rollback.md pour le schéma)"
