# BioLearn — Diagrama Entidad-Relación (esquema final, 51 tablas)

Generado directamente desde `biolearn_database.sql`. Dividido en los mismos 8 dominios del diccionario de datos.

## Dominio 1: Usuarios Seguridad Perfiles
```mermaid
erDiagram
    USUARIO ||--o{ HISTORIAL_ACCESO : "id_usuario"
    USUARIO ||--o{ SESION_OTP : "id_usuario"
    USUARIO ||--o{ VERIFICACION_CUENTA : "id_usuario"
    USUARIO ||--o{ RECUPERACION_CONTRASENA : "id_usuario"
    USUARIO ||--o{ PERFIL_ADMIN : "id_usuario"
    USUARIO ||--o{ PERFIL_DOCENTE : "id_usuario"
    USUARIO ||--o{ PERFIL_CONTADOR : "id_usuario"
    USUARIO ||--o{ PERFIL_ESTUDIANTE : "id_usuario"
    INSTITUCION ||--o{ PERFIL_ESTUDIANTE : "id_institucion"
    GRADO ||--o{ PERFIL_ESTUDIANTE : "id_grado"
    AREA_ACADEMICA ||--o{ PERFIL_ESTUDIANTE : "id_area"
    USUARIO ||--o{ SUPERVISOR_INSTITUCION : "id_usuario"
    INSTITUCION ||--o{ SUPERVISOR_INSTITUCION : "id_institucion"
    PERFIL_ESTUDIANTE ||--o{ HISTORIAL_GRADO : "id_estudiante"
    GRADO ||--o{ HISTORIAL_GRADO : "id_grado"
    INSTITUCION ||--o{ HISTORIAL_GRADO : "id_institucion"
    USUARIO {
        INT_UNSIGNED id_usuario PK
        VARCHAR_100 nombre
        VARCHAR_100 apellido
        VARCHAR_60 nombre_usuario
        VARCHAR_150 correo
        VARCHAR_255 contrasena
        ENUM_admin_docente_estudiante_supervisor_contador tipo_usuario
        ENUM_pendiente_activo_inactivo_suspendido estado
        VARCHAR_30 doc_identidad
        DATE fecha_nacimiento
        VARCHAR_20 telefono
        VARCHAR_200 direccion
        VARCHAR_100 ciudad
        VARCHAR_500 foto_perfil
        VARCHAR_100 rol_detalle
        DATETIME fecha_registro
        DATETIME fecha_actualizacion
    }
    AREA_ACADEMICA {
        TINYINT_UNSIGNED id_area PK
        VARCHAR_100 nombre
        TEXT descripcion
        ENUM_activo_inactivo estado
        DATETIME fecha_creacion
        DATETIME fecha_actualizacion
    }
    GRADO {
        TINYINT_UNSIGNED id_grado PK
        VARCHAR_60 nombre
        ENUM_primaria_secundaria_media nivel
        TINYINT_UNSIGNED orden
        TEXT descripcion
        ENUM_activo_inactivo estado
        DATETIME fecha_creacion
        DATETIME fecha_actualizacion
    }
    HISTORIAL_ACCESO {
        INT_UNSIGNED id_historial PK
        INT_UNSIGNED id_usuario FK
        ENUM_login_logout_fallo_login_cambio_contrasena accion
        ENUM_exitosa_fallida_expirada estado_sesion
        VARCHAR_45 ip_origen
        VARCHAR_150 dispositivo
        VARCHAR_100 navegador
        VARCHAR_100 ubicacion
        SMALLINT_UNSIGNED duracion_minutos
        VARCHAR_64 hash_token_sesion
        DATETIME fecha_acceso
    }
    SESION_OTP {
        INT_UNSIGNED id_sesion_otp PK
        INT_UNSIGNED id_usuario FK
        CHAR_6 codigo
        DATETIME expira_en
        TINYINT_1 usado
        DATETIME creado_en
    }
    VERIFICACION_CUENTA {
        INT_UNSIGNED id_verificacion PK
        INT_UNSIGNED id_usuario FK
        VARCHAR_128 token
        ENUM_activacion_cambio_correo tipo
        ENUM_pendiente_usado_expirado estado
        DATETIME expira_en
        DATETIME usado_en
        DATETIME creado_en
    }
    RECUPERACION_CONTRASENA {
        INT_UNSIGNED id_recuperacion PK
        INT_UNSIGNED id_usuario FK
        VARCHAR_128 token
        ENUM_pendiente_usado_expirado estado
        VARCHAR_45 ip_solicitud
        DATETIME expira_en
        DATETIME usado_en
        DATETIME creado_en
    }
    INSTITUCION {
        INT_UNSIGNED id_institucion PK
        VARCHAR_200 nombre
        VARCHAR_20 nit
        VARCHAR_200 rector
        VARCHAR_300 direccion
        VARCHAR_20 telefono
        VARCHAR_150 correo
        VARCHAR_500 logo_url
        CHAR_8 codigo_afiliacion
        SMALLINT_nullable cupos_contratados
        ENUM_activa_inactiva_suspendida estado
        DATETIME creado_en
        DATETIME actualizado_en
    }
    PERFIL_ADMIN {
        INT_UNSIGNED id_admin PK
        INT_UNSIGNED id_usuario FK
        ENUM_superadmin_admin_institucional_admin_contenido rol_admin
        ENUM_activo_inactivo estado
        DATETIME asignado_en
        DATETIME actualizado_en
    }
    PERFIL_DOCENTE {
        INT_UNSIGNED id_docente PK
        INT_UNSIGNED id_usuario FK
        VARCHAR_150 especialidad
        TEXT experiencia_laboral
        ENUM_planta_contratista_honorarios tipo_contrato
        ENUM_activo_inactivo_suspendido estado
        DATETIME asignado_en
        DATETIME actualizado_en
    }
    PERFIL_CONTADOR {
        INT_UNSIGNED id_contador PK
        INT_UNSIGNED id_usuario FK
        ENUM_administrador_financiero_auxiliar_auditor rol_contador
        ENUM_activo_inactivo estado
        DATETIME asignado_en
        DATETIME actualizado_en
    }
    PERFIL_ESTUDIANTE {
        INT_UNSIGNED id_estudiante PK
        INT_UNSIGNED id_usuario FK
        INT_UNSIGNED id_institucion FK
        TINYINT_UNSIGNED id_grado FK
        TINYINT_UNSIGNED id_area FK
        ENUM_activo_inactivo_egresado estado
        DATE fecha_ingreso
        DATETIME actualizado_en
    }
    SUPERVISOR_INSTITUCION {
        INT_UNSIGNED id_supervisor_inst PK
        INT_UNSIGNED id_usuario FK
        INT_UNSIGNED id_institucion FK
        VARCHAR_150 cargo
        TINYINT_1 acceso_reportes
        TINYINT_1 acceso_foros
        ENUM_activo_inactivo_suspendido estado
        DATETIME asignado_en
        DATETIME actualizado_en
    }
    HISTORIAL_GRADO {
        INT_UNSIGNED id_historial_grado PK
        INT_UNSIGNED id_estudiante FK
        TINYINT_UNSIGNED id_grado FK
        INT_UNSIGNED id_institucion FK
        DATE fecha_inicio
        DATE fecha_fin
        TEXT observaciones
        DATETIME registrado_en
    }
```

