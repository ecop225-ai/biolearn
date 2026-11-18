# BioLearn · Despliegue en el servidor (Azure + Docker)

Esta carpeta publica BioLearn en una máquina virtual Ubuntu con Docker. Se usa el mismo repositorio
del proyecto: el servidor descarga el código con `git clone` / `git pull` y construye las imágenes allí.
El desarrollo local (IntelliJ, MySQL Workbench, `127.0.0.1:5173`) **no cambia**.

| Servicio | Imagen | Qué hace | Puerto |
|---|---|---|---|
| `mysql` | `mysql:8.4` (LTS) | Base `biolearn_db`. Datos en el volumen `mysql-datos` | interno |
| `backend` | se construye desde `backend/Dockerfile` | API de Spring Boot. Archivos subidos en el volumen `archivos` | interno (8080) |
| `apache` | se construye desde `apache/Dockerfile` (`httpd:2.4`) | Publica `frontend/` y reenvía `/api`, `/swagger-ui` y `/v3/api-docs` al backend | **80 y 443** |
| `certbot` | `certbot/certbot` (solo a pedido) | Obtiene y renueva el certificado HTTPS | — |

```
Navegador ──► Apache :80 ──► /prototipo-auth/…, /administrador/…  (archivos de frontend/)
                    └──────► /api/…  ──► backend:8080 ──► mysql:3306
```

El frontend detecta dónde está: en `localhost` / `127.0.0.1` sigue usando `http://localhost:8080`;
en el servidor usa la misma dirección del sitio (Apache reenvía `/api`), así que no hace falta CORS.

## Archivos

| Archivo | Uso |
|---|---|
| `docker-compose.yml` | Define los tres servicios, los volúmenes y el reinicio automático (`unless-stopped`) |
| `.env.example` | Plantilla de las variables del servidor. La copia real, `.env`, **nunca** se sube a GitHub |
| `mysql/01_biolearn.sql` | Esquema consolidado (58 tablas) + datos iniciales. MySQL lo ejecuta solo la primera vez |
| `apache/Dockerfile`, `apache/biolearn.conf` | Apache con proxy inverso, compresión y cabeceras de seguridad |
| `scripts/respaldo.sh` | Respaldo de la base (`.sql.gz`) y de los archivos subidos (`.tar.gz`) en `respaldos/` |
| `scripts/restaurar.sh` | Restaura la base desde un respaldo |
| `scripts/aplicar_migracion.sh` | Aplica una migración nueva (hace un respaldo antes) |
| `scripts/actualizar.sh` | `git pull` + reconstruye y reinicia los contenedores |
| `scripts/cambiar_clave_admin.sh` | Cambia la contraseña de una cuenta (la del administrador inicial) |
| `scripts/https.sh`, `scripts/renovar_https.sh` | Obtienen y renuevan el certificado HTTPS gratuito de Let's Encrypt para el dominio |
| `scripts/importar_archivos.sh` | Carga en el servidor los archivos subidos (comprobantes y PDF) traídos del equipo local |

## Etapa 3 · Publicar BioLearn (primera vez)

Requisitos: Etapas 1 y 2 hechas (máquina `vm-biolearn` con Docker) y estos cambios ya subidos a GitHub.

**3.1 Encender y entrar.** En el portal de Azure pulsa **Iniciar** en `vm-biolearn` y, desde PowerShell:
```powershell
ssh -i "$env:USERPROFILE\.ssh\vm-biolearn_key.pem" azureuser@64.236.158.56
```

**3.2 Descargar el proyecto**
```bash
git clone https://github.com/ecop225-ai/biolearn.git
cd biolearn/despliegue
```

**3.3 Crear el archivo `.env` con claves nuevas**
```bash
cp .env.example .env
openssl rand -hex 16   # contraseña de root de MySQL
openssl rand -hex 16   # contraseña del usuario biolearn_user
openssl rand -hex 32   # clave JWT
nano .env
```
Pega cada valor en su línea (`MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`, `JWT_SECRET`), revisa `SITIO_URL`
y guarda con `Ctrl+O`, `Enter`, `Ctrl+X`. Luego protege el archivo: `chmod 600 .env`.
No tomes captura del contenido de `.env`.

**3.4 Construir y arrancar** (la primera vez tarda de 5 a 10 minutos: descarga MySQL y compila el backend)
```bash
docker compose up -d --build
docker compose ps
```
Los tres servicios deben quedar `running` (MySQL además `healthy`). Para ver el arranque del backend:
`docker compose logs -f backend` (sales con `Ctrl+C`); debe terminar en `Started BiolearnBackendApplication`.

**3.5 Probar en el navegador**
- Sitio: `http://64.236.158.56` (abre la pantalla de inicio de sesión)
- Swagger: `http://64.236.158.56/swagger-ui.html`

**3.6 Cambiar la contraseña del administrador inicial** (la del repositorio, `Admin2026`, es pública)
```bash
bash scripts/cambiar_clave_admin.sh
```
Inicia sesión con `admin@biolearn.co` y la contraseña nueva.

**3.7 (Recomendado) Limitar SSH a tu IP.** Portal de Azure → `vm-biolearn` → **Redes** → regla del puerto 22
→ **Origen**: *Mi dirección IP* → Guardar. Si tu IP de casa cambia y no puedes entrar, vuelve a esa regla
y actualízala.

## Etapa 4 · Dominio gratuito y HTTPS

