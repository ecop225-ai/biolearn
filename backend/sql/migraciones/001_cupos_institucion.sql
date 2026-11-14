-- ════════════════════════════════════════════════════════════════════════════
-- BioLearn · Migración 001 — cupos contratados por institución
-- Aplicar UNA sola vez sobre una base creada con una versión anterior de schema.sql.
-- (Si creas la base desde cero con el schema.sql actual, NO necesitas esta migración.)
-- ════════════════════════════════════════════════════════════════════════════
USE biolearn_db;

-- NULL = sin límite definido: así las instituciones que ya existen siguen aceptando
-- estudiantes. El módulo de Instituciones exigirá el valor al crear o editar.
-- Los cupos USADOS no se guardan: se calculan contando perfil_estudiante activos afiliados.
ALTER TABLE institucion
    ADD COLUMN cupos_contratados SMALLINT UNSIGNED NULL AFTER codigo_afiliacion;

-- Verificación
SELECT id_institucion, nombre, codigo_afiliacion, cupos_contratados FROM institucion;
