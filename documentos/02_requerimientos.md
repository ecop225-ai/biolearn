# BioLearn — Requerimientos del sistema

Los requerimientos funcionales se derivan directamente de las historias de usuario (`biolearn_historias_usuario.xlsx`); los no funcionales se basan en las decisiones de arquitectura y seguridad ya documentadas para el sistema.

---

## 1. Requerimientos funcionales

### Módulo: Administrador

| Código | Requerimiento |
|---|---|
| RF-001 | El sistema debe permitir a **ADM** autenticarse con una cuenta de administrador aprovisionada por script (sin registro público de administradores), con el fin de permitir la gestión del contenido educativo y supervisión del sistema. |
| RF-002 | El sistema debe permitir a **ADM** gestión de usuarios: crear, editar, activar y desactivar cuentas, con el fin de administrar el acceso y permisos de todos los roles del sistema. |
| RF-003 | El sistema debe permitir a **ADM** crear, editar y eliminar cursos desde el panel de administración, con el fin de gestionar el catálogo de cursos disponibles en la plataforma. |
| RF-004 | El sistema debe permitir a **ADM** registrar y editar instituciones con sus cupos contratados, y asignar a cada una como máximo un representante (supervisor) activo, con el fin de administrar las instituciones educativas afiliadas a la plataforma. |
| RF-005 | El sistema debe permitir a **ADM** generación de reportes académicos, financieros e institucionales, con el fin de obtener información consolidada del sistema para la toma de decisiones. |

### Módulo: Docente

| Código | Requerimiento |
|---|---|
| RF-006 | El sistema debe permitir a **DOCENTE** activar la cuenta creada por el Administrador a partir de un enlace de invitación, definiendo su propia contraseña, con el fin de acceder a las herramientas de enseñanza de la plataforma. |
| RF-007 | El sistema debe permitir a **DOCENTE** crear curso, lecciones y publicar contenido con imagen de portada, con el fin de diseñar y publicar el contenido educativo para los estudiantes. |
| RF-008 | El sistema debe permitir a **DOCENTE** crear evaluaciones con preguntas y explicación de respuesta, con el fin de evaluar el aprendizaje con retroalimentación disponible al agotar los intentos. |
| RF-009 | El sistema debe permitir a **DOCENTE** moderar foros: leer aportes completos y responder estudiantes, con el fin de retroalimentar a los estudiantes y mantener la calidad del debate académico. |
| RF-010 | El sistema debe permitir a **DOCENTE** crear sesiones en línea, generar código de asistencia y emitir certificados, con el fin de facilitar clases en vivo y certificar la participación de los estudiantes. |
| RF-011 | El sistema debe permitir a **DOCENTE** seguimiento académico y generación de reportes del curso, con el fin de monitorear el progreso de los estudiantes y generar informes de desempeño. |

### Módulo: Contador

| Código | Requerimiento |
|---|---|
| RF-012 | El sistema debe permitir a **ADM** activar usuario contador que podrá ingresar y visualizar pagos y facturas, con el fin de dar acceso al contador a las facturas generadas por los pagos de los estudiantes. |
| RF-013 | El sistema debe permitir a **CONTADOR** panel de pagos, facturas y gestión de reembolsos, con el fin de controlar el flujo financiero y atender solicitudes de reembolso de estudiantes. |
| RF-014 | El sistema debe permitir a **CONTADOR** generar reportes financieros por período, con el fin de obtener resúmenes de ingresos, facturas y reembolsos para control contable. |

### Módulo: Representante institucional (Supervisor)

| Código | Requerimiento |
|---|---|
| RF-015 | El sistema debe permitir a **ADM** registrar una institución con sus datos y cupos contratados, generando automáticamente un código de afiliación único de 8 caracteres, con el fin de creación y activación del perfil institucional en la base de datos. |
| RF-016 | El sistema debe permitir a **SUPERVISOR** monitorear el desempeño de estudiantes: avance, promedio y actividades, con el fin de facilitar el seguimiento académico de los estudiantes de la institución. |
| RF-017 | El sistema debe permitir a **SUPERVISOR** participar en foros y verificar certificados de sus estudiantes, con el fin de acompañar el proceso educativo y validar los logros de los estudiantes. |

