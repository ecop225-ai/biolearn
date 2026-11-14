# BioLearn — Backend

API REST de la plataforma educativa BioLearn, construida con Java 17 y Spring Boot 3.3.4.

## Requisitos
- JDK 17 (Eclipse Temurin recomendado)
- Maven (gestionado por el wrapper de Spring Boot / IntelliJ)
- MySQL 8.x

## Puesta en marcha

1. Crea la base de datos, las tablas y los datos iniciales (detalle en `documentos/10_guia_entorno.md`):
   ```
   mysql -u root -p -e "CREATE DATABASE biolearn_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
   mysql -u root -p biolearn_db < schema.sql
   mysql -u root -p < sql/datos_iniciales.sql
   ```
   Si tu base ya existía, aplica en su lugar las migraciones pendientes de `sql/migraciones/` (en orden) y luego `sql/datos_iniciales.sql`.
2. Copia `src/main/resources/application.properties.example` como `application.properties` (en la misma carpeta) y completa tu contraseña real de MySQL y una clave JWT de al menos 32 caracteres.
3. Abre la carpeta `backend/` en IntelliJ IDEA como proyecto Maven.
4. Ejecuta la clase `BiolearnBackendApplication`.
5. Verifica en `http://localhost:8080/swagger-ui.html`. Administrador inicial: `admin@biolearn.co` / `Admin2026` (cámbiala).

Mientras no haya envío real de correos, los tokens de activación, invitación y recuperación se imprimen en la consola de IntelliJ.

## Scripts SQL

| Archivo | Uso |
|---|---|
| `schema.sql` | Estructura completa (51 tablas). Siempre refleja la versión más reciente del modelo. |
| `sql/datos_iniciales.sql` | Grados, áreas académicas, administrador inicial e instituciones de prueba. Idempotente (`INSERT IGNORE`). |
| `sql/migraciones/001_cupos_institucion.sql` | Agrega `institucion.cupos_contratados` a bases creadas antes de este cambio. |
| `sql/migraciones/002_sincronizar_estado_supervisores.sql` | Corrige supervisores desactivados antes de la regla de representante único, cuyo vínculo con la institución había quedado activo. |

## Estructura de paquetes

```
co.biolearn
 ├── config       → configuración general (seguridad, OpenAPI, CORS)
 ├── controller   → endpoints REST
 ├── dto          → objetos de transferencia de datos (entrada/salida de la API)
 ├── enums        → catálogos fijos (roles, estados)
 ├── exception    → manejo centralizado de errores
 ├── model        → entidades JPA (mapean las tablas de la base de datos)
 ├── repository   → acceso a datos (Spring Data JPA)
 ├── security     → autenticación JWT
 └── service      → lógica de negocio
```

## Módulos implementados

- **Autenticación** (`/api/auth/**`): registro público de estudiantes (con validación de código de institución y cupos), login (solo cuentas activas), verificación de correo y su reenvío, activación de invitaciones de personal, recuperación y restablecimiento de contraseña, consulta del usuario autenticado.
- **Usuarios** (`/api/usuarios/**`, exclusivo de rol ADMIN): creación de cuentas de Docente, Contador y Supervisor institucional (un representante activo por institución), listado con filtros, detalle, edición de perfil, cambio de estado y reenvío de invitaciones.
