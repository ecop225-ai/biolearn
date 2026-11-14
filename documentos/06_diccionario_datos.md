# BioLearn — Diccionario de datos (esquema final, MySQL)

Basado en `biolearn_database.sql`. Cubre las 51 tablas agrupadas en 8 dominios. Generado automáticamente a partir del script para garantizar que coincide al 100%.

---

## 0. Los 8 módulos de BioLearn

**1. Usuarios, seguridad y perfiles por rol** — Identidad central del sistema. La tabla `usuario` guarda lo común a los 5 roles, y cada uno tiene su propio perfil enlazado con los datos que le son propios. Incluye también el logo propio de cada institución. También vive aquí toda la seguridad de acceso: verificación de cuenta, recuperación de contraseña, códigos OTP e historial de accesos.

**2. Cursos y contenido académico** — Los cursos organizados por área y grado, con sus lecciones y todo el contenido que las compone: materiales, recursos de apoyo y artículos científicos. Incluye el horario de un curso y varios docentes colaboradores además del titular.

**3. Actividades y evaluaciones** — Tareas para entregar y calificar, y evaluaciones tipo quiz con preguntas, opciones, intentos y resultados, incluidas las preguntas abiertas que un docente revisa manualmente.

**4. Foros** — Discusión dentro de cada lección: aportes con respuestas anidadas, sus adjuntos, y la calificación de participación cuando aplica.

**5. Sesiones, eventos, asistencia y certificados** — Clases en vivo, eventos institucionales, registro de asistencia (a sesión o a evento), y los certificados emitidos con su plantilla visual personalizable.

**6. Inscripciones y progreso** — El vínculo entre un estudiante y un curso, y cómo va avanzando lección por lección. Alimenta las barras de progreso del estudiante y el seguimiento del Representante institucional.

**7. Finanzas** — Datos de facturación de BioLearn (con historial: cada factura guarda una foto de qué razón social/NIT/logo tenía en el momento de emitirse), pagos (incluyendo carritos de varias inscripciones), facturas y reembolsos.

**8. Comunicación y reportes** — Mensajería interna, notificaciones del sistema, y los reportes académicos/financieros/institucionales — que ahora pueden llevar tanto el logo de BioLearn como el de la institución sobre la que tratan.

---

## 1. Usuarios, seguridad y perfiles por rol

### `usuario`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_usuario | INT UNSIGNED | No | **PK** |
| nombre | VARCHAR(100) | No |  |
| apellido | VARCHAR(100) | No |  |
| nombre_usuario | VARCHAR(60) | No |  |
| correo | VARCHAR(150) | No |  |
| contrasena | VARCHAR(255) | No |  |
| tipo_usuario | ENUM('admin','docente','estudiante','supervisor','contador') | No |  |
| estado | ENUM('pendiente','activo','inactivo','suspendido') | No |  |
| doc_identidad | VARCHAR(30) | Sí |  |
| fecha_nacimiento | DATE | Sí |  |
| telefono | VARCHAR(20) | Sí |  |
| direccion | VARCHAR(200) | Sí |  |
| ciudad | VARCHAR(100) | Sí |  |
| foto_perfil | VARCHAR(500) | Sí |  |
| rol_detalle | VARCHAR(100) | Sí |  |
| fecha_registro | DATETIME | No |  |
| fecha_actualizacion | DATETIME | No |  |

### `area_academica`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_area | TINYINT UNSIGNED | No | **PK** |
| nombre | VARCHAR(100) | No |  |
| descripcion | TEXT | Sí |  |
| estado | ENUM('activo','inactivo') | No |  |
| fecha_creacion | DATETIME | No |  |
| fecha_actualizacion | DATETIME | No |  |

### `grado`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_grado | TINYINT UNSIGNED | No | **PK** |
| nombre | VARCHAR(60) | No |  |
| nivel | ENUM('primaria','secundaria','media') | No |  |
| orden | TINYINT UNSIGNED | No |  |
| descripcion | TEXT | Sí |  |
| estado | ENUM('activo','inactivo') | No |  |
| fecha_creacion | DATETIME | No |  |
| fecha_actualizacion | DATETIME | No |  |

