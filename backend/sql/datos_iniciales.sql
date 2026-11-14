-- ════════════════════════════════════════════════════════════════════════════
-- BioLearn · Datos iniciales (catálogos, administrador inicial e instituciones de prueba)
-- Ejecutar DESPUÉS de schema.sql. Es seguro ejecutarlo varias veces: usa INSERT IGNORE,
-- así que no duplica ni sobrescribe registros que ya existan (por nombre, correo o NIT).
-- ════════════════════════════════════════════════════════════════════════════
USE biolearn_db;

-- ── Grados (los id 1..6 coinciden con GRADOS_DEMO del prototipo de registro) ──
INSERT IGNORE INTO grado (id_grado, nombre, nivel, orden) VALUES
    (1, 'Sexto',    'secundaria', 6),
    (2, 'Séptimo',  'secundaria', 7),
    (3, 'Octavo',   'secundaria', 8),
    (4, 'Noveno',   'secundaria', 9),
    (5, 'Décimo',   'media',      10),
    (6, 'Undécimo', 'media',      11);

-- ── Áreas académicas (los id 1..3 coinciden con AREAS_DEMO del prototipo de registro) ──
INSERT IGNORE INTO area_academica (id_area, nombre, descripcion) VALUES
    (1, 'Biología molecular',      'Genética, bioquímica y biología celular'),
    (2, 'Ecología y conservación', 'Ecosistemas, biodiversidad y conservación de especies'),
    (3, 'Bioinformática',          'Análisis computacional de datos biológicos');

-- ── Administrador inicial ──
-- La cuenta de Administrador NO se crea desde la interfaz (sería un riesgo de seguridad):
-- se aprovisiona aquí. Credenciales: admin@biolearn.co / Admin2026  →  CÁMBIALA después de ingresar.
INSERT IGNORE INTO usuario (nombre, apellido, nombre_usuario, correo, contrasena, tipo_usuario, estado)
VALUES ('Administrador', 'BioLearn', 'admin', 'admin@biolearn.co',
        '$2a$10$QSMJ.VGjCLa8pTt1h0/YW.4U0MqFL1Anb6CUXril9ux7mWQfiG96.', 'admin', 'activo');

INSERT IGNORE INTO perfil_admin (id_usuario, rol_admin)
SELECT id_usuario, 'superadmin' FROM usuario WHERE correo = 'admin@biolearn.co';

-- ── Instituciones de prueba (código de afiliación de 8 caracteres, coherente con los prototipos) ──
INSERT IGNORE INTO institucion (nombre, nit, rector, telefono, correo, codigo_afiliacion, cupos_contratados, estado) VALUES
    ('Colegio San Rafael',             '900.223.114-2', 'Marta Loaiza',  '601 745 8820', 'rectoria@sanrafael.edu.co',    'SR7K2MQX', 120, 'activa'),
    ('Corporación Andina de Estudios', '800.556.902-1', 'Felipe Duarte', '601 622 4471', 'direccion@andina.edu.co',      'AN4P8TWZ',  60, 'activa'),
    ('Instituto Nueva Semilla',        '901.774.220-5', 'Camila Ortiz',  '602 310 9954', 'rectoria@nuevasemilla.edu.co', 'NS9H3DLE',  40, 'activa');

-- Verificación
SELECT 'grados' AS tabla, COUNT(*) AS filas FROM grado
UNION ALL SELECT 'areas', COUNT(*) FROM area_academica
UNION ALL SELECT 'admins', COUNT(*) FROM perfil_admin
UNION ALL SELECT 'instituciones', COUNT(*) FROM institucion;
