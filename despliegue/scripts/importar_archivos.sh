#!/usr/bin/env bash
# BioLearn · Cargar en el servidor los archivos subidos (comprobantes de pago y PDF de los cursos)
# Uso:  bash scripts/importar_archivos.sh respaldos/uploads.tar.gz
# El .tar.gz debe contener la carpeta «uploads» (en Windows:  tar -czf uploads.tar.gz -C backend uploads).
# Los archivos se agregan al volumen «archivos»; los que ya existen con el mismo nombre se reemplazan.
set -euo pipefail
cd "$(dirname "$0")/.."
ARCHIVO="${1:?Indica el archivo .tar.gz con la carpeta uploads}"
[ -f "$ARCHIVO" ] || { echo "No existe $ARCHIVO"; exit 1; }
docker compose exec -T backend tar xzf - -C /app < "$ARCHIVO"
echo "Archivos en el servidor: $(docker compose exec -T backend sh -c 'ls /app/uploads | wc -l')"