### `historial_acceso`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_historial | INT UNSIGNED | No | **PK** |
| id_usuario | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| accion | ENUM('login','logout','fallo_login','cambio_contrasena') | No |  |
| estado_sesion | ENUM('exitosa','fallida','expirada') | No |  |
| ip_origen | VARCHAR(45) | Sí |  |
| dispositivo | VARCHAR(150) | Sí |  |
| navegador | VARCHAR(100) | Sí |  |
| ubicacion | VARCHAR(100) | Sí |  |
| duracion_minutos | SMALLINT UNSIGNED | Sí |  |
| hash_token_sesion | VARCHAR(64) | Sí |  |
| fecha_acceso | DATETIME | No |  |

### `sesion_otp`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_sesion_otp | INT UNSIGNED | No | **PK** |
| id_usuario | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| codigo | CHAR(6) | No |  |
| expira_en | DATETIME | No |  |
| usado | TINYINT(1) | No |  |
| creado_en | DATETIME | No |  |

### `verificacion_cuenta`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_verificacion | INT UNSIGNED | No | **PK** |
| id_usuario | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| token | VARCHAR(128) | No |  |
| tipo | ENUM('activacion','cambio_correo') | No |  |
| estado | ENUM('pendiente','usado','expirado') | No |  |
| expira_en | DATETIME | No |  |
| usado_en | DATETIME | Sí |  |
| creado_en | DATETIME | No |  |

### `recuperacion_contrasena`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_recuperacion | INT UNSIGNED | No | **PK** |
| id_usuario | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| token | VARCHAR(128) | No |  |
| estado | ENUM('pendiente','usado','expirado') | No |  |
| ip_solicitud | VARCHAR(45) | Sí |  |
| expira_en | DATETIME | No |  |
| usado_en | DATETIME | Sí |  |
| creado_en | DATETIME | No |  |