## Dominio 2: Cursos Contenido Academico
```mermaid
erDiagram
    GRADO ||--o{ CURSO : "id_grado"
    AREA_ACADEMICA ||--o{ CURSO : "id_area"
    PERFIL_ADMIN ||--o{ CURSO : "id_admin_creador"
    PERFIL_DOCENTE ||--o{ CURSO : "id_docente_titular"
    INSTITUCION ||--o{ INSTITUCION_CURSO_CONTRATADO : "id_institucion"
    CURSO ||--o{ INSTITUCION_CURSO_CONTRATADO : "id_curso"
    CURSO ||--o{ CURSO_DOCENTE : "id_curso"
    PERFIL_DOCENTE ||--o{ CURSO_DOCENTE : "id_docente"
    CURSO ||--o{ HORARIO_CURSO : "id_curso"
    CURSO ||--o{ LECCION : "id_curso"
    LECCION ||--o{ LECCION : "id_prerequisito"
    LECCION ||--o{ MATERIAL_CONTENIDO : "id_leccion"
    PERFIL_DOCENTE ||--o{ MATERIAL_CONTENIDO : "id_docente"
    MATERIAL_CONTENIDO ||--o{ RECURSO : "id_material"
    PERFIL_DOCENTE ||--o{ RECURSO : "id_docente"
    MATERIAL_CONTENIDO ||--o{ ARTICULO_CIENTIFICO : "id_material"
    PERFIL_DOCENTE ||--o{ ARTICULO_CIENTIFICO : "id_docente"
    MATERIAL_CONTENIDO ||--o{ RECURSO_APOYO : "id_material"
    RECURSO ||--o{ RECURSO_APOYO : "id_recurso"
    ARTICULO_CIENTIFICO ||--o{ RECURSO_APOYO : "id_articulo"
    USUARIO ||--o{ RECURSO_APOYO : "id_usuario"
    CURSO {
        INT_UNSIGNED id_curso PK
        TINYINT_UNSIGNED id_grado FK
        TINYINT_UNSIGNED id_area FK
        INT_UNSIGNED id_admin_creador FK
        INT_UNSIGNED id_docente_titular FK
        VARCHAR_150 nombre
        VARCHAR_500 imagen_portada
        TEXT descripcion
        TEXT objetivos
        DECIMAL_10_2 precio
        TINYINT_UNSIGNED creditos
        SMALLINT_UNSIGNED horas_estimadas
        ENUM_borrador_activo_cerrado_archivado estado
        DATETIME creado_en
        DATETIME actualizado_en
    }
    INSTITUCION_CURSO_CONTRATADO {
        INT_UNSIGNED id_institucion_curso PK
        INT_UNSIGNED id_institucion FK
        INT_UNSIGNED id_curso FK
        DATETIME contratado_en
    }
    CURSO_DOCENTE {
        INT_UNSIGNED id_curso_docente PK
        INT_UNSIGNED id_curso FK
        INT_UNSIGNED id_docente FK
        ENUM_titular_colaborador_invitado rol
        ENUM_activo_inactivo estado
        DATETIME asignado_en
    }
    HORARIO_CURSO {
        INT_UNSIGNED id_horario PK
        INT_UNSIGNED id_curso FK
        ENUM_lunes_martes_miercoles_jueves_viernes_sabado_domingo dia_semana
        TIME hora_inicio
        TIME hora_fin
    }
    LECCION {
        INT_UNSIGNED id_leccion PK
        INT_UNSIGNED id_curso FK
        INT_UNSIGNED id_prerequisito FK
        VARCHAR_150 nombre
        VARCHAR_500 imagen_portada
        TEXT descripcion
        SMALLINT_UNSIGNED orden
        ENUM_teoria_practica_evaluacion_mixta tipo
        ENUM_borrador_activa_inactiva_archivada estado
        DATETIME creado_en
        DATETIME actualizado_en
    }
    MATERIAL_CONTENIDO {
        INT_UNSIGNED id_material PK
        INT_UNSIGNED id_leccion FK
        INT_UNSIGNED id_docente FK
        VARCHAR_250 nombre
        ENUM_lectura_video_laboratorio_juego_documento_articulo tipo
        TEXT descripcion
        TINYINT_UNSIGNED orden
        ENUM_activo_inactivo_archivado estado
        DATETIME creado_en
        DATETIME actualizado_en
    }
    RECURSO {
        INT_UNSIGNED id_recurso PK
        INT_UNSIGNED id_material FK
        INT_UNSIGNED id_docente FK
        VARCHAR_200 nombre
        TEXT descripcion
        VARCHAR_1000 url
        ENUM_video_documento_imagen_audio_enlace_interactivo tipo_recurso
        SMALLINT_UNSIGNED duracion_min
        ENUM_basico_intermedio_avanzado nivel_academico
        TINYINT_UNSIGNED orden
        ENUM_activo_inactivo estado
        DATETIME creado_en
    }
    ARTICULO_CIENTIFICO {
        INT_UNSIGNED id_articulo PK
        INT_UNSIGNED id_material FK
        INT_UNSIGNED id_docente FK
        VARCHAR_300 titulo
        VARCHAR_500 autores
        TEXT resumen
        VARCHAR_250 revista
        VARCHAR_100 doi
        VARCHAR_1000 url
        DATE fecha_publicacion
        ENUM_activo_inactivo estado
        DATETIME creado_en
    }
    RECURSO_APOYO {
        INT_UNSIGNED id_recurso_apoyo PK
        INT_UNSIGNED id_material FK
        INT_UNSIGNED id_recurso FK
        INT_UNSIGNED id_articulo FK
        INT_UNSIGNED id_usuario FK
        ENUM_documento_video_enlace_imagen_interactivo_otro tipo_recurso
        ENUM_primario_complementario_referencia_evaluativo tipo_contenido
        VARCHAR_250 titulo
        VARCHAR_500 url
        VARCHAR_500 ruta_imagen
        TINYINT_UNSIGNED orden
        ENUM_activo_inactivo estado
        DATETIME fecha_creacion
        DATETIME fecha_actualizacion
    }
```

