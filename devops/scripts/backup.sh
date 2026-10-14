#!/usr/bin/env bash
# Sauvegarde : base anaqsup (format custom pg_dump) et volume des documents.
#   ./backup.sh <staging|production> [libellé]        (cron quotidien : libellé « quotidienne »)
# RPO cible : 24 h. Copie hors site si BACKUP_RCLONE_REMOTE est renseigné.
set -Eeuo pipefail
# shellcheck source-path=SCRIPTDIR source=common.sh
source "$(dirname "$(readlink -f "$0")")/common.sh"

ENV_NAME=${1:-}
LABEL=${2:-quotidienne}
require_env "$ENV_NAME"
ENV_FILE=$(env_file "$ENV_NAME")
CURRENT=$(current_release)
[[ -n $CURRENT ]] || die "aucune release active"
export ANAQSUP_VERSION
ANAQSUP_VERSION=$(basename "$CURRENT")

DEST="$BASE/backups/$(date -u +%Y%m%dT%H%M%SZ)-$LABEL"
mkdir -p "$DEST"
chmod 700 "$DEST"

for db in anaqsup; do
  log "pg_dump $db"
  compose "$CURRENT" exec -T postgres pg_dump -U postgres --format=custom --compress=6 "$db" >"$DEST/$db.dump"
done

log "Archive du volume documents"
docker run --rm -v anaqsup_documents:/data:ro -v "$DEST":/backup postgres:18-alpine \
  tar czf /backup/documents.tar.gz -C /data .

(cd "$DEST" && sha256sum ./*.dump documents.tar.gz >SHA256SUMS)
log "✓ Sauvegarde : $DEST ($(du -sh "$DEST" | cut -f1))"

REMOTE=$(env_value "$ENV_FILE" BACKUP_RCLONE_REMOTE)
if [[ -n $REMOTE ]]; then
  log "Copie hors site → $REMOTE"
  rclone copy "$DEST" "$REMOTE:anaqsup-$ENV_NAME/$(basename "$DEST")"
fi

find "$BASE/backups" -mindepth 1 -maxdepth 1 -type d -mtime +14 -exec rm -rf {} +
