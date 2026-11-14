# BioLearn — Metodología, librerías y frameworks por capa

Cierra los puntos de documentación pendientes del checklist original: metodología de desarrollo, y el detalle de librerías/frameworks usados en cada capa de la arquitectura.

---

## 1. Metodología de desarrollo de software

**Metodología seleccionada: Scrum (ágil).**

**Por qué esta metodología y no otra:** el proyecto ya nació organizado en historias de usuario con formato ágil completo (rol, funcionalidad, razón, escenarios y criterios de aceptación en formato Gherkin: contexto/evento/resultado) — es decir, la forma en que se documentaron los requerimientos desde el principio ya asume una metodología ágil, no una tradicional en cascada. Scrum encaja naturalmente con esa base.

**Cómo se aplica en este proyecto:**

| Elemento de Scrum | Cómo se traduce en BioLearn |
|---|---|
| Product Backlog | Las 32 historias de usuario documentadas, clasificadas por módulo (rol) |
| Sprint | Cada módulo del sistema (Auth, Usuarios, Cursos, Finanzas...) se aborda como una unidad de trabajo completa: diseño → código → prueba, antes de pasar al siguiente |
| Historia de usuario como unidad de trabajo | Cada historia (`SOL-XXX` / `ACT-XXX`) es la unidad mínima de planificación y desarrollo |
| Incrementos entregables | Cada módulo terminado es funcional de punta a punta (por ejemplo: Auth ya expone 6 endpoints probados, no código a medias) |
| Retrospectiva / mejora continua | Cada vez que una prueba reveló un problema de diseño (ej. el tipo de dato `Integer` vs `Long`, o la falta del botón "Authorize" en Swagger), se corrigió antes de avanzar al siguiente módulo, en vez de acumular deuda técnica |
| Control de versiones por incremento | Una rama de Git por historia de usuario (`feature/HU-login`, por ejemplo), integrada a `main` solo cuando esa funcionalidad está probada |

**Diferencia con un enfoque en cascada:** en cascada, todo el análisis y diseño se cierra antes de escribir una sola línea de código, y los cambios posteriores son costosos. Aquí, la arquitectura y los diagramas se ajustaron varias veces a medida que se codificaba (por ejemplo, el diagrama de clases y el diccionario de datos se corrigieron después de encontrar inconsistencias reales al construir el módulo Auth) — eso es exactamente el espíritu iterativo de Scrum: el diseño se refina con lo que se aprende al construir, no se congela desde el día uno.

---

## 2. Librerías y frameworks por capa

| Capa | Framework / Librería | Versión | Para qué se usa |
|---|---|---|---|
| **Presentación** (futuro) | React | — | Interfaz de usuario, un módulo por rol *(pendiente de construir)* |
| **API / Web** | Spring Web (`spring-boot-starter-web`) | 3.3.4 | Expone los endpoints REST (`@RestController`), maneja peticiones y respuestas HTTP |
| **API / Web** | Spring Validation (`spring-boot-starter-validation`) | 3.3.4 | Valida los DTOs de entrada (`@NotBlank`, `@Email`, `@Size`) antes de que lleguen a la lógica de negocio |
| **API / Web** | SpringDoc OpenAPI (`springdoc-openapi-starter-webmvc-ui`) | 2.6.0 | Genera automáticamente la documentación interactiva de la API (Swagger UI) a partir de los controladores |
| **Seguridad** (transversal) | Spring Security (`spring-boot-starter-security`) | 3.3.4 | Autenticación, autorización por rol, cifrado de contraseñas (BCrypt) |
| **Seguridad** (transversal) | JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) | 0.12.6 | Generación y validación de tokens JWT para mantener la sesión sin estado |
| **Negocio / Servicio** | Spring (`@Service`, incluido en Spring Boot) | 3.3.4 | Orquesta las reglas de negocio (ej. "validar código de institución antes de afiliar a un estudiante") |
| **Acceso a datos** | Spring Data JPA (`spring-boot-starter-data-jpa`) | 3.3.4 | Abstrae el acceso a la base de datos mediante repositorios (`JpaRepository`), evitando escribir SQL manual para operaciones básicas |
| **Base de datos** | MySQL Connector/J | 8.3.0 | Driver JDBC que permite a la aplicación Java comunicarse con el motor MySQL |
| **Base de datos** | MySQL Server | 8.x | Motor de base de datos relacional donde vive el esquema de 51 tablas |
| **Transversal (todas las capas)** | Lombok | (gestionada por Spring Boot) | Genera automáticamente getters, setters, constructores y builders, reduciendo código repetitivo en entidades y DTOs |
| **Pruebas** | Spring Boot Test + Spring Security Test | 3.3.4 | Base para escribir pruebas unitarias y de integración de controladores, servicios y seguridad |
| **Pruebas** | H2 Database | (en memoria) | Base de datos en memoria usada solo durante las pruebas automatizadas, para no afectar la base de datos real de desarrollo |

**Nota sobre la capa de Presentación:** el frontend en React aún no se ha construido (queda como siguiente gran bloque del proyecto); los prototipos HTML/CSS/JS ya entregados sirvieron como especificación funcional y de diseño para ese futuro desarrollo, tal como se explicó en su momento.

---

## 3. Ambiente de pruebas (nota preliminar)

El proyecto ya incluye, desde el `pom.xml`, las dependencias necesarias para pruebas unitarias y de integración (`spring-boot-starter-test`, `spring-security-test`, `h2`), aunque **las pruebas propiamente dichas aún no se han escrito** — corresponden al punto 19 del checklist original y se desarrollarán junto con cada módulo de código, no como una tarea aparte al final.

**Cómo estará organizado ese ambiente cuando se implemente:**
- Las pruebas usarán una base de datos **H2 en memoria**, completamente aislada de la base de datos real de desarrollo (`biolearn_db` en MySQL) — así ninguna prueba automatizada puede borrar o corromper datos reales por accidente.
- Cada prueba se ejecutará de forma independiente, con datos de prueba propios (no depende de que existan filas específicas insertadas manualmente).
- Se documentará, junto con las pruebas de cada módulo, qué casos cubre cada una (caso exitoso, caso de error de validación, caso de regla de negocio violada), retomando los mismos criterios de aceptación ya definidos en las historias de usuario.
