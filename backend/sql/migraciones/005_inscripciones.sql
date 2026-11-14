-- =====================================================================
-- BioLearn · Migración 005 · Inscripciones y progreso del estudiante (Fase 3 de Cursos)
--   1. Código de inscripción de cada curso contratado por una institución: el colegio lo entrega
--      a sus estudiantes para vincular su inscripción (cada estudiante paga su curso).
--   2. Estado «pendiente_pago» de la inscripción (reserva de un curso con precio hasta que el
--      módulo de Pagos la active) y vínculo opcional con la contratación usada.
--   3. Progreso por material: qué materiales vio cada estudiante, para calcular el porcentaje real.
-- Ejecutar una sola vez en MySQL Workbench (biolearn_db).
-- =====================================================================
USE biolearn_db;

-- 1. Código de inscripción (los existentes reciben uno aleatorio de 8 caracteres)
ALTER TABLE institucion_curso_contratado
  ADD COLUMN codigo_inscripcion CHAR(8) NULL AFTER id_curso;
UPDATE institucion_curso_contratado
   SET codigo_inscripcion = UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 8))
 WHERE codigo_inscripcion IS NULL;
ALTER TABLE institucion_curso_contratado
  MODIFY codigo_inscripcion CHAR(8) NOT NULL,
  ADD UNIQUE KEY uq_contratacion_codigo (codigo_inscripcion);

-- 2. Inscripción: estado pendiente de pago y vínculo con la contratación
ALTER TABLE inscripcion
  MODIFY estado ENUM('pendiente_pago','activa','completada','retirada','suspendida') NOT NULL DEFAULT 'activa',
  ADD COLUMN id_institucion_curso INT UNSIGNED NULL AFTER id_estudiante,
  ADD CONSTRAINT fk_insc_contratacion FOREIGN KEY (id_institucion_curso)
      REFERENCES institucion_curso_contratado(id_institucion_curso) ON DELETE SET NULL ON UPDATE CASCADE;

-- 3. Progreso por material
CREATE TABLE progreso_material (
    id_progreso_material INT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_inscripcion       INT UNSIGNED NOT NULL,
    id_material          INT UNSIGNED NOT NULL,
    visto_en             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_progreso_material PRIMARY KEY (id_progreso_material),
    CONSTRAINT uq_progreso_material UNIQUE (id_inscripcion, id_material),
    CONSTRAINT fk_pm_inscripcion FOREIGN KEY (id_inscripcion)
        REFERENCES inscripcion(id_inscripcion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pm_material FOREIGN KEY (id_material)
        REFERENCES material_contenido(id_material) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
