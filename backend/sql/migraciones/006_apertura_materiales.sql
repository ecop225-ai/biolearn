-- =====================================================================
-- BioLearn · Migración 006 · Marcado automático de materiales vistos (Fase 3 de Cursos)
--   El portal marca un material como visto solo cuando el estudiante lo revisó según su tipo
--   (llegar al final de una lectura, ver el video, mantener abierto un laboratorio…), y el
--   servidor exige un tiempo mínimo desde que se abrió. Para eso se registra la apertura:
--     abierto_en: cuándo abrió el material por primera vez.
--     visto_en:   cuándo cumplió el criterio (vacío mientras no lo cumpla).
-- Ejecutar una sola vez en MySQL Workbench (biolearn_db) y luego recrear biolearn_test.
-- =====================================================================
USE biolearn_db;

ALTER TABLE progreso_material
  ADD COLUMN abierto_en DATETIME NULL AFTER id_material,
  MODIFY visto_en DATETIME NULL DEFAULT NULL;

-- Los materiales ya marcados se consideran abiertos en la misma fecha en que se vieron
SET SQL_SAFE_UPDATES = 0;
UPDATE progreso_material SET abierto_en = visto_en WHERE abierto_en IS NULL;
SET SQL_SAFE_UPDATES = 1;

ALTER TABLE progreso_material MODIFY abierto_en DATETIME NOT NULL;
