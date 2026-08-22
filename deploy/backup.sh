#!/usr/bin/env bash
# Daily Postgres backup for the KWTC ranking DB. Run via cron (see deploy/README.md).
#
# Dumps the `ranking-postgres` container's database to a gzipped .sql file OUTSIDE the Docker
# volume (so `docker compose down -v` or a bad `docker volume rm` can't take the backups with
# it), and prunes backups older than $RETENTION_DAYS.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="$BACKEND_DIR/.env"
BACKUP_DIR="${BACKUP_DIR:-$HOME/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

POSTGRES_DB="ranking"
POSTGRES_USER="ranking"
if [ -f "$ENV_FILE" ]; then
	# Only pull the two values we need; ignore everything else (secrets included) in .env.
	db_line=$(grep -E '^POSTGRES_DB=' "$ENV_FILE" || true)
	user_line=$(grep -E '^POSTGRES_USER=' "$ENV_FILE" || true)
	[ -n "$db_line" ] && POSTGRES_DB="${db_line#POSTGRES_DB=}"
	[ -n "$user_line" ] && POSTGRES_USER="${user_line#POSTGRES_USER=}"
fi

mkdir -p "$BACKUP_DIR"
timestamp="$(date +%Y%m%d-%H%M%S)"
out_file="$BACKUP_DIR/ranking-$timestamp.sql.gz"
tmp_file="$out_file.tmp"

echo "[$(date -Iseconds)] Backing up database '$POSTGRES_DB' to $out_file"
docker exec ranking-postgres pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" | gzip > "$tmp_file"
mv "$tmp_file" "$out_file"
echo "[$(date -Iseconds)] Backup complete: $(du -h "$out_file" | cut -f1)"

find "$BACKUP_DIR" -maxdepth 1 -name 'ranking-*.sql.gz' -mtime "+$RETENTION_DAYS" -print -delete
