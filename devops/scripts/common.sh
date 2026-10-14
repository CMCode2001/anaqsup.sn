# shellcheck shell=bash
# Fonctions partagées par les scripts d'exploitation (sourcé, jamais exécuté directement).
#
# Arborescence serveur :
#   /opt/anaqsup/releases/<version>/   contenu de devops/ déposé par la CI (immuable)
#   /opt/anaqsup/current -> releases/<version>
#   /opt/anaqsup/shared/<env>.env      secrets et hôtes (chmod 600, jamais dans git)
#   /opt/anaqsup/backups/              sauvegardes locales (rotation 14 jours)

BASE=${ANAQSUP_BASE:-/opt/anaqsup}

log() { printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"; }
die() { log "ERREUR : $*" >&2; exit 1; }

require_env() {
  case "${1:-}" in
    staging | production) ;;
    *) die "environnement attendu : staging | production (reçu : '${1:-}')" ;;
  esac
}

env_file() { printf '%s/shared/%s.env' "$BASE" "$1"; }

# Lit une valeur du fichier d'environnement sans l'exécuter (valeurs avec espaces tolérées).
env_value() { grep -E "^$2=" "$1" | tail -n1 | cut -d= -f2- || true; }

current_release() { readlink -f "$BASE/current" 2>/dev/null || true; }

# compose <dossier-release> <arguments docker compose…>
compose() {
  local release=$1
  shift
  docker compose --project-name anaqsup --env-file "$ENV_FILE" -f "$release/compose/compose.yaml" "$@"
}
