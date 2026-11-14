# BioLearn — Historias de usuario

Extraídas del documento fuente `biolearn_historias_usuario.xlsx`, con criterios de aceptación desarrollados en formato ágil (contexto, evento, resultado esperado). Aquí se presenta el enunciado de cada historia (rol, funcionalidad y razón); los escenarios y criterios detallados están en el Excel original.

> **Actualización (ajuste tras la implementación de Autenticación y Usuarios):** se corrigieron las historias `SOL-ADM-001`, `SOL-ADM-002`, `SOL-ADM-003`, `SOL-INS-001`, `SOL-DOC-001` (ambas versiones) y `SOL-EST-001` para que coincidan con las decisiones de diseño adoptadas: el administrador inicial se aprovisiona por script; Docentes, Contadores y Representantes se activan por invitación (no se autorregistran con código); el estudiante activa su cuenta verificando el correo y paga al inscribirse a cada curso; cada institución tiene cupos contratados, un código de afiliación de 8 caracteres generado por el sistema y como máximo un representante activo.

---

## Clasificadas por módulo

### Administrador

| ID | Rol | Submódulo | Historia |
|---|---|---|---|
| `SOL-ADM-001` | ADM | Configuración cuenta Administrador | **Autenticación del administrador con cuenta aprovisionada por script** — _para_ Permitir la gestión del contenido educativo y supervisión del sistema sin exponer un registro público de administradores |
| `SOL-ADM-002` | ADM | Gestión de usuarios | **Gestión de usuarios: crear, editar, activar y desactivar cuentas** — _para_ Administrar el acceso y permisos de todos los roles del sistema |
| `ACT-ADM-001` | ADM | Gestión de cursos | **Crear, editar y eliminar cursos desde el panel de administración** — _para_ Gestionar el catálogo de cursos disponibles en la plataforma |
| `SOL-ADM-003` | ADM | Gestión de instituciones | **Registrar y editar instituciones (con cupos contratados y código de afiliación de 8 caracteres generado por el sistema) y asignarles un representante activo** — _para_ Administrar las instituciones educativas afiliadas a la plataforma |
| `SOL-ADM-004` | ADM | Reportes y mensajes | **Generación de reportes académicos, financieros e institucionales** — _para_ Obtener información consolidada del sistema para la toma de decisiones |

### Docente

| ID | Rol | Submódulo | Historia |
|---|---|---|---|
| `SOL-DOC-001` | DOCENTE | Registro y activación | **Activación de la cuenta del docente a partir de la invitación del Administrador, definiendo su propia contraseña** — _para_ Permitir al docente acceder a las herramientas de enseñanza de la plataforma |
| `ACT-DOC-001` | DOCENTE | Gestión de cursos y lecciones | **Crear curso, lecciones y publicar contenido con imagen de portada** — _para_ Diseñar y publicar el contenido educativo para los estudiantes |
| `ACT-DOC-002` | DOCENTE | Evaluaciones | **Crear evaluaciones con preguntas y explicación de respuesta** — _para_ Evaluar el aprendizaje con retroalimentación disponible al agotar los intentos |
| `ACT-DOC-003` | DOCENTE | Foros y participación | **Moderar foros: leer aportes completos y responder estudiantes** — _para_ Retroalimentar a los estudiantes y mantener la calidad del debate académico |
| `ACT-DOC-005` | DOCENTE | Sesiones, eventos y certificados | **Crear sesiones en línea, generar código de asistencia y emitir certificados** — _para_ Facilitar clases en vivo y certificar la participación de los estudiantes |
| `ACT-DOC-004` | DOCENTE | Reportes | **Seguimiento académico y generación de reportes del curso** — _para_ Monitorear el progreso de los estudiantes y generar informes de desempeño |

### Contador

