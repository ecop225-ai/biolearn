#!/usr/bin/env bash
# BioLearn · Restaurar la base de datos desde un respaldo hecho con respaldo.sh
# Uso:  bash scripts/restaurar.sh respaldos/biolearn_db_AAAA-MM-DD_HHMM.sql.gz
# ¡Reemplaza las tablas actuales por las del respaldo! Haz antes un respaldo nuevo si tienes dudas.
set -euo pipefail
cd "$(dirname "$0")/.."
ARCHIVO="${1:?Indica el archivo .sql.gz que quieres restaurar}"
[ -f "$ARCHIVO" ] || { echo "No existe $ARCHIVO"; exit 1; }
read -rp "Se reemplazará la base biolearn_db con $ARCHIVO. Escribe SI para continuar: " OK
[ "$OK" = "SI" ] || { echo "Cancelado."; exit 0; }
gunzip -c "$ARCHIVO" | docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 biolearn_db 2>/dev/null'
docker compose restart backend
echo "Base restaurada desde $ARCHIVO."
