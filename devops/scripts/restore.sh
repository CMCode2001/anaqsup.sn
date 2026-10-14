#!/usr/bin/env bash
# Restauration d'une sauvegarde produite par backup.sh. DESTRUCTIF : écrase les données courantes.
#   ./restore.sh <staging|production> <dossier-de-sauvegarde>
# RTO cible : 2 h. Tester chaque mois sur staging (docs/runbooks/sauvegarde-restauration.md).
set -Eeuo pipefail
# shellcheck source-path=SCRIPTDIR source=common.sh
source "$(dirname "$(readlink -f "$0")")/common.sh"

ENV_NAME=${1:-}
SOURCE=${2:?dossier de sauvegarde requis}
require_env "$ENV_NAME"
ENV_FILE=$(env_file "$ENV_NAME")
CURRENT=$(current_release)
[[ -n $CURRENT ]] || die "aucune release active"
[[ -f $SOURCE/SHA256SUMS ]] || die "sauvegarde invalide : $SOURCE"
export ANAQSUP_VERSION
ANAQSUP_VERSION=$(basename "$CURRENT")

(cd "$SOURCE" && sha256sum --check --quiet SHA256SUMS) || die "empreintes incorrectes : sauvegarde corrompue"

read -r -p "Restaurer $(basename "$SOURCE") sur $ENV_NAME ? Les données actuelles seront écrasées. Tapez « $ENV_NAME » : " answer
[[ $answer == "$ENV_NAME" ]] || die "abandon"

log "Arrêt des applications (Nginx continue de servir le cache public)"
compose "$CURRENT" stop backend portail backoffice

for db in anaqsup; do
  log "pg_restore $db"
  compose "$CURRENT" exec -T postgres pg_restore -U postgres --clean --if-exists --dbname="$db" <"$SOURCE/$db.dump"
done

log "Restauration du volume documents"
docker run --rm -v anaqsup_documents:/data -v "$SOURCE":/backup:ro postgres:18-alpine \
  sh -c 'find /data -mindepth 1 -delete && tar xzf /backup/documents.tar.gz -C /data'

compose "$CURRENT" up -d --wait --wait-timeout 300
log "✓ Restauration terminée depuis $(basename "$SOURCE")"
