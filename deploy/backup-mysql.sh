#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$PROJECT_DIR/backups}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BACKUP_FILE="$BACKUP_DIR/urbelix_${STAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"
cd "$PROJECT_DIR"

docker compose exec -T db sh -c \
  'exec mysqldump --single-transaction --quick --lock-tables=false \
   -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
  | gzip > "$BACKUP_FILE"

echo "Backup creado: $BACKUP_FILE"