### Módulo: Docente institucional

| Código | Requerimiento |
|---|---|
| RF-018 | El sistema debe permitir a **DOCENTE** institucional activar su cuenta a partir de la invitación del Administrador, con el fin de registro exitoso del docente en la base de datos con acceso al material de su área. |
| RF-019 | El sistema debe permitir a **DOCENTE** moderación y seguimiento de los foros de discusión, con el fin de retroalimentación y correcciones de las respuestas de los estudiantes. |
| RF-020 | El sistema debe permitir a **DOCENTE** generación de reporte académico de desempeño de estudiantes, con el fin de el sistema entrega un informe para facilitar el monitoreo del progreso estudiantil. |

### Módulo: Estudiante

| Código | Requerimiento |
|---|---|
| RF-021 | El sistema debe permitir a **ESTUDIANTE** registrarse de forma independiente o con el código de su institución (validando que la institución esté activa y tenga cupos) y activar su cuenta verificando el correo; una cuenta sin verificar no puede iniciar sesión. |
| RF-022 | El sistema debe permitir a **ESTUDIANTE** acceder a lecciones: lecturas, videos, laboratorios y juegos educativos, con el fin de comprender los conceptos clave y completar el material educativo del curso. |
| RF-023 | El sistema debe permitir a **ESTUDIANTE** realizar evaluaciones con retroalimentación al agotar los intentos, con el fin de evaluar el aprendizaje y recibir orientación al finalizar todos los intentos disponibles. |
| RF-024 | El sistema debe permitir a **ESTUDIANTE** realizar y entregar actividades, ver calificación y retroalimentación del docente, con el fin de completar las tareas asignadas y conocer el resultado del desempeño propio. |
| RF-025 | El sistema debe permitir a **ESTUDIANTE** participar en foros: publicar aportes y responder compañeros, con el fin de intercambiar conocimientos y recibir retroalimentación del docente y compañeros. |
| RF-026 | El sistema debe permitir a **ESTUDIANTE** inscribirse a cursos: catálogo, detalle, carrito y pago, con el fin de acceder a cursos adicionales y gestionar inscripciones con pago en línea. |
| RF-027 | El sistema debe permitir a **ESTUDIANTE** participar en eventos, registrar asistencia y descargar certificado, con el fin de obtener reconocimiento por participación en eventos académicos virtuales. |
| RF-028 | El sistema debe permitir a **ESTUDIANTE** ver progreso académico, solicitar reporte y gestionar certificados, con el fin de monitorear el desempeño académico y acceder a documentos oficiales de la plataforma. |

### Módulo: Mensajería (todos los roles)

| Código | Requerimiento |
|---|---|
| RF-029 | El sistema debe permitir a **cualquier usuario autenticado** enviar y recibir mensajes internos entre usuarios del sistema, con el fin de permitir comunicación directa entre cualquier par de usuarios sin importar su rol. |

### Módulo: Retroalimentación

| Código | Requerimiento |
|---|---|
| RF-030 | El sistema debe permitir a **DOCENTE / SUPERVISOR** dar retroalimentación formal sobre evaluaciones, actividades, foros y sesiones, con el fin de orientar el proceso de aprendizaje del estudiante con observaciones y sugerencias estructuradas. |
| RF-031 | El sistema debe permitir a **ESTUDIANTE** ver y leer la retroalimentación formal recibida del docente o supervisor, con el fin de conocer las observaciones y sugerencias para mejorar el desempeño académico. |

### Módulo: Perfil y Seguridad (todos los roles)

| Código | Requerimiento |
|---|---|
| RF-032 | El sistema debe permitir a **cualquier usuario autenticado** editar perfil, actualizar foto y gestionar contraseña, con el fin de mantener la información del usuario actualizada y garantizar la seguridad de la cuenta. |

