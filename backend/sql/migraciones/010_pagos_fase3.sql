-- =====================================================================
-- BioLearn · Migración 010 · Pagos, Fase 3: datos editables y reportes financieros
--   - datos_facturacion: cada cambio crea una versión nueva (las facturas y notas anteriores conservan
--     la suya). Nuevas columnas ciudad, correo, telefono y quién hizo el cambio.
--   - datos_recaudo (nueva): cuenta bancaria y Nequi donde el estudiante transfiere. También versionada.
--   - reporte_financiero (nueva): historial de los reportes generados (tipo, período, formato, motivo y autor).
-- Ejecutar una sola vez en MySQL Workbench (biolearn_db) y luego recrear biolearn_test
-- (sql/crear_biolearn_test.md). La base queda con 58 tablas.
-- =====================================================================
USE biolearn_db;

ALTER TABLE datos_facturacion
  ADD COLUMN ciudad VARCHAR(100) NULL AFTER direccion,
  ADD COLUMN correo VARCHAR(150) NULL AFTER ciudad,
  ADD COLUMN telefono VARCHAR(20) NULL AFTER correo,
  ADD COLUMN id_usuario_actualizo INT UNSIGNED NULL AFTER logo_url,
  ADD CONSTRAINT fk_datos_facturacion_usuario FOREIGN KEY (id_usuario_actualizo) REFERENCES usuario (id_usuario);

UPDATE datos_facturacion SET ciudad = 'Medellín' WHERE ciudad IS NULL;

CREATE TABLE datos_recaudo (
  id_datos_recaudo     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  banco                VARCHAR(80)  NOT NULL,
  tipo_cuenta          ENUM('ahorros','corriente') NOT NULL,
  numero_cuenta        VARCHAR(20)  NOT NULL,
  titular              VARCHAR(150) NOT NULL,
  documento_titular    VARCHAR(20)  NOT NULL,
  telefono_nequi       VARCHAR(10)  NULL,
  instrucciones        VARCHAR(500) NULL,
  id_usuario_actualizo INT UNSIGNED NULL,
  actualizado_en       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_datos_recaudo_usuario FOREIGN KEY (id_usuario_actualizo) REFERENCES usuario (id_usuario),
  INDEX idx_datos_recaudo_vigente (actualizado_en)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Los datos que hasta ahora estaban escritos en el portal del estudiante
INSERT INTO datos_recaudo (banco, tipo_cuenta, numero_cuenta, titular, documento_titular, telefono_nequi, instrucciones)
SELECT 'Bancolombia', 'ahorros', '00012345678', 'BioLearn S.A.S.', '901000000-0', NULL,
       'Escribe en la referencia de la transferencia el número de tu pago.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM datos_recaudo);

CREATE TABLE reporte_financiero (
  id_reporte_financiero INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tipo                  ENUM('ingresos','facturas','reembolsos','general') NOT NULL,
  desde                 DATE         NOT NULL,
  hasta                 DATE         NOT NULL,
  formato               ENUM('excel','pdf') NOT NULL,
  motivo                VARCHAR(300) NOT NULL,
  id_usuario            INT UNSIGNED NOT NULL,
  generado_en           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reporte_financiero_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
  INDEX idx_reporte_financiero_fecha (generado_en)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Verificación
SELECT * FROM datos_facturacion ORDER BY actualizado_en DESC, id_datos_facturacion DESC;
SELECT * FROM datos_recaudo;
SELECT COUNT(*) AS tablas FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'biolearn_db';