## Dominio 3: Actividades Evaluaciones
```mermaid
erDiagram
    LECCION ||--o{ ACTIVIDAD : "id_leccion"
    PERFIL_DOCENTE ||--o{ ACTIVIDAD : "id_docente"
    ACTIVIDAD ||--o{ ENTREGA_ACTIVIDAD : "id_actividad"
    PERFIL_ESTUDIANTE ||--o{ ENTREGA_ACTIVIDAD : "id_estudiante"
    PERFIL_DOCENTE ||--o{ ENTREGA_ACTIVIDAD : "id_revisor"
    LECCION ||--o{ EVALUACION : "id_leccion"
    PERFIL_DOCENTE ||--o{ EVALUACION : "id_docente"
    EVALUACION ||--o{ PREGUNTA : "id_evaluacion"
    PREGUNTA ||--o{ OPCION_PREGUNTA : "id_pregunta"
    EVALUACION ||--o{ RESULTADO_EVALUACION : "id_evaluacion"
    INSCRIPCION ||--o{ RESULTADO_EVALUACION : "id_inscripcion"
    RESULTADO_EVALUACION ||--o{ RESPUESTA_ESTUDIANTE : "id_resultado"
    PREGUNTA ||--o{ RESPUESTA_ESTUDIANTE : "id_pregunta"
    OPCION_PREGUNTA ||--o{ RESPUESTA_ESTUDIANTE : "id_opcion"
    PERFIL_DOCENTE ||--o{ RESPUESTA_ESTUDIANTE : "id_revisor"
    ACTIVIDAD {
        INT_UNSIGNED id_actividad PK
        INT_UNSIGNED id_leccion FK
        INT_UNSIGNED id_docente FK
        VARCHAR_250 nombre
        TEXT descripcion
        TEXT instrucciones
        ENUM_tarea_proyecto_investigacion_ejercicio_otro tipo
        DATETIME fecha_disponible
        DATETIME fecha_limite
        TINYINT_1 permite_entrega_tardia
        DECIMAL_10_2 puntaje_maximo
        ENUM_activa_inactiva_cerrada estado
        DATETIME creado_en
        DATETIME actualizado_en
    }
    ENTREGA_ACTIVIDAD {
        INT_UNSIGNED id_entrega PK
        INT_UNSIGNED id_actividad FK
        INT_UNSIGNED id_estudiante FK
        INT_UNSIGNED id_revisor FK
        VARCHAR_250 nombre_archivo
        VARCHAR_1000 url_archivo
        ENUM_documento_video_audio_imagen_presentacion_otro tipo_archivo
        INT_UNSIGNED tamanio_kb
        TINYINT_1 es_tardia
        DECIMAL_10_2 calificacion
        TEXT comentario_revisor
        DATETIME fecha_entrega
        DATETIME fecha_revision
        ENUM_entregada_revisada_rechazada estado
    }
    EVALUACION {
        INT_UNSIGNED id_evaluacion PK
        INT_UNSIGNED id_leccion FK
        INT_UNSIGNED id_docente FK
        VARCHAR_200 nombre
        TEXT descripcion
        ENUM_diagnostica_formativa_sumativa tipo
        SMALLINT_UNSIGNED tiempo_limite_min
        TINYINT_UNSIGNED intentos_permitidos
        DECIMAL_10_2 puntaje_maximo
        DECIMAL_5_2 ponderacion
        DATETIME fecha_aplicacion
        DATETIME fecha_apertura
        DATETIME fecha_cierre
        ENUM_borrador_activa_cerrada estado
        DATETIME creado_en
        DATETIME actualizado_en
    }
    PREGUNTA {
        INT_UNSIGNED id_pregunta PK
        INT_UNSIGNED id_evaluacion FK
        TEXT enunciado
        ENUM_seleccion_multiple_verdadero_falso_abierta tipo
        DECIMAL_10_2 puntaje
        TEXT retroalimentacion
        TINYINT_UNSIGNED orden
        ENUM_activa_inactiva estado
        DATETIME creado_en
        DATETIME actualizado_en
    }
    OPCION_PREGUNTA {
        INT_UNSIGNED id_opcion PK
        INT_UNSIGNED id_pregunta FK
        VARCHAR_500 texto
        TINYINT_1 es_correcta
        TEXT retroalimentacion
        TINYINT_UNSIGNED orden
        DATETIME creado_en
        DATETIME actualizado_en
    }
    RESULTADO_EVALUACION {
        INT_UNSIGNED id_resultado PK
        INT_UNSIGNED id_evaluacion FK
        INT_UNSIGNED id_inscripcion FK
        TINYINT_UNSIGNED numero_intento
        DECIMAL_10_2 puntaje_total
        DECIMAL_5_2 porcentaje
        TINYINT_1 aprobado
        ENUM_pendiente_calculado_publicado_revisado estado
        DATETIME completado_en
    }
    RESPUESTA_ESTUDIANTE {
        INT_UNSIGNED id_respuesta PK
        INT_UNSIGNED id_resultado FK
        INT_UNSIGNED id_pregunta FK
        INT_UNSIGNED id_opcion FK
        INT_UNSIGNED id_revisor FK
        TEXT contenido_abierto
        ENUM_pendiente_correcta_incorrecta_parcial estado_revision
        DECIMAL_10_2 puntaje_obtenido
        TEXT comentario_revisor
        DATETIME revisado_en
    }
```

