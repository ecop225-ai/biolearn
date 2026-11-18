#!/usr/bin/env bash
# BioLearn · Actualizar el servidor con la última versión de GitHub
# Uso (en el servidor, dentro de despliegue/):  bash scripts/actualizar.sh
# Si el cambio trae una migración nueva en backend/sql/migraciones/, aplícala primero con aplicar_migracion.sh.
set -euo pipefail
cd "$(dirname "$0")/.."
ANTES=$(git rev-parse HEAD)
git pull --ff-only
NUEVAS=$(git diff --name-only "$ANTES" HEAD -- ../backend/sql/migraciones/ || true)
if [ -n "$NUEVAS" ]; then
  echo "Llegaron migraciones nuevas:"; echo "$NUEVAS"
  echo "Aplícalas con bash scripts/aplicar_migracion.sh <archivo> y vuelve a ejecutar este script."
  read -rp "¿Ya las aplicaste? (s/n): " R; [ "$R" = "s" ] || exit 0
fi
docker compose up -d --build
docker image prune -f >/dev/null
docker compose ps
