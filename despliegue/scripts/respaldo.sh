#!/usr/bin/env bash
# BioLearn · Respaldo de la base de datos y de los archivos subidos
# Uso (en el servidor, dentro de despliegue/):  bash scripts/respaldo.sh
# Deja en despliegue/respaldos/ dos archivos con la fecha: la base (.sql.gz) y los archivos subidos (.tar.gz).
# La carpeta respaldos/ no se sube a GitHub. Para sacar una copia a tu computador, desde PowerShell:
#   scp -i "$env:USERPROFILE\.ssh\vm-biolearn_key.pem" azureuser@64.236.158.56:~/biolearn/despliegue/respaldos/* .
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p respaldos
FECHA=$(date +%Y-%m-%d_%H%M)

docker compose exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --triggers --default-character-set=utf8mb4 biolearn_db' \
  | gzip > "respaldos/biolearn_db_${FECHA}.sql.gz"

docker compose exec -T backend tar czf - -C /app uploads > "respaldos/archivos_${FECHA}.tar.gz"

ls -lh "respaldos/biolearn_db_${FECHA}.sql.gz" "respaldos/archivos_${FECHA}.tar.gz"
echo "Respaldo terminado."