## Dominio 4: Foros
```mermaid
erDiagram
    LECCION ||--o{ FORO : "id_leccion"
    PERFIL_DOCENTE ||--o{ FORO : "id_docente"
    FORO ||--o{ COMENTARIO_FORO : "id_foro"
    USUARIO ||--o{ COMENTARIO_FORO : "id_usuario"
    COMENTARIO_FORO ||--o{ COMENTARIO_FORO : "id_padre"
    COMENTARIO_FORO ||--o{ ADJUNTO_COMENTARIO : "id_comentario"
    USUARIO ||--o{ ADJUNTO_COMENTARIO : "id_usuario"
    FORO ||--o{ CALIFICACION_FORO : "id_foro"
    INSCRIPCION ||--o{ CALIFICACION_FORO : "id_inscripcion"
    PERFIL_DOCENTE ||--o{ CALIFICACION_FORO : "id_docente"
    FORO {
        INT_UNSIGNED id_foro PK
        INT_UNSIGNED id_leccion FK
        INT_UNSIGNED id_docente FK
        VARCHAR_250 nombre
        TEXT descripcion
        TEXT instrucciones
        VARCHAR_250 tema
        TEXT reglas_interaccion
        ENUM_abierto_solo_estudiantes_moderado tipo_participante
        DATETIME fecha_apertura
        DATETIME fecha_cierre
        ENUM_activo_cerrado_archivado estado
        DATETIME creado_en
        DATETIME actualizado_en
    }
    COMENTARIO_FORO {
        INT_UNSIGNED id_comentario PK
        INT_UNSIGNED id_foro FK
        INT_UNSIGNED id_usuario FK
        INT_UNSIGNED id_padre FK
        VARCHAR_250 asunto
        TEXT contenido
        ENUM_aporte_respuesta_retroalimentacion tipo
        ENUM_activo_moderado_eliminado estado
        DATETIME creado_en
        DATETIME actualizado_en
    }
    ADJUNTO_COMENTARIO {
        INT_UNSIGNED id_adjunto PK
        INT_UNSIGNED id_comentario FK
        INT_UNSIGNED id_usuario FK
        VARCHAR_1000 url
        ENUM_imagen_documento_video_enlace_otro tipo
        VARCHAR_250 nombre_archivo
        INT_UNSIGNED tamanio_kb
        DATETIME creado_en
    }
    CALIFICACION_FORO {
        INT_UNSIGNED id_calificacion PK
        INT_UNSIGNED id_foro FK
        INT_UNSIGNED id_inscripcion FK
        INT_UNSIGNED id_docente FK
        SMALLINT_UNSIGNED cantidad_aportes
        ENUM_excelente_bueno_regular_insuficiente calidad_aportes
        DECIMAL_5_2 calificacion
        TEXT retroalimentacion
        ENUM_borrador_publicada estado
        DATETIME calificado_en
        DATETIME actualizado_en
    }
```

