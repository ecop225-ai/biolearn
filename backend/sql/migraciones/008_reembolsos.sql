-- =====================================================================
-- BioLearn · Migración 008 · Módulo de Pagos, Fase 2 (reembolsos y notas crédito)
--   - inscripcion: estado «en_reembolso» (el curso queda en pausa mientras se revisa la solicitud).
--   - reembolso: una solicitud por pago con los cursos elegidos; estados devuelto y cancelado;
--     medio y datos de la devolución (mismo medio, cuenta bancaria o Nequi) y su referencia.
--   - reembolso_inscripcion (nueva): cursos de la solicitud con su valor y el avance al solicitar.
--   - nota_credito (nueva): una por reembolso aprobado, sobre la factura del pago (BIO-NC-AAAA-000001).
--   - consecutivo_nota_credito (nueva): numeración sin huecos ni repetidos por año.
-- Ejecutar una sola vez en MySQL Workbench (biolearn_db) y luego recrear biolearn_test. Quedan 56 tablas.
-- =====================================================================
USE biolearn_db;

ALTER TABLE inscripcion
  MODIFY estado ENUM('pendiente_pago','activa','completada','retirada','suspendida','en_reembolso') NOT NULL DEFAULT 'activa';

ALTER TABLE reembolso
  MODIFY estado ENUM('solicitado','aprobado','devuelto','rechazado','cancelado') NOT NULL DEFAULT 'solicitado',
  MODIFY motivo VARCHAR(500) NOT NULL,
  MODIFY justificacion VARCHAR(500) NULL,
  ADD COLUMN medio_devolucion ENUM('mismo_medio','cuenta_bancaria','nequi') NOT NULL DEFAULT 'mismo_medio' AFTER monto,
  ADD COLUMN banco VARCHAR(100) NULL AFTER medio_devolucion,
  ADD COLUMN tipo_cuenta ENUM('ahorros','corriente') NULL AFTER banco,
  ADD COLUMN numero_cuenta VARCHAR(20) NULL AFTER tipo_cuenta,
  ADD COLUMN titular VARCHAR(150) NULL AFTER numero_cuenta,
  ADD COLUMN documento_titular VARCHAR(15) NULL AFTER titular,
  ADD COLUMN telefono_nequi VARCHAR(10) NULL AFTER documento_titular,
  ADD COLUMN referencia_devolucion VARCHAR(200) NULL AFTER resuelto_en,
  ADD COLUMN comprobante_devolucion_url VARCHAR(500) NULL AFTER referencia_devolucion,
  ADD COLUMN devuelto_en DATETIME NULL AFTER comprobante_devolucion_url,
  ADD INDEX idx_reembolso_estado (estado),
  ADD INDEX idx_reembolso_solicitado (solicitado_en);

CREATE TABLE reembolso_inscripcion (
    id_reembolso_inscripcion INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    id_reembolso             INT UNSIGNED  NOT NULL,
    id_pago_inscripcion      INT UNSIGNED  NOT NULL,
    monto                    DECIMAL(12,2) NOT NULL,              -- valor pagado por el curso (monto_asignado)
    avance                   DECIMAL(5,2)  NOT NULL DEFAULT 0,    -- avance del curso al solicitar (debe ser < 20)
    estado_inscripcion_previo ENUM('activa','retirada') NOT NULL, -- estado al que vuelve si se rechaza o se cancela
    CONSTRAINT pk_reembolso_inscripcion PRIMARY KEY (id_reembolso_inscripcion),
    CONSTRAINT uq_reembolso_linea      UNIQUE (id_reembolso, id_pago_inscripcion),
    CONSTRAINT fk_ri_reembolso         FOREIGN KEY (id_reembolso)
        REFERENCES reembolso(id_reembolso) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ri_pago_inscripcion  FOREIGN KEY (id_pago_inscripcion)
        REFERENCES pago_inscripcion(id_pago_inscripcion) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Cursos de una solicitud de reembolso; un curso pagado solo puede estar en una solicitud vigente.';

CREATE TABLE nota_credito (
    id_nota_credito      INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    id_reembolso         INT UNSIGNED    NOT NULL,
    id_factura           INT UNSIGNED    NOT NULL,
    id_estudiante        INT UNSIGNED    NOT NULL,
    id_datos_facturacion INT UNSIGNED    NULL,
    numero_nota          VARCHAR(50)     NOT NULL,
    motivo               VARCHAR(500)    NOT NULL,
    monto_total          DECIMAL(12,2)   NOT NULL,
    enviada_por_correo   TINYINT(1)      NOT NULL DEFAULT 0,
    enviada_en           DATETIME        NULL,
    fecha_emision        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_nota_credito          PRIMARY KEY (id_nota_credito),
    CONSTRAINT uq_nota_numero           UNIQUE (numero_nota),
    CONSTRAINT uq_nota_reembolso        UNIQUE (id_reembolso),
    CONSTRAINT fk_nc_reembolso          FOREIGN KEY (id_reembolso)
        REFERENCES reembolso(id_reembolso) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_nc_factura            FOREIGN KEY (id_factura)
        REFERENCES factura(id_factura) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_nc_estudiante         FOREIGN KEY (id_estudiante)
        REFERENCES perfil_estudiante(id_estudiante) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_nc_datos_facturacion  FOREIGN KEY (id_datos_facturacion)
        REFERENCES datos_facturacion(id_datos_facturacion) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Nota crédito de un reembolso aprobado: corrige la factura sin anularla; una línea por curso reembolsado.';

CREATE TABLE consecutivo_nota_credito (
    anio     SMALLINT UNSIGNED NOT NULL,
    ultimo   INT UNSIGNED      NOT NULL DEFAULT 0,
    CONSTRAINT pk_consecutivo_nota_credito PRIMARY KEY (anio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Último número de nota crédito emitido por año; se bloquea la fila al emitir (SELECT … FOR UPDATE).';

-- Verificación
SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'biolearn_db' AND TABLE_NAME = 'reembolso' ORDER BY ORDINAL_POSITION;
SELECT COUNT(*) AS tablas FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'biolearn_db';