| ID | Rol | Submódulo | Historia |
|---|---|---|---|
| `SOL-ADM-012` | ADM | Activación de usuario contador | **Activar usuario contador que podrá ingresar y visualizar pagos y facturas** — _para_ Dar acceso al contador a las facturas generadas por los pagos de los estudiantes |
| `ACT-CONT-001` | CONTADOR | Actividades del contador | **Panel de pagos, facturas y gestión de reembolsos** — _para_ Controlar el flujo financiero y atender solicitudes de reembolso de estudiantes |
| `ACT-CONT-002` | CONTADOR | Actividades del contador | **Generar reportes financieros por período** — _para_ Obtener resúmenes de ingresos, facturas y reembolsos para control contable |

### Representante institucional (Supervisor)

| ID | Rol | Submódulo | Historia |
|---|---|---|---|
| `SOL-INS-001` | ADM | Registro de institución | **Registrar una institución con sus datos, cupos contratados y un código de afiliación de 8 caracteres generado por el sistema** — _para_ Creación y activación del perfil institucional en la base de datos |
| `ACT-SUP-001` | SUPERVISOR | Actividades del supervisor | **Monitorear el desempeño de estudiantes: avance, promedio y actividades** — _para_ Facilitar el seguimiento académico de los estudiantes de la institución |
| `ACT-SUP-002` | SUPERVISOR | Actividades del supervisor | **Participar en foros y verificar certificados de sus estudiantes** — _para_ Acompañar el proceso educativo y validar los logros de los estudiantes |

### Docente institucional

| ID | Rol | Submódulo | Historia |
|---|---|---|---|
| `SOL-DOC-001` | DOCENTE | Registro docente | **Activar la cuenta del docente institucional a partir de la invitación del Administrador** — _para_ Registro exitoso del docente en la base de datos con acceso al material de su área |
| `ACT-DOCIN-001` | DOCENTE | Actividades del docente institucional | **Moderación y seguimiento de los foros de discusión** — _para_ Retroalimentación y correcciones de las respuestas de los estudiantes |
| `ACT-DOCIN-002` | DOCENTE | Actividades del docente institucional | **Generación de reporte académico de desempeño de estudiantes** — _para_ El sistema entrega un informe para facilitar el monitoreo del progreso estudiantil |

### Estudiante

| ID | Rol | Submódulo | Historia |
|---|---|---|---|
| `SOL-EST-001` | ESTUDIANTE | Registro y activación | **Registrarse (de forma independiente o con el código de su institución) y activar la cuenta verificando el correo** — _para_ Acceder a la plataforma; el pago se realiza al inscribirse a cada curso (ACT-EST-004) |
| `ACT-EST-001` | ESTUDIANTE | Acceso al contenido educativo | **Acceder a lecciones: lecturas, videos, laboratorios y juegos educativos** — _para_ Comprender los conceptos clave y completar el material educativo del curso |
| `ACT-EST-002` | ESTUDIANTE | Acceso al contenido educativo | **Realizar evaluaciones con retroalimentación al agotar los intentos** — _para_ Evaluar el aprendizaje y recibir orientación al finalizar todos los intentos disponibles |
| `ACT-EST-007` | ESTUDIANTE | Actividades | **Realizar y entregar actividades, ver calificación y retroalimentación del docente** — _para_ Completar las tareas asignadas y conocer el resultado del desempeño propio |
| `ACT-EST-003` | ESTUDIANTE | Participación académica | **Participar en foros: publicar aportes y responder compañeros** — _para_ Intercambiar conocimientos y recibir retroalimentación del docente y compañeros |
| `ACT-EST-004` | ESTUDIANTE | Participación académica | **Inscribirse a cursos: catálogo, detalle, carrito y pago** — _para_ Acceder a cursos adicionales y gestionar inscripciones con pago en línea |
| `ACT-EST-005` | ESTUDIANTE | Eventos y certificados | **Participar en eventos, registrar asistencia y descargar certificado** — _para_ Obtener reconocimiento por participación en eventos académicos virtuales |
| `ACT-EST-006` | ESTUDIANTE | Reportes y notas | **Ver progreso académico, solicitar reporte y gestionar certificados** — _para_ Monitorear el desempeño académico y acceder a documentos oficiales de la plataforma |