## Dominio 5: Sesiones Eventos Certificados
```mermaid
erDiagram
    LECCION ||--o{ SESION_ONLINE : "id_leccion"
    PERFIL_DOCENTE ||--o{ SESION_ONLINE : "id_docente"
    PERFIL_DOCENTE ||--o{ EVENTO_VIRTUAL : "id_docente"
    LECCION ||--o{ EVENTO_VIRTUAL : "id_leccion"
    USUARIO ||--o{ ASISTENCIA : "id_usuario"
    SESION_ONLINE ||--o{ ASISTENCIA : "id_sesion"
    EVENTO_VIRTUAL ||--o{ ASISTENCIA : "id_evento"
    USUARIO ||--o{ CERTIFICADO : "id_emisor"
    USUARIO ||--o{ CERTIFICADO : "id_receptor"
    EVENTO_VIRTUAL ||--o{ CERTIFICADO : "id_evento"
    INSCRIPCION ||--o{ CERTIFICADO : "id_inscripcion"
    PLANTILLA_CERTIFICADO ||--o{ CERTIFICADO : "id_plantilla"
    PERFIL_DOCENTE ||--o{ PLANTILLA_CERTIFICADO : "id_docente"
    SESION_ONLINE {
        INT_UNSIGNED id_sesion PK
        INT_UNSIGNED id_leccion FK
        INT_UNSIGNED id_docente FK
        VARCHAR_250 nombre
        VARCHAR_150 tematica
        TEXT descripcion
        VARCHAR_1000 enlace
        ENUM_meet_youtube_zoom_teams_otro plataforma
        VARCHAR_20 codigo_asistencia
        DATETIME fecha_inicio
        DATETIME fecha_fin
        VARCHAR_1000 grabacion_url
        ENUM_programada_en_curso_finalizada_cancelada estado
        DATETIME creado_en
        DATETIME actualizado_en
    }
    EVENTO_VIRTUAL {
        INT_UNSIGNED id_evento PK
        INT_UNSIGNED id_docente FK
        INT_UNSIGNED id_leccion FK
        VARCHAR_200 nombre
        TEXT descripcion
        VARCHAR_1000 enlace_acceso
        DATETIME fecha_inicio
        DATETIME fecha_fin
        TINYINT_1 emite_certificado
        ENUM_programado_en_curso_finalizado_cancelado estado
        DATETIME creado_en
        DATETIME actualizado_en
    }
    ASISTENCIA {
        INT_UNSIGNED id_asistencia PK
        INT_UNSIGNED id_usuario FK
        INT_UNSIGNED id_sesion FK
        INT_UNSIGNED id_evento FK
        TINYINT_1 asistio
        VARCHAR_20 codigo_ingresado
        DATETIME fecha_ingreso
        DATETIME fecha_salida
        SMALLINT_UNSIGNED duracion_min
        DECIMAL_5_2 calificacion
        TEXT comentarios
        ENUM_alto_medio_bajo_ninguno nivel_participacion
        DATETIME registrado_en
    }
    CERTIFICADO {
        INT_UNSIGNED id_certificado PK
        INT_UNSIGNED id_emisor FK
        INT_UNSIGNED id_receptor FK
        INT_UNSIGNED id_evento FK
        INT_UNSIGNED id_inscripcion FK
        INT_UNSIGNED id_plantilla FK
        VARCHAR_200 nombre
        VARCHAR_80 codigo_validacion
        ENUM_emitido_anulado estado
        DATETIME emitido_en
    }
    PLANTILLA_CERTIFICADO {
        INT_UNSIGNED id_plantilla PK
        INT_UNSIGNED id_docente FK
        VARCHAR_7 color_acento
        VARCHAR_500 logo_url
        VARCHAR_150 firma_representante_nombre
        VARCHAR_500 firma_imagen_url
        TEXT texto_reconocimiento
        DATETIME creado_en
        DATETIME actualizado_en
    }
```

