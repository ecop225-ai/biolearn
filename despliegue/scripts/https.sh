#!/usr/bin/env bash
# BioLearn · Obtener el certificado HTTPS gratuito de Let's Encrypt para el dominio del sitio
# Requisitos: DOMINIO en .env, el dominio ya apunta a la IP del servidor y el puerto 80 está abierto.
# Uso (en el servidor, dentro de despliegue/):  bash scripts/https.sh
set -euo pipefail
cd "$(dirname "$0")/.."
DOMINIO=$(grep -E '^DOMINIO=' .env | cut -d= -f2- | tr -d ' "\r')
[ -n "$DOMINIO" ] || { echo "Falta DOMINIO en .env (por ejemplo DOMINIO=biolearn-sena.duckdns.org)"; exit 1; }

echo "1/4 · ¿El dominio apunta a este servidor?"
IP_DOMINIO=$(getent hosts "$DOMINIO" | awk '{print $1}' | head -1 || true)
IP_SERVIDOR=$(curl -s https://api.ipify.org || true)
echo "     $DOMINIO → ${IP_DOMINIO:-sin respuesta} · IP pública del servidor: ${IP_SERVIDOR:-desconocida}"
[ -n "$IP_DOMINIO" ] || { echo "El dominio todavía no resuelve. Revisa la IP en el proveedor del dominio y espera unos minutos."; exit 1; }

echo "2/4 · Apache con el volumen de validación"
docker compose up -d apache

echo "3/4 · Solicitud del certificado a Let's Encrypt"
docker compose run --rm certbot certonly --webroot -w /var/www/certbot -d "$DOMINIO" \
  --register-unsafely-without-email --agree-tos --non-interactive --keep-until-expiring

echo "4/4 · Reinicio de Apache con HTTPS"
docker compose restart apache
sleep 3
curl -sI "https://$DOMINIO" | head -1 || true
echo "Listo: https://$DOMINIO"
echo "Recuerda: en .env, SITIO_URL=https://$DOMINIO y luego  docker compose up -d backend"
