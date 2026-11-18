-- =====================================================================
-- BioLearn · Migración 011 · Acceso a los cursos por tiempo
--   - curso.meses_para_terminar (12 por defecto, de 1 a 36) y curso.meses_repaso (6 por defecto, de 0 a 24):
--     los define el Administrador en cada curso. Un cambio aplica a los plazos que empiecen después.
--   - inscripcion: estados «vencida» (pasó el plazo sin terminar: sin acceso, conserva el avance y se renueva
--     pagando) y «cerrada» (terminó el repaso: sin acceso al contenido, conserva notas y certificado).
--   - inscripcion.acceso_desde / acceso_hasta: plazo para terminar, desde que el curso se activa (pago aprobado
--     o inscripción gratuita). repaso_hasta: fin del repaso, desde que se completa el curso.
--   - inscripcion.aviso_vencimiento_en / aviso_repaso_en: cuándo se envió el aviso de 15 días (una sola vez).
-- Ejecutar una sola vez en MySQL Workbench (biolearn_db) y luego recrear biolearn_test
-- (sql/crear_biolearn_test.md). Siguen siendo 58 tablas.
-- =====================================================================
USE biolearn_db;

ALTER TABLE curso
  ADD COLUMN meses_para_terminar TINYINT UNSIGNED NOT NULL DEFAULT 12 AFTER horas_estimadas,
  ADD COLUMN meses_repaso        TINYINT UNSIGNED NOT NULL DEFAULT 6  AFTER meses_para_terminar;

ALTER TABLE inscripcion
  MODIFY estado ENUM('pendiente_pago','activa','completada','retirada','suspendida','en_reembolso','vencida','cerrada') NOT NULL DEFAULT 'activa',
  ADD COLUMN acceso_desde         DATETIME NULL AFTER fecha_completacion,
  ADD COLUMN acceso_hasta         DATETIME NULL AFTER acceso_desde,
  ADD COLUMN repaso_hasta         DATETIME NULL AFTER acceso_hasta,
  ADD COLUMN aviso_vencimiento_en DATETIME NULL AFTER repaso_hasta,
  ADD COLUMN aviso_repaso_en      DATETIME NULL AFTER aviso_vencimiento_en,
  ADD INDEX idx_inscripcion_acceso_hasta (estado, acceso_hasta),
  ADD INDEX idx_inscripcion_repaso_hasta (estado, repaso_hasta);

-- Las inscripciones que ya tuvieron acceso empiezan su plazo en el último pago aprobado del curso
-- (o en la fecha de inscripción si el curso es gratuito). MySQL Workbench bloquea los UPDATE sin llave
-- primaria en el WHERE (error 1175): se desactiva el modo seguro solo para estas instrucciones.
SET SQL_SAFE_UPDATES = 0;

UPDATE inscripcion i
  JOIN curso c ON c.id_curso = i.id_curso
   SET i.acceso_desde = COALESCE(
         (SELECT MAX(p.fecha_pago)
            FROM pago_inscripcion pi JOIN pago p ON p.id_pago = pi.id_pago
           WHERE pi.id_inscripcion = i.id_inscripcion AND p.estado IN ('aprobado', 'reembolsado')),
         i.fecha_inscripcion)
 WHERE i.estado IN ('activa', 'completada', 'suspendida', 'en_reembolso', 'retirada');

UPDATE inscripcion i
  JOIN curso c ON c.id_curso = i.id_curso
   SET i.acceso_hasta = DATE_ADD(i.acceso_desde, INTERVAL c.meses_para_terminar MONTH)
 WHERE i.acceso_desde IS NOT NULL;

UPDATE inscripcion i
  JOIN curso c ON c.id_curso = i.id_curso
   SET i.repaso_hasta = DATE_ADD(i.fecha_completacion, INTERVAL c.meses_repaso MONTH)
 WHERE i.estado = 'completada' AND i.fecha_completacion IS NOT NULL;

SET SQL_SAFE_UPDATES = 1;

-- Verificación
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'biolearn_db' AND TABLE_NAME IN ('curso', 'inscripcion')
   AND COLUMN_NAME IN ('meses_para_terminar', 'meses_repaso', 'estado', 'acceso_desde', 'acceso_hasta', 'repaso_hasta',
                       'aviso_vencimiento_en', 'aviso_repaso_en');
SELECT estado, COUNT(*) AS inscripciones, MIN(acceso_hasta) AS primer_vencimiento, MIN(repaso_hasta) AS primer_fin_repaso
  FROM inscripcion GROUP BY estado;
