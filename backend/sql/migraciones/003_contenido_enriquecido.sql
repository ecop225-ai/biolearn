-- =====================================================================
-- BioLearn · Migración 003 · Contenido enriquecido de cursos y materiales
-- Fase 2 del módulo de Cursos: conserva en la base los campos que el prototipo del
-- docente ya ofrecía y que no tenían columna.
--   curso:              código del curso, color representativo e información para el
--                       estudiante antes de inscribirse (HTML del editor enriquecido)
--   material_contenido: contenido de la lectura (HTML), color de fondo de la lectura y
--                       guía de observación de videos, laboratorios y juegos (HTML)
-- Los datos propios de cada tipo de material usan tablas que ya existían:
--   enlace, proveedor y duración → recurso;  autores, revista y DOI → articulo_cientifico.
-- Ejecutar una sola vez en MySQL Workbench (biolearn_db).
-- =====================================================================
USE biolearn_db;

ALTER TABLE curso
  ADD COLUMN codigo          VARCHAR(20)  NULL AFTER nombre,
  ADD COLUMN color           CHAR(7)      NULL AFTER imagen_portada,
  ADD COLUMN info_estudiante MEDIUMTEXT   NULL AFTER objetivos,
  ADD UNIQUE KEY uq_curso_codigo (codigo);

ALTER TABLE material_contenido
  ADD COLUMN contenido   MEDIUMTEXT NULL AFTER descripcion,
  ADD COLUMN color_fondo CHAR(7)    NULL AFTER contenido,
  ADD COLUMN guia        MEDIUMTEXT NULL AFTER color_fondo;