## Dominio 6: Inscripciones Progreso
```mermaid
erDiagram
    CURSO ||--o{ INSCRIPCION : "id_curso"
    PERFIL_ESTUDIANTE ||--o{ INSCRIPCION : "id_estudiante"
    INSCRIPCION ||--o{ PROGRESO_LECCION : "id_inscripcion"
    LECCION ||--o{ PROGRESO_LECCION : "id_leccion"
    INSCRIPCION {
        INT_UNSIGNED id_inscripcion PK
        INT_UNSIGNED id_curso FK
        INT_UNSIGNED id_estudiante FK
        ENUM_activa_completada_retirada_suspendida estado
        DATETIME fecha_inscripcion
        DATETIME fecha_completacion
        DATETIME actualizado_en
    }
    PROGRESO_LECCION {
        INT_UNSIGNED id_progreso PK
        INT_UNSIGNED id_inscripcion FK
        INT_UNSIGNED id_leccion FK
        ENUM_pendiente_en_progreso_completada estado
        DECIMAL_5_2 porcentaje
        DATETIME iniciado_en
        DATETIME completado_en
        DATETIME actualizado_en
    }
```

## Dominio 7: Finanzas
```mermaid
erDiagram
    PERFIL_ESTUDIANTE ||--o{ PAGO : "id_estudiante"
    PERFIL_CONTADOR ||--o{ PAGO : "id_contador"
    PAGO ||--o{ PAGO_INSCRIPCION : "id_pago"
    INSCRIPCION ||--o{ PAGO_INSCRIPCION : "id_inscripcion"
    PAGO ||--o{ FACTURA : "id_pago"
    PERFIL_ESTUDIANTE ||--o{ FACTURA : "id_estudiante"
    DATOS_FACTURACION ||--o{ FACTURA : "id_datos_facturacion"
    PAGO ||--o{ REEMBOLSO : "id_pago"
    USUARIO ||--o{ REEMBOLSO : "id_solicitante"
    PERFIL_CONTADOR ||--o{ REEMBOLSO : "id_contador"
    DATOS_FACTURACION {
        INT_UNSIGNED id_datos_facturacion PK
        VARCHAR_200 razon_social
        VARCHAR_20 nit
        VARCHAR_255 direccion
        VARCHAR_500 logo_url
        DATETIME actualizado_en
    }
    PAGO {
        INT_UNSIGNED id_pago PK
        INT_UNSIGNED id_estudiante FK
        INT_UNSIGNED id_contador FK
        ENUM_pse_wompi_tarjeta_wompi_nequi_transferencia_otro medio_pago
        DECIMAL_12_2 monto
        DATETIME fecha_pago
        VARCHAR_200 referencia_externa
        ENUM_pendiente_aprobado_rechazado_reembolsado estado
        DATETIME creado_en
        DATETIME actualizado_en
    }
    PAGO_INSCRIPCION {
        INT_UNSIGNED id_pago_inscripcion PK
        INT_UNSIGNED id_pago FK
        INT_UNSIGNED id_inscripcion FK
        DECIMAL_12_2 monto_asignado
    }
    FACTURA {
        INT_UNSIGNED id_factura PK
        INT_UNSIGNED id_pago FK
        INT_UNSIGNED id_estudiante FK
        INT_UNSIGNED id_datos_facturacion FK
        VARCHAR_50 numero_factura
        TEXT descripcion
        DECIMAL_12_2 monto_total
        DECIMAL_12_2 monto_pagado
        ENUM_emitida_anulada estado
        TINYINT_1 enviada_por_correo
        DATETIME enviada_en
        DATETIME fecha_emision
    }
    REEMBOLSO {
        INT_UNSIGNED id_reembolso PK
        INT_UNSIGNED id_pago FK
        INT_UNSIGNED id_solicitante FK
        INT_UNSIGNED id_contador FK
        TEXT motivo
        DECIMAL_12_2 monto
        TEXT justificacion
        ENUM_solicitado_aprobado_rechazado estado
        DATETIME solicitado_en
        DATETIME resuelto_en
    }
```

