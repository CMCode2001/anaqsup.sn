#!/usr/bin/env bash
# Renouvellement Let's Encrypt par webroot (Nginx reste en service), puis rechargement à chaud.
#   ./tls-renew.sh <staging|production>      (cron deux fois par jour, sans effet si non échu)
set -Eeuo pipefail
# shellcheck source-path=SCRIPTDIR source=common.sh
source "$(dirname "$(readlink -f "$0")")/common.sh"

ENV_NAME=${1:-}
require_env "$ENV_NAME"
ENV_FILE=$(env_file "$ENV_NAME")
CURRENT=$(current_release)
[[ -n $CURRENT ]] || die "aucune release active"
export ANAQSUP_VERSION
ANAQSUP_VERSION=$(basename "$CURRENT")

compose "$CURRENT" --profile tls run --rm certbot renew --webroot -w /var/www/certbot --quiet
compose "$CURRENT" exec -T nginx nginx -s reload
