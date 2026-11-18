#!/usr/bin/env bash
# BioLearn · Cambiar la contraseña de una cuenta directamente en la base del servidor
# Se usa para reemplazar la clave inicial del administrador (Admin2026) apenas se publica el sitio.
# Uso:  bash scripts/cambiar_clave_admin.sh
# La contraseña se escribe sin que se vea en pantalla y se guarda cifrada con BCrypt (como lo hace el backend).
set -euo pipefail
cd "$(dirname "$0")/.."
read -rp "Correo de la cuenta [admin@biolearn.co]: " CORREO
CORREO=${CORREO:-admin@biolearn.co}
read -rsp "Nueva contraseña (mínimo 8 caracteres): " C1; echo
read -rsp "Repítela: " C2; echo
[ "$C1" = "$C2" ] || { echo "Las contraseñas no coinciden."; exit 1; }
[ ${#C1} -ge 8 ] || { echo "Debe tener al menos 8 caracteres."; exit 1; }
case "$CORREO" in *\'*) echo "Correo no válido."; exit 1;; esac
HASH=$(docker compose exec -T apache htpasswd -nbBC 10 x "$C1" | cut -d: -f2- | tr -d '\r\n')
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot biolearn_db' <<SQL
UPDATE usuario SET contrasena = '$HASH' WHERE correo = '$CORREO';
SELECT correo, estado, IF(contrasena = '$HASH', 'contraseña actualizada', 'SIN CAMBIOS') AS resultado
  FROM usuario WHERE correo = '$CORREO';
SQL
