#!/usr/bin/env bash
# Émission initiale du certificat Let's Encrypt (un seul certificat SAN pour tous les hôtes).
# À lancer UNE fois par environnement, avant le premier déploiement (Nginx ne démarre pas sans certificat).
# Lancer ce script vaut acceptation des conditions d'utilisation de Let's Encrypt.
#   ./tls-issue.sh <staging|production> [--test]   (--test : autorité de test, sans limite de quota)
set -Eeuo pipefail
# shellcheck source-path=SCRIPTDIR source=common.sh
source "$(dirname "$(readlink -f "$0")")/common.sh"

ENV_NAME=${1:-}
require_env "$ENV_NAME"
ENV_FILE=$(env_file "$ENV_NAME")
RELEASE=$(current_release)
RELEASE=${RELEASE:-$(dirname "$(dirname "$(readlink -f "$0")")")}
export ANAQSUP_VERSION=${ANAQSUP_VERSION:-bootstrap}

EMAIL=$(env_value "$ENV_FILE" TLS_CONTACT_EMAIL)
[[ -n $EMAIL ]] || die "TLS_CONTACT_EMAIL manquant dans $ENV_FILE"

DOMAINS=()
for key in PUBLIC_HOST WWW_HOST API_HOST ADMIN_HOST; do
  host=$(env_value "$ENV_FILE" "$key")
  [[ -n $host ]] || die "$key manquant dans $ENV_FILE"
  DOMAINS+=(-d "$host")
done

EXTRA=()
[[ ${2:-} == --test ]] && EXTRA+=(--test-cert)

log "Libération du port 80 et émission pour : ${DOMAINS[*]}"
compose "$RELEASE" stop nginx 2>/dev/null || true
compose "$RELEASE" --profile tls run --rm -p 80:80 certbot certonly --standalone --non-interactive \
  --agree-tos --no-eff-email -m "$EMAIL" --cert-name anaqsup --key-type ecdsa "${EXTRA[@]}" "${DOMAINS[@]}"
log "✓ Certificat émis. Renouvellement : tls-renew.sh (cron installé par Ansible)."
