# BioLearn — Mecanismos de seguridad

## 1. Autenticación (¿quién eres?)

| Mecanismo | Cómo se implementa |
|---|---|
| **Contraseñas** | Nunca se guardan en texto plano. Se cifran con **BCrypt** (`org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`) antes de guardarse en `usuario.contrasena`. |
| **Política de contraseñas** | Mínimo 8 caracteres, al menos una mayúscula y un número. Se valida en el backend (`PoliticaContrasena`, anotación `@Pattern`) en registro, restablecimiento y activación por invitación, y el prototipo muestra el mismo checklist. |
| **Login** | `POST /api/auth/login` con correo **o** nombre de usuario + contraseña → si coincide, el servidor genera un **JWT (JSON Web Token)** firmado y lo devuelve. |
| **Sesión** | No hay sesión en el servidor (stateless): el frontend guarda el JWT y lo manda en cada petición dentro del header `Authorization: Bearer <token>`. |
| **Estados de cuenta** | Solo una cuenta `activo` puede iniciar sesión. `pendiente` (correo sin verificar o invitación sin completar) recibe 403 con código `CUENTA_NO_VERIFICADA`; `inactivo` y `suspendido` reciben 403 con su propio mensaje. |
| **Revocación de sesiones** | El filtro JWT comprueba en cada petición que la cuenta siga activa: desactivar o suspender a un usuario invalida su token de inmediato. |
| **401 vs 403** | Sin token, token vencido o cuenta ya no activa → **401** (volver a iniciar sesión). Autenticado pero sin el rol requerido → **403**. |
| **Expiración del token** | 8 horas. Al vencer, el usuario debe volver a iniciar sesión (o se usa un *refresh token* si se quiere mantener la sesión más tiempo). |
| **Recuperación de contraseña** | Se genera un token de un solo uso con expiración corta (30 min, como ya se ve en el prototipo de Auth), enviado por correo. Nunca se envía la contraseña ni se permite adivinarla. |

## 2. Autorización (¿qué puedes hacer?)

BioLearn tiene 5 roles con permisos muy distintos (Admin, Docente, Contador, Supervisor, Estudiante). Esto se controla en dos niveles:

- **A nivel de endpoint** con Spring Security, usando anotaciones como `@PreAuthorize("hasRole('CONTADOR')")` en los controladores. Ejemplo: solo un `CONTADOR` puede llamar `POST /api/pagos/{id}/confirmar`.
- **A nivel de dato**, en la capa de Servicio: un `SUPERVISOR` solo puede ver estudiantes de **su propia institución** (no de otras), aunque técnicamente tenga acceso al endpoint `GET /api/estudiantes`. Esto se valida comparando `institucion_id` del usuario autenticado contra el del estudiante consultado — nunca confiando en un ID que mande el frontend.

## 3. Protección de datos personales

Como ya vimos en la Política de Datos del prototipo de Auth (Ley 1581 de 2012 de Colombia):

- **HTTPS obligatorio** en producción — nunca se transmiten credenciales ni datos de pago sin cifrar.
- **Datos de menores de edad**: si el titular es menor, su registro debe quedar asociado a la autorización de un acudiente (campo adicional a validar en el flujo de registro).
- **Principio de mínimo privilegio**: cada rol solo ve los datos que necesita (un Docente no ve los pagos de un estudiante; un Contador no ve las respuestas de un examen).
- **Derecho al olvido**: debe existir un proceso (aunque sea manual al inicio) para eliminar o anonimizar los datos de un usuario que lo solicite.

## 4. Protección contra ataques comunes

| Riesgo | Mitigación |
|---|---|
| **Inyección SQL** | Uso exclusivo de Spring Data JPA / consultas parametrizadas — nunca concatenar texto para armar SQL. |
| **XSS** (scripts maliciosos en foros, comentarios) | Sanitizar todo contenido enriquecido que el usuario escribe (lecturas, aportes de foro) antes de guardarlo o mostrarlo; React ya escapa por defecto, pero el HTML enriquecido de foros/lecciones necesita una librería de sanitización (ej. `DOMPurify` en frontend o `OWASP Java HTML Sanitizer` en backend). |
| **CSRF** | Al ser una API stateless con JWT (no cookies de sesión), el riesgo es bajo, pero se configura Spring Security para no depender de cookies. |
| **Fuerza bruta en login** | Límite de intentos fallidos por IP/usuario (ej. bloqueo temporal tras 5 intentos). |
| **Subida de archivos maliciosos** | Validar tipo real de archivo (no solo la extensión) y tamaño máximo (ya definimos 10 MB en los prototipos) antes de guardarlo. |
| **CORS** | Configurar Spring para aceptar peticiones solo desde el dominio real del frontend, no desde cualquier origen. |

## 5. Casos específicos de BioLearn que necesitan reglas de seguridad propias

Estos salieron directamente de la lógica que construimos en los prototipos:

- **Código de afiliación institucional**: al registrarse un estudiante con un código, se debe validar que la institución tenga cupos disponibles (estudiantes activos afiliados < `cupos_contratados`) antes de aceptar la afiliación — si no, rechazar aunque el código sea válido. Si `cupos_contratados` es NULL no hay límite.
- **Docentes, Contadores y Supervisores no se autorregistran**: solo se crean por invitación desde el módulo Admin. El endpoint de registro público (`/api/auth/registro`) solo crea cuentas de Estudiante: no recibe ningún campo de rol. Además, un token de invitación de personal no sirve para `/verificar-cuenta` (activaría la cuenta sin contraseña) y un token de estudiante no sirve para `/completar-invitacion`.
- **Invitaciones pendientes**: el token de invitación expira a los 7 días y solo permite fijar la contraseña una vez. El Administrador puede reenviarla (`POST /api/usuarios/{id}/reenviar-invitacion`); al reenviar, el token anterior queda invalidado. Lo mismo aplica al enlace de verificación del estudiante (`POST /api/auth/reenviar-verificacion`, con respuesta genérica para no revelar qué correos existen).
- **Administrador inicial**: no existe registro público de administradores; la primera cuenta se aprovisiona con `backend/sql/datos_iniciales.sql` y su contraseña debe cambiarse tras el primer ingreso.
- **Representante institucional**: cada institución tiene como máximo un supervisor activo. Desactivar al representante libera la institución; reactivarlo se rechaza si otro ya la representa.
- **Aprobación de reembolsos**: solo el rol Contador puede aprobar/rechazar, y la acción debe quedar auditada (quién, cuándo, con qué justificación) — ya lo modelamos en la tabla `reembolsos` con `resuelto_por_id`.
- **Verificación de certificados**: el endpoint público de verificación (`GET /api/certificados/verificar/{codigo}`) debe ser de solo lectura y no exponer datos sensibles del estudiante más allá de nombre y curso.

## 6. Resumen de responsabilidades por capa

| Capa | Qué controla |
|---|---|
| Spring Security (filtro) | Verifica que el JWT sea válido y no haya expirado, antes de que la petición llegue al Controller |
| Controller | Verifica el rol con `@PreAuthorize` |
| Service | Verifica reglas de negocio específicas (pertenencia a institución, cupos, estados válidos) |
| Repository / BD | Restricciones de integridad (`UNIQUE`, `FOREIGN KEY`) como última línea de defensa |
