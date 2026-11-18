-- =====================================================================
-- BioLearn · Base de datos para el servidor (esquema consolidado + datos iniciales)
-- Ubicación: despliegue/mysql/01_biolearn.sql
--
-- Equivale a ejecutar, sobre una base vacía y en este orden:
--   backend/schema.sql
--   backend/sql/datos_iniciales.sql
--   backend/sql/migraciones/002 … 011 (la 001 ya está incluida en schema.sql y
--   010_pagos_fase3_continuacion.sql solo era para bases donde la 010 se detuvo a medias)
-- Resultado: 58 tablas, con grados, áreas, administrador inicial, instituciones de prueba,
-- datos de facturación y datos de recaudo de ejemplo.
--
-- Uso: el contenedor de MySQL lo ejecuta SOLO la primera vez que arranca con el volumen vacío
-- (carpeta /docker-entrypoint-initdb.d). No borra tablas: si la base ya existe, falla sin tocar nada.
-- Las migraciones futuras (012 en adelante) se aplican a mano; ver despliegue/README.md.
--
-- Administrador inicial: admin@biolearn.co / Admin2026  →  cámbiala en el servidor (README, paso 3.6).
-- =====================================================================


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `actividad` (
  `id_actividad` int unsigned NOT NULL AUTO_INCREMENT,
  `id_leccion` int unsigned NOT NULL,
  `id_docente` int unsigned NOT NULL,
  `nombre` varchar(250) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci,
  `instrucciones` text COLLATE utf8mb4_unicode_ci,
  `tipo` enum('tarea','proyecto','investigacion','ejercicio','otro') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'tarea',
  `fecha_disponible` datetime DEFAULT NULL,
  `fecha_limite` datetime DEFAULT NULL,
  `permite_entrega_tardia` tinyint(1) NOT NULL DEFAULT '0',
  `puntaje_maximo` decimal(10,2) NOT NULL DEFAULT '10.00',
  `estado` enum('activa','inactiva','cerrada') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activa',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_actividad`),
  KEY `fk_act_leccion` (`id_leccion`),
  KEY `fk_act_docente` (`id_docente`),
  CONSTRAINT `fk_act_docente` FOREIGN KEY (`id_docente`) REFERENCES `perfil_docente` (`id_docente`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_act_leccion` FOREIGN KEY (`id_leccion`) REFERENCES `leccion` (`id_leccion`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `actividad` DISABLE KEYS */;
/*!40000 ALTER TABLE `actividad` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `adjunto_comentario` (
  `id_adjunto` int unsigned NOT NULL AUTO_INCREMENT,
  `id_comentario` int unsigned NOT NULL,
  `id_usuario` int unsigned NOT NULL,
  `url` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo` enum('imagen','documento','video','enlace','otro') COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre_archivo` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tamanio_kb` int unsigned DEFAULT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_adjunto`),
  KEY `fk_adj_comentario` (`id_comentario`),
  KEY `fk_adj_usuario` (`id_usuario`),
  CONSTRAINT `fk_adj_comentario` FOREIGN KEY (`id_comentario`) REFERENCES `comentario_foro` (`id_comentario`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_adj_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `adjunto_comentario` DISABLE KEYS */;
/*!40000 ALTER TABLE `adjunto_comentario` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `area_academica` (
  `id_area` tinyint unsigned NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci,
  `estado` enum('activo','inactivo') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `fecha_creacion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_area`),
  UNIQUE KEY `uq_area_nombre` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `area_academica` DISABLE KEYS */;
INSERT INTO `area_academica` (`id_area`, `nombre`, `descripcion`, `estado`, `fecha_creacion`, `fecha_actualizacion`) VALUES (1,'Biología molecular','Genética, bioquímica y biología celular','activo','2026-11-18 04:26:53','2026-11-18 04:26:53');
INSERT INTO `area_academica` (`id_area`, `nombre`, `descripcion`, `estado`, `fecha_creacion`, `fecha_actualizacion`) VALUES (2,'Ecología y conservación','Ecosistemas, biodiversidad y conservación de especies','activo','2026-11-18 04:26:53','2026-11-18 04:26:53');
INSERT INTO `area_academica` (`id_area`, `nombre`, `descripcion`, `estado`, `fecha_creacion`, `fecha_actualizacion`) VALUES (3,'Bioinformática','Análisis computacional de datos biológicos','activo','2026-11-18 04:26:53','2026-11-18 04:26:53');
/*!40000 ALTER TABLE `area_academica` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `articulo_cientifico` (
  `id_articulo` int unsigned NOT NULL AUTO_INCREMENT,
  `id_material` int unsigned NOT NULL,
  `id_docente` int unsigned NOT NULL,
  `titulo` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL,
  `autores` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `resumen` text COLLATE utf8mb4_unicode_ci,
  `revista` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `doi` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_publicacion` date DEFAULT NULL,
  `estado` enum('activo','inactivo') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_articulo`),
  KEY `fk_art_material` (`id_material`),
  KEY `fk_art_docente` (`id_docente`),
  CONSTRAINT `fk_art_docente` FOREIGN KEY (`id_docente`) REFERENCES `perfil_docente` (`id_docente`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_art_material` FOREIGN KEY (`id_material`) REFERENCES `material_contenido` (`id_material`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `articulo_cientifico` DISABLE KEYS */;
/*!40000 ALTER TABLE `articulo_cientifico` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asistencia` (
  `id_asistencia` int unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` int unsigned NOT NULL,
  `id_sesion` int unsigned DEFAULT NULL,
  `id_evento` int unsigned DEFAULT NULL,
  `asistio` tinyint(1) NOT NULL DEFAULT '0',
  `codigo_ingresado` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_ingreso` datetime DEFAULT NULL,
  `fecha_salida` datetime DEFAULT NULL,
  `duracion_min` smallint unsigned DEFAULT NULL,
  `calificacion` decimal(5,2) DEFAULT NULL,
  `comentarios` text COLLATE utf8mb4_unicode_ci,
  `nivel_participacion` enum('alto','medio','bajo','ninguno') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `registrado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_asistencia`),
  KEY `fk_asist_usuario` (`id_usuario`),
  KEY `idx_asistencia_sesion` (`id_sesion`,`id_usuario`),
  KEY `idx_asistencia_evento` (`id_evento`,`id_usuario`),
  CONSTRAINT `fk_asist_evento` FOREIGN KEY (`id_evento`) REFERENCES `evento_virtual` (`id_evento`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_asist_sesion` FOREIGN KEY (`id_sesion`) REFERENCES `sesion_online` (`id_sesion`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_asist_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Fusiona seguimiento_participacion. CHECK garantiza que apunte a sesion o evento.';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `asistencia` DISABLE KEYS */;
/*!40000 ALTER TABLE `asistencia` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `calificacion_foro` (
  `id_calificacion` int unsigned NOT NULL AUTO_INCREMENT,
  `id_foro` int unsigned NOT NULL,
  `id_inscripcion` int unsigned NOT NULL,
  `id_docente` int unsigned NOT NULL,
  `cantidad_aportes` smallint unsigned NOT NULL DEFAULT '0',
  `calidad_aportes` enum('excelente','bueno','regular','insuficiente') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'regular',
  `calificacion` decimal(5,2) NOT NULL DEFAULT '0.00',
  `retroalimentacion` text COLLATE utf8mb4_unicode_ci,
  `estado` enum('borrador','publicada') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'borrador',
  `calificado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_calificacion`),
  UNIQUE KEY `uq_calif_foro` (`id_foro`,`id_inscripcion`),
  KEY `fk_cf_inscripcion` (`id_inscripcion`),
  KEY `fk_cf_docente` (`id_docente`),
  CONSTRAINT `fk_cf_docente` FOREIGN KEY (`id_docente`) REFERENCES `perfil_docente` (`id_docente`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_cf_foro` FOREIGN KEY (`id_foro`) REFERENCES `foro` (`id_foro`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_cf_inscripcion` FOREIGN KEY (`id_inscripcion`) REFERENCES `inscripcion` (`id_inscripcion`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `calificacion_foro` DISABLE KEYS */;
/*!40000 ALTER TABLE `calificacion_foro` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `certificado` (
  `id_certificado` int unsigned NOT NULL AUTO_INCREMENT,
  `id_emisor` int unsigned NOT NULL,
  `id_receptor` int unsigned NOT NULL,
  `id_evento` int unsigned DEFAULT NULL,
  `id_inscripcion` int unsigned DEFAULT NULL,
  `id_plantilla` int unsigned DEFAULT NULL,
  `nombre` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `codigo_validacion` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `estado` enum('emitido','anulado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'emitido',
  `emitido_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_certificado`),
  UNIQUE KEY `uq_cert_codigo` (`codigo_validacion`),
  KEY `fk_cert_emisor` (`id_emisor`),
  KEY `fk_cert_receptor` (`id_receptor`),
  KEY `fk_cert_evento` (`id_evento`),
  KEY `fk_cert_inscripcion` (`id_inscripcion`),
  KEY `fk_cert_plantilla` (`id_plantilla`),
  CONSTRAINT `fk_cert_emisor` FOREIGN KEY (`id_emisor`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_cert_evento` FOREIGN KEY (`id_evento`) REFERENCES `evento_virtual` (`id_evento`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_cert_inscripcion` FOREIGN KEY (`id_inscripcion`) REFERENCES `inscripcion` (`id_inscripcion`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_cert_plantilla` FOREIGN KEY (`id_plantilla`) REFERENCES `plantilla_certificado` (`id_plantilla`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_cert_receptor` FOREIGN KEY (`id_receptor`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `certificado` DISABLE KEYS */;
/*!40000 ALTER TABLE `certificado` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comentario_foro` (
  `id_comentario` int unsigned NOT NULL AUTO_INCREMENT,
  `id_foro` int unsigned NOT NULL,
  `id_usuario` int unsigned NOT NULL,
  `id_padre` int unsigned DEFAULT NULL,
  `asunto` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contenido` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo` enum('aporte','respuesta','retroalimentacion') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'aporte',
  `estado` enum('activo','moderado','eliminado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_comentario`),
  KEY `fk_com_usuario` (`id_usuario`),
  KEY `fk_com_padre` (`id_padre`),
  KEY `idx_comentario_foro` (`id_foro`,`id_padre`),
  CONSTRAINT `fk_com_foro` FOREIGN KEY (`id_foro`) REFERENCES `foro` (`id_foro`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_com_padre` FOREIGN KEY (`id_padre`) REFERENCES `comentario_foro` (`id_comentario`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_com_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sin tabla hilo intermedia. Jerarquia via id_padre autoreferencial.';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `comentario_foro` DISABLE KEYS */;
/*!40000 ALTER TABLE `comentario_foro` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `consecutivo_factura` (
  `anio` smallint unsigned NOT NULL,
  `ultimo` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`anio`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Último número de factura emitido por año; se bloquea la fila al emitir (SELECT … FOR UPDATE).';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `consecutivo_factura` DISABLE KEYS */;
/*!40000 ALTER TABLE `consecutivo_factura` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `consecutivo_nota_credito` (
  `anio` smallint unsigned NOT NULL,
  `ultimo` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`anio`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Último número de nota crédito emitido por año; se bloquea la fila al emitir (SELECT … FOR UPDATE).';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `consecutivo_nota_credito` DISABLE KEYS */;
/*!40000 ALTER TABLE `consecutivo_nota_credito` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `curso` (
  `id_curso` int unsigned NOT NULL AUTO_INCREMENT,
  `id_grado` tinyint unsigned NOT NULL,
  `id_area` tinyint unsigned NOT NULL,
  `id_admin_creador` int unsigned DEFAULT NULL,
  `id_docente_titular` int unsigned DEFAULT NULL,
  `nombre` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `codigo` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `imagen_portada` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `color` char(7) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `objetivos` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `info_estudiante` mediumtext COLLATE utf8mb4_unicode_ci,
  `precio` decimal(10,2) NOT NULL DEFAULT '0.00',
  `solo_institucional` tinyint(1) NOT NULL DEFAULT '0',
  `creditos` tinyint unsigned NOT NULL DEFAULT '0',
  `horas_estimadas` smallint unsigned DEFAULT NULL,
  `meses_para_terminar` tinyint unsigned NOT NULL DEFAULT '12',
  `meses_repaso` tinyint unsigned NOT NULL DEFAULT '6',
  `estado` enum('borrador','activo','cerrado','archivado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'borrador',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_curso`),
  UNIQUE KEY `uq_curso_codigo` (`codigo`),
  KEY `fk_curso_grado` (`id_grado`),
  KEY `fk_curso_area` (`id_area`),
  KEY `fk_curso_admin` (`id_admin_creador`),
  KEY `fk_curso_docente` (`id_docente_titular`),
  CONSTRAINT `fk_curso_admin` FOREIGN KEY (`id_admin_creador`) REFERENCES `perfil_admin` (`id_admin`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_curso_area` FOREIGN KEY (`id_area`) REFERENCES `area_academica` (`id_area`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_curso_docente` FOREIGN KEY (`id_docente_titular`) REFERENCES `perfil_docente` (`id_docente`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_curso_grado` FOREIGN KEY (`id_grado`) REFERENCES `grado` (`id_grado`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `curso` DISABLE KEYS */;
/*!40000 ALTER TABLE `curso` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `curso_docente` (
  `id_curso_docente` int unsigned NOT NULL AUTO_INCREMENT,
  `id_curso` int unsigned NOT NULL,
  `id_docente` int unsigned NOT NULL,
  `rol` enum('titular','colaborador','invitado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'colaborador',
  `estado` enum('activo','inactivo') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `asignado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_curso_docente`),
  UNIQUE KEY `uq_curso_docente` (`id_curso`,`id_docente`),
  KEY `fk_cd_docente` (`id_docente`),
  CONSTRAINT `fk_cd_curso` FOREIGN KEY (`id_curso`) REFERENCES `curso` (`id_curso`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_cd_docente` FOREIGN KEY (`id_docente`) REFERENCES `perfil_docente` (`id_docente`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Docentes adicionales al titular. El titular ya está en curso.id_docente_titular.';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `curso_docente` DISABLE KEYS */;
/*!40000 ALTER TABLE `curso_docente` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `datos_facturacion` (
  `id_datos_facturacion` int unsigned NOT NULL AUTO_INCREMENT,
  `razon_social` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nit` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `direccion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ciudad` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `telefono` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_usuario_actualizo` int unsigned DEFAULT NULL,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_datos_facturacion`),
  KEY `fk_datos_facturacion_usuario` (`id_usuario_actualizo`),
  CONSTRAINT `fk_datos_facturacion_usuario` FOREIGN KEY (`id_usuario_actualizo`) REFERENCES `usuario` (`id_usuario`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Fila única (o una por institución si el negocio lo requiere). La usan Contador y Estudiante al generar/descargar una factura.';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `datos_facturacion` DISABLE KEYS */;
INSERT INTO `datos_facturacion` (`id_datos_facturacion`, `razon_social`, `nit`, `direccion`, `ciudad`, `correo`, `telefono`, `logo_url`, `id_usuario_actualizo`, `actualizado_en`) VALUES (1,'BioLearn S.A.S.','901000000-0','Medellín, Antioquia, Colombia','Medellín',NULL,NULL,NULL,NULL,'2026-11-18 04:26:53');
/*!40000 ALTER TABLE `datos_facturacion` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `datos_recaudo` (
  `id_datos_recaudo` int unsigned NOT NULL AUTO_INCREMENT,
  `banco` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_cuenta` enum('ahorros','corriente') COLLATE utf8mb4_unicode_ci NOT NULL,
  `numero_cuenta` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `titular` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `documento_titular` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `telefono_nequi` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `instrucciones` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_usuario_actualizo` int unsigned DEFAULT NULL,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_datos_recaudo`),
  KEY `fk_datos_recaudo_usuario` (`id_usuario_actualizo`),
  KEY `idx_datos_recaudo_vigente` (`actualizado_en`),
  CONSTRAINT `fk_datos_recaudo_usuario` FOREIGN KEY (`id_usuario_actualizo`) REFERENCES `usuario` (`id_usuario`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `datos_recaudo` DISABLE KEYS */;
INSERT INTO `datos_recaudo` (`id_datos_recaudo`, `banco`, `tipo_cuenta`, `numero_cuenta`, `titular`, `documento_titular`, `telefono_nequi`, `instrucciones`, `id_usuario_actualizo`, `actualizado_en`) VALUES (1,'Bancolombia','ahorros','00012345678','BioLearn S.A.S.','901000000-0',NULL,'Escribe en la referencia de la transferencia el número de tu pago.',NULL,'2026-11-18 04:26:53');
/*!40000 ALTER TABLE `datos_recaudo` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entrega_actividad` (
  `id_entrega` int unsigned NOT NULL AUTO_INCREMENT,
  `id_actividad` int unsigned NOT NULL,
  `id_estudiante` int unsigned NOT NULL,
  `id_revisor` int unsigned DEFAULT NULL,
  `nombre_archivo` varchar(250) COLLATE utf8mb4_unicode_ci NOT NULL,
  `url_archivo` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_archivo` enum('documento','video','audio','imagen','presentacion','otro') COLLATE utf8mb4_unicode_ci NOT NULL,
  `tamanio_kb` int unsigned DEFAULT NULL,
  `es_tardia` tinyint(1) NOT NULL DEFAULT '0',
  `calificacion` decimal(10,2) DEFAULT NULL,
  `comentario_revisor` text COLLATE utf8mb4_unicode_ci,
  `fecha_entrega` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_revision` datetime DEFAULT NULL,
  `estado` enum('entregada','revisada','rechazada') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'entregada',
  PRIMARY KEY (`id_entrega`),
  KEY `fk_entrega_actividad` (`id_actividad`),
  KEY `fk_entrega_estudiante` (`id_estudiante`),
  KEY `fk_entrega_revisor` (`id_revisor`),
  CONSTRAINT `fk_entrega_actividad` FOREIGN KEY (`id_actividad`) REFERENCES `actividad` (`id_actividad`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_entrega_estudiante` FOREIGN KEY (`id_estudiante`) REFERENCES `perfil_estudiante` (`id_estudiante`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_entrega_revisor` FOREIGN KEY (`id_revisor`) REFERENCES `perfil_docente` (`id_docente`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Fusiona Archivo + resultado_actividad. El entregable y su calificacion en una sola entidad.';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `entrega_actividad` DISABLE KEYS */;
/*!40000 ALTER TABLE `entrega_actividad` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluacion` (
  `id_evaluacion` int unsigned NOT NULL AUTO_INCREMENT,
  `id_leccion` int unsigned NOT NULL,
  `id_docente` int unsigned NOT NULL,
  `nombre` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci,
  `tipo` enum('diagnostica','formativa','sumativa') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'formativa',
  `tiempo_limite_min` smallint unsigned DEFAULT NULL,
  `intentos_permitidos` tinyint unsigned NOT NULL DEFAULT '2',
  `puntaje_maximo` decimal(10,2) NOT NULL DEFAULT '10.00',
  `ponderacion` decimal(5,2) NOT NULL DEFAULT '100.00',
  `fecha_aplicacion` datetime DEFAULT NULL,
  `fecha_apertura` datetime DEFAULT NULL,
  `fecha_cierre` datetime DEFAULT NULL,
  `estado` enum('borrador','activa','cerrada') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'borrador',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_evaluacion`),
  KEY `fk_eval_leccion` (`id_leccion`),
  KEY `fk_eval_docente` (`id_docente`),
  CONSTRAINT `fk_eval_docente` FOREIGN KEY (`id_docente`) REFERENCES `perfil_docente` (`id_docente`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_eval_leccion` FOREIGN KEY (`id_leccion`) REFERENCES `leccion` (`id_leccion`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `evaluacion` DISABLE KEYS */;
/*!40000 ALTER TABLE `evaluacion` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evento_virtual` (
  `id_evento` int unsigned NOT NULL AUTO_INCREMENT,
  `id_docente` int unsigned NOT NULL,
  `id_leccion` int unsigned DEFAULT NULL,
  `nombre` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci,
  `enlace_acceso` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_inicio` datetime NOT NULL,
  `fecha_fin` datetime DEFAULT NULL,
  `emite_certificado` tinyint(1) NOT NULL DEFAULT '0',
  `estado` enum('programado','en_curso','finalizado','cancelado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'programado',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_evento`),
  KEY `fk_evento_docente` (`id_docente`),
  KEY `fk_evento_leccion` (`id_leccion`),
  CONSTRAINT `fk_evento_docente` FOREIGN KEY (`id_docente`) REFERENCES `perfil_docente` (`id_docente`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_evento_leccion` FOREIGN KEY (`id_leccion`) REFERENCES `leccion` (`id_leccion`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `evento_virtual` DISABLE KEYS */;
/*!40000 ALTER TABLE `evento_virtual` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `factura` (
  `id_factura` int unsigned NOT NULL AUTO_INCREMENT,
  `id_pago` int unsigned NOT NULL,
  `id_estudiante` int unsigned NOT NULL,
  `id_datos_facturacion` int unsigned DEFAULT NULL,
  `numero_factura` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci,
  `monto_total` decimal(12,2) NOT NULL,
  `monto_pagado` decimal(12,2) DEFAULT NULL,
  `estado` enum('emitida','anulada') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'emitida',
  `enviada_por_correo` tinyint(1) NOT NULL DEFAULT '0',
  `enviada_en` datetime DEFAULT NULL,
  `fecha_emision` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_factura`),
  UNIQUE KEY `uq_factura_numero` (`numero_factura`),
  KEY `fk_fact_pago` (`id_pago`),
  KEY `fk_fact_estudiante` (`id_estudiante`),
  KEY `fk_fact_datos_facturacion` (`id_datos_facturacion`),
  CONSTRAINT `fk_fact_datos_facturacion` FOREIGN KEY (`id_datos_facturacion`) REFERENCES `datos_facturacion` (`id_datos_facturacion`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_fact_estudiante` FOREIGN KEY (`id_estudiante`) REFERENCES `perfil_estudiante` (`id_estudiante`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_fact_pago` FOREIGN KEY (`id_pago`) REFERENCES `pago` (`id_pago`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `factura` DISABLE KEYS */;
/*!40000 ALTER TABLE `factura` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `foro` (
  `id_foro` int unsigned NOT NULL AUTO_INCREMENT,
  `id_leccion` int unsigned NOT NULL,
  `id_docente` int unsigned NOT NULL,
  `nombre` varchar(250) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci,
  `instrucciones` text COLLATE utf8mb4_unicode_ci,
  `tema` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reglas_interaccion` text COLLATE utf8mb4_unicode_ci,
  `tipo_participante` enum('abierto','solo_estudiantes','moderado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'abierto',
  `fecha_apertura` datetime DEFAULT NULL,
  `fecha_cierre` datetime DEFAULT NULL,
  `estado` enum('activo','cerrado','archivado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_foro`),
  KEY `fk_foro_leccion` (`id_leccion`),
  KEY `fk_foro_docente` (`id_docente`),
  CONSTRAINT `fk_foro_docente` FOREIGN KEY (`id_docente`) REFERENCES `perfil_docente` (`id_docente`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_foro_leccion` FOREIGN KEY (`id_leccion`) REFERENCES `leccion` (`id_leccion`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `foro` DISABLE KEYS */;
/*!40000 ALTER TABLE `foro` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grado` (
  `id_grado` tinyint unsigned NOT NULL AUTO_INCREMENT,
  `nombre` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nivel` enum('primaria','secundaria','media') COLLATE utf8mb4_unicode_ci NOT NULL,
  `orden` tinyint unsigned NOT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci,
  `estado` enum('activo','inactivo') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `fecha_creacion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_grado`),
  UNIQUE KEY `uq_grado_nombre` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `grado` DISABLE KEYS */;
INSERT INTO `grado` (`id_grado`, `nombre`, `nivel`, `orden`, `descripcion`, `estado`, `fecha_creacion`, `fecha_actualizacion`) VALUES (1,'Sexto','secundaria',6,NULL,'activo','2026-11-18 04:26:53','2026-11-18 04:26:53');
INSERT INTO `grado` (`id_grado`, `nombre`, `nivel`, `orden`, `descripcion`, `estado`, `fecha_creacion`, `fecha_actualizacion`) VALUES (2,'Séptimo','secundaria',7,NULL,'activo','2026-11-18 04:26:53','2026-11-18 04:26:53');
INSERT INTO `grado` (`id_grado`, `nombre`, `nivel`, `orden`, `descripcion`, `estado`, `fecha_creacion`, `fecha_actualizacion`) VALUES (3,'Octavo','secundaria',8,NULL,'activo','2026-11-18 04:26:53','2026-11-18 04:26:53');
INSERT INTO `grado` (`id_grado`, `nombre`, `nivel`, `orden`, `descripcion`, `estado`, `fecha_creacion`, `fecha_actualizacion`) VALUES (4,'Noveno','secundaria',9,NULL,'activo','2026-11-18 04:26:53','2026-11-18 04:26:53');
INSERT INTO `grado` (`id_grado`, `nombre`, `nivel`, `orden`, `descripcion`, `estado`, `fecha_creacion`, `fecha_actualizacion`) VALUES (5,'Décimo','media',10,NULL,'activo','2026-11-18 04:26:53','2026-11-18 04:26:53');
INSERT INTO `grado` (`id_grado`, `nombre`, `nivel`, `orden`, `descripcion`, `estado`, `fecha_creacion`, `fecha_actualizacion`) VALUES (6,'Undécimo','media',11,NULL,'activo','2026-11-18 04:26:53','2026-11-18 04:26:53');
/*!40000 ALTER TABLE `grado` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `historial_acceso` (
  `id_historial` int unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` int unsigned NOT NULL,
  `accion` enum('login','logout','fallo_login','cambio_contrasena') COLLATE utf8mb4_unicode_ci NOT NULL,
  `estado_sesion` enum('exitosa','fallida','expirada') COLLATE utf8mb4_unicode_ci NOT NULL,
  `ip_origen` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dispositivo` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `navegador` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ubicacion` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `duracion_minutos` smallint unsigned DEFAULT NULL,
  `hash_token_sesion` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_acceso` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_historial`),
  KEY `fk_hist_usuario` (`id_usuario`),
  CONSTRAINT `fk_hist_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Solo inserciones. Auditoría de accesos.';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `historial_acceso` DISABLE KEYS */;
/*!40000 ALTER TABLE `historial_acceso` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `historial_grado` (
  `id_historial_grado` int unsigned NOT NULL AUTO_INCREMENT,
  `id_estudiante` int unsigned NOT NULL,
  `id_grado` tinyint unsigned NOT NULL,
  `id_institucion` int unsigned DEFAULT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_fin` date DEFAULT NULL,
  `observaciones` text COLLATE utf8mb4_unicode_ci,
  `registrado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_historial_grado`),
  KEY `fk_hg_estudiante` (`id_estudiante`),
  KEY `fk_hg_grado` (`id_grado`),
  KEY `fk_hg_institucion` (`id_institucion`),
  CONSTRAINT `fk_hg_estudiante` FOREIGN KEY (`id_estudiante`) REFERENCES `perfil_estudiante` (`id_estudiante`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_hg_grado` FOREIGN KEY (`id_grado`) REFERENCES `grado` (`id_grado`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_hg_institucion` FOREIGN KEY (`id_institucion`) REFERENCES `institucion` (`id_institucion`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `historial_grado` DISABLE KEYS */;
/*!40000 ALTER TABLE `historial_grado` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `horario_curso` (
  `id_horario` int unsigned NOT NULL AUTO_INCREMENT,
  `id_curso` int unsigned NOT NULL,
  `dia_semana` enum('lunes','martes','miercoles','jueves','viernes','sabado','domingo') COLLATE utf8mb4_unicode_ci NOT NULL,
  `hora_inicio` time NOT NULL,
  `hora_fin` time NOT NULL,
  PRIMARY KEY (`id_horario`),
  KEY `fk_horario_curso` (`id_curso`),
  CONSTRAINT `fk_horario_curso` FOREIGN KEY (`id_curso`) REFERENCES `curso` (`id_curso`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `horario_curso` DISABLE KEYS */;
/*!40000 ALTER TABLE `horario_curso` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inscripcion` (
  `id_inscripcion` int unsigned NOT NULL AUTO_INCREMENT,
  `id_curso` int unsigned NOT NULL,
  `id_estudiante` int unsigned NOT NULL,
  `id_institucion_curso` int unsigned DEFAULT NULL,
  `estado` enum('pendiente_pago','activa','completada','retirada','suspendida','en_reembolso','vencida','cerrada') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activa',
  `fecha_inscripcion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_completacion` datetime DEFAULT NULL,
  `acceso_desde` datetime DEFAULT NULL,
  `acceso_hasta` datetime DEFAULT NULL,
  `repaso_hasta` datetime DEFAULT NULL,
  `aviso_vencimiento_en` datetime DEFAULT NULL,
  `aviso_repaso_en` datetime DEFAULT NULL,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_inscripcion`),
  UNIQUE KEY `uq_inscripcion` (`id_curso`,`id_estudiante`),
  KEY `idx_inscripcion_est` (`id_estudiante`,`estado`),
  KEY `fk_insc_contratacion` (`id_institucion_curso`),
  KEY `idx_inscripcion_acceso_hasta` (`estado`,`acceso_hasta`),
  KEY `idx_inscripcion_repaso_hasta` (`estado`,`repaso_hasta`),
  CONSTRAINT `fk_insc_contratacion` FOREIGN KEY (`id_institucion_curso`) REFERENCES `institucion_curso_contratado` (`id_institucion_curso`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_insc_curso` FOREIGN KEY (`id_curso`) REFERENCES `curso` (`id_curso`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_insc_estudiante` FOREIGN KEY (`id_estudiante`) REFERENCES `perfil_estudiante` (`id_estudiante`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Une curso con estudiante. Se genera al confirmar el pago.';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `inscripcion` DISABLE KEYS */;
/*!40000 ALTER TABLE `inscripcion` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `institucion` (
  `id_institucion` int unsigned NOT NULL AUTO_INCREMENT,
  `nombre` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nit` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rector` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `direccion` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `telefono` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `codigo_afiliacion` char(8) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cupos_contratados` smallint unsigned DEFAULT NULL,
  `estado` enum('activa','inactiva','suspendida') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activa',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_institucion`),
  UNIQUE KEY `uq_institucion_nit` (`nit`),
  UNIQUE KEY `uq_institucion_codigo` (`codigo_afiliacion`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `institucion` DISABLE KEYS */;
INSERT INTO `institucion` (`id_institucion`, `nombre`, `nit`, `rector`, `direccion`, `telefono`, `correo`, `logo_url`, `codigo_afiliacion`, `cupos_contratados`, `estado`, `creado_en`, `actualizado_en`) VALUES (1,'Colegio San Rafael','900.223.114-2','Marta Loaiza',NULL,'601 745 8820','rectoria@sanrafael.edu.co',NULL,'SR7K2MQX',120,'activa','2026-11-18 04:26:53','2026-11-18 04:26:53');
INSERT INTO `institucion` (`id_institucion`, `nombre`, `nit`, `rector`, `direccion`, `telefono`, `correo`, `logo_url`, `codigo_afiliacion`, `cupos_contratados`, `estado`, `creado_en`, `actualizado_en`) VALUES (2,'Corporación Andina de Estudios','800.556.902-1','Felipe Duarte',NULL,'601 622 4471','direccion@andina.edu.co',NULL,'AN4P8TWZ',60,'activa','2026-11-18 04:26:53','2026-11-18 04:26:53');
INSERT INTO `institucion` (`id_institucion`, `nombre`, `nit`, `rector`, `direccion`, `telefono`, `correo`, `logo_url`, `codigo_afiliacion`, `cupos_contratados`, `estado`, `creado_en`, `actualizado_en`) VALUES (3,'Instituto Nueva Semilla','901.774.220-5','Camila Ortiz',NULL,'602 310 9954','rectoria@nuevasemilla.edu.co',NULL,'NS9H3DLE',40,'activa','2026-11-18 04:26:53','2026-11-18 04:26:53');
/*!40000 ALTER TABLE `institucion` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `institucion_curso_contratado` (
  `id_institucion_curso` int unsigned NOT NULL AUTO_INCREMENT,
  `id_institucion` int unsigned NOT NULL,
  `id_curso` int unsigned NOT NULL,
  `codigo_inscripcion` char(8) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contratado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_institucion_curso`),
  UNIQUE KEY `uq_institucion_curso` (`id_institucion`,`id_curso`),
  UNIQUE KEY `uq_contratacion_codigo` (`codigo_inscripcion`),
  KEY `fk_ic_curso` (`id_curso`),
  CONSTRAINT `fk_ic_curso` FOREIGN KEY (`id_curso`) REFERENCES `curso` (`id_curso`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ic_institucion` FOREIGN KEY (`id_institucion`) REFERENCES `institucion` (`id_institucion`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `institucion_curso_contratado` DISABLE KEYS */;
/*!40000 ALTER TABLE `institucion_curso_contratado` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `leccion` (
  `id_leccion` int unsigned NOT NULL AUTO_INCREMENT,
  `id_curso` int unsigned NOT NULL,
  `id_prerequisito` int unsigned DEFAULT NULL,
  `nombre` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `imagen_portada` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci,
  `orden` smallint unsigned NOT NULL,
  `tipo` enum('teoria','practica','evaluacion','mixta') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'teoria',
  `estado` enum('borrador','activa','inactiva','archivada') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'borrador',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_leccion`),
  UNIQUE KEY `uq_leccion_orden` (`id_curso`,`orden`),
  KEY `fk_leccion_prerequisito` (`id_prerequisito`),
  KEY `idx_leccion_curso` (`id_curso`,`orden`),
  CONSTRAINT `fk_leccion_curso` FOREIGN KEY (`id_curso`) REFERENCES `curso` (`id_curso`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_leccion_prerequisito` FOREIGN KEY (`id_prerequisito`) REFERENCES `leccion` (`id_leccion`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `leccion` DISABLE KEYS */;
/*!40000 ALTER TABLE `leccion` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `material_contenido` (
  `id_material` int unsigned NOT NULL AUTO_INCREMENT,
  `id_leccion` int unsigned NOT NULL,
  `id_docente` int unsigned NOT NULL,
  `nombre` varchar(250) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo` enum('lectura','video','laboratorio','juego','documento','articulo') COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci,
  `contenido` mediumtext COLLATE utf8mb4_unicode_ci,
  `color_fondo` char(7) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `guia` mediumtext COLLATE utf8mb4_unicode_ci,
  `orden` tinyint unsigned NOT NULL DEFAULT '1',
  `estado` enum('activo','inactivo','archivado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_material`),
  KEY `fk_mat_docente` (`id_docente`),
  KEY `idx_material_leccion` (`id_leccion`,`orden`),
  CONSTRAINT `fk_mat_docente` FOREIGN KEY (`id_docente`) REFERENCES `perfil_docente` (`id_docente`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_mat_leccion` FOREIGN KEY (`id_leccion`) REFERENCES `leccion` (`id_leccion`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sin modo_offline (plataforma online) ni archivo directo (usa tabla recurso).';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `material_contenido` DISABLE KEYS */;
/*!40000 ALTER TABLE `material_contenido` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mensaje` (
  `id_mensaje` int unsigned NOT NULL AUTO_INCREMENT,
  `id_emisor` int unsigned NOT NULL,
  `id_receptor` int unsigned NOT NULL,
  `id_curso` int unsigned DEFAULT NULL,
  `asunto` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contenido` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `leido` tinyint(1) NOT NULL DEFAULT '0',
  `leido_en` datetime DEFAULT NULL,
  `estado_emisor` enum('activo','eliminado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `estado_receptor` enum('activo','eliminado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `enviado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_mensaje`),
  KEY `fk_msg_emisor` (`id_emisor`),
  KEY `fk_msg_receptor` (`id_receptor`),
  KEY `fk_msg_curso` (`id_curso`),
  CONSTRAINT `fk_msg_curso` FOREIGN KEY (`id_curso`) REFERENCES `curso` (`id_curso`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_msg_emisor` FOREIGN KEY (`id_emisor`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_msg_receptor` FOREIGN KEY (`id_receptor`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `mensaje` DISABLE KEYS */;
/*!40000 ALTER TABLE `mensaje` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nota_credito` (
  `id_nota_credito` int unsigned NOT NULL AUTO_INCREMENT,
  `id_reembolso` int unsigned NOT NULL,
  `id_factura` int unsigned NOT NULL,
  `id_estudiante` int unsigned NOT NULL,
  `id_datos_facturacion` int unsigned DEFAULT NULL,
  `numero_nota` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `motivo` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `monto_total` decimal(12,2) NOT NULL,
  `enviada_por_correo` tinyint(1) NOT NULL DEFAULT '0',
  `enviada_en` datetime DEFAULT NULL,
  `fecha_emision` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_nota_credito`),
  UNIQUE KEY `uq_nota_numero` (`numero_nota`),
  UNIQUE KEY `uq_nota_reembolso` (`id_reembolso`),
  KEY `fk_nc_factura` (`id_factura`),
  KEY `fk_nc_estudiante` (`id_estudiante`),
  KEY `fk_nc_datos_facturacion` (`id_datos_facturacion`),
  CONSTRAINT `fk_nc_datos_facturacion` FOREIGN KEY (`id_datos_facturacion`) REFERENCES `datos_facturacion` (`id_datos_facturacion`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_nc_estudiante` FOREIGN KEY (`id_estudiante`) REFERENCES `perfil_estudiante` (`id_estudiante`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_nc_factura` FOREIGN KEY (`id_factura`) REFERENCES `factura` (`id_factura`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_nc_reembolso` FOREIGN KEY (`id_reembolso`) REFERENCES `reembolso` (`id_reembolso`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Nota crédito de un reembolso aprobado: corrige la factura sin anularla; una línea por curso reembolsado.';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `nota_credito` DISABLE KEYS */;
/*!40000 ALTER TABLE `nota_credito` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notificacion` (
  `id_notificacion` int unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` int unsigned NOT NULL,
  `id_usuario_emisor` int unsigned DEFAULT NULL,
  `tipo` enum('pago','inscripcion','leccion','evaluacion','foro','mensaje','certificado','reembolso','sistema') COLLATE utf8mb4_unicode_ci NOT NULL,
  `entidad_id` int unsigned DEFAULT NULL,
  `titulo` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contenido` text COLLATE utf8mb4_unicode_ci,
  `estado_notificacion` enum('pendiente','enviada','leida','archivada') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pendiente',
  `leida` tinyint(1) NOT NULL DEFAULT '0',
  `leida_en` datetime DEFAULT NULL,
  `creada_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_notificacion`),
  KEY `fk_notif_emisor` (`id_usuario_emisor`),
  KEY `idx_notif_usuario` (`id_usuario`,`leida`),
  CONSTRAINT `fk_notif_emisor` FOREIGN KEY (`id_usuario_emisor`) REFERENCES `usuario` (`id_usuario`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_notif_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='entidad_id + tipo permiten notificar sobre cualquier evento del sistema (pago, factura, leccion, foro...).';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `notificacion` DISABLE KEYS */;
/*!40000 ALTER TABLE `notificacion` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `opcion_pregunta` (
  `id_opcion` int unsigned NOT NULL AUTO_INCREMENT,
  `id_pregunta` int unsigned NOT NULL,
  `texto` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `es_correcta` tinyint(1) NOT NULL DEFAULT '0',
  `retroalimentacion` text COLLATE utf8mb4_unicode_ci,
  `orden` tinyint unsigned NOT NULL DEFAULT '1',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_opcion`),
  KEY `fk_opcion_pregunta` (`id_pregunta`),
  CONSTRAINT `fk_opcion_pregunta` FOREIGN KEY (`id_pregunta`) REFERENCES `pregunta` (`id_pregunta`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `opcion_pregunta` DISABLE KEYS */;
/*!40000 ALTER TABLE `opcion_pregunta` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pago` (
  `id_pago` int unsigned NOT NULL AUTO_INCREMENT,
  `id_estudiante` int unsigned NOT NULL,
  `id_contador` int unsigned DEFAULT NULL,
  `medio_pago` enum('pse','wompi_tarjeta','wompi_nequi','transferencia','otro') COLLATE utf8mb4_unicode_ci NOT NULL,
  `monto` decimal(12,2) NOT NULL,
  `fecha_pago` datetime DEFAULT NULL,
  `referencia_externa` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('pendiente','aprobado','rechazado','cancelado','reembolsado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pendiente',
  `motivo_rechazo` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `comprobante_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_pago`),
  KEY `fk_pago_contador` (`id_contador`),
  KEY `idx_pago_estudiante` (`id_estudiante`,`estado`),
  KEY `idx_pago_estado` (`estado`),
  KEY `idx_pago_creado` (`creado_en`),
  CONSTRAINT `fk_pago_contador` FOREIGN KEY (`id_contador`) REFERENCES `perfil_contador` (`id_contador`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_pago_estudiante` FOREIGN KEY (`id_estudiante`) REFERENCES `perfil_estudiante` (`id_estudiante`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `pago` DISABLE KEYS */;
/*!40000 ALTER TABLE `pago` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pago_inscripcion` (
  `id_pago_inscripcion` int unsigned NOT NULL AUTO_INCREMENT,
  `id_pago` int unsigned NOT NULL,
  `id_inscripcion` int unsigned NOT NULL,
  `monto_asignado` decimal(12,2) NOT NULL,
  PRIMARY KEY (`id_pago_inscripcion`),
  UNIQUE KEY `uq_pago_inscripcion` (`id_pago`,`id_inscripcion`),
  KEY `fk_pi_inscripcion` (`id_inscripcion`),
  CONSTRAINT `fk_pi_inscripcion` FOREIGN KEY (`id_inscripcion`) REFERENCES `inscripcion` (`id_inscripcion`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_pi_pago` FOREIGN KEY (`id_pago`) REFERENCES `pago` (`id_pago`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Un pago puede cubrir varias inscripciones (carrito).';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `pago_inscripcion` DISABLE KEYS */;
/*!40000 ALTER TABLE `pago_inscripcion` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `perfil_admin` (
  `id_admin` int unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` int unsigned NOT NULL,
  `rol_admin` enum('superadmin','admin_institucional','admin_contenido') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'admin_institucional',
  `estado` enum('activo','inactivo') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `asignado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_admin`),
  UNIQUE KEY `uq_admin_usuario` (`id_usuario`),
  CONSTRAINT `fk_admin_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `perfil_admin` DISABLE KEYS */;
INSERT INTO `perfil_admin` (`id_admin`, `id_usuario`, `rol_admin`, `estado`, `asignado_en`, `actualizado_en`) VALUES (1,1,'superadmin','activo','2026-11-18 04:26:53','2026-11-18 04:26:53');
/*!40000 ALTER TABLE `perfil_admin` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `perfil_contador` (
  `id_contador` int unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` int unsigned NOT NULL,
  `rol_contador` enum('administrador_financiero','auxiliar','auditor') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'auxiliar',
  `estado` enum('activo','inactivo') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `asignado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_contador`),
  UNIQUE KEY `uq_contador_usuario` (`id_usuario`),
  CONSTRAINT `fk_contador_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `perfil_contador` DISABLE KEYS */;
/*!40000 ALTER TABLE `perfil_contador` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `perfil_docente` (
  `id_docente` int unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` int unsigned NOT NULL,
  `especialidad` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `experiencia_laboral` text COLLATE utf8mb4_unicode_ci,
  `tipo_contrato` enum('planta','contratista','honorarios') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'contratista',
  `estado` enum('activo','inactivo','suspendido') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `asignado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_docente`),
  UNIQUE KEY `uq_docente_usuario` (`id_usuario`),
  CONSTRAINT `fk_docente_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `perfil_docente` DISABLE KEYS */;
/*!40000 ALTER TABLE `perfil_docente` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `perfil_estudiante` (
  `id_estudiante` int unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` int unsigned NOT NULL,
  `id_institucion` int unsigned DEFAULT NULL,
  `id_grado` tinyint unsigned NOT NULL,
  `id_area` tinyint unsigned NOT NULL,
  `estado` enum('activo','inactivo','egresado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `fecha_ingreso` date DEFAULT NULL,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_estudiante`),
  UNIQUE KEY `uq_estudiante_usuario` (`id_usuario`),
  KEY `fk_est_institucion` (`id_institucion`),
  KEY `fk_est_grado` (`id_grado`),
  KEY `fk_est_area` (`id_area`),
  CONSTRAINT `fk_est_area` FOREIGN KEY (`id_area`) REFERENCES `area_academica` (`id_area`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_est_grado` FOREIGN KEY (`id_grado`) REFERENCES `grado` (`id_grado`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_est_institucion` FOREIGN KEY (`id_institucion`) REFERENCES `institucion` (`id_institucion`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_est_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `perfil_estudiante` DISABLE KEYS */;
/*!40000 ALTER TABLE `perfil_estudiante` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plantilla_certificado` (
  `id_plantilla` int unsigned NOT NULL AUTO_INCREMENT,
  `id_docente` int unsigned DEFAULT NULL,
  `color_acento` varchar(7) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `firma_representante_nombre` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `firma_imagen_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `texto_reconocimiento` text COLLATE utf8mb4_unicode_ci,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_plantilla`),
  KEY `fk_plantilla_docente` (`id_docente`),
  CONSTRAINT `fk_plantilla_docente` FOREIGN KEY (`id_docente`) REFERENCES `perfil_docente` (`id_docente`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `plantilla_certificado` DISABLE KEYS */;
/*!40000 ALTER TABLE `plantilla_certificado` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pregunta` (
  `id_pregunta` int unsigned NOT NULL AUTO_INCREMENT,
  `id_evaluacion` int unsigned NOT NULL,
  `enunciado` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo` enum('seleccion_multiple','verdadero_falso','abierta') COLLATE utf8mb4_unicode_ci NOT NULL,
  `puntaje` decimal(10,2) NOT NULL DEFAULT '1.00',
  `retroalimentacion` text COLLATE utf8mb4_unicode_ci,
  `orden` tinyint unsigned NOT NULL DEFAULT '1',
  `estado` enum('activa','inactiva') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activa',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_pregunta`),
  KEY `fk_preg_evaluacion` (`id_evaluacion`),
  CONSTRAINT `fk_preg_evaluacion` FOREIGN KEY (`id_evaluacion`) REFERENCES `evaluacion` (`id_evaluacion`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `pregunta` DISABLE KEYS */;
/*!40000 ALTER TABLE `pregunta` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `progreso_leccion` (
  `id_progreso` int unsigned NOT NULL AUTO_INCREMENT,
  `id_inscripcion` int unsigned NOT NULL,
  `id_leccion` int unsigned NOT NULL,
  `estado` enum('pendiente','en_progreso','completada') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pendiente',
  `porcentaje` decimal(5,2) NOT NULL DEFAULT '0.00',
  `iniciado_en` datetime DEFAULT NULL,
  `completado_en` datetime DEFAULT NULL,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_progreso`),
  UNIQUE KEY `uq_progreso` (`id_inscripcion`,`id_leccion`),
  KEY `fk_prog_leccion` (`id_leccion`),
  KEY `idx_progreso_insc` (`id_inscripcion`),
  CONSTRAINT `fk_prog_inscripcion` FOREIGN KEY (`id_inscripcion`) REFERENCES `inscripcion` (`id_inscripcion`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_prog_leccion` FOREIGN KEY (`id_leccion`) REFERENCES `leccion` (`id_leccion`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `progreso_leccion` DISABLE KEYS */;
/*!40000 ALTER TABLE `progreso_leccion` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `progreso_material` (
  `id_progreso_material` int unsigned NOT NULL AUTO_INCREMENT,
  `id_inscripcion` int unsigned NOT NULL,
  `id_material` int unsigned NOT NULL,
  `abierto_en` datetime NOT NULL,
  `visto_en` datetime DEFAULT NULL,
  PRIMARY KEY (`id_progreso_material`),
  UNIQUE KEY `uq_progreso_material` (`id_inscripcion`,`id_material`),
  KEY `fk_pm_material` (`id_material`),
  CONSTRAINT `fk_pm_inscripcion` FOREIGN KEY (`id_inscripcion`) REFERENCES `inscripcion` (`id_inscripcion`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_pm_material` FOREIGN KEY (`id_material`) REFERENCES `material_contenido` (`id_material`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `progreso_material` DISABLE KEYS */;
/*!40000 ALTER TABLE `progreso_material` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recuperacion_contrasena` (
  `id_recuperacion` int unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` int unsigned NOT NULL,
  `token` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `estado` enum('pendiente','usado','expirado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pendiente',
  `ip_solicitud` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expira_en` datetime NOT NULL,
  `usado_en` datetime DEFAULT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_recuperacion`),
  UNIQUE KEY `uq_recup_token` (`token`),
  KEY `fk_recup_usuario` (`id_usuario`),
  CONSTRAINT `fk_recup_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `recuperacion_contrasena` DISABLE KEYS */;
/*!40000 ALTER TABLE `recuperacion_contrasena` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recurso` (
  `id_recurso` int unsigned NOT NULL AUTO_INCREMENT,
  `id_material` int unsigned NOT NULL,
  `id_docente` int unsigned NOT NULL,
  `nombre` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci,
  `url` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_recurso` enum('video','documento','imagen','audio','enlace','interactivo') COLLATE utf8mb4_unicode_ci NOT NULL,
  `duracion_min` smallint unsigned DEFAULT NULL,
  `nivel_academico` enum('basico','intermedio','avanzado') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `orden` tinyint unsigned NOT NULL DEFAULT '1',
  `estado` enum('activo','inactivo') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_recurso`),
  KEY `fk_recurso_material` (`id_material`),
  KEY `fk_recurso_docente` (`id_docente`),
  CONSTRAINT `fk_recurso_docente` FOREIGN KEY (`id_docente`) REFERENCES `perfil_docente` (`id_docente`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_recurso_material` FOREIGN KEY (`id_material`) REFERENCES `material_contenido` (`id_material`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `recurso` DISABLE KEYS */;
/*!40000 ALTER TABLE `recurso` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recurso_apoyo` (
  `id_recurso_apoyo` int unsigned NOT NULL AUTO_INCREMENT,
  `id_material` int unsigned NOT NULL,
  `id_recurso` int unsigned DEFAULT NULL,
  `id_articulo` int unsigned DEFAULT NULL,
  `id_usuario` int unsigned NOT NULL,
  `tipo_recurso` enum('documento','video','enlace','imagen','interactivo','otro') COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_contenido` enum('primario','complementario','referencia','evaluativo') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'complementario',
  `titulo` varchar(250) COLLATE utf8mb4_unicode_ci NOT NULL,
  `url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ruta_imagen` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `orden` tinyint unsigned NOT NULL DEFAULT '1',
  `estado` enum('activo','inactivo') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `fecha_creacion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_recurso_apoyo`),
  KEY `fk_ra_material` (`id_material`),
  KEY `fk_ra_recurso` (`id_recurso`),
  KEY `fk_ra_articulo` (`id_articulo`),
  KEY `fk_ra_usuario` (`id_usuario`),
  CONSTRAINT `fk_ra_articulo` FOREIGN KEY (`id_articulo`) REFERENCES `articulo_cientifico` (`id_articulo`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_ra_material` FOREIGN KEY (`id_material`) REFERENCES `material_contenido` (`id_material`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ra_recurso` FOREIGN KEY (`id_recurso`) REFERENCES `recurso` (`id_recurso`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_ra_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Vincula recursos complementarios y articulos a un material de contenido con tipo y orden.';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `recurso_apoyo` DISABLE KEYS */;
/*!40000 ALTER TABLE `recurso_apoyo` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reembolso` (
  `id_reembolso` int unsigned NOT NULL AUTO_INCREMENT,
  `id_pago` int unsigned NOT NULL,
  `id_solicitante` int unsigned NOT NULL,
  `id_contador` int unsigned DEFAULT NULL,
  `motivo` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `monto` decimal(12,2) NOT NULL,
  `medio_devolucion` enum('mismo_medio','cuenta_bancaria','nequi') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'mismo_medio',
  `banco` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tipo_cuenta` enum('ahorros','corriente') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `numero_cuenta` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `titular` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `documento_titular` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `telefono_nequi` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `justificacion` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `aprobacion_automatica` tinyint(1) NOT NULL DEFAULT '0',
  `estado` enum('solicitado','aprobado','devuelto','rechazado','cancelado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'solicitado',
  `solicitado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resuelto_en` datetime DEFAULT NULL,
  `referencia_devolucion` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `comprobante_devolucion_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `devuelto_en` datetime DEFAULT NULL,
  PRIMARY KEY (`id_reembolso`),
  KEY `fk_ree_pago` (`id_pago`),
  KEY `fk_ree_solicitante` (`id_solicitante`),
  KEY `fk_ree_contador` (`id_contador`),
  KEY `idx_reembolso_estado` (`estado`),
  KEY `idx_reembolso_solicitado` (`solicitado_en`),
  CONSTRAINT `fk_ree_contador` FOREIGN KEY (`id_contador`) REFERENCES `perfil_contador` (`id_contador`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_ree_pago` FOREIGN KEY (`id_pago`) REFERENCES `pago` (`id_pago`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_ree_solicitante` FOREIGN KEY (`id_solicitante`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `reembolso` DISABLE KEYS */;
/*!40000 ALTER TABLE `reembolso` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reembolso_inscripcion` (
  `id_reembolso_inscripcion` int unsigned NOT NULL AUTO_INCREMENT,
  `id_reembolso` int unsigned NOT NULL,
  `id_pago_inscripcion` int unsigned NOT NULL,
  `monto` decimal(12,2) NOT NULL,
  `avance` decimal(5,2) NOT NULL DEFAULT '0.00',
  `estado_inscripcion_previo` enum('activa','retirada') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_reembolso_inscripcion`),
  UNIQUE KEY `uq_reembolso_linea` (`id_reembolso`,`id_pago_inscripcion`),
  KEY `fk_ri_pago_inscripcion` (`id_pago_inscripcion`),
  CONSTRAINT `fk_ri_pago_inscripcion` FOREIGN KEY (`id_pago_inscripcion`) REFERENCES `pago_inscripcion` (`id_pago_inscripcion`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_ri_reembolso` FOREIGN KEY (`id_reembolso`) REFERENCES `reembolso` (`id_reembolso`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Cursos de una solicitud de reembolso; un curso pagado solo puede estar en una solicitud vigente.';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `reembolso_inscripcion` DISABLE KEYS */;
/*!40000 ALTER TABLE `reembolso_inscripcion` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reporte` (
  `id_reporte` int unsigned NOT NULL AUTO_INCREMENT,
  `id_solicitante` int unsigned NOT NULL,
  `id_curso` int unsigned DEFAULT NULL,
  `id_inscripcion` int unsigned DEFAULT NULL,
  `id_institucion` int unsigned DEFAULT NULL,
  `id_supervisor_inst` int unsigned DEFAULT NULL,
  `id_contador` int unsigned DEFAULT NULL,
  `id_factura` int unsigned DEFAULT NULL,
  `id_datos_facturacion` int unsigned DEFAULT NULL,
  `tipo` enum('academico','financiero','institucional','mixto') COLLATE utf8mb4_unicode_ci NOT NULL,
  `formato` enum('pdf','excel','csv') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_corte` datetime DEFAULT NULL,
  `motivo` text COLLATE utf8mb4_unicode_ci,
  `descripcion` text COLLATE utf8mb4_unicode_ci,
  `contenido` longtext COLLATE utf8mb4_unicode_ci,
  `estado` enum('pendiente','generado','validado','cerrado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pendiente',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_reporte`),
  KEY `fk_rep_solicitante` (`id_solicitante`),
  KEY `fk_rep_curso` (`id_curso`),
  KEY `fk_rep_inscripcion` (`id_inscripcion`),
  KEY `fk_rep_institucion` (`id_institucion`),
  KEY `fk_rep_supervisor` (`id_supervisor_inst`),
  KEY `fk_rep_contador` (`id_contador`),
  KEY `fk_rep_factura` (`id_factura`),
  KEY `fk_rep_datos_facturacion` (`id_datos_facturacion`),
  CONSTRAINT `fk_rep_contador` FOREIGN KEY (`id_contador`) REFERENCES `perfil_contador` (`id_contador`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_rep_curso` FOREIGN KEY (`id_curso`) REFERENCES `curso` (`id_curso`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_rep_datos_facturacion` FOREIGN KEY (`id_datos_facturacion`) REFERENCES `datos_facturacion` (`id_datos_facturacion`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_rep_factura` FOREIGN KEY (`id_factura`) REFERENCES `factura` (`id_factura`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_rep_inscripcion` FOREIGN KEY (`id_inscripcion`) REFERENCES `inscripcion` (`id_inscripcion`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_rep_institucion` FOREIGN KEY (`id_institucion`) REFERENCES `institucion` (`id_institucion`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_rep_solicitante` FOREIGN KEY (`id_solicitante`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_rep_supervisor` FOREIGN KEY (`id_supervisor_inst`) REFERENCES `supervisor_institucion` (`id_supervisor_inst`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `reporte` DISABLE KEYS */;
/*!40000 ALTER TABLE `reporte` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reporte_certificado` (
  `id_reporte` int unsigned NOT NULL,
  `id_certificado` int unsigned NOT NULL,
  `incluido_en_calculo` tinyint(1) NOT NULL DEFAULT '1',
  `observaciones` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id_reporte`,`id_certificado`),
  KEY `fk_rc_certificado` (`id_certificado`),
  CONSTRAINT `fk_rc_certificado` FOREIGN KEY (`id_certificado`) REFERENCES `certificado` (`id_certificado`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_rc_reporte` FOREIGN KEY (`id_reporte`) REFERENCES `reporte` (`id_reporte`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `reporte_certificado` DISABLE KEYS */;
/*!40000 ALTER TABLE `reporte_certificado` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reporte_financiero` (
  `id_reporte_financiero` int unsigned NOT NULL AUTO_INCREMENT,
  `tipo` enum('ingresos','facturas','reembolsos','general') COLLATE utf8mb4_unicode_ci NOT NULL,
  `desde` date NOT NULL,
  `hasta` date NOT NULL,
  `formato` enum('excel','pdf') COLLATE utf8mb4_unicode_ci NOT NULL,
  `motivo` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_usuario` int unsigned NOT NULL,
  `generado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_reporte_financiero`),
  KEY `fk_reporte_financiero_usuario` (`id_usuario`),
  KEY `idx_reporte_financiero_fecha` (`generado_en`),
  CONSTRAINT `fk_reporte_financiero_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `reporte_financiero` DISABLE KEYS */;
/*!40000 ALTER TABLE `reporte_financiero` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `respuesta_estudiante` (
  `id_respuesta` int unsigned NOT NULL AUTO_INCREMENT,
  `id_resultado` int unsigned NOT NULL,
  `id_pregunta` int unsigned NOT NULL,
  `id_opcion` int unsigned DEFAULT NULL,
  `id_revisor` int unsigned DEFAULT NULL,
  `contenido_abierto` text COLLATE utf8mb4_unicode_ci,
  `estado_revision` enum('pendiente','correcta','incorrecta','parcial') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pendiente',
  `puntaje_obtenido` decimal(10,2) DEFAULT NULL,
  `comentario_revisor` text COLLATE utf8mb4_unicode_ci,
  `revisado_en` datetime DEFAULT NULL,
  PRIMARY KEY (`id_respuesta`),
  UNIQUE KEY `uq_respuesta` (`id_resultado`,`id_pregunta`),
  KEY `fk_resp_pregunta` (`id_pregunta`),
  KEY `fk_resp_opcion` (`id_opcion`),
  KEY `fk_resp_revisor` (`id_revisor`),
  KEY `idx_respuesta_resultado` (`id_resultado`),
  CONSTRAINT `fk_resp_opcion` FOREIGN KEY (`id_opcion`) REFERENCES `opcion_pregunta` (`id_opcion`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_resp_pregunta` FOREIGN KEY (`id_pregunta`) REFERENCES `pregunta` (`id_pregunta`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_resp_resultado` FOREIGN KEY (`id_resultado`) REFERENCES `resultado_evaluacion` (`id_resultado`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_resp_revisor` FOREIGN KEY (`id_revisor`) REFERENCES `perfil_docente` (`id_docente`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `respuesta_estudiante` DISABLE KEYS */;
/*!40000 ALTER TABLE `respuesta_estudiante` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resultado_evaluacion` (
  `id_resultado` int unsigned NOT NULL AUTO_INCREMENT,
  `id_evaluacion` int unsigned NOT NULL,
  `id_inscripcion` int unsigned NOT NULL,
  `numero_intento` tinyint unsigned NOT NULL DEFAULT '1',
  `puntaje_total` decimal(10,2) NOT NULL DEFAULT '0.00',
  `porcentaje` decimal(5,2) NOT NULL DEFAULT '0.00',
  `aprobado` tinyint(1) NOT NULL DEFAULT '0',
  `estado` enum('pendiente','calculado','publicado','revisado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'calculado',
  `completado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_resultado`),
  UNIQUE KEY `uq_resultado_intento` (`id_evaluacion`,`id_inscripcion`,`numero_intento`),
  KEY `idx_resultado_insc` (`id_inscripcion`,`id_evaluacion`),
  CONSTRAINT `fk_res_evaluacion` FOREIGN KEY (`id_evaluacion`) REFERENCES `evaluacion` (`id_evaluacion`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_res_inscripcion` FOREIGN KEY (`id_inscripcion`) REFERENCES `inscripcion` (`id_inscripcion`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `resultado_evaluacion` DISABLE KEYS */;
/*!40000 ALTER TABLE `resultado_evaluacion` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `retroalimentacion` (
  `id_retroalimentacion` int unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario_emisor` int unsigned NOT NULL,
  `id_usuario_receptor` int unsigned NOT NULL,
  `id_respuesta` int unsigned DEFAULT NULL,
  `id_resultado` int unsigned DEFAULT NULL,
  `id_asistencia` int unsigned DEFAULT NULL,
  `id_comentario` int unsigned DEFAULT NULL,
  `id_calificacion_foro` int unsigned DEFAULT NULL,
  `id_entrega` int unsigned DEFAULT NULL,
  `comentarios` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `sugerencias` text COLLATE utf8mb4_unicode_ci,
  `estado` enum('borrador','enviada','leida','archivada') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'enviada',
  `fecha_retroalimentacion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_retroalimentacion`),
  KEY `fk_retro_emisor` (`id_usuario_emisor`),
  KEY `fk_retro_receptor` (`id_usuario_receptor`),
  KEY `fk_retro_respuesta` (`id_respuesta`),
  KEY `fk_retro_resultado` (`id_resultado`),
  KEY `fk_retro_asistencia` (`id_asistencia`),
  KEY `fk_retro_comentario` (`id_comentario`),
  KEY `fk_retro_calificacion_foro` (`id_calificacion_foro`),
  KEY `fk_retro_entrega` (`id_entrega`),
  CONSTRAINT `fk_retro_asistencia` FOREIGN KEY (`id_asistencia`) REFERENCES `asistencia` (`id_asistencia`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_retro_calificacion_foro` FOREIGN KEY (`id_calificacion_foro`) REFERENCES `calificacion_foro` (`id_calificacion`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_retro_comentario` FOREIGN KEY (`id_comentario`) REFERENCES `comentario_foro` (`id_comentario`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_retro_emisor` FOREIGN KEY (`id_usuario_emisor`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_retro_entrega` FOREIGN KEY (`id_entrega`) REFERENCES `entrega_actividad` (`id_entrega`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_retro_receptor` FOREIGN KEY (`id_usuario_receptor`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_retro_respuesta` FOREIGN KEY (`id_respuesta`) REFERENCES `respuesta_estudiante` (`id_respuesta`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_retro_resultado` FOREIGN KEY (`id_resultado`) REFERENCES `resultado_evaluacion` (`id_resultado`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Retroalimentacion formal entre docente/supervisor y estudiante.';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `retroalimentacion` DISABLE KEYS */;
/*!40000 ALTER TABLE `retroalimentacion` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sesion_online` (
  `id_sesion` int unsigned NOT NULL AUTO_INCREMENT,
  `id_leccion` int unsigned NOT NULL,
  `id_docente` int unsigned NOT NULL,
  `nombre` varchar(250) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tematica` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci,
  `enlace` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `plataforma` enum('meet','youtube','zoom','teams','otro') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'meet',
  `codigo_asistencia` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_inicio` datetime NOT NULL,
  `fecha_fin` datetime DEFAULT NULL,
  `grabacion_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('programada','en_curso','finalizada','cancelada') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'programada',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_sesion`),
  KEY `fk_sesion_leccion` (`id_leccion`),
  KEY `fk_sesion_docente` (`id_docente`),
  CONSTRAINT `fk_sesion_docente` FOREIGN KEY (`id_docente`) REFERENCES `perfil_docente` (`id_docente`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_sesion_leccion` FOREIGN KEY (`id_leccion`) REFERENCES `leccion` (`id_leccion`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `sesion_online` DISABLE KEYS */;
/*!40000 ALTER TABLE `sesion_online` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sesion_otp` (
  `id_sesion_otp` int unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` int unsigned NOT NULL,
  `codigo` char(6) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expira_en` datetime NOT NULL,
  `usado` tinyint(1) NOT NULL DEFAULT '0',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_sesion_otp`),
  KEY `fk_otp_usuario` (`id_usuario`),
  CONSTRAINT `fk_otp_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `sesion_otp` DISABLE KEYS */;
/*!40000 ALTER TABLE `sesion_otp` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supervisor_institucion` (
  `id_supervisor_inst` int unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` int unsigned NOT NULL,
  `id_institucion` int unsigned NOT NULL,
  `cargo` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `acceso_reportes` tinyint(1) NOT NULL DEFAULT '1',
  `acceso_foros` tinyint(1) NOT NULL DEFAULT '1',
  `estado` enum('activo','inactivo','suspendido') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activo',
  `asignado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_supervisor_inst`),
  UNIQUE KEY `uq_supervisor_usuario` (`id_usuario`),
  KEY `fk_sup_institucion` (`id_institucion`),
  CONSTRAINT `fk_sup_institucion` FOREIGN KEY (`id_institucion`) REFERENCES `institucion` (`id_institucion`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_sup_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Supervisor representa a UNA institución. UNIQUE en id_usuario impide asignación múltiple.';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `supervisor_institucion` DISABLE KEYS */;
/*!40000 ALTER TABLE `supervisor_institucion` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario` (
  `id_usuario` int unsigned NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `apellido` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre_usuario` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `correo` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contrasena` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_usuario` enum('admin','docente','estudiante','supervisor','contador') COLLATE utf8mb4_unicode_ci NOT NULL,
  `estado` enum('pendiente','activo','inactivo','suspendido') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pendiente',
  `doc_identidad` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_nacimiento` date DEFAULT NULL,
  `telefono` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `direccion` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ciudad` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `foto_perfil` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rol_detalle` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_registro` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `uq_usuario_correo` (`correo`),
  UNIQUE KEY `uq_usuario_nombre_u` (`nombre_usuario`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabla central de identidad. Todos los roles heredan de aquí.';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `usuario` DISABLE KEYS */;
INSERT INTO `usuario` (`id_usuario`, `nombre`, `apellido`, `nombre_usuario`, `correo`, `contrasena`, `tipo_usuario`, `estado`, `doc_identidad`, `fecha_nacimiento`, `telefono`, `direccion`, `ciudad`, `foto_perfil`, `rol_detalle`, `fecha_registro`, `fecha_actualizacion`) VALUES (1,'Administrador','BioLearn','admin','admin@biolearn.co','$2a$10$QSMJ.VGjCLa8pTt1h0/YW.4U0MqFL1Anb6CUXril9ux7mWQfiG96.','admin','activo',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-11-18 04:26:53','2026-11-18 04:26:53');
/*!40000 ALTER TABLE `usuario` ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `verificacion_cuenta` (
  `id_verificacion` int unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` int unsigned NOT NULL,
  `token` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo` enum('activacion','cambio_correo') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'activacion',
  `estado` enum('pendiente','usado','expirado') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pendiente',
  `expira_en` datetime NOT NULL,
  `usado_en` datetime DEFAULT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_verificacion`),
  UNIQUE KEY `uq_verif_token` (`token`),
  KEY `fk_verif_usuario` (`id_usuario`),
  CONSTRAINT `fk_verif_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `verificacion_cuenta` DISABLE KEYS */;
/*!40000 ALTER TABLE `verificacion_cuenta` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