### Mensajería (todos los roles)

| ID | Rol | Submódulo | Historia |
|---|---|---|---|
| `ACT-MSG-001` | TODOS | Mensajería interna — todos los roles | **Enviar y recibir mensajes internos entre usuarios del sistema** — _para_ Permitir comunicación directa entre cualquier par de usuarios sin importar su rol |

### Retroalimentación

| ID | Rol | Submódulo | Historia |
|---|---|---|---|
| `ACT-RET-001` | DOCENTE / SUPERVISOR | Retroalimentación formal del docente | **Dar retroalimentación formal sobre evaluaciones, actividades, foros y sesiones** — _para_ Orientar el proceso de aprendizaje del estudiante con observaciones y sugerencias estructuradas |
| `ACT-RET-002` | ESTUDIANTE | Retroalimentación recibida por el estudiante | **Ver y leer la retroalimentación formal recibida del docente o supervisor** — _para_ Conocer las observaciones y sugerencias para mejorar el desempeño académico |

### Perfil y Seguridad (todos los roles)

| ID | Rol | Submódulo | Historia |
|---|---|---|---|
| `ACT-PERF-001` | TODOS | Gestión de perfil y seguridad de la cuenta | **Editar perfil, actualizar foto y gestionar contraseña** — _para_ Mantener la información del usuario actualizada y garantizar la seguridad de la cuenta |

---

## Todas las historias (sin clasificar)

Listado plano de las 32 historias, en formato "Como [rol], quiero [funcionalidad], para [razón]".

