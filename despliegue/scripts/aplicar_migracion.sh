#!/usr/bin/env bash
# BioLearn · Aplicar una migración nueva de backend/sql/migraciones/ a la base del servidor
# Uso:  bash scripts/aplicar_migracion.sh ../backend/sql/migraciones/012_ejemplo.sql
# Hazlo después de  git pull  y ANTES de reconstruir el backend (bash scripts/actualizar.sh lo pregunta).
set -euo pipefail
cd "$(dirname "$0")/.."
MIGRACION="${1:?Indica el archivo .sql de la migración}"
[ -f "$MIGRACION" ] || { echo "No existe $MIGRACION"; exit 1; }
bash scripts/respaldo.sh
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 biolearn_db 2>/dev/null' < "$MIGRACION"
echo "Migración aplicada: $MIGRACION"
