-- =====================================================================
-- BioLearn · Limpieza de los datos que crean las suites de Selenium
-- Ubicación sugerida: backend/sql/limpieza_datos_selenium.sql
--
-- Las suites crean datos con nombres reconocibles para poder repetirse:
--   Usuarios      → correo nombre.<13 dígitos>@biolearn.co   (AdminUsuariosSeleniumTest)
--                   rep.selenium.<13 dígitos>@biolearn.co      (CP-U18)
--   Instituciones → "Colegio Selenium …"                       (InstitucionesCatalogos y CP-U18)
--   Grados        → "Grado Selenium …"                         (InstitucionesCatalogosSeleniumTest)
--   Áreas         → "Área Selenium …"                          (InstitucionesCatalogosSeleniumTest)
--   Cursos        → "Curso Selenium …"                         (CursosSeleniumTest, EstudianteSeleniumTest, AccesoSeleniumTest y PagoSeleniumTest,
--                   con sus inscripciones, pagos, facturas, reembolsos y notas crédito)
--
-- Uso en MySQL Workbench:
--   1. Ejecuta la PARTE 1 (vista previa) y revisa qué se va a borrar.
--   2. Ejecuta la PARTE 2 completa (selecciónala toda y pulsa el rayo).
--   3. Vuelve a ejecutar la PARTE 1: todas las consultas deben salir vacías.
-- =====================================================================

USE biolearn_db;

-- ---------------------------------------------------------------------
-- PARTE 1 · VISTA PREVIA (no borra nada)
-- ---------------------------------------------------------------------
SELECT id_usuario, nombre, apellido, correo, tipo_usuario, estado
FROM usuario
WHERE correo REGEXP '[.][0-9]{13}@biolearn[.]co$';

SELECT id_institucion, nombre, nit, estado
FROM institucion
WHERE nombre LIKE 'Colegio Selenium %';

SELECT id_curso, nombre, estado
FROM curso
WHERE nombre LIKE 'Curso Selenium %';

-- Grados y áreas de prueba; la columna "en_uso" indica si algún curso o estudiante los usa
-- (esos NO se borran, para no afectar datos reales).
SELECT g.id_grado, g.nombre, g.estado,
       (SELECT COUNT(*) FROM curso c WHERE c.id_grado = g.id_grado)
     + (SELECT COUNT(*) FROM perfil_estudiante pe WHERE pe.id_grado = g.id_grado)
     + (SELECT COUNT(*) FROM historial_grado hg WHERE hg.id_grado = g.id_grado) AS en_uso
FROM grado g
WHERE g.nombre LIKE 'Grado Selenium %';

SELECT a.id_area, a.nombre, a.estado,
       (SELECT COUNT(*) FROM curso c WHERE c.id_area = a.id_area)
     + (SELECT COUNT(*) FROM perfil_estudiante pe WHERE pe.id_area = a.id_area) AS en_uso
FROM area_academica a
WHERE a.nombre LIKE 'Área Selenium %';


-- ---------------------------------------------------------------------
-- PARTE 2 · BORRADO (ejecutar el bloque completo)
-- ---------------------------------------------------------------------
SET SQL_SAFE_UPDATES = 0;
START TRANSACTION;

-- 1. Cursos de prueba: primero lo que depende de ellos
-- Pagos, Fase 1: facturas, notificaciones, líneas y pagos que incluyen cursos de prueba
CREATE TEMPORARY TABLE tmp_pagos_prueba AS
  SELECT DISTINCT pi.id_pago FROM pago_inscripcion pi
  JOIN inscripcion i ON i.id_inscripcion = pi.id_inscripcion
  JOIN curso c ON c.id_curso = i.id_curso WHERE c.nombre LIKE 'Curso Selenium %';
-- Pagos, Fase 2: notas crédito, notificaciones y reembolsos de esos pagos (antes que las facturas)
DELETE FROM nota_credito     WHERE id_reembolso IN (SELECT id_reembolso FROM reembolso WHERE id_pago IN (SELECT id_pago FROM tmp_pagos_prueba));
DELETE FROM notificacion     WHERE tipo = 'reembolso' AND entidad_id IN (SELECT id_reembolso FROM reembolso WHERE id_pago IN (SELECT id_pago FROM tmp_pagos_prueba));
DELETE FROM reembolso        WHERE id_pago IN (SELECT id_pago FROM tmp_pagos_prueba);   -- borra en cascada reembolso_inscripcion
DELETE FROM factura          WHERE id_pago IN (SELECT id_pago FROM tmp_pagos_prueba);
DELETE FROM notificacion     WHERE tipo = 'pago' AND entidad_id IN (SELECT id_pago FROM tmp_pagos_prueba);
DELETE FROM pago_inscripcion WHERE id_pago IN (SELECT id_pago FROM tmp_pagos_prueba);
DELETE FROM pago             WHERE id_pago IN (SELECT id_pago FROM tmp_pagos_prueba);
DROP TEMPORARY TABLE tmp_pagos_prueba;
-- Acceso por tiempo: avisos de plazo y repaso de las inscripciones de los cursos de prueba
DELETE n FROM notificacion n JOIN inscripcion i ON n.tipo = 'inscripcion' AND n.entidad_id = i.id_inscripcion
  JOIN curso c ON c.id_curso = i.id_curso WHERE c.nombre LIKE 'Curso Selenium %';
