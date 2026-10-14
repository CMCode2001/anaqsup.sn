#!/bin/bash
# Exécuté UNE seule fois, à l'initialisation du volume PostgreSQL.
# Moindre privilège : l'application a son propre rôle, propriétaire de sa base,
# sans droit superutilisateur. Les extensions (unaccent, pg_trgm) sont « trusted » et créées par Flyway.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
  -v app_password="$ANAQSUP_DB_PASSWORD" <<'SQL'
CREATE ROLE anaqsup LOGIN PASSWORD :'app_password';
CREATE DATABASE anaqsup OWNER anaqsup ENCODING 'UTF8' LOCALE_PROVIDER icu ICU_LOCALE 'fr-SN' TEMPLATE template0;
REVOKE ALL ON DATABASE anaqsup FROM PUBLIC;

SQL
