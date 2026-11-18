#!/usr/bin/env bash
# BioLearn · Renovar el certificado HTTPS (Let's Encrypt lo emite por 90 días; renueva si faltan menos de 30)
# Uso:  bash scripts/renovar_https.sh     (conviene ejecutarlo cada vez que se enciende el servidor)
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose run --rm certbot renew --webroot -w /var/www/certbot
docker compose exec apache httpd -k graceful
echo "Certificado revisado."
