-- =====================================================================
-- BioLearn · Migración 009 · Ajustes después de las pruebas de la Fase 2 de Pagos
--   - reembolso.aprobacion_automatica: la solicitud se aprobó sola (todos los cursos con 0 % de avance
--     y dentro del plazo). Si el pago fue con tarjeta o Nequi, la pasarela devuelve el dinero de inmediato;
--     con PSE o transferencia queda «por devolver» para que el contador registre la devolución.
--   - curso.solo_institucional: el curso solo se ofrece a colegios. No aparece en el catálogo abierto;
--     lo ven los estudiantes de un colegio que lo contrató y se inscriben con el código del colegio.
-- Ejecutar una sola vez en MySQL Workbench (biolearn_db) y luego recrear biolearn_test
-- (sql/crear_biolearn_test.md). Siguen siendo 56 tablas.
-- =====================================================================
USE biolearn_db;

ALTER TABLE reembolso
  ADD COLUMN aprobacion_automatica TINYINT(1) NOT NULL DEFAULT 0 AFTER justificacion;

ALTER TABLE curso
  ADD COLUMN solo_institucional TINYINT(1) NOT NULL DEFAULT 0 AFTER precio;

-- Verificación
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'biolearn_db'
   AND ((TABLE_NAME = 'reembolso' AND COLUMN_NAME = 'aprobacion_automatica')
     OR (TABLE_NAME = 'curso' AND COLUMN_NAME = 'solo_institucional'));

-- Cursos publicados que hoy no tienen lecciones activas (la nueva regla impide que vuelva a pasar;
-- revisa estos casos: agrega una lección activa o pide al Administrador cerrar el curso).
SELECT c.id_curso, c.nombre, c.estado
  FROM curso c
 WHERE c.estado = 'activo'
   AND NOT EXISTS (SELECT 1 FROM leccion l WHERE l.id_curso = c.id_curso AND l.estado = 'activa');