---

## 2. Requerimientos no funcionales

### Seguridad

| Código | Requerimiento |
|---|---|
| RNF-001 | El sistema debe almacenar las contraseñas cifradas con BCrypt, nunca en texto plano. |
| RNF-002 | El sistema debe usar autenticación basada en tokens JWT con expiración configurable (8 horas por defecto). |
| RNF-003 | El sistema debe restringir cada operación según el rol del usuario autenticado (ADMIN, DOCENTE, CONTADOR, SUPERVISOR, ESTUDIANTE), tanto a nivel de endpoint como de datos (ej. un supervisor solo accede a estudiantes de su propia institución). |
| RNF-004 | El sistema debe ofrecer un mecanismo de recuperación de contraseña mediante enlace de un solo uso con expiración corta (30 minutos). |
| RNF-005 | El sistema debe validar el tipo real y el tamaño máximo (10 MB) de cualquier archivo adjunto antes de almacenarlo. |
| RNF-006 | El sistema debe cumplir con la Ley 1581 de 2012 de Colombia sobre tratamiento de datos personales, incluyendo el manejo especial de datos de menores de edad. |

### Disponibilidad y rendimiento

| Código | Requerimiento |
|---|---|
| RNF-007 | El sistema debe estar disponible para los 5 roles de usuario de forma concurrente sin degradación perceptible del tiempo de respuesta. |
| RNF-008 | Las consultas de listados (cursos, usuarios, pagos) no deben superar 2 segundos de respuesta bajo condiciones normales de carga. |
| RNF-009 | El sistema debe soportar el crecimiento del catálogo de cursos e instituciones sin requerir cambios estructurales en la base de datos. |

### Usabilidad

| Código | Requerimiento |
|---|---|
| RNF-010 | La interfaz debe adaptar su navegación y funcionalidades disponibles según el rol del usuario autenticado. |
| RNF-011 | El sistema debe notificar al usuario (notificaciones en la aplicación y por correo) ante eventos relevantes: pagos, calificaciones, respuestas en foros, invitaciones de cuenta. |
| RNF-012 | Los formularios de registro y pago deben validar la información en tiempo real antes de permitir el envío. |

### Compatibilidad

| Código | Requerimiento |
|---|---|
| RNF-013 | El frontend debe ser una aplicación web responsiva, accesible desde navegadores de escritorio y dispositivos móviles. |
| RNF-014 | El sistema debe integrarse con pasarelas de pago externas (PSE, Wompi) para el procesamiento de matrículas. |
| RNF-015 | El sistema debe permitir embeber contenido de plataformas externas de videoconferencia (Google Meet, Zoom, Teams) y de YouTube para material audiovisual. |

### Mantenibilidad

| Código | Requerimiento |
|---|---|
| RNF-016 | El backend debe estar organizado en capas (presentación, API, seguridad, negocio, acceso a datos) para permitir modificaciones aisladas por capa. |
| RNF-017 | El código fuente debe versionarse en un repositorio Git, con historial de cambios trazable por historia de usuario. |
| RNF-018 | La base de datos debe mantener un diccionario de datos y diagramas (clases, ER, componentes) actualizados junto con el script de creación. |

### Auditoría y trazabilidad

| Código | Requerimiento |
|---|---|
| RNF-019 | El sistema debe registrar el historial de accesos de cada usuario (fecha, evento). |
| RNF-020 | El sistema debe conservar trazabilidad de quién aprueba o rechaza un reembolso, y su justificación. |
| RNF-021 | Cada factura debe conservar una copia histórica de los datos de facturación vigentes al momento de su emisión, independiente de cambios posteriores. |
| RNF-022 | Todas las contraseñas definidas por usuarios (registro, restablecimiento y activación por invitación) deben tener mínimo 8 caracteres, al menos una mayúscula y al menos un número; la política se valida tanto en el frontend como en el backend. |
| RNF-023 | La desactivación o suspensión de una cuenta debe invalidar de inmediato sus sesiones activas (tokens JWT emitidos). |