- **`SOL-ADM-001`** — Como ADM, quiero autenticarme con una cuenta de administrador aprovisionada por script (no existe registro público de administradores), para permitir la gestión del contenido educativo y supervisión del sistema.
- **`SOL-ADM-002`** — Como ADM, quiero gestión de usuarios: crear, editar, activar y desactivar cuentas, para administrar el acceso y permisos de todos los roles del sistema.
- **`ACT-ADM-001`** — Como ADM, quiero crear, editar y eliminar cursos desde el panel de administración, para gestionar el catálogo de cursos disponibles en la plataforma.
- **`SOL-ADM-003`** — Como ADM, quiero registrar y editar instituciones con sus cupos contratados y asignarles un representante activo, para administrar las instituciones educativas afiliadas a la plataforma.
- **`SOL-ADM-004`** — Como ADM, quiero generación de reportes académicos, financieros e institucionales, para obtener información consolidada del sistema para la toma de decisiones.
- **`SOL-DOC-001`** — Como DOCENTE, quiero activar mi cuenta a partir de la invitación del Administrador definiendo mi propia contraseña, para acceder a las herramientas de enseñanza de la plataforma.
- **`ACT-DOC-001`** — Como DOCENTE, quiero crear curso, lecciones y publicar contenido con imagen de portada, para diseñar y publicar el contenido educativo para los estudiantes.
- **`ACT-DOC-002`** — Como DOCENTE, quiero crear evaluaciones con preguntas y explicación de respuesta, para evaluar el aprendizaje con retroalimentación disponible al agotar los intentos.
- **`ACT-DOC-003`** — Como DOCENTE, quiero moderar foros: leer aportes completos y responder estudiantes, para retroalimentar a los estudiantes y mantener la calidad del debate académico.
- **`ACT-DOC-005`** — Como DOCENTE, quiero crear sesiones en línea, generar código de asistencia y emitir certificados, para facilitar clases en vivo y certificar la participación de los estudiantes.
- **`ACT-DOC-004`** — Como DOCENTE, quiero seguimiento académico y generación de reportes del curso, para monitorear el progreso de los estudiantes y generar informes de desempeño.
- **`SOL-ADM-012`** — Como ADM, quiero activar usuario contador que podrá ingresar y visualizar pagos y facturas, para dar acceso al contador a las facturas generadas por los pagos de los estudiantes.
- **`ACT-CONT-001`** — Como CONTADOR, quiero panel de pagos, facturas y gestión de reembolsos, para controlar el flujo financiero y atender solicitudes de reembolso de estudiantes.
- **`ACT-CONT-002`** — Como CONTADOR, quiero generar reportes financieros por período, para obtener resúmenes de ingresos, facturas y reembolsos para control contable.
- **`SOL-INS-001`** — Como ADM, quiero registrar una institución con sus datos, cupos contratados y un código de afiliación de 8 caracteres generado por el sistema, para creación y activación del perfil institucional en la base de datos.
- **`ACT-SUP-001`** — Como SUPERVISOR, quiero monitorear el desempeño de estudiantes: avance, promedio y actividades, para facilitar el seguimiento académico de los estudiantes de la institución.
- **`ACT-SUP-002`** — Como SUPERVISOR, quiero participar en foros y verificar certificados de sus estudiantes, para acompañar el proceso educativo y validar los logros de los estudiantes.
- **`SOL-DOC-001`** — Como DOCENTE institucional, quiero activar mi cuenta a partir de la invitación del Administrador, para acceder al material de mi área y los cursos que dicto.
- **`ACT-DOCIN-001`** — Como DOCENTE, quiero moderación y seguimiento de los foros de discusión, para retroalimentación y correcciones de las respuestas de los estudiantes.
- **`ACT-DOCIN-002`** — Como DOCENTE, quiero generación de reporte académico de desempeño de estudiantes, para el sistema entrega un informe para facilitar el monitoreo del progreso estudiantil.
- **`SOL-EST-001`** — Como ESTUDIANTE, quiero registrarme de forma independiente o con el código de mi institución y activar mi cuenta verificando el correo, para acceder a la plataforma (el pago se realiza al inscribirme a cada curso).
- **`ACT-EST-001`** — Como ESTUDIANTE, quiero acceder a lecciones: lecturas, videos, laboratorios y juegos educativos, para comprender los conceptos clave y completar el material educativo del curso.
- **`ACT-EST-002`** — Como ESTUDIANTE, quiero realizar evaluaciones con retroalimentación al agotar los intentos, para evaluar el aprendizaje y recibir orientación al finalizar todos los intentos disponibles.
- **`ACT-EST-007`** — Como ESTUDIANTE, quiero realizar y entregar actividades, ver calificación y retroalimentación del docente, para completar las tareas asignadas y conocer el resultado del desempeño propio.
- **`ACT-EST-003`** — Como ESTUDIANTE, quiero participar en foros: publicar aportes y responder compañeros, para intercambiar conocimientos y recibir retroalimentación del docente y compañeros.
- **`ACT-EST-004`** — Como ESTUDIANTE, quiero inscribirse a cursos: catálogo, detalle, carrito y pago, para acceder a cursos adicionales y gestionar inscripciones con pago en línea.
- **`ACT-EST-005`** — Como ESTUDIANTE, quiero participar en eventos, registrar asistencia y descargar certificado, para obtener reconocimiento por participación en eventos académicos virtuales.
- **`ACT-EST-006`** — Como ESTUDIANTE, quiero ver progreso académico, solicitar reporte y gestionar certificados, para monitorear el desempeño académico y acceder a documentos oficiales de la plataforma.
- **`ACT-MSG-001`** — Como usuario del sistema, quiero enviar y recibir mensajes internos entre usuarios del sistema, para permitir comunicación directa entre cualquier par de usuarios sin importar su rol.
- **`ACT-RET-001`** — Como DOCENTE / SUPERVISOR, quiero dar retroalimentación formal sobre evaluaciones, actividades, foros y sesiones, para orientar el proceso de aprendizaje del estudiante con observaciones y sugerencias estructuradas.
- **`ACT-RET-002`** — Como ESTUDIANTE, quiero ver y leer la retroalimentación formal recibida del docente o supervisor, para conocer las observaciones y sugerencias para mejorar el desempeño académico.
- **`ACT-PERF-001`** — Como usuario del sistema, quiero editar perfil, actualizar foto y gestionar contraseña, para mantener la información del usuario actualizada y garantizar la seguridad de la cuenta.