1. **Dominio:** en https://www.duckdns.org inicia sesión (GitHub o Google), crea el subdominio (por ejemplo
   `biolearn-sena`) y en *current ip* escribe `64.236.158.56` → *update ip*. Comprueba desde PowerShell:
   `nslookup biolearn-sena.duckdns.org` debe responder `64.236.158.56`.
2. **Actualizar el servidor:** `bash scripts/actualizar.sh` (trae la configuración de HTTPS).
3. **Configurar:** `nano .env` → `DOMINIO=biolearn-sena.duckdns.org` y `SITIO_URL=https://biolearn-sena.duckdns.org`.
4. **Certificado:** `bash scripts/https.sh` (Let's Encrypt valida el dominio por el puerto 80 y entrega el certificado).
5. **Aplicar la dirección nueva en el backend:** `docker compose up -d backend`.
6. Abrir `https://biolearn-sena.duckdns.org`: candado en el navegador; `http://` y la IP redirigen a HTTPS.

El certificado dura 90 días: al encender el servidor para una revisión, ejecuta `bash scripts/renovar_https.sh`.

## Migrar los datos del equipo local al servidor

En el equipo de desarrollo (Windows):
1. (Opcional) En MySQL Workbench ejecuta `backend/sql/limpieza_datos_selenium.sql` para no llevar los datos de las pruebas automáticas.
2. Workbench → **Server → Data Export** → `biolearn_db`, *Dump Structure and Data*, **Export to Self-Contained File**
   (por ejemplo `biolearn_local.sql`) → *Start Export*.
3. Empaqueta los archivos subidos (desde la carpeta del proyecto): `tar -czf uploads.tar.gz -C backend uploads`.
4. Cópialos al servidor:
   `scp -i "$env:USERPROFILE\.ssh\vm-biolearn_key.pem" biolearn_local.sql uploads.tar.gz azureuser@64.236.158.56:~/biolearn/despliegue/respaldos/`

En el servidor (`~/biolearn/despliegue`):
5. `bash scripts/respaldo.sh` (respaldo de lo que hay, por si hay que volver atrás).
6. `bash scripts/restaurar.sh respaldos/biolearn_local.sql` y `bash scripts/importar_archivos.sh respaldos/uploads.tar.gz`.
7. `bash scripts/cambiar_clave_admin.sh`: la base local trae la contraseña local del administrador.
8. Validar: número de usuarios, cursos e inscripciones igual al del equipo local; abrir un PDF y un comprobante.

## Migración de MySQL 8.0 a 8.4 LTS (mantenimiento adaptativo)

MySQL 8.0 salió de soporte en abril de 2026. La imagen del servicio `mysql` pasó a `mysql:8.4`; al arrancar, MySQL 8.4
actualiza solo las tablas del sistema y el diccionario de datos del volumen existente.
1. `bash scripts/respaldo.sh` (obligatorio: una base actualizada a 8.4 no se puede volver a abrir con 8.0).
2. `bash scripts/actualizar.sh` (descarga `mysql:8.4` y recrea el contenedor).
3. `docker compose logs mysql | grep -i upgrade` debe mostrar la actualización de 8.0 a 8.4 completada.
4. Verificar la versión y las 58 tablas, e iniciar sesión en el sitio.
5. Si algo falla: volver a `mysql:8.0` en `docker-compose.yml`, borrar el volumen (`docker compose down`, `docker volume rm biolearn_mysql-datos`),
   arrancar y restaurar el respaldo del paso 1 con `restaurar.sh`.

## Operación diaria

| Tarea | Cómo |
|---|---|
| Apagar para no gastar crédito | Portal → **Detener** (queda *Detenida (desasignada)*) |
| Encender para una revisión | Portal → **Iniciar**; en 1–2 minutos los contenedores arrancan solos |
| Ver el estado | `docker compose ps` |
| Ver el registro del backend | `docker compose logs --tail 100 backend` |
| Publicar cambios nuevos de GitHub | `bash scripts/actualizar.sh` |
| Respaldo | `bash scripts/respaldo.sh` |
| Copiar respaldos a tu PC (PowerShell) | `scp -i "$env:USERPROFILE\.ssh\vm-biolearn_key.pem" azureuser@64.236.158.56:~/biolearn/despliegue/respaldos/* .` |
| Restaurar | `bash scripts/restaurar.sh respaldos/<archivo>.sql.gz` |
| Migración nueva (012…) | `bash scripts/aplicar_migracion.sh ../backend/sql/migraciones/<archivo>.sql` |

### Correos en el servidor

El envío de correos está apagado (`CORREO_HABILITADO=false`). Los tokens de activación, invitación y
recuperación quedan en el registro del backend:
```bash
docker compose logs backend | grep "Token de"
```
y el enlace se arma con la dirección del sitio:

| Caso | Enlace |
|---|---|
| Verificar un estudiante | `http://64.236.158.56/prototipo-auth/biolearn_auth_prototipo.html?verificar=TOKEN` |
| Activar personal invitado | `…/biolearn_auth_prototipo.html?invitacion=TOKEN` |
| Recuperar contraseña | `…/biolearn_auth_prototipo.html?recuperar=TOKEN` |

### Seguridad

- Solo el puerto 80 de Apache (y 22 para SSH) está abierto; MySQL y el backend no se exponen a Internet.
- Las claves reales están solo en `despliegue/.env` del servidor (`chmod 600`), nunca en GitHub.
- La llave `vm-biolearn_key.pem` se queda en tu computador; no se sube ni se comparte.
- `respaldos/` y `.env` están en `despliegue/.gitignore`.
