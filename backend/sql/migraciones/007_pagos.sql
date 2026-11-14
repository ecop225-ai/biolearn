-- =====================================================================
-- BioLearn · Migración 007 · Módulo de Pagos, Fase 1 (carrito, pago y factura)
--   - pago: estado «cancelado» (el estudiante cancela un pago por transferencia pendiente),
--     motivo del rechazo y comprobante de la transferencia.
--   - consecutivo_factura: numeración BIO-FAC-AAAA-000001 sin huecos ni repetidos por año.
--   - datos_facturacion: registro inicial de BioLearn (el Administrador lo edita en la Fase 3).
-- Ejecutar una sola vez en MySQL Workbench (biolearn_db) y luego recrear biolearn_test.
-- =====================================================================
USE biolearn_db;

ALTER TABLE pago
  MODIFY estado ENUM('pendiente','aprobado','rechazado','cancelado','reembolsado') NOT NULL DEFAULT 'pendiente',
  ADD COLUMN motivo_rechazo VARCHAR(500) NULL AFTER estado,
  ADD COLUMN comprobante_url VARCHAR(500) NULL AFTER motivo_rechazo,
  ADD INDEX idx_pago_estado (estado),
  ADD INDEX idx_pago_creado (creado_en);

CREATE TABLE consecutivo_factura (
    anio     SMALLINT UNSIGNED NOT NULL,
    ultimo   INT UNSIGNED      NOT NULL DEFAULT 0,
    CONSTRAINT pk_consecutivo_factura PRIMARY KEY (anio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Último número de factura emitido por año; se bloquea la fila al emitir (SELECT … FOR UPDATE).';

-- Datos de facturación iniciales (solo si la tabla está vacía)
INSERT INTO datos_facturacion (razon_social, nit, direccion)
SELECT 'BioLearn S.A.S.', '901000000-0', 'Medellín, Antioquia, Colombia'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM datos_facturacion);

-- Verificación
SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'biolearn_db' AND TABLE_NAME = 'pago' AND COLUMN_NAME IN ('estado','motivo_rechazo','comprobante_url');
SELECT * FROM datos_facturacion;