### `institucion`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_institucion | INT UNSIGNED | No | **PK** |
| nombre | VARCHAR(200) | No |  |
| nit | VARCHAR(20) | No |  |
| rector | VARCHAR(200) | Sí |  |
| direccion | VARCHAR(300) | Sí |  |
| telefono | VARCHAR(20) | Sí |  |
| correo | VARCHAR(150) | Sí |  |
| logo_url | VARCHAR(500) | Sí |  |
| codigo_afiliacion | CHAR(8) | No | UNIQUE. Generado por el sistema: 8 caracteres de `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (sin I, O, 0, 1 para evitar confusiones al dictarlo) |
| cupos_contratados | SMALLINT UNSIGNED | Sí | Cupos de estudiantes contratados. NULL = sin límite (instituciones anteriores a la migración 001). Los cupos usados no se almacenan: se calculan contando `perfil_estudiante` activos de la institución |
| estado | ENUM('activa','inactiva','suspendida') | No |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `perfil_admin`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_admin | INT UNSIGNED | No | **PK** |
| id_usuario | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| rol_admin | ENUM('superadmin','admin_institucional','admin_contenido') | No |  |
| estado | ENUM('activo','inactivo') | No |  |
| asignado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `perfil_docente`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_docente | INT UNSIGNED | No | **PK** |
| id_usuario | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| especialidad | VARCHAR(150) | Sí |  |
| experiencia_laboral | TEXT | Sí |  |
| tipo_contrato | ENUM('planta','contratista','honorarios') | No |  |
| estado | ENUM('activo','inactivo','suspendido') | No |  |
| asignado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `perfil_contador`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_contador | INT UNSIGNED | No | **PK** |
| id_usuario | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| rol_contador | ENUM('administrador_financiero','auxiliar','auditor') | No |  |
| estado | ENUM('activo','inactivo') | No |  |
| asignado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `perfil_estudiante`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_estudiante | INT UNSIGNED | No | **PK** |
| id_usuario | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| id_institucion | INT UNSIGNED | Sí | FK → `institucion.id_institucion` |
| id_grado | TINYINT UNSIGNED | No | FK → `grado.id_grado` |
| id_area | TINYINT UNSIGNED | No | FK → `area_academica.id_area` |
| estado | ENUM('activo','inactivo','egresado') | No |  |
| fecha_ingreso | DATE | Sí |  |
| actualizado_en | DATETIME | No |  |

### `supervisor_institucion`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_supervisor_inst | INT UNSIGNED | No | **PK** |
| id_usuario | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| id_institucion | INT UNSIGNED | No | FK → `institucion.id_institucion` |
| cargo | VARCHAR(150) | Sí |  |
| acceso_reportes | TINYINT(1) | No |  |
| acceso_foros | TINYINT(1) | No |  |
| estado | ENUM('activo','inactivo','suspendido') | No | Sincronizado con `usuario.estado` del supervisor. Regla de negocio (validada en el servicio): como máximo UN registro `activo` por institución |
| asignado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `historial_grado`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_historial_grado | INT UNSIGNED | No | **PK** |
| id_estudiante | INT UNSIGNED | No | FK → `perfil_estudiante.id_estudiante` |
| id_grado | TINYINT UNSIGNED | No | FK → `grado.id_grado` |
| id_institucion | INT UNSIGNED | Sí | FK → `institucion.id_institucion` |
| fecha_inicio | DATE | No |  |
| fecha_fin | DATE | Sí |  |
| observaciones | TEXT | Sí |  |
| registrado_en | DATETIME | No |  |

## 2. Cursos y contenido académico

### `curso`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_curso | INT UNSIGNED | No | **PK** |
| id_grado | TINYINT UNSIGNED | No | FK → `grado.id_grado` |
| id_area | TINYINT UNSIGNED | No | FK → `area_academica.id_area` |
| id_admin_creador | INT UNSIGNED | Sí | FK → `perfil_admin.id_admin` |
| id_docente_titular | INT UNSIGNED | Sí | FK → `perfil_docente.id_docente` |
| nombre | VARCHAR(150) | No |  |
| imagen_portada | VARCHAR(500) | Sí |  |
| descripcion | TEXT | No |  |
| objetivos | TEXT | No |  |
| precio | DECIMAL(10,2) | No |  |
| creditos | TINYINT UNSIGNED | No |  |
| horas_estimadas | SMALLINT UNSIGNED | Sí |  |
| estado | ENUM('borrador','activo','cerrado','archivado') | No |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `institucion_curso_contratado`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_institucion_curso | INT UNSIGNED | No | **PK** |
| id_institucion | INT UNSIGNED | No | FK → `institucion.id_institucion` |
| id_curso | INT UNSIGNED | No | FK → `curso.id_curso` |
| contratado_en | DATETIME | No |  |

### `curso_docente`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_curso_docente | INT UNSIGNED | No | **PK** |
| id_curso | INT UNSIGNED | No | FK → `curso.id_curso` |
| id_docente | INT UNSIGNED | No | FK → `perfil_docente.id_docente` |
| rol | ENUM('titular','colaborador','invitado') | No |  |
| estado | ENUM('activo','inactivo') | No |  |
| asignado_en | DATETIME | No |  |

### `horario_curso`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_horario | INT UNSIGNED | No | **PK** |
| id_curso | INT UNSIGNED | No | FK → `curso.id_curso` |
| dia_semana | ENUM('lunes','martes','miercoles','jueves','viernes','sabado','domingo') | No |  |
| hora_inicio | TIME | No |  |
| hora_fin | TIME | No |  |

### `leccion`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_leccion | INT UNSIGNED | No | **PK** |
| id_curso | INT UNSIGNED | No | FK → `curso.id_curso` |
| id_prerequisito | INT UNSIGNED | Sí | FK → `leccion.id_leccion` |
| nombre | VARCHAR(150) | No |  |
| imagen_portada | VARCHAR(500) | Sí |  |
| descripcion | TEXT | Sí |  |
| orden | SMALLINT UNSIGNED | No |  |
| tipo | ENUM('teoria','practica','evaluacion','mixta') | No |  |
| estado | ENUM('borrador','activa','inactiva','archivada') | No |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `material_contenido`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_material | INT UNSIGNED | No | **PK** |
| id_leccion | INT UNSIGNED | No | FK → `leccion.id_leccion` |
| id_docente | INT UNSIGNED | No | FK → `perfil_docente.id_docente` |
| nombre | VARCHAR(250) | No |  |
| tipo | ENUM('lectura','video','laboratorio','juego','documento','articulo') | No |  |
| descripcion | TEXT | Sí |  |
| orden | TINYINT UNSIGNED | No |  |
| estado | ENUM('activo','inactivo','archivado') | No |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `recurso`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_recurso | INT UNSIGNED | No | **PK** |
| id_material | INT UNSIGNED | No | FK → `material_contenido.id_material` |
| id_docente | INT UNSIGNED | No | FK → `perfil_docente.id_docente` |
| nombre | VARCHAR(200) | No |  |
| descripcion | TEXT | Sí |  |
| url | VARCHAR(1000) | No |  |
| tipo_recurso | ENUM('video','documento','imagen','audio','enlace','interactivo') | No |  |
| duracion_min | SMALLINT UNSIGNED | Sí |  |
| nivel_academico | ENUM('basico','intermedio','avanzado') | Sí |  |
| orden | TINYINT UNSIGNED | No |  |
| estado | ENUM('activo','inactivo') | No |  |
| creado_en | DATETIME | No |  |

### `articulo_cientifico`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_articulo | INT UNSIGNED | No | **PK** |
| id_material | INT UNSIGNED | No | FK → `material_contenido.id_material` |
| id_docente | INT UNSIGNED | No | FK → `perfil_docente.id_docente` |
| titulo | VARCHAR(300) | No |  |
| autores | VARCHAR(500) | No |  |
| resumen | TEXT | Sí |  |
| revista | VARCHAR(250) | Sí |  |
| doi | VARCHAR(100) | Sí |  |
| url | VARCHAR(1000) | Sí |  |
| fecha_publicacion | DATE | Sí |  |
| estado | ENUM('activo','inactivo') | No |  |
| creado_en | DATETIME | No |  |

### `recurso_apoyo`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_recurso_apoyo | INT UNSIGNED | No | **PK** |
| id_material | INT UNSIGNED | No | FK → `material_contenido.id_material` |
| id_recurso | INT UNSIGNED | Sí | FK → `recurso.id_recurso` |
| id_articulo | INT UNSIGNED | Sí | FK → `articulo_cientifico.id_articulo` |
| id_usuario | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| tipo_recurso | ENUM('documento','video','enlace','imagen','interactivo','otro') | No |  |
| tipo_contenido | ENUM('primario','complementario','referencia','evaluativo') | No |  |
| titulo | VARCHAR(250) | No |  |
| url | VARCHAR(500) | Sí |  |
| ruta_imagen | VARCHAR(500) | Sí |  |
| orden | TINYINT UNSIGNED | No |  |
| estado | ENUM('activo','inactivo') | No |  |
| fecha_creacion | DATETIME | No |  |
| fecha_actualizacion | DATETIME | No |  |

## 3. Actividades y evaluaciones

### `actividad`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_actividad | INT UNSIGNED | No | **PK** |
| id_leccion | INT UNSIGNED | No | FK → `leccion.id_leccion` |
| id_docente | INT UNSIGNED | No | FK → `perfil_docente.id_docente` |
| nombre | VARCHAR(250) | No |  |
| descripcion | TEXT | Sí |  |
| instrucciones | TEXT | Sí |  |
| tipo | ENUM('tarea','proyecto','investigacion','ejercicio','otro') | No |  |
| fecha_disponible | DATETIME | Sí |  |
| fecha_limite | DATETIME | Sí |  |
| permite_entrega_tardia | TINYINT(1) | No |  |
| puntaje_maximo | DECIMAL(10,2) | No |  |
| estado | ENUM('activa','inactiva','cerrada') | No |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `entrega_actividad`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_entrega | INT UNSIGNED | No | **PK** |
| id_actividad | INT UNSIGNED | No | FK → `actividad.id_actividad` |
| id_estudiante | INT UNSIGNED | No | FK → `perfil_estudiante.id_estudiante` |
| id_revisor | INT UNSIGNED | Sí | FK → `perfil_docente.id_docente` |
| nombre_archivo | VARCHAR(250) | No |  |
| url_archivo | VARCHAR(1000) | No |  |
| tipo_archivo | ENUM('documento','video','audio','imagen','presentacion','otro') | No |  |
| tamanio_kb | INT UNSIGNED | Sí |  |
| es_tardia | TINYINT(1) | No |  |
| calificacion | DECIMAL(10,2) | Sí |  |
| comentario_revisor | TEXT | Sí |  |
| fecha_entrega | DATETIME | No |  |
| fecha_revision | DATETIME | Sí |  |
| estado | ENUM('entregada','revisada','rechazada') | No |  |

### `evaluacion`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_evaluacion | INT UNSIGNED | No | **PK** |
| id_leccion | INT UNSIGNED | No | FK → `leccion.id_leccion` |
| id_docente | INT UNSIGNED | No | FK → `perfil_docente.id_docente` |
| nombre | VARCHAR(200) | No |  |
| descripcion | TEXT | Sí |  |
| tipo | ENUM('diagnostica','formativa','sumativa') | No |  |
| tiempo_limite_min | SMALLINT UNSIGNED | Sí |  |
| intentos_permitidos | TINYINT UNSIGNED | No |  |
| puntaje_maximo | DECIMAL(10,2) | No |  |
| ponderacion | DECIMAL(5,2) | No |  |
| fecha_aplicacion | DATETIME | Sí |  |
| fecha_apertura | DATETIME | Sí |  |
| fecha_cierre | DATETIME | Sí |  |
| estado | ENUM('borrador','activa','cerrada') | No |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `pregunta`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_pregunta | INT UNSIGNED | No | **PK** |
| id_evaluacion | INT UNSIGNED | No | FK → `evaluacion.id_evaluacion` |
| enunciado | TEXT | No |  |
| tipo | ENUM('seleccion_multiple','verdadero_falso','abierta') | No |  |
| puntaje | DECIMAL(10,2) | No |  |
| retroalimentacion | TEXT | Sí |  |
| orden | TINYINT UNSIGNED | No |  |
| estado | ENUM('activa','inactiva') | No |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `opcion_pregunta`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_opcion | INT UNSIGNED | No | **PK** |
| id_pregunta | INT UNSIGNED | No | FK → `pregunta.id_pregunta` |
| texto | VARCHAR(500) | No |  |
| es_correcta | TINYINT(1) | No |  |
| retroalimentacion | TEXT | Sí |  |
| orden | TINYINT UNSIGNED | No |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `resultado_evaluacion`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_resultado | INT UNSIGNED | No | **PK** |
| id_evaluacion | INT UNSIGNED | No | FK → `evaluacion.id_evaluacion` |
| id_inscripcion | INT UNSIGNED | No | FK → `inscripcion.id_inscripcion` |
| numero_intento | TINYINT UNSIGNED | No |  |
| puntaje_total | DECIMAL(10,2) | No |  |
| porcentaje | DECIMAL(5,2) | No |  |
| aprobado | TINYINT(1) | No |  |
| estado | ENUM('pendiente','calculado','publicado','revisado') | No |  |
| completado_en | DATETIME | No |  |

### `respuesta_estudiante`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_respuesta | INT UNSIGNED | No | **PK** |
| id_resultado | INT UNSIGNED | No | FK → `resultado_evaluacion.id_resultado` |
| id_pregunta | INT UNSIGNED | No | FK → `pregunta.id_pregunta` |
| id_opcion | INT UNSIGNED | Sí | FK → `opcion_pregunta.id_opcion` |
| id_revisor | INT UNSIGNED | Sí | FK → `perfil_docente.id_docente` |
| contenido_abierto | TEXT | Sí |  |
| estado_revision | ENUM('pendiente','correcta','incorrecta','parcial') | No |  |
| puntaje_obtenido | DECIMAL(10,2) | Sí |  |
| comentario_revisor | TEXT | Sí |  |
| revisado_en | DATETIME | Sí |  |

## 4. Foros

### `foro`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_foro | INT UNSIGNED | No | **PK** |
| id_leccion | INT UNSIGNED | No | FK → `leccion.id_leccion` |
| id_docente | INT UNSIGNED | No | FK → `perfil_docente.id_docente` |
| nombre | VARCHAR(250) | No |  |
| descripcion | TEXT | Sí |  |
| instrucciones | TEXT | Sí |  |
| tema | VARCHAR(250) | Sí |  |
| reglas_interaccion | TEXT | Sí |  |
| tipo_participante | ENUM('abierto','solo_estudiantes','moderado') | No |  |
| fecha_apertura | DATETIME | Sí |  |
| fecha_cierre | DATETIME | Sí |  |
| estado | ENUM('activo','cerrado','archivado') | No |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `comentario_foro`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_comentario | INT UNSIGNED | No | **PK** |
| id_foro | INT UNSIGNED | No | FK → `foro.id_foro` |
| id_usuario | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| id_padre | INT UNSIGNED | Sí | FK → `comentario_foro.id_comentario` |
| asunto | VARCHAR(250) | Sí |  |
| contenido | TEXT | No |  |
| tipo | ENUM('aporte','respuesta','retroalimentacion') | No |  |
| estado | ENUM('activo','moderado','eliminado') | No |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `adjunto_comentario`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_adjunto | INT UNSIGNED | No | **PK** |
| id_comentario | INT UNSIGNED | No | FK → `comentario_foro.id_comentario` |
| id_usuario | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| url | VARCHAR(1000) | No |  |
| tipo | ENUM('imagen','documento','video','enlace','otro') | No |  |
| nombre_archivo | VARCHAR(250) | Sí |  |
| tamanio_kb | INT UNSIGNED | Sí |  |
| creado_en | DATETIME | No |  |

### `calificacion_foro`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_calificacion | INT UNSIGNED | No | **PK** |
| id_foro | INT UNSIGNED | No | FK → `foro.id_foro` |
| id_inscripcion | INT UNSIGNED | No | FK → `inscripcion.id_inscripcion` |
| id_docente | INT UNSIGNED | No | FK → `perfil_docente.id_docente` |
| cantidad_aportes | SMALLINT UNSIGNED | No |  |
| calidad_aportes | ENUM('excelente','bueno','regular','insuficiente') | No |  |
| calificacion | DECIMAL(5,2) | No |  |
| retroalimentacion | TEXT | Sí |  |
| estado | ENUM('borrador','publicada') | No |  |
| calificado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

## 5. Sesiones, eventos, asistencia y certificados

### `sesion_online`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_sesion | INT UNSIGNED | No | **PK** |
| id_leccion | INT UNSIGNED | No | FK → `leccion.id_leccion` |
| id_docente | INT UNSIGNED | No | FK → `perfil_docente.id_docente` |
| nombre | VARCHAR(250) | No |  |
| tematica | VARCHAR(150) | Sí |  |
| descripcion | TEXT | Sí |  |
| enlace | VARCHAR(1000) | No |  |
| plataforma | ENUM('meet','youtube','zoom','teams','otro') | No |  |
| codigo_asistencia | VARCHAR(20) | Sí |  |
| fecha_inicio | DATETIME | No |  |
| fecha_fin | DATETIME | Sí |  |
| grabacion_url | VARCHAR(1000) | Sí |  |
| estado | ENUM('programada','en_curso','finalizada','cancelada') | No |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `evento_virtual`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_evento | INT UNSIGNED | No | **PK** |
| id_docente | INT UNSIGNED | No | FK → `perfil_docente.id_docente` |
| id_leccion | INT UNSIGNED | Sí | FK → `leccion.id_leccion` |
| nombre | VARCHAR(200) | No |  |
| descripcion | TEXT | Sí |  |
| enlace_acceso | VARCHAR(1000) | Sí |  |
| fecha_inicio | DATETIME | No |  |
| fecha_fin | DATETIME | Sí |  |
| emite_certificado | TINYINT(1) | No |  |
| estado | ENUM('programado','en_curso','finalizado','cancelado') | No |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `asistencia`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_asistencia | INT UNSIGNED | No | **PK** |
| id_usuario | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| id_sesion | INT UNSIGNED | Sí | FK → `sesion_online.id_sesion` |
| id_evento | INT UNSIGNED | Sí | FK → `evento_virtual.id_evento` |
| asistio | TINYINT(1) | No |  |
| codigo_ingresado | VARCHAR(20) | Sí |  |
| fecha_ingreso | DATETIME | Sí |  |
| fecha_salida | DATETIME | Sí |  |
| duracion_min | SMALLINT UNSIGNED | Sí |  |
| calificacion | DECIMAL(5,2) | Sí |  |
| comentarios | TEXT | Sí |  |
| nivel_participacion | ENUM('alto','medio','bajo','ninguno') | Sí |  |
| registrado_en | DATETIME | No |  |

### `certificado`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_certificado | INT UNSIGNED | No | **PK** |
| id_emisor | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| id_receptor | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| id_evento | INT UNSIGNED | Sí | FK → `evento_virtual.id_evento` |
| id_inscripcion | INT UNSIGNED | Sí | FK → `inscripcion.id_inscripcion` |
| id_plantilla | INT UNSIGNED | Sí | FK → `plantilla_certificado.id_plantilla` |
| nombre | VARCHAR(200) | No |  |
| codigo_validacion | VARCHAR(80) | No |  |
| estado | ENUM('emitido','anulado') | No |  |
| emitido_en | DATETIME | No |  |

### `plantilla_certificado`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_plantilla | INT UNSIGNED | No | **PK** |
| id_docente | INT UNSIGNED | Sí | FK → `perfil_docente.id_docente` |
| color_acento | VARCHAR(7) | Sí |  |
| logo_url | VARCHAR(500) | Sí |  |
| firma_representante_nombre | VARCHAR(150) | Sí |  |
| firma_imagen_url | VARCHAR(500) | Sí |  |
| texto_reconocimiento | TEXT | Sí |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

## 6. Inscripciones y progreso

### `inscripcion`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_inscripcion | INT UNSIGNED | No | **PK** |
| id_curso | INT UNSIGNED | No | FK → `curso.id_curso` |
| id_estudiante | INT UNSIGNED | No | FK → `perfil_estudiante.id_estudiante` |
| estado | ENUM('activa','completada','retirada','suspendida') | No |  |
| fecha_inscripcion | DATETIME | No |  |
| fecha_completacion | DATETIME | Sí |  |
| actualizado_en | DATETIME | No |  |

### `progreso_leccion`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_progreso | INT UNSIGNED | No | **PK** |
| id_inscripcion | INT UNSIGNED | No | FK → `inscripcion.id_inscripcion` |
| id_leccion | INT UNSIGNED | No | FK → `leccion.id_leccion` |
| estado | ENUM('pendiente','en_progreso','completada') | No |  |
| porcentaje | DECIMAL(5,2) | No |  |
| iniciado_en | DATETIME | Sí |  |
| completado_en | DATETIME | Sí |  |
| actualizado_en | DATETIME | No |  |

## 7. Finanzas

### `datos_facturacion`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_datos_facturacion | INT UNSIGNED | No | **PK** |
| razon_social | VARCHAR(200) | No |  |
| nit | VARCHAR(20) | No |  |
| direccion | VARCHAR(255) | Sí |  |
| logo_url | VARCHAR(500) | Sí |  |
| actualizado_en | DATETIME | No |  |

### `pago`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_pago | INT UNSIGNED | No | **PK** |
| id_estudiante | INT UNSIGNED | No | FK → `perfil_estudiante.id_estudiante` |
| id_contador | INT UNSIGNED | Sí | FK → `perfil_contador.id_contador` |
| medio_pago | ENUM('pse','wompi_tarjeta','wompi_nequi','transferencia','otro') | No |  |
| monto | DECIMAL(12,2) | No |  |
| fecha_pago | DATETIME | Sí |  |
| referencia_externa | VARCHAR(200) | Sí |  |
| estado | ENUM('pendiente','aprobado','rechazado','reembolsado') | No |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `pago_inscripcion`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_pago_inscripcion | INT UNSIGNED | No | **PK** |
| id_pago | INT UNSIGNED | No | FK → `pago.id_pago` |
| id_inscripcion | INT UNSIGNED | No | FK → `inscripcion.id_inscripcion` |
| monto_asignado | DECIMAL(12,2) | No |  |

### `factura`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_factura | INT UNSIGNED | No | **PK** |
| id_pago | INT UNSIGNED | No | FK → `pago.id_pago` |
| id_estudiante | INT UNSIGNED | No | FK → `perfil_estudiante.id_estudiante` |
| id_datos_facturacion | INT UNSIGNED | Sí | FK → `datos_facturacion.id_datos_facturacion` |
| numero_factura | VARCHAR(50) | No |  |
| descripcion | TEXT | Sí |  |
| monto_total | DECIMAL(12,2) | No |  |
| monto_pagado | DECIMAL(12,2) | Sí |  |
| estado | ENUM('emitida','anulada') | No |  |
| enviada_por_correo | TINYINT(1) | No |  |
| enviada_en | DATETIME | Sí |  |
| fecha_emision | DATETIME | No |  |

### `reembolso`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_reembolso | INT UNSIGNED | No | **PK** |
| id_pago | INT UNSIGNED | No | FK → `pago.id_pago` |
| id_solicitante | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| id_contador | INT UNSIGNED | Sí | FK → `perfil_contador.id_contador` |
| motivo | TEXT | No |  |
| monto | DECIMAL(12,2) | No |  |
| justificacion | TEXT | Sí |  |
| estado | ENUM('solicitado','aprobado','rechazado') | No |  |
| solicitado_en | DATETIME | No |  |
| resuelto_en | DATETIME | Sí |  |

## 8. Comunicación y reportes

### `mensaje`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_mensaje | INT UNSIGNED | No | **PK** |
| id_emisor | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| id_receptor | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| id_curso | INT UNSIGNED | Sí | FK → `curso.id_curso` |
| asunto | VARCHAR(200) | Sí |  |
| contenido | TEXT | No |  |
| leido | TINYINT(1) | No |  |
| leido_en | DATETIME | Sí |  |
| estado_emisor | ENUM('activo','eliminado') | No |  |
| estado_receptor | ENUM('activo','eliminado') | No |  |
| enviado_en | DATETIME | No |  |

### `notificacion`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_notificacion | INT UNSIGNED | No | **PK** |
| id_usuario | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| id_usuario_emisor | INT UNSIGNED | Sí | FK → `usuario.id_usuario` |
| tipo | ENUM('pago','inscripcion','leccion','evaluacion','foro','mensaje','certificado','reembolso','sistema') | No |  |
| entidad_id | INT UNSIGNED | Sí |  |
| titulo | VARCHAR(200) | No |  |
| contenido | TEXT | Sí |  |
| estado_notificacion | ENUM('pendiente','enviada','leida','archivada') | No |  |
| leida | TINYINT(1) | No |  |
| leida_en | DATETIME | Sí |  |
| creada_en | DATETIME | No |  |

### `reporte`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_reporte | INT UNSIGNED | No | **PK** |
| id_solicitante | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| id_curso | INT UNSIGNED | Sí | FK → `curso.id_curso` |
| id_inscripcion | INT UNSIGNED | Sí | FK → `inscripcion.id_inscripcion` |
| id_institucion | INT UNSIGNED | Sí | FK → `institucion.id_institucion` |
| id_supervisor_inst | INT UNSIGNED | Sí | FK → `supervisor_institucion.id_supervisor_inst` |
| id_contador | INT UNSIGNED | Sí | FK → `perfil_contador.id_contador` |
| id_factura | INT UNSIGNED | Sí | FK → `factura.id_factura` |
| id_datos_facturacion | INT UNSIGNED | Sí | FK → `datos_facturacion.id_datos_facturacion` |
| tipo | ENUM('academico','financiero','institucional','mixto') | No |  |
| formato | ENUM('pdf','excel','csv') | Sí |  |
| fecha_corte | DATETIME | Sí |  |
| motivo | TEXT | Sí |  |
| descripcion | TEXT | Sí |  |
| contenido | LONGTEXT | Sí |  |
| estado | ENUM('pendiente','generado','validado','cerrado') | No |  |
| creado_en | DATETIME | No |  |
| actualizado_en | DATETIME | No |  |

### `reporte_certificado`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_reporte | INT UNSIGNED | No | **PK** · FK → `reporte.id_reporte` |
| id_certificado | INT UNSIGNED | No | **PK** · FK → `certificado.id_certificado` |
| incluido_en_calculo | TINYINT(1) | No |  |
| observaciones | TEXT | Sí |  |

### `retroalimentacion`
| Columna | Tipo | Null | Notas |
|---|---|---|---|
| id_retroalimentacion | INT UNSIGNED | No | **PK** |
| id_usuario_emisor | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| id_usuario_receptor | INT UNSIGNED | No | FK → `usuario.id_usuario` |
| id_respuesta | INT UNSIGNED | Sí | FK → `respuesta_estudiante.id_respuesta` |
| id_resultado | INT UNSIGNED | Sí | FK → `resultado_evaluacion.id_resultado` |
| id_asistencia | INT UNSIGNED | Sí | FK → `asistencia.id_asistencia` |
| id_comentario | INT UNSIGNED | Sí | FK → `comentario_foro.id_comentario` |
| id_calificacion_foro | INT UNSIGNED | Sí | FK → `calificacion_foro.id_calificacion` |
| id_entrega | INT UNSIGNED | Sí | FK → `entrega_actividad.id_entrega` |
| comentarios | TEXT | No |  |
| sugerencias | TEXT | Sí |  |
| estado | ENUM('borrador','enviada','leida','archivada') | No |  |
| fecha_retroalimentacion | DATETIME | No |  |
| fecha_actualizacion | DATETIME | No |  |

## Resumen (51 tablas)

`usuario` · `area_academica` · `grado` · `historial_acceso` · `sesion_otp` · `verificacion_cuenta` · `recuperacion_contrasena` · `institucion` · `perfil_admin` · `perfil_docente` · `perfil_contador` · `perfil_estudiante` · `supervisor_institucion` · `historial_grado` · `curso` · `institucion_curso_contratado` · `curso_docente` · `horario_curso` · `leccion` · `material_contenido` · `recurso` · `articulo_cientifico` · `recurso_apoyo` · `actividad` · `entrega_actividad` · `evaluacion` · `pregunta` · `opcion_pregunta` · `resultado_evaluacion` · `respuesta_estudiante` · `foro` · `comentario_foro` · `adjunto_comentario` · `calificacion_foro` · `sesion_online` · `evento_virtual` · `asistencia` · `certificado` · `plantilla_certificado` · `inscripcion` · `progreso_leccion` · `datos_facturacion` · `pago` · `pago_inscripcion` · `factura` · `reembolso` · `mensaje` · `notificacion` · `reporte` · `reporte_certificado` · `retroalimentacion`