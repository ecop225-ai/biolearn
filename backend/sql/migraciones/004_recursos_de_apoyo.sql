-- =====================================================================
-- BioLearn · Migración 004 · Registro de recursos de apoyo
-- Desde esta versión, cada recurso o artículo de un material queda vinculado en la tabla
-- recurso_apoyo con su tipo de contenido:
--   primario       → recurso principal del material (el video, el laboratorio, el documento…)
--   complementario → recursos de apoyo adicionales (POST /api/materiales/{id}/recursos)
--   referencia     → artículo científico del material
-- Esta migración registra los recursos y artículos que ya existían. Se puede ejecutar más de una
-- vez: solo inserta los que todavía no están vinculados.
-- =====================================================================
USE biolearn_db;

-- 1. Recursos: el primero de un material de tipo video, laboratorio, juego o documento es el principal
INSERT INTO recurso_apoyo (id_material, id_recurso, id_usuario, tipo_recurso, tipo_contenido, titulo, url, orden, estado)
SELECT r.id_material,
       r.id_recurso,
       pd.id_usuario,
       CASE r.tipo_recurso WHEN 'audio' THEN 'otro' ELSE r.tipo_recurso END,
       CASE WHEN m.tipo IN ('video','laboratorio','juego','documento')
                 AND r.orden = (SELECT MIN(r2.orden) FROM recurso r2 WHERE r2.id_material = r.id_material)
            THEN 'primario' ELSE 'complementario' END,
       LEFT(r.nombre, 250),
       LEFT(r.url, 500),
       LEAST(r.orden, 255),
       r.estado
FROM recurso r
JOIN material_contenido m ON m.id_material = r.id_material
JOIN perfil_docente pd    ON pd.id_docente = r.id_docente
WHERE NOT EXISTS (SELECT 1 FROM recurso_apoyo ra WHERE ra.id_recurso = r.id_recurso);

-- 2. Artículos científicos: quedan como referencia
INSERT INTO recurso_apoyo (id_material, id_articulo, id_usuario, tipo_recurso, tipo_contenido, titulo, url, orden, estado)
SELECT a.id_material,
       a.id_articulo,
       pd.id_usuario,
       'enlace',
       'referencia',
       LEFT(a.titulo, 250),
       LEFT(a.url, 500),
       1,
       a.estado
FROM articulo_cientifico a
JOIN perfil_docente pd ON pd.id_docente = a.id_docente
WHERE NOT EXISTS (SELECT 1 FROM recurso_apoyo ra WHERE ra.id_articulo = a.id_articulo);

-- Comprobación
SELECT tipo_contenido, COUNT(*) AS cantidad FROM recurso_apoyo GROUP BY tipo_contenido;