-- Fase 3: inscripciones de los cursos de prueba (su progreso se borra en cascada)
DELETE i FROM inscripcion i JOIN curso c ON c.id_curso = i.id_curso WHERE c.nombre LIKE 'Curso Selenium %';
DELETE hc FROM horario_curso hc JOIN curso c ON c.id_curso = hc.id_curso WHERE c.nombre LIKE 'Curso Selenium %';
DELETE cd FROM curso_docente cd JOIN curso c ON c.id_curso = cd.id_curso WHERE c.nombre LIKE 'Curso Selenium %';
DELETE ic FROM institucion_curso_contratado ic JOIN curso c ON c.id_curso = ic.id_curso WHERE c.nombre LIKE 'Curso Selenium %';
DELETE FROM curso WHERE nombre LIKE 'Curso Selenium %';

-- 2. Usuarios de prueba: primero sus perfiles y asignaciones, luego las cuentas
CREATE TEMPORARY TABLE tmp_usuarios_prueba AS
  SELECT id_usuario FROM usuario WHERE correo REGEXP '[.][0-9]{13}@biolearn[.]co$';

DELETE cd FROM curso_docente cd
  JOIN perfil_docente pd ON pd.id_docente = cd.id_docente
  WHERE pd.id_usuario IN (SELECT id_usuario FROM tmp_usuarios_prueba);
DELETE FROM perfil_docente         WHERE id_usuario IN (SELECT id_usuario FROM tmp_usuarios_prueba);
DELETE FROM perfil_contador        WHERE id_usuario IN (SELECT id_usuario FROM tmp_usuarios_prueba);
DELETE FROM supervisor_institucion WHERE id_usuario IN (SELECT id_usuario FROM tmp_usuarios_prueba);
DELETE FROM usuario                WHERE id_usuario IN (SELECT id_usuario FROM tmp_usuarios_prueba);

-- 3. Instituciones de prueba: primero la asignación de representantes, luego las instituciones
--    (las contrataciones de cursos se borran solas y los estudiantes afiliados quedan sin institución)
CREATE TEMPORARY TABLE tmp_instituciones_prueba AS
  SELECT id_institucion FROM institucion WHERE nombre LIKE 'Colegio Selenium %';

DELETE FROM supervisor_institucion WHERE id_institucion IN (SELECT id_institucion FROM tmp_instituciones_prueba);
DELETE FROM institucion            WHERE id_institucion IN (SELECT id_institucion FROM tmp_instituciones_prueba);

-- 4. Grados y áreas de prueba que NO estén en uso
DELETE FROM grado
WHERE nombre LIKE 'Grado Selenium %'
  AND NOT EXISTS (SELECT 1 FROM curso c WHERE c.id_grado = grado.id_grado)
  AND NOT EXISTS (SELECT 1 FROM perfil_estudiante pe WHERE pe.id_grado = grado.id_grado)
  AND NOT EXISTS (SELECT 1 FROM historial_grado hg WHERE hg.id_grado = grado.id_grado);

DELETE FROM area_academica
WHERE nombre LIKE 'Área Selenium %'
  AND NOT EXISTS (SELECT 1 FROM curso c WHERE c.id_area = area_academica.id_area)
  AND NOT EXISTS (SELECT 1 FROM perfil_estudiante pe WHERE pe.id_area = area_academica.id_area);

-- 5. Pagos, Fase 3: registros de los reportes generados por FinanzasSeleniumTest (las versiones de
--    datos_facturacion que crea CP-FI05 se conservan: la última deja los datos como estaban)
DELETE FROM reporte_financiero WHERE motivo LIKE '%Selenium%';

COMMIT;

DROP TEMPORARY TABLE tmp_usuarios_prueba;
DROP TEMPORARY TABLE tmp_instituciones_prueba;
SET SQL_SAFE_UPDATES = 1;
