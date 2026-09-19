# BioLearn — Backend

API REST de BioLearn, construida en Java + Spring Boot, con persistencia en MySQL.

## Requisitos

- JDK 17 o superior
- Maven (viene integrado en IntelliJ, no hace falta instalarlo aparte)
- MySQL Server 8.x corriendo en `localhost:3306`

## Primeros pasos

1. Crea la base de datos ejecutando `schema.sql` (está en la raíz del proyecto) en MySQL Workbench o por consola:
   ```
   mysql -u root -p < schema.sql
   ```
2. Copia `src/main/resources/application.properties.example` a `src/main/resources/application.properties`.
3. Edita `application.properties` con tu usuario y clave reales de MySQL.
4. Abre el proyecto en IntelliJ IDEA (`File → Open` y selecciona esta carpeta).
5. Espera a que Maven descargue las dependencias (aparece abajo a la derecha).
6. Ejecuta `BiolearnBackendApplication.java` (botón ▶ verde).
7. La API queda disponible en `http://localhost:8080`.
8. La documentación interactiva (Swagger) queda en `http://localhost:8080/swagger-ui.html`.

## Estructura de paquetes

Corresponde al diagrama de paquetes del documento de arquitectura:

```
co.biolearn
 ├── config       → configuración de Spring (CORS, Swagger)
 ├── security     → JWT, filtros de autenticación
 ├── controller   → endpoints REST
 ├── dto          → objetos de entrada/salida de la API
 ├── service      → interfaces de lógica de negocio
 │    └── impl    → implementación de esa lógica
 ├── repository   → acceso a datos (Spring Data JPA)
 ├── model        → entidades (tablas de la base de datos)
 ├── enums        → catálogos fijos (roles, estados)
 ├── exception    → manejo de errores
 └── util         → utilidades y mapeadores
```

## Convenciones de commits (sugeridas)

```
feat: agrega endpoint de login
fix: corrige validación de código de institución
docs: actualiza README
test: agrega pruebas unitarias de PagoService
```
