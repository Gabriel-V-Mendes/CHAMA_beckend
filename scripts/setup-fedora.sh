#!/usr/bin/env bash
# Script idempotente para instalar e configurar JDK 25, Maven e PostgreSQL no Fedora.
set -euo pipefail

CHAMA_DB_NAME="${CHAMA_DB_NAME:-chama}"
CHAMA_DB_USER="${CHAMA_DB_USER:-chama_app}"
CHAMA_DB_PASSWORD="${CHAMA_DB_PASSWORD:-chama_dev_local_2026}"

echo "[1/5] Instalando pacotes Java 25 (JDK), Maven e PostgreSQL..."
dnf install -y java-25-openjdk-devel maven postgresql postgresql-server postgresql-contrib

echo "[2/5] Inicializando cluster PostgreSQL (se ainda não inicializado)..."
if [ ! -f /var/lib/pgsql/data/PG_VERSION ]; then
  postgresql-setup --initdb
fi

echo "[3/5] Configurando autenticação scram-sha-256 para conexões locais TCP..."
PG_HBA="/var/lib/pgsql/data/pg_hba.conf"
if [ -f "$PG_HBA" ]; then
  if [ ! -f "${PG_HBA}.bak" ]; then
    cp "$PG_HBA" "${PG_HBA}.bak"
  fi
  sed -i -E 's/(127\.0\.0\.1\/32[[:space:]]+)ident/\1scram-sha-256/g' "$PG_HBA"
  sed -i -E 's/(::1\/128[[:space:]]+)ident/\1scram-sha-256/g' "$PG_HBA"
fi

echo "[4/5] Habilitando e iniciando o serviço PostgreSQL..."
systemctl enable --now postgresql
systemctl reload postgresql

echo "[5/5] Criando usuário e banco de dados de desenvolvimento '${CHAMA_DB_NAME}'..."
runuser -u postgres -- psql -v ON_ERROR_STOP=1 <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${CHAMA_DB_USER}') THEN
    CREATE ROLE ${CHAMA_DB_USER} WITH LOGIN PASSWORD '${CHAMA_DB_PASSWORD}';
  ELSE
    ALTER ROLE ${CHAMA_DB_USER} WITH LOGIN PASSWORD '${CHAMA_DB_PASSWORD}';
  END IF;
END
\$\$;

SELECT 'CREATE DATABASE ${CHAMA_DB_NAME} OWNER ${CHAMA_DB_USER}'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${CHAMA_DB_NAME}')\gexec

GRANT ALL PRIVILEGES ON DATABASE ${CHAMA_DB_NAME} TO ${CHAMA_DB_USER};
SQL

echo "Setup concluído! Java, Maven e PostgreSQL configurados com sucesso."
