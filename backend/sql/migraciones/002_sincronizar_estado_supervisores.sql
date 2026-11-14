-- ════════════════════════════════════════════════════════════════════════════
-- BioLearn · Migración 002 — sincronizar supervisor_institucion.estado con usuario.estado
-- Antes de la regla "un representante activo por institución", desactivar o suspender a un
-- supervisor no actualizaba su vínculo con la institución: el vínculo quedaba 'activo' y
-- seguía bloqueando la asignación de un nuevo representante. Este script corrige esos casos.
-- Es seguro ejecutarlo varias veces.
-- ════════════════════════════════════════════════════════════════════════════
USE biolearn_db;

-- 1. Ver qué registros están desincronizados (antes de corregir)
SELECT u.id_usuario, u.nombre, u.apellido, u.estado AS estado_usuario,
       s.estado AS estado_vinculo, i.nombre AS institucion
FROM supervisor_institucion s
JOIN usuario u     ON u.id_usuario = s.id_usuario
JOIN institucion i ON i.id_institucion = s.id_institucion
WHERE (u.estado = 'inactivo'   AND s.estado <> 'inactivo')
   OR (u.estado = 'suspendido' AND s.estado <> 'suspendido');

-- 2. Corregir. (El modo "safe updates" de Workbench bloquea UPDATE con JOIN; se desactiva solo para esta sesión.)
SET SQL_SAFE_UPDATES = 0;

UPDATE supervisor_institucion s
JOIN usuario u ON u.id_usuario = s.id_usuario
SET s.estado = u.estado
WHERE u.estado IN ('inactivo', 'suspendido')
  AND s.estado <> u.estado;

SET SQL_SAFE_UPDATES = 1;

-- 3. Resultado: representantes activos por institución
SELECT i.id_institucion, i.nombre, i.codigo_afiliacion,
       COALESCE(SUM(s.estado = 'activo'), 0) AS representantes_activos
FROM institucion i
LEFT JOIN supervisor_institucion s ON s.id_institucion = i.id_institucion
GROUP BY i.id_institucion, i.nombre, i.codigo_afiliacion
ORDER BY i.id_institucion;
