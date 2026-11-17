USE biolearn_db;

SET SQL_SAFE_UPDATES = 0;
UPDATE datos_facturacion SET ciudad = 'Medellín' WHERE ciudad IS NULL;
SET SQL_SAFE_UPDATES = 1;

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
