#!/usr/bin/env bash
# BioLearn · Restaurar la base de datos desde un respaldo hecho con respaldo.sh
# Uso:  bash scripts/restaurar.sh respaldos/biolearn_db_AAAA-MM-DD_HHMM.sql.gz
#   También acepta un .sql sin comprimir (por ejemplo, la exportación de MySQL Workbench del equipo local).
# ¡Reemplaza las tablas actuales por las del respaldo! Haz antes un respaldo nuevo si tienes dudas.
set -euo pipefail
cd "$(dirname "$0")/.."
ARCHIVO="${1:?Indica el archivo .sql o .sql.gz que quieres restaurar}"
[ -f "$ARCHIVO" ] || { echo "No existe $ARCHIVO"; exit 1; }
read -rp "Se reemplazará la base biolearn_db con $ARCHIVO. Escribe SI para continuar: " OK
[ "$OK" = "SI" ] || { echo "Cancelado."; exit 0; }
case "$ARCHIVO" in *.gz) LEER="gunzip -c";; *) LEER="cat";; esac
$LEER "$ARCHIVO" | docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 biolearn_db'
docker compose restart backend
echo "Base restaurada desde $ARCHIVO."
