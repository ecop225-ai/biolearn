-- ═══════════════════════════════════════════════════════════════════════════
-- BIOLearn — Modelo Relacional
-- ═══════════════════════════════════════════════════════════════════════════


SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;


-- ────────────────────────────────────────────────────────────────
-- tabla usuario
-- Tabla central del sistema. tipo_usuario determina el dashboard
-- y los permisos de cada persona.
-- ────────────────────────────────────────────────────────────────
CREATE TABLE usuario (
    id_usuario          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    nombre              VARCHAR(100)    NOT NULL,
    apellido            VARCHAR(100)    NOT NULL,
    nombre_usuario      VARCHAR(60)     NOT NULL,
    correo              VARCHAR(150)    NOT NULL,
    contrasena          VARCHAR(255)    NOT NULL,              -- hash BCrypt, nunca texto plano
    tipo_usuario        ENUM('admin','docente','estudiante','supervisor','contador')               NOT NULL,
    estado              ENUM('pendiente','activo','inactivo','suspendido')               NOT NULL DEFAULT 'pendiente',
    doc_identidad       VARCHAR(30)     NULL,
    fecha_nacimiento    DATE            NULL,
    telefono            VARCHAR(20)     NULL,
    direccion           VARCHAR(200)    NULL,
    ciudad              VARCHAR(100)    NULL,
    foto_perfil         VARCHAR(500)    NULL,                  -- URL en almacenamiento
    rol_detalle         VARCHAR(100)    NULL,                  -- descripción adicional: especialidad, grado, cargo, etc.
    fecha_registro      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario           PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_correo    UNIQUE (correo),
    CONSTRAINT uq_usuario_nombre_u  UNIQUE (nombre_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tabla central de identidad. Todos los roles heredan de aquí.';


-- ────────────────────────────────────────────────────────────────
-- tabla area_academica
-- ────────────────────────────────────────────────────────────────
CREATE TABLE area_academica (
    id_area             TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre              VARCHAR(100)    NOT NULL,              -- 'Biología', 'Ciencias Naturales' ...
    descripcion         TEXT            NULL,
    estado              ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
    fecha_creacion      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_area              PRIMARY KEY (id_area),
    CONSTRAINT uq_area_nombre       UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla grado
-- ────────────────────────────────────────────────────────────────
CREATE TABLE grado (
    id_grado            TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre              VARCHAR(60)     NOT NULL,              -- 'Grado 6°', 'Grado 7°' ...
    nivel               ENUM('primaria','secundaria','media') NOT NULL,
    orden               TINYINT UNSIGNED NOT NULL,             -- orden en el selector
    descripcion         TEXT            NULL,
    estado              ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
    fecha_creacion      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_grado             PRIMARY KEY (id_grado),
    CONSTRAINT uq_grado_nombre      UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla historial_acceso
-- Auditoría de login/logout e intentos fallidos con IP y dispositivo.
-- ────────────────────────────────────────────────────────────────
CREATE TABLE historial_acceso (
    id_historial        INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_usuario          INT UNSIGNED    NOT NULL,
    accion              ENUM('login','logout','fallo_login','cambio_contrasena') NOT NULL,
    estado_sesion       ENUM('exitosa','fallida','expirada') NOT NULL,
    ip_origen           VARCHAR(45)     NULL,
    dispositivo         VARCHAR(150)    NULL,
    navegador           VARCHAR(100)    NULL,
    ubicacion           VARCHAR(100)    NULL,
    duracion_minutos    SMALLINT UNSIGNED NULL,
    hash_token_sesion   VARCHAR(64)     NULL,
    fecha_acceso        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_historial         PRIMARY KEY (id_historial),
    CONSTRAINT fk_hist_usuario      FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Solo inserciones. Auditoría de accesos.';


-- ────────────────────────────────────────────────────────────────
-- tabla sesion_otp
-- Almacena códigos de un solo uso para recuperación de contraseña.
-- ────────────────────────────────────────────────────────────────
CREATE TABLE sesion_otp (
    id_sesion_otp       INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_usuario          INT UNSIGNED    NOT NULL,
    codigo              CHAR(6)         NOT NULL,              -- 6 dígitos numéricos
    expira_en           DATETIME        NOT NULL,              -- ahora + 10 minutos
    usado               TINYINT(1)      NOT NULL DEFAULT 0,
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sesion_otp        PRIMARY KEY (id_sesion_otp),
    CONSTRAINT fk_otp_usuario       FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- Verificación de cuenta por email/token
-- ────────────────────────────────────────────────────────────────
CREATE TABLE verificacion_cuenta (
    id_verificacion     INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_usuario          INT UNSIGNED    NOT NULL,
    token               VARCHAR(128)    NOT NULL,              -- UUID v4 o SHA-256
    tipo                ENUM('activacion','cambio_correo') NOT NULL DEFAULT 'activacion',
    estado              ENUM('pendiente','usado','expirado') NOT NULL DEFAULT 'pendiente',
    expira_en           DATETIME        NOT NULL,
    usado_en            DATETIME        NULL,
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_verificacion      PRIMARY KEY (id_verificacion),
    CONSTRAINT uq_verif_token       UNIQUE (token),
    CONSTRAINT fk_verif_usuario     FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- Tokens de recuperación de contraseña
-- ────────────────────────────────────────────────────────────────
CREATE TABLE recuperacion_contrasena (
    id_recuperacion     INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_usuario          INT UNSIGNED    NOT NULL,
    token               VARCHAR(128)    NOT NULL,
    estado              ENUM('pendiente','usado','expirado') NOT NULL DEFAULT 'pendiente',
    ip_solicitud        VARCHAR(45)     NULL,
    expira_en           DATETIME        NOT NULL,
    usado_en            DATETIME        NULL,
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_recuperacion      PRIMARY KEY (id_recuperacion),
    CONSTRAINT uq_recup_token       UNIQUE (token),
    CONSTRAINT fk_recup_usuario     FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla Institucion
-- Instituciones educativas que contratan el servicio BIOLearn.
-- ────────────────────────────────────────────────────────────────
CREATE TABLE institucion (
    id_institucion      INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    nombre              VARCHAR(200)    NOT NULL,
    nit                 VARCHAR(20)     NOT NULL,
    rector              VARCHAR(200)    NULL,
    direccion           VARCHAR(300)    NULL,
    telefono            VARCHAR(20)     NULL,
    correo              VARCHAR(150)    NULL,
    logo_url            VARCHAR(500)    NULL,
    codigo_afiliacion   CHAR(8)         NOT NULL,              -- código que los estudiantes usan para afiliarse
    estado              ENUM('activa','inactiva','suspendida') NOT NULL DEFAULT 'activa',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_institucion           PRIMARY KEY (id_institucion),
    CONSTRAINT uq_institucion_nit       UNIQUE (nit),
    CONSTRAINT uq_institucion_codigo    UNIQUE (codigo_afiliacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla perfil Administrador
-- ────────────────────────────────────────────────────────────────
CREATE TABLE perfil_admin (
    id_admin            INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_usuario          INT UNSIGNED    NOT NULL,
    rol_admin           ENUM('superadmin','admin_institucional','admin_contenido') NOT NULL DEFAULT 'admin_institucional',
    estado              ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
    asignado_en         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_admin             PRIMARY KEY (id_admin),
    CONSTRAINT uq_admin_usuario     UNIQUE (id_usuario),
    CONSTRAINT fk_admin_usuario     FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla perfil docente sistema
-- ────────────────────────────────────────────────────────────────
CREATE TABLE perfil_docente (
    id_docente          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_usuario          INT UNSIGNED    NOT NULL,
    especialidad        VARCHAR(150)    NULL,
    experiencia_laboral TEXT            NULL,
    tipo_contrato       ENUM('planta','contratista','honorarios') NOT NULL DEFAULT 'contratista',
    estado              ENUM('activo','inactivo','suspendido') NOT NULL DEFAULT 'activo',
    asignado_en         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_docente           PRIMARY KEY (id_docente),
    CONSTRAINT uq_docente_usuario   UNIQUE (id_usuario),
    CONSTRAINT fk_docente_usuario   FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla perfil del contador
-- ────────────────────────────────────────────────────────────────
CREATE TABLE perfil_contador (
    id_contador         INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_usuario          INT UNSIGNED    NOT NULL,
    rol_contador        ENUM('administrador_financiero','auxiliar','auditor') NOT NULL DEFAULT 'auxiliar',
    estado              ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
    asignado_en         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_contador          PRIMARY KEY (id_contador),
    CONSTRAINT uq_contador_usuario  UNIQUE (id_usuario),
    CONSTRAINT fk_contador_usuario  FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla perfil estudiante
-- Estudiante vinculado a una institución con grado y área.
-- ────────────────────────────────────────────────────────────────
CREATE TABLE perfil_estudiante (
    id_estudiante       INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_usuario          INT UNSIGNED    NOT NULL,
    id_institucion      INT UNSIGNED    NULL,
    id_grado            TINYINT UNSIGNED NOT NULL,
    id_area             TINYINT UNSIGNED NOT NULL,
    estado              ENUM('activo','inactivo','egresado') NOT NULL DEFAULT 'activo',
    fecha_ingreso       DATE            NULL,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_estudiante            PRIMARY KEY (id_estudiante),
    CONSTRAINT uq_estudiante_usuario    UNIQUE (id_usuario),
    CONSTRAINT fk_est_usuario           FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_est_institucion       FOREIGN KEY (id_institucion)
        REFERENCES institucion(id_institucion) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_est_grado             FOREIGN KEY (id_grado)
        REFERENCES grado(id_grado) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_est_area              FOREIGN KEY (id_area)
        REFERENCES area_academica(id_area) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla supervisor de la institucion
-- ────────────────────────────────────────────────────────────────
CREATE TABLE supervisor_institucion (
    id_supervisor_inst  INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_usuario          INT UNSIGNED    NOT NULL,              -- usuario con tipo_usuario='supervisor'
    id_institucion      INT UNSIGNED    NOT NULL,              -- institución que representa (1:1 estricto)
    cargo               VARCHAR(150)    NULL,                  -- ej: Rector, Coordinador académico, Jefe de área
    acceso_reportes     TINYINT(1)      NOT NULL DEFAULT 1,    -- puede generar y descargar reportes
    acceso_foros        TINYINT(1)      NOT NULL DEFAULT 1,    -- puede participar en foros de su institución
    estado              ENUM('activo','inactivo','suspendido') NOT NULL DEFAULT 'activo',
    asignado_en         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_supervisor_inst       PRIMARY KEY (id_supervisor_inst),
    CONSTRAINT uq_supervisor_usuario    UNIQUE (id_usuario),       -- un supervisor = una sola institución
    CONSTRAINT fk_sup_usuario           FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_sup_institucion       FOREIGN KEY (id_institucion)
        REFERENCES institucion(id_institucion) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Supervisor representa a UNA institución. UNIQUE en id_usuario impide asignación múltiple.';


-- ────────────────────────────────────────────────────────────────
-- tabla historial_grado_estudiante
-- Progresión del estudiante por grado a lo largo del tiempo.
-- ────────────────────────────────────────────────────────────────
CREATE TABLE historial_grado (
    id_historial_grado  INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_estudiante       INT UNSIGNED    NOT NULL,
    id_grado            TINYINT UNSIGNED NOT NULL,
    id_institucion      INT UNSIGNED    NULL,
    fecha_inicio        DATE            NOT NULL,
    fecha_fin           DATE            NULL,
    observaciones       TEXT            NULL,
    registrado_en       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_historial_grado       PRIMARY KEY (id_historial_grado),
    CONSTRAINT fk_hg_estudiante         FOREIGN KEY (id_estudiante)
        REFERENCES perfil_estudiante(id_estudiante) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_hg_grado              FOREIGN KEY (id_grado)
        REFERENCES grado(id_grado) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_hg_institucion        FOREIGN KEY (id_institucion)
        REFERENCES institucion(id_institucion) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla curso
-- creados por Administrador o Docente del sistema
-- ────────────────────────────────────────────────────────────────
CREATE TABLE curso (
    id_curso            INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_grado            TINYINT UNSIGNED NOT NULL,
    id_area             TINYINT UNSIGNED NOT NULL,
    id_admin_creador    INT UNSIGNED    NULL,                  -- admin que lo crea (NULL si lo crea el docente)
    id_docente_titular  INT UNSIGNED    NULL,                  -- docente titular (NULL si lo crea el admin sin docente)
    nombre              VARCHAR(150)    NOT NULL,
    imagen_portada      VARCHAR(500)    NULL,
    descripcion         TEXT            NOT NULL,
    objetivos           TEXT            NOT NULL,
    precio              DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    creditos            TINYINT UNSIGNED NOT NULL DEFAULT 0,
    horas_estimadas     SMALLINT UNSIGNED NULL,
    estado              ENUM('borrador','activo','cerrado','archivado')               NOT NULL DEFAULT 'borrador',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_curso                 PRIMARY KEY (id_curso),
    CONSTRAINT fk_curso_grado           FOREIGN KEY (id_grado)
        REFERENCES grado(id_grado) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_curso_area            FOREIGN KEY (id_area)
        REFERENCES area_academica(id_area) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_curso_admin           FOREIGN KEY (id_admin_creador)
        REFERENCES perfil_admin(id_admin) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_curso_docente         FOREIGN KEY (id_docente_titular)
        REFERENCES perfil_docente(id_docente) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla institucion_curso_contratado
-- Cursos que una institución contrata para sus estudiantes afiliados
-- (checkboxes de "Cursos contratados" en Admin → Instituciones).
-- ────────────────────────────────────────────────────────────────
CREATE TABLE institucion_curso_contratado (
    id_institucion_curso    INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_institucion          INT UNSIGNED    NOT NULL,
    id_curso                INT UNSIGNED    NOT NULL,
    contratado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_institucion_curso         PRIMARY KEY (id_institucion_curso),
    CONSTRAINT uq_institucion_curso         UNIQUE (id_institucion, id_curso),
    CONSTRAINT fk_ic_institucion            FOREIGN KEY (id_institucion)
        REFERENCES institucion(id_institucion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ic_curso                  FOREIGN KEY (id_curso)
        REFERENCES curso(id_curso) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla curso docente
-- ────────────────────────────────────────────────────────────────
CREATE TABLE curso_docente (
    id_curso_docente    INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_curso            INT UNSIGNED    NOT NULL,
    id_docente          INT UNSIGNED    NOT NULL,
    rol                 ENUM('titular','colaborador','invitado') NOT NULL DEFAULT 'colaborador',
    estado              ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
    asignado_en         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_curso_docente         PRIMARY KEY (id_curso_docente),
    CONSTRAINT uq_curso_docente         UNIQUE (id_curso, id_docente),
    CONSTRAINT fk_cd_curso              FOREIGN KEY (id_curso)
        REFERENCES curso(id_curso) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cd_docente            FOREIGN KEY (id_docente)
        REFERENCES perfil_docente(id_docente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Docentes adicionales al titular. El titular ya está en curso.id_docente_titular.';


-- ────────────────────────────────────────────────────────────────
-- tabla horario_curso
-- ────────────────────────────────────────────────────────────────
CREATE TABLE horario_curso (
    id_horario          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_curso            INT UNSIGNED    NOT NULL,
    dia_semana          ENUM('lunes','martes','miercoles','jueves','viernes','sabado','domingo') NOT NULL,
    hora_inicio         TIME            NOT NULL,
    hora_fin            TIME            NOT NULL,
    CONSTRAINT pk_horario               PRIMARY KEY (id_horario),
    CONSTRAINT fk_horario_curso         FOREIGN KEY (id_curso)
        REFERENCES curso(id_curso) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla inscripcion
-- ────────────────────────────────────────────────────────────────
CREATE TABLE inscripcion (
    id_inscripcion      INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_curso            INT UNSIGNED    NOT NULL,
    id_estudiante       INT UNSIGNED    NOT NULL,
    estado              ENUM('activa','completada','retirada','suspendida')               NOT NULL DEFAULT 'activa',
    fecha_inscripcion   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_completacion  DATETIME        NULL,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_inscripcion           PRIMARY KEY (id_inscripcion),
    CONSTRAINT uq_inscripcion           UNIQUE (id_curso, id_estudiante),
    CONSTRAINT fk_insc_curso            FOREIGN KEY (id_curso)
        REFERENCES curso(id_curso) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_insc_estudiante       FOREIGN KEY (id_estudiante)
        REFERENCES perfil_estudiante(id_estudiante) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Une curso con estudiante. Se genera al confirmar el pago.';


-- ────────────────────────────────────────────────────────────────
-- tabla leccion
-- ────────────────────────────────────────────────────────────────
CREATE TABLE leccion (
    id_leccion          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_curso            INT UNSIGNED    NOT NULL,
    id_prerequisito     INT UNSIGNED    NULL,                  -- FK autoreferencial
    nombre              VARCHAR(150)    NOT NULL,
    imagen_portada      VARCHAR(500)    NULL,
    descripcion         TEXT            NULL,
    orden               SMALLINT UNSIGNED NOT NULL,
    tipo                ENUM('teoria','practica','evaluacion','mixta') NOT NULL DEFAULT 'teoria',
    estado              ENUM('borrador','activa','inactiva','archivada') NOT NULL DEFAULT 'borrador',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_leccion               PRIMARY KEY (id_leccion),
    CONSTRAINT uq_leccion_orden         UNIQUE (id_curso, orden),
    CONSTRAINT fk_leccion_curso         FOREIGN KEY (id_curso)
        REFERENCES curso(id_curso) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_leccion_prerequisito  FOREIGN KEY (id_prerequisito)
        REFERENCES leccion(id_leccion) ON DELETE SET NULL ON UPDATE CASCADE
        -- ON DELETE SET NULL: si se borra el prerequisito, la lección queda libre
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla progreso de la leccion
-- ────────────────────────────────────────────────────────────────
CREATE TABLE progreso_leccion (
    id_progreso         INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_inscripcion      INT UNSIGNED    NOT NULL,
    id_leccion          INT UNSIGNED    NOT NULL,
    estado              ENUM('pendiente','en_progreso','completada') NOT NULL DEFAULT 'pendiente',
    porcentaje          DECIMAL(5,2)    NOT NULL DEFAULT 0.00,
    iniciado_en         DATETIME        NULL,
    completado_en       DATETIME        NULL,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_progreso              PRIMARY KEY (id_progreso),
    CONSTRAINT uq_progreso              UNIQUE (id_inscripcion, id_leccion),
    CONSTRAINT fk_prog_inscripcion      FOREIGN KEY (id_inscripcion)
        REFERENCES inscripcion(id_inscripcion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_prog_leccion          FOREIGN KEY (id_leccion)
        REFERENCES leccion(id_leccion) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla material de contenido
-- ────────────────────────────────────────────────────────────────
CREATE TABLE material_contenido (
    id_material         INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_leccion          INT UNSIGNED    NOT NULL,
    id_docente          INT UNSIGNED    NOT NULL,
    nombre              VARCHAR(250)    NOT NULL,
    tipo                ENUM('lectura','video','laboratorio','juego','documento','articulo') NOT NULL,
    descripcion         TEXT            NULL,
    orden               TINYINT UNSIGNED NOT NULL DEFAULT 1,
    estado              ENUM('activo','inactivo','archivado') NOT NULL DEFAULT 'activo',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_material              PRIMARY KEY (id_material),
    CONSTRAINT fk_mat_leccion           FOREIGN KEY (id_leccion)
        REFERENCES leccion(id_leccion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_mat_docente           FOREIGN KEY (id_docente)
        REFERENCES perfil_docente(id_docente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Sin modo_offline (plataforma online) ni archivo directo (usa tabla recurso).';


-- ────────────────────────────────────────────────────────────────
-- tabla recurso
-- ────────────────────────────────────────────────────────────────
CREATE TABLE recurso (
    id_recurso          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_material         INT UNSIGNED    NOT NULL,
    id_docente          INT UNSIGNED    NOT NULL,
    nombre              VARCHAR(200)    NOT NULL,
    descripcion         TEXT            NULL,
    url                 VARCHAR(1000)   NOT NULL,
    tipo_recurso        ENUM('video','documento','imagen','audio','enlace','interactivo') NOT NULL,
    duracion_min        SMALLINT UNSIGNED NULL,
    nivel_academico     ENUM('basico','intermedio','avanzado') NULL,
    orden               TINYINT UNSIGNED NOT NULL DEFAULT 1,
    estado              ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_recurso               PRIMARY KEY (id_recurso),
    CONSTRAINT fk_recurso_material      FOREIGN KEY (id_material)
        REFERENCES material_contenido(id_material) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_recurso_docente       FOREIGN KEY (id_docente)
        REFERENCES perfil_docente(id_docente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla articulo cientifico
-- ────────────────────────────────────────────────────────────────
CREATE TABLE articulo_cientifico (
    id_articulo         INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_material         INT UNSIGNED    NOT NULL,
    id_docente          INT UNSIGNED    NOT NULL,
    titulo              VARCHAR(300)    NOT NULL,
    autores             VARCHAR(500)    NOT NULL,
    resumen             TEXT            NULL,
    revista             VARCHAR(250)    NULL,
    doi                 VARCHAR(100)    NULL,
    url                 VARCHAR(1000)   NULL,
    fecha_publicacion   DATE            NULL,
    estado              ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_articulo              PRIMARY KEY (id_articulo),
    CONSTRAINT fk_art_material          FOREIGN KEY (id_material)
        REFERENCES material_contenido(id_material) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_art_docente           FOREIGN KEY (id_docente)
        REFERENCES perfil_docente(id_docente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla recurso de apoyo
-- ────────────────────────────────────────────────────────────────
CREATE TABLE recurso_apoyo (
    id_recurso_apoyo        INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_material             INT UNSIGNED    NOT NULL,
    id_recurso              INT UNSIGNED    NULL,
    id_articulo             INT UNSIGNED    NULL,
    id_usuario              INT UNSIGNED    NOT NULL,
    tipo_recurso            ENUM('documento','video','enlace','imagen','interactivo','otro') NOT NULL,
    tipo_contenido          ENUM('primario','complementario','referencia','evaluativo') NOT NULL DEFAULT 'complementario',
    titulo                  VARCHAR(250)    NOT NULL,
    url                     VARCHAR(500)    NULL,
    ruta_imagen             VARCHAR(500)    NULL,
    orden                   TINYINT UNSIGNED NOT NULL DEFAULT 1,
    estado                  ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
    fecha_creacion          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_recurso_apoyo             PRIMARY KEY (id_recurso_apoyo),
    CONSTRAINT fk_ra_material               FOREIGN KEY (id_material)
        REFERENCES material_contenido(id_material) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ra_recurso                FOREIGN KEY (id_recurso)
        REFERENCES recurso(id_recurso) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_ra_articulo               FOREIGN KEY (id_articulo)
        REFERENCES articulo_cientifico(id_articulo) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_ra_usuario                FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Vincula recursos complementarios y articulos a un material de contenido con tipo y orden.';


-- ────────────────────────────────────────────────────────────────
-- tabla actividad
-- ────────────────────────────────────────────────────────────────
CREATE TABLE actividad (
    id_actividad        INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_leccion          INT UNSIGNED    NOT NULL,
    id_docente          INT UNSIGNED    NOT NULL,
    nombre              VARCHAR(250)    NOT NULL,
    descripcion         TEXT            NULL,
    instrucciones       TEXT            NULL,
    tipo                ENUM('tarea','proyecto','investigacion','ejercicio','otro') NOT NULL DEFAULT 'tarea',
    fecha_disponible    DATETIME        NULL,
    fecha_limite        DATETIME        NULL,
    permite_entrega_tardia TINYINT(1)   NOT NULL DEFAULT 0,
    puntaje_maximo      DECIMAL(10,2)   NOT NULL DEFAULT 10.00,
    estado              ENUM('activa','inactiva','cerrada') NOT NULL DEFAULT 'activa',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_actividad             PRIMARY KEY (id_actividad),
    CONSTRAINT fk_act_leccion           FOREIGN KEY (id_leccion)
        REFERENCES leccion(id_leccion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_act_docente           FOREIGN KEY (id_docente)
        REFERENCES perfil_docente(id_docente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla entrega de actividad
-- ────────────────────────────────────────────────────────────────
CREATE TABLE entrega_actividad (
    id_entrega          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_actividad        INT UNSIGNED    NOT NULL,
    id_estudiante       INT UNSIGNED    NOT NULL,
    id_revisor          INT UNSIGNED    NULL,                  -- docente que revisa
    nombre_archivo      VARCHAR(250)    NOT NULL,
    url_archivo         VARCHAR(1000)   NOT NULL,
    tipo_archivo        ENUM('documento','video','audio','imagen','presentacion','otro') NOT NULL,
    tamanio_kb          INT UNSIGNED    NULL,
    es_tardia           TINYINT(1)      NOT NULL DEFAULT 0,
    calificacion        DECIMAL(10,2)   NULL,
    comentario_revisor  TEXT            NULL,
    fecha_entrega       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_revision      DATETIME        NULL,
    estado              ENUM('entregada','revisada','rechazada') NOT NULL DEFAULT 'entregada',
    CONSTRAINT pk_entrega               PRIMARY KEY (id_entrega),
    CONSTRAINT fk_entrega_actividad     FOREIGN KEY (id_actividad)
        REFERENCES actividad(id_actividad) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_entrega_estudiante    FOREIGN KEY (id_estudiante)
        REFERENCES perfil_estudiante(id_estudiante) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_entrega_revisor       FOREIGN KEY (id_revisor)
        REFERENCES perfil_docente(id_docente) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Fusiona Archivo + resultado_actividad. El entregable y su calificacion en una sola entidad.';


-- ────────────────────────────────────────────────────────────────
-- tabla foro
-- ────────────────────────────────────────────────────────────────
CREATE TABLE foro (
    id_foro             INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_leccion          INT UNSIGNED    NOT NULL,
    id_docente          INT UNSIGNED    NOT NULL,
    nombre              VARCHAR(250)    NOT NULL,
    descripcion         TEXT            NULL,
    instrucciones       TEXT            NULL,
    tema                VARCHAR(250)    NULL,
    reglas_interaccion  TEXT            NULL,
    tipo_participante   ENUM('abierto','solo_estudiantes','moderado') NOT NULL DEFAULT 'abierto',
    fecha_apertura      DATETIME        NULL,
    fecha_cierre        DATETIME        NULL,
    estado              ENUM('activo','cerrado','archivado') NOT NULL DEFAULT 'activo',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_foro                  PRIMARY KEY (id_foro),
    CONSTRAINT fk_foro_leccion          FOREIGN KEY (id_leccion)
        REFERENCES leccion(id_leccion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_foro_docente          FOREIGN KEY (id_docente)
        REFERENCES perfil_docente(id_docente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla comentario de foro
-- ────────────────────────────────────────────────────────────────
CREATE TABLE comentario_foro (
    id_comentario       INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_foro             INT UNSIGNED    NOT NULL,
    id_usuario          INT UNSIGNED    NOT NULL,
    id_padre            INT UNSIGNED    NULL,                  -- NULL = raíz; NOT NULL = respuesta
    asunto              VARCHAR(250)    NULL,
    contenido           TEXT            NOT NULL,
    tipo                ENUM('aporte','respuesta','retroalimentacion') NOT NULL DEFAULT 'aporte',
    estado              ENUM('activo','moderado','eliminado') NOT NULL DEFAULT 'activo',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_comentario            PRIMARY KEY (id_comentario),
    CONSTRAINT fk_com_foro              FOREIGN KEY (id_foro)
        REFERENCES foro(id_foro) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_com_usuario           FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_com_padre             FOREIGN KEY (id_padre)
        REFERENCES comentario_foro(id_comentario) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Sin tabla hilo intermedia. Jerarquia via id_padre autoreferencial.';


-- ────────────────────────────────────────────────────────────────
-- tabla comentario adjunto
-- ────────────────────────────────────────────────────────────────
CREATE TABLE adjunto_comentario (
    id_adjunto          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_comentario       INT UNSIGNED    NOT NULL,
    id_usuario          INT UNSIGNED    NOT NULL,
    url                 VARCHAR(1000)   NOT NULL,
    tipo                ENUM('imagen','documento','video','enlace','otro') NOT NULL,
    nombre_archivo      VARCHAR(250)    NULL,
    tamanio_kb          INT UNSIGNED    NULL,
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_adjunto               PRIMARY KEY (id_adjunto),
    CONSTRAINT fk_adj_comentario        FOREIGN KEY (id_comentario)
        REFERENCES comentario_foro(id_comentario) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_adj_usuario           FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; 


-- ────────────────────────────────────────────────────────────────
-- tabla calificacion de foro
-- ────────────────────────────────────────────────────────────────
CREATE TABLE calificacion_foro (
    id_calificacion     INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_foro             INT UNSIGNED    NOT NULL,
    id_inscripcion      INT UNSIGNED    NOT NULL,
    id_docente          INT UNSIGNED    NOT NULL,
    cantidad_aportes    SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    calidad_aportes     ENUM('excelente','bueno','regular','insuficiente') NOT NULL DEFAULT 'regular',
    calificacion        DECIMAL(5,2)    NOT NULL DEFAULT 0.00,
    retroalimentacion   TEXT            NULL,
    estado              ENUM('borrador','publicada') NOT NULL DEFAULT 'borrador',
    calificado_en       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_calif_foro            PRIMARY KEY (id_calificacion),
    CONSTRAINT uq_calif_foro            UNIQUE (id_foro, id_inscripcion),
    CONSTRAINT fk_cf_foro               FOREIGN KEY (id_foro)
        REFERENCES foro(id_foro) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cf_inscripcion        FOREIGN KEY (id_inscripcion)
        REFERENCES inscripcion(id_inscripcion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cf_docente            FOREIGN KEY (id_docente)
        REFERENCES perfil_docente(id_docente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla sesion online
-- ────────────────────────────────────────────────────────────────
CREATE TABLE sesion_online (
    id_sesion           INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_leccion          INT UNSIGNED    NOT NULL,
    id_docente          INT UNSIGNED    NOT NULL,
    nombre              VARCHAR(250)    NOT NULL,
    tematica            VARCHAR(150)    NULL,
    descripcion         TEXT            NULL,
    enlace              VARCHAR(1000)   NOT NULL,
    plataforma          ENUM('meet','youtube','zoom','teams','otro') NOT NULL DEFAULT 'meet',
    codigo_asistencia   VARCHAR(20)     NULL,                  -- código temporal que ingresa el estudiante
    fecha_inicio        DATETIME        NOT NULL,
    fecha_fin           DATETIME        NULL,
    grabacion_url       VARCHAR(1000)   NULL,
    estado              ENUM('programada','en_curso','finalizada','cancelada') NOT NULL DEFAULT 'programada',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_sesion                PRIMARY KEY (id_sesion),
    CONSTRAINT fk_sesion_leccion        FOREIGN KEY (id_leccion)
        REFERENCES leccion(id_leccion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_sesion_docente        FOREIGN KEY (id_docente)
        REFERENCES perfil_docente(id_docente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla evento virtual
-- ────────────────────────────────────────────────────────────────
CREATE TABLE evento_virtual (
    id_evento           INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_docente          INT UNSIGNED    NOT NULL,
    id_leccion          INT UNSIGNED    NULL,                  -- opcional: puede ser evento institucional
    nombre              VARCHAR(200)    NOT NULL,
    descripcion         TEXT            NULL,
    enlace_acceso       VARCHAR(1000)   NULL,
    fecha_inicio        DATETIME        NOT NULL,
    fecha_fin           DATETIME        NULL,
    emite_certificado   TINYINT(1)      NOT NULL DEFAULT 0,
    estado              ENUM('programado','en_curso','finalizado','cancelado') NOT NULL DEFAULT 'programado',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_evento                PRIMARY KEY (id_evento),
    CONSTRAINT fk_evento_docente        FOREIGN KEY (id_docente)
        REFERENCES perfil_docente(id_docente) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_evento_leccion        FOREIGN KEY (id_leccion)
        REFERENCES leccion(id_leccion) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla asistencia
-- ────────────────────────────────────────────────────────────────
CREATE TABLE asistencia (
    id_asistencia       INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_usuario          INT UNSIGNED    NOT NULL,
    id_sesion           INT UNSIGNED    NULL,
    id_evento           INT UNSIGNED    NULL,
    asistio             TINYINT(1)      NOT NULL DEFAULT 0,
    codigo_ingresado    VARCHAR(20)     NULL,
    fecha_ingreso       DATETIME        NULL,
    fecha_salida        DATETIME        NULL,
    duracion_min        SMALLINT UNSIGNED NULL,
    calificacion        DECIMAL(5,2)    NULL,
    comentarios         TEXT            NULL,
    nivel_participacion ENUM('alto','medio','bajo','ninguno') NULL,
    registrado_en       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_asistencia            PRIMARY KEY (id_asistencia),
    CONSTRAINT fk_asist_usuario         FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_asist_sesion          FOREIGN KEY (id_sesion)
        REFERENCES sesion_online(id_sesion) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_asist_evento          FOREIGN KEY (id_evento)
        REFERENCES evento_virtual(id_evento) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Fusiona seguimiento_participacion. CHECK garantiza que apunte a sesion o evento.';


-- ────────────────────────────────────────────────────────────────
-- tabla plantilla_certificado
-- Plantilla visual del certificado (color, logo, firma, texto).
-- La personaliza el docente o el administrador de la institución.
-- ────────────────────────────────────────────────────────────────
CREATE TABLE plantilla_certificado (
    id_plantilla            INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_docente              INT UNSIGNED    NULL,                  -- NULL = plantilla institucional por defecto
    color_acento            VARCHAR(7)      NULL,                  -- hex, ej. '#2C4A42'
    logo_url                VARCHAR(500)    NULL,
    firma_representante_nombre VARCHAR(150) NULL,
    firma_imagen_url        VARCHAR(500)    NULL,
    texto_reconocimiento    TEXT            NULL,
    creado_en               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_plantilla_certificado     PRIMARY KEY (id_plantilla),
    CONSTRAINT fk_plantilla_docente         FOREIGN KEY (id_docente)
        REFERENCES perfil_docente(id_docente) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla certificado
-- ────────────────────────────────────────────────────────────────
CREATE TABLE certificado (
    id_certificado      INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_emisor           INT UNSIGNED    NOT NULL,              -- docente u organizador
    id_receptor         INT UNSIGNED    NOT NULL,              -- estudiante o participante
    id_evento           INT UNSIGNED    NULL,                  -- certificado de evento
    id_inscripcion      INT UNSIGNED    NULL,                  -- certificado de curso completado
    id_plantilla        INT UNSIGNED    NULL,                  -- plantilla visual usada al emitirlo
    nombre              VARCHAR(200)    NOT NULL,
    codigo_validacion   VARCHAR(80)     NOT NULL,              -- código único público para verificar
    estado              ENUM('emitido','anulado') NOT NULL DEFAULT 'emitido',
    emitido_en          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_certificado           PRIMARY KEY (id_certificado),
    CONSTRAINT uq_cert_codigo           UNIQUE (codigo_validacion),
    CONSTRAINT fk_cert_emisor           FOREIGN KEY (id_emisor)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_cert_receptor         FOREIGN KEY (id_receptor)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_cert_evento           FOREIGN KEY (id_evento)
        REFERENCES evento_virtual(id_evento) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_cert_inscripcion      FOREIGN KEY (id_inscripcion)
        REFERENCES inscripcion(id_inscripcion) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_cert_plantilla        FOREIGN KEY (id_plantilla)
        REFERENCES plantilla_certificado(id_plantilla) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla evaluacion
-- ────────────────────────────────────────────────────────────────
CREATE TABLE evaluacion (
    id_evaluacion       INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_leccion          INT UNSIGNED    NOT NULL,
    id_docente          INT UNSIGNED    NOT NULL,
    nombre              VARCHAR(200)    NOT NULL,
    descripcion         TEXT            NULL,
    tipo                ENUM('diagnostica','formativa','sumativa') NOT NULL DEFAULT 'formativa',
    tiempo_limite_min   SMALLINT UNSIGNED NULL,                -- NULL = sin límite
    intentos_permitidos TINYINT UNSIGNED NOT NULL DEFAULT 2,
    puntaje_maximo      DECIMAL(10,2)   NOT NULL DEFAULT 10.00,
    ponderacion         DECIMAL(5,2)    NOT NULL DEFAULT 100.00, -- peso en el curso (%)
    fecha_aplicacion    DATETIME        NULL,
    fecha_apertura      DATETIME        NULL,
    fecha_cierre        DATETIME        NULL,
    estado              ENUM('borrador','activa','cerrada') NOT NULL DEFAULT 'borrador',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_evaluacion            PRIMARY KEY (id_evaluacion),
    CONSTRAINT fk_eval_leccion          FOREIGN KEY (id_leccion)
        REFERENCES leccion(id_leccion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_eval_docente          FOREIGN KEY (id_docente)
        REFERENCES perfil_docente(id_docente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla pregunta
-- ────────────────────────────────────────────────────────────────
CREATE TABLE pregunta (
    id_pregunta         INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_evaluacion       INT UNSIGNED    NOT NULL,
    enunciado           TEXT            NOT NULL,
    tipo                ENUM('seleccion_multiple','verdadero_falso','abierta')              NOT NULL,
    puntaje             DECIMAL(10,2)   NOT NULL DEFAULT 1.00,
    retroalimentacion   TEXT            NULL,     -- explicación de la respuesta correcta
                                                  -- visible al estudiante SOLO al agotar todos los intentos
    orden               TINYINT UNSIGNED NOT NULL DEFAULT 1,
    estado              ENUM('activa','inactiva') NOT NULL DEFAULT 'activa',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_pregunta              PRIMARY KEY (id_pregunta),
    CONSTRAINT fk_preg_evaluacion       FOREIGN KEY (id_evaluacion)
        REFERENCES evaluacion(id_evaluacion) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla opcion de pregunta
-- ────────────────────────────────────────────────────────────────
CREATE TABLE opcion_pregunta (
    id_opcion           INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_pregunta         INT UNSIGNED    NOT NULL,
    texto               VARCHAR(500)    NOT NULL,
    es_correcta         TINYINT(1)      NOT NULL DEFAULT 0,
    retroalimentacion   TEXT            NULL,     -- por qué esta opción es correcta o incorrecta
    orden               TINYINT UNSIGNED NOT NULL DEFAULT 1,
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_opcion                PRIMARY KEY (id_opcion),
    CONSTRAINT fk_opcion_pregunta       FOREIGN KEY (id_pregunta)
        REFERENCES pregunta(id_pregunta) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla resultado de evaluacion
-- ────────────────────────────────────────────────────────────────
CREATE TABLE resultado_evaluacion (
    id_resultado        INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_evaluacion       INT UNSIGNED    NOT NULL,
    id_inscripcion      INT UNSIGNED    NOT NULL,              -- de aquí se deriva el estudiante (inscripcion.id_estudiante)
    numero_intento      TINYINT UNSIGNED NOT NULL DEFAULT 1,
    puntaje_total       DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    porcentaje          DECIMAL(5,2)    NOT NULL DEFAULT 0.00,
    aprobado            TINYINT(1)      NOT NULL DEFAULT 0,    -- 1 si porcentaje >= umbral de aprobación
    estado              ENUM('pendiente','calculado','publicado','revisado') NOT NULL DEFAULT 'calculado',
    completado_en       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_resultado             PRIMARY KEY (id_resultado),
    CONSTRAINT uq_resultado_intento     UNIQUE (id_evaluacion, id_inscripcion, numero_intento),
    CONSTRAINT fk_res_evaluacion        FOREIGN KEY (id_evaluacion)
        REFERENCES evaluacion(id_evaluacion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_res_inscripcion       FOREIGN KEY (id_inscripcion)
        REFERENCES inscripcion(id_inscripcion) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla respuesta de estudiante
-- ────────────────────────────────────────────────────────────────
CREATE TABLE respuesta_estudiante (
    id_respuesta        INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_resultado        INT UNSIGNED    NOT NULL,
    id_pregunta         INT UNSIGNED    NOT NULL,
    id_opcion           INT UNSIGNED    NULL,                  -- NULL para preguntas abiertas
    id_revisor          INT UNSIGNED    NULL,                  -- docente que revisa pregunta abierta
    contenido_abierto   TEXT            NULL,
    estado_revision     ENUM('pendiente','correcta','incorrecta','parcial') NOT NULL DEFAULT 'pendiente',
    puntaje_obtenido    DECIMAL(10,2)   NULL,
    comentario_revisor  TEXT            NULL,
    revisado_en         DATETIME        NULL,
    CONSTRAINT pk_respuesta             PRIMARY KEY (id_respuesta),
    CONSTRAINT uq_respuesta             UNIQUE (id_resultado, id_pregunta),
    CONSTRAINT fk_resp_resultado        FOREIGN KEY (id_resultado)
        REFERENCES resultado_evaluacion(id_resultado) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_resp_pregunta         FOREIGN KEY (id_pregunta)
        REFERENCES pregunta(id_pregunta) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_resp_opcion           FOREIGN KEY (id_opcion)
        REFERENCES opcion_pregunta(id_opcion) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_resp_revisor          FOREIGN KEY (id_revisor)
        REFERENCES perfil_docente(id_docente) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla datos_facturacion
-- Configuración institucional para el encabezado de las facturas
-- (razón social, NIT y logo). Fila única, editable desde Admin.
-- ────────────────────────────────────────────────────────────────
CREATE TABLE datos_facturacion (
    id_datos_facturacion INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    razon_social         VARCHAR(200)    NOT NULL,
    nit                  VARCHAR(20)     NOT NULL,
    direccion            VARCHAR(255)    NULL,
    logo_url             VARCHAR(500)    NULL,
    actualizado_en       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_datos_facturacion     PRIMARY KEY (id_datos_facturacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Fila única (o una por institución si el negocio lo requiere). La usan Contador y Estudiante al generar/descargar una factura.';


-- ────────────────────────────────────────────────────────────────
-- tabla pago
-- ────────────────────────────────────────────────────────────────
CREATE TABLE pago (
    id_pago             INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_estudiante       INT UNSIGNED    NOT NULL,              -- estudiante directamente involucrado
    id_contador         INT UNSIGNED    NULL,                  -- contador que confirma el pago (NULL = aún sin confirmar)
    medio_pago          ENUM('pse','wompi_tarjeta','wompi_nequi','transferencia','otro') NOT NULL,
    monto               DECIMAL(12,2)   NOT NULL,
    fecha_pago          DATETIME        NULL,
    referencia_externa  VARCHAR(200)    NULL,                  -- ID de la pasarela PSE/Wompi
    estado              ENUM('pendiente','aprobado','rechazado','reembolsado') NOT NULL DEFAULT 'pendiente',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_pago                  PRIMARY KEY (id_pago),
    CONSTRAINT fk_pago_estudiante       FOREIGN KEY (id_estudiante)
        REFERENCES perfil_estudiante(id_estudiante) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_pago_contador         FOREIGN KEY (id_contador)
        REFERENCES perfil_contador(id_contador) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla pago inscripcion
-- ────────────────────────────────────────────────────────────────
CREATE TABLE pago_inscripcion (
    id_pago_inscripcion INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_pago             INT UNSIGNED    NOT NULL,
    id_inscripcion      INT UNSIGNED    NOT NULL,
    monto_asignado      DECIMAL(12,2)   NOT NULL,
    CONSTRAINT pk_pago_inscripcion      PRIMARY KEY (id_pago_inscripcion),
    CONSTRAINT uq_pago_inscripcion      UNIQUE (id_pago, id_inscripcion),
    CONSTRAINT fk_pi_pago               FOREIGN KEY (id_pago)
        REFERENCES pago(id_pago) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_pi_inscripcion        FOREIGN KEY (id_inscripcion)
        REFERENCES inscripcion(id_inscripcion) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Un pago puede cubrir varias inscripciones (carrito).';


-- ────────────────────────────────────────────────────────────────
-- tabla factura
-- ────────────────────────────────────────────────────────────────
CREATE TABLE factura (
    id_factura          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_pago             INT UNSIGNED    NOT NULL,
    id_estudiante       INT UNSIGNED    NOT NULL,
    id_datos_facturacion INT UNSIGNED   NULL,                  -- foto de qué razón social/NIT/logo tenía BioLearn al emitirla
    numero_factura      VARCHAR(50)     NOT NULL,
    descripcion         TEXT            NULL,
    monto_total         DECIMAL(12,2)   NOT NULL,
    monto_pagado        DECIMAL(12,2)   NULL,
    estado              ENUM('emitida','anulada') NOT NULL DEFAULT 'emitida',
    enviada_por_correo  TINYINT(1)      NOT NULL DEFAULT 0,
    enviada_en          DATETIME        NULL,
    fecha_emision       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_factura               PRIMARY KEY (id_factura),
    CONSTRAINT uq_factura_numero        UNIQUE (numero_factura),
    CONSTRAINT fk_fact_pago             FOREIGN KEY (id_pago)
        REFERENCES pago(id_pago) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_fact_estudiante       FOREIGN KEY (id_estudiante)
        REFERENCES perfil_estudiante(id_estudiante) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_fact_datos_facturacion FOREIGN KEY (id_datos_facturacion)
        REFERENCES datos_facturacion(id_datos_facturacion) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla reembolso
-- ────────────────────────────────────────────────────────────────
CREATE TABLE reembolso (
    id_reembolso        INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_pago             INT UNSIGNED    NOT NULL,
    id_solicitante      INT UNSIGNED    NOT NULL,              -- usuario que solicita
    id_contador         INT UNSIGNED    NULL,                  -- contador que resuelve
    motivo              TEXT            NOT NULL,
    monto               DECIMAL(12,2)   NOT NULL,
    justificacion       TEXT            NULL,                  -- motivo de aprobación o rechazo
    estado              ENUM('solicitado','aprobado','rechazado') NOT NULL DEFAULT 'solicitado',
    solicitado_en       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resuelto_en         DATETIME        NULL,
    CONSTRAINT pk_reembolso             PRIMARY KEY (id_reembolso),
    CONSTRAINT fk_ree_pago              FOREIGN KEY (id_pago)
        REFERENCES pago(id_pago) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_ree_solicitante       FOREIGN KEY (id_solicitante)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_ree_contador          FOREIGN KEY (id_contador)
        REFERENCES perfil_contador(id_contador) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla mensaje
-- ────────────────────────────────────────────────────────────────
CREATE TABLE mensaje (
    id_mensaje          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_emisor           INT UNSIGNED    NOT NULL,
    id_receptor         INT UNSIGNED    NOT NULL,
    id_curso            INT UNSIGNED    NULL,                  -- contexto opcional
    asunto              VARCHAR(200)    NULL,
    contenido           TEXT            NOT NULL,
    leido               TINYINT(1)      NOT NULL DEFAULT 0,
    leido_en            DATETIME        NULL,
    estado_emisor       ENUM('activo','eliminado') NOT NULL DEFAULT 'activo',
    estado_receptor     ENUM('activo','eliminado') NOT NULL DEFAULT 'activo',
    enviado_en          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_mensaje               PRIMARY KEY (id_mensaje),
    CONSTRAINT fk_msg_emisor            FOREIGN KEY (id_emisor)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_msg_receptor          FOREIGN KEY (id_receptor)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_msg_curso             FOREIGN KEY (id_curso)
        REFERENCES curso(id_curso) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla notificacion
-- ────────────────────────────────────────────────────────────────
CREATE TABLE notificacion (
    id_notificacion     INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_usuario          INT UNSIGNED    NOT NULL,              -- usuario destinatario
    id_usuario_emisor   INT UNSIGNED    NULL,                  -- usuario que generó la notificación (NULL = sistema)
    tipo                ENUM('pago','inscripcion','leccion','evaluacion','foro','mensaje','certificado','reembolso','sistema') NOT NULL,
    entidad_id          INT UNSIGNED    NULL,                  -- id del registro relacionado (pago, factura, leccion...
                                                                -- según 'tipo'); no lleva FK propia porque apunta
                                                                -- a una tabla distinta según el caso (referencia polimórfica,
                                                                -- resuelta en la capa de Servicio, no en la base de datos)
    titulo              VARCHAR(200)    NOT NULL,
    contenido           TEXT            NULL,
    estado_notificacion ENUM('pendiente','enviada','leida','archivada') NOT NULL DEFAULT 'pendiente',
    leida               TINYINT(1)      NOT NULL DEFAULT 0,
    leida_en            DATETIME        NULL,
    creada_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notificacion          PRIMARY KEY (id_notificacion),
    CONSTRAINT fk_notif_emisor          FOREIGN KEY (id_usuario_emisor)
        REFERENCES usuario(id_usuario) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_notif_usuario         FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='entidad_id + tipo permiten notificar sobre cualquier evento del sistema (pago, factura, leccion, foro...).';


-- ────────────────────────────────────────────────────────────────
-- tabla reporte
-- ────────────────────────────────────────────────────────────────
CREATE TABLE reporte (
    id_reporte          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_solicitante      INT UNSIGNED    NOT NULL,
    id_curso            INT UNSIGNED    NULL,
    id_inscripcion      INT UNSIGNED    NULL,
    id_institucion      INT UNSIGNED    NULL,                  -- institución sobre la que trata el reporte (directo, sin pasar por un supervisor)
    id_supervisor_inst  INT UNSIGNED    NULL,                  -- si lo solicitó puntualmente un representante institucional
    id_contador         INT UNSIGNED    NULL,
    id_factura          INT UNSIGNED    NULL,
    id_datos_facturacion INT UNSIGNED   NULL,                  -- logo/razón social de BioLearn vigentes al generar el reporte
    tipo                ENUM('academico','financiero','institucional','mixto') NOT NULL,
    formato             ENUM('pdf','excel','csv') NULL,
    fecha_corte         DATETIME        NULL,
    motivo              TEXT            NULL,
    descripcion         TEXT            NULL,
    contenido           LONGTEXT        NULL,                  -- JSON del reporte generado
    estado              ENUM('pendiente','generado','validado','cerrado') NOT NULL DEFAULT 'pendiente',
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_reporte               PRIMARY KEY (id_reporte),
    CONSTRAINT fk_rep_solicitante       FOREIGN KEY (id_solicitante)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_rep_curso             FOREIGN KEY (id_curso)
        REFERENCES curso(id_curso) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_rep_inscripcion       FOREIGN KEY (id_inscripcion)
        REFERENCES inscripcion(id_inscripcion) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_rep_institucion       FOREIGN KEY (id_institucion)
        REFERENCES institucion(id_institucion) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_rep_supervisor        FOREIGN KEY (id_supervisor_inst)
        REFERENCES supervisor_institucion(id_supervisor_inst) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_rep_contador          FOREIGN KEY (id_contador)
        REFERENCES perfil_contador(id_contador) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_rep_factura           FOREIGN KEY (id_factura)
        REFERENCES factura(id_factura) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_rep_datos_facturacion FOREIGN KEY (id_datos_facturacion)
        REFERENCES datos_facturacion(id_datos_facturacion) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla reporte de certificado
-- ────────────────────────────────────────────────────────────────
CREATE TABLE reporte_certificado (
    id_reporte          INT UNSIGNED    NOT NULL,
    id_certificado      INT UNSIGNED    NOT NULL,
    incluido_en_calculo TINYINT(1)      NOT NULL DEFAULT 1,
    observaciones       TEXT            NULL,
    CONSTRAINT pk_rep_cert              PRIMARY KEY (id_reporte, id_certificado),
    CONSTRAINT fk_rc_reporte            FOREIGN KEY (id_reporte)
        REFERENCES reporte(id_reporte) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_rc_certificado        FOREIGN KEY (id_certificado)
        REFERENCES certificado(id_certificado) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ────────────────────────────────────────────────────────────────
-- tabla retroalimentacion
-- ────────────────────────────────────────────────────────────────
CREATE TABLE retroalimentacion (
    id_retroalimentacion    INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_usuario_emisor       INT UNSIGNED    NOT NULL,
    id_usuario_receptor     INT UNSIGNED    NOT NULL,
    id_respuesta            INT UNSIGNED    NULL,
    id_resultado            INT UNSIGNED    NULL,
    id_asistencia           INT UNSIGNED    NULL,
    id_comentario           INT UNSIGNED    NULL,
    id_calificacion_foro    INT UNSIGNED    NULL,
    id_entrega              INT UNSIGNED    NULL,
    comentarios             TEXT            NOT NULL,
    sugerencias             TEXT            NULL,
    estado                  ENUM('borrador','enviada','leida','archivada') NOT NULL DEFAULT 'enviada',
    fecha_retroalimentacion DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_retroalimentacion         PRIMARY KEY (id_retroalimentacion),
    CONSTRAINT fk_retro_emisor              FOREIGN KEY (id_usuario_emisor)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_retro_receptor            FOREIGN KEY (id_usuario_receptor)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_retro_respuesta           FOREIGN KEY (id_respuesta)
        REFERENCES respuesta_estudiante(id_respuesta) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_retro_resultado           FOREIGN KEY (id_resultado)
        REFERENCES resultado_evaluacion(id_resultado) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_retro_asistencia          FOREIGN KEY (id_asistencia)
        REFERENCES asistencia(id_asistencia) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_retro_comentario          FOREIGN KEY (id_comentario)
        REFERENCES comentario_foro(id_comentario) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_retro_calificacion_foro   FOREIGN KEY (id_calificacion_foro)
        REFERENCES calificacion_foro(id_calificacion) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_retro_entrega             FOREIGN KEY (id_entrega)
        REFERENCES entrega_actividad(id_entrega) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Retroalimentacion formal entre docente/supervisor y estudiante.';


-- ═══════════════════════════════════════════════════════════════════════════

CREATE INDEX idx_leccion_curso        ON leccion (id_curso, orden);
CREATE INDEX idx_progreso_insc        ON progreso_leccion (id_inscripcion);
CREATE INDEX idx_resultado_insc       ON resultado_evaluacion (id_inscripcion, id_evaluacion);
CREATE INDEX idx_respuesta_resultado  ON respuesta_estudiante (id_resultado);
CREATE INDEX idx_comentario_foro      ON comentario_foro (id_foro, id_padre);
CREATE INDEX idx_notif_usuario        ON notificacion (id_usuario, leida);
CREATE INDEX idx_pago_estudiante      ON pago (id_estudiante, estado);
CREATE INDEX idx_inscripcion_est      ON inscripcion (id_estudiante, estado);
CREATE INDEX idx_material_leccion     ON material_contenido (id_leccion, orden);
CREATE INDEX idx_asistencia_sesion    ON asistencia (id_sesion, id_usuario);
CREATE INDEX idx_asistencia_evento    ON asistencia (id_evento, id_usuario);

SET FOREIGN_KEY_CHECKS = 1;
-- ═══════════════════════════════════════════════════════════════════════════