## Dominio 8: Comunicacion Reportes
```mermaid
erDiagram
    USUARIO ||--o{ MENSAJE : "id_emisor"
    USUARIO ||--o{ MENSAJE : "id_receptor"
    CURSO ||--o{ MENSAJE : "id_curso"
    USUARIO ||--o{ NOTIFICACION : "id_usuario_emisor"
    USUARIO ||--o{ NOTIFICACION : "id_usuario"
    USUARIO ||--o{ REPORTE : "id_solicitante"
    CURSO ||--o{ REPORTE : "id_curso"
    INSCRIPCION ||--o{ REPORTE : "id_inscripcion"
    INSTITUCION ||--o{ REPORTE : "id_institucion"
    SUPERVISOR_INSTITUCION ||--o{ REPORTE : "id_supervisor_inst"
    PERFIL_CONTADOR ||--o{ REPORTE : "id_contador"
    FACTURA ||--o{ REPORTE : "id_factura"
    DATOS_FACTURACION ||--o{ REPORTE : "id_datos_facturacion"
    REPORTE ||--o{ REPORTE_CERTIFICADO : "id_reporte"
    CERTIFICADO ||--o{ REPORTE_CERTIFICADO : "id_certificado"
    USUARIO ||--o{ RETROALIMENTACION : "id_usuario_emisor"
    USUARIO ||--o{ RETROALIMENTACION : "id_usuario_receptor"
    RESPUESTA_ESTUDIANTE ||--o{ RETROALIMENTACION : "id_respuesta"
    RESULTADO_EVALUACION ||--o{ RETROALIMENTACION : "id_resultado"
    ASISTENCIA ||--o{ RETROALIMENTACION : "id_asistencia"
    COMENTARIO_FORO ||--o{ RETROALIMENTACION : "id_comentario"
    CALIFICACION_FORO ||--o{ RETROALIMENTACION : "id_calificacion_foro"
    ENTREGA_ACTIVIDAD ||--o{ RETROALIMENTACION : "id_entrega"
    MENSAJE {
        INT_UNSIGNED id_mensaje PK
        INT_UNSIGNED id_emisor FK
        INT_UNSIGNED id_receptor FK
        INT_UNSIGNED id_curso FK
        VARCHAR_200 asunto
        TEXT contenido
        TINYINT_1 leido
        DATETIME leido_en
        ENUM_activo_eliminado estado_emisor
        ENUM_activo_eliminado estado_receptor
        DATETIME enviado_en
    }
    NOTIFICACION {
        INT_UNSIGNED id_notificacion PK
        INT_UNSIGNED id_usuario FK
        INT_UNSIGNED id_usuario_emisor FK
        ENUM_pago_inscripcion_leccion_evaluacion_foro_mensaje_certificado_reembolso_sistema tipo
        INT_UNSIGNED entidad_id
        VARCHAR_200 titulo
        TEXT contenido
        ENUM_pendiente_enviada_leida_archivada estado_notificacion
        TINYINT_1 leida
        DATETIME leida_en
        DATETIME creada_en
    }
    REPORTE {
        INT_UNSIGNED id_reporte PK
        INT_UNSIGNED id_solicitante FK
        INT_UNSIGNED id_curso FK
        INT_UNSIGNED id_inscripcion FK
        INT_UNSIGNED id_institucion FK
        INT_UNSIGNED id_supervisor_inst FK
        INT_UNSIGNED id_contador FK
        INT_UNSIGNED id_factura FK
        INT_UNSIGNED id_datos_facturacion FK
        ENUM_academico_financiero_institucional_mixto tipo
        ENUM_pdf_excel_csv formato
        DATETIME fecha_corte
        TEXT motivo
        TEXT descripcion
        LONGTEXT contenido
        ENUM_pendiente_generado_validado_cerrado estado
        DATETIME creado_en
        DATETIME actualizado_en
    }
    REPORTE_CERTIFICADO {
        INT_UNSIGNED id_reporte PK,FK
        INT_UNSIGNED id_certificado PK,FK
        TINYINT_1 incluido_en_calculo
        TEXT observaciones
    }
    RETROALIMENTACION {
        INT_UNSIGNED id_retroalimentacion PK
        INT_UNSIGNED id_usuario_emisor FK
        INT_UNSIGNED id_usuario_receptor FK
        INT_UNSIGNED id_respuesta FK
        INT_UNSIGNED id_resultado FK
        INT_UNSIGNED id_asistencia FK
        INT_UNSIGNED id_comentario FK
        INT_UNSIGNED id_calificacion_foro FK
        INT_UNSIGNED id_entrega FK
        TEXT comentarios
        TEXT sugerencias
        ENUM_borrador_enviada_leida_archivada estado
        DATETIME fecha_retroalimentacion
        DATETIME fecha_actualizacion
    }
```
