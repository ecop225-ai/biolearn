# BioLearn — Documentación del proyecto

Esta carpeta reúne toda la documentación técnica del proyecto BioLearn, generada durante las fases de análisis, diseño y arquitectura, previas y paralelas a la implementación del código.

## Documento principal

📄 **[BioLearn_Documento_Tecnico.docx](./BioLearn_Documento_Tecnico.docx)** — documento consolidado con introducción, objetivos, metodología, arquitectura, modelo de datos, seguridad, implementación y pruebas de cada módulo. Es el documento de referencia para la sustentación del proyecto; se actualiza a medida que se completa cada módulo del sistema.

## Documentos de análisis y requerimientos

| # | Documento | Contenido |
|---|---|---|
| 01 | [Historias de usuario](./01_historias_usuario.md) | 32 historias, clasificadas por módulo/rol y sin clasificar |
| 02 | [Requerimientos del sistema](./02_requerimientos.md) | 32 requerimientos funcionales + 23 no funcionales |
| 07 | [Matriz de trazabilidad](./07_matriz_trazabilidad.md) | Conecta las historias de usuario (por rol) con los dominios de datos (por tema) |

## Documentos de arquitectura y diseño

| # | Documento | Contenido |
|---|---|---|
| 03 | [Arquitectura del sistema](./03_arquitectura.md) | Capas, diagrama de paquetes, diagrama de clases, diagrama de componentes |
| 04 | [Diagrama de clases por dominio](./04_diagrama_clases_por_dominio.md) | El diagrama de clases dividido en 8 diagramas más pequeños, uno por dominio |
| 05 | [Diagrama Entidad-Relación completo](./05_diagrama_er_completo.md) | Las 51 tablas con sus atributos y llaves, generado directamente del script SQL |
| 06 | [Diccionario de datos](./06_diccionario_datos.md) | Descripción columna por columna de las 51 tablas, agrupadas en 8 dominios |
| 09 | [Mapa de navegación](./09_mapa_navegacion.md) | Flujo de pantallas de cada uno de los 5 roles del sistema |

## Documentos de seguridad y metodología

| # | Documento | Contenido |
|---|---|---|
| 08 | [Mecanismos de seguridad](./08_mecanismos_seguridad.md) | Autenticación, autorización, protección de datos, defensas técnicas |
| 11 | [Metodología, librerías y frameworks](./11_metodologia_librerias.md) | Justificación de Scrum para este proyecto, y tabla de tecnologías por capa |

## Ambiente de desarrollo

| # | Documento | Contenido |
|---|---|---|
| 10 | [Guía de configuración del entorno](./10_guia_entorno.md) | Instalación de JDK, IDE, Git, MySQL, y verificación del ambiente |
| — | [biolearn_database.sql](./biolearn_database.sql) | Script completo de creación de la base de datos (51 tablas) |

---

*Documentación generada como parte del desarrollo del proyecto BioLearn — plataforma educativa de biología con 5 roles de usuario (Administrador, Docente, Contador, Representante institucional, Estudiante).*
