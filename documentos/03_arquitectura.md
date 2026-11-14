# BioLearn — Arquitectura del sistema

**Stack definido:** Java + Spring Boot (backend) · MySQL (base de datos) · React (frontend)

Este documento traduce lo ya validado en los prototipos (roles, flujos, reglas de negocio) a una arquitectura formal en capas, con sus diagramas de paquetes, clases y componentes.

---

## 1. Arquitectura en capas

| Capa | Responsabilidad | Tecnología | Ejemplos de BioLearn |
|---|---|---|---|
| **Presentación** | Interfaz de usuario, un módulo por rol | React (SPA) + React Router | Pantallas de Docente, Estudiante, Admin, etc. |
| **API / Web** | Expone los endpoints REST, valida entrada, traduce HTTP ↔ objetos internos | Spring Boot (`@RestController`), DTOs | `POST /api/cursos`, `GET /api/pagos/{id}` |
| **Seguridad (transversal)** | Autenticación y autorización | Spring Security + JWT | Login, control de acceso por rol |
| **Negocio / Servicio** | Reglas de negocio, orquestación | Spring (`@Service`) | "Al aprobar un pago, emitir factura"; "bloquear lección hasta completar la anterior" |
| **Acceso a datos** | Persistencia, consultas | Spring Data JPA (`@Repository`) | `CursoRepository`, `PagoRepository` |
| **Base de datos** | Almacenamiento | MySQL | Tablas: usuarios, cursos, pagos, facturas... |

**Por qué esta separación:** cada capa solo conoce a la que tiene justo debajo (el Controller no le habla directo a la base de datos, por ejemplo). Esto es lo que en tu lista corresponde a *"identificar las capas"* y es la base para justificar el *diagrama de componentes* y los *patrones de diseño* (ver sección 5).

---

## 2. Diagrama de paquetes

Organización del código fuente del backend (`co.biolearn`), reflejando las capas de arriba:

```mermaid
graph TD
    A[co.biolearn] --> B[config]
    A --> C[security]
    A --> D[controller]
    A --> E[dto]
    A --> F[service]
    F --> F1[service.impl]
    A --> G[repository]
    A --> H[model]
    A --> I[enums]
    A --> J[exception]
    A --> K[util]

    D -.usa.-> E
    D -.usa.-> F
    F1 -.usa.-> G
    F1 -.usa.-> H
    G -.usa.-> H
    C -.protege.-> D

    style A fill:#2C4A42,color:#fff
    style D fill:#E6EDE9
    style F fill:#E6EDE9
    style G fill:#E6EDE9
```

| Paquete | Contiene |
|---|---|
| `config` | Configuración de Spring: CORS, Swagger/OpenAPI, beans generales |
| `security` | `JwtUtil`, filtro de autenticación, `UserDetailsService` |
| `controller` | Un controlador REST por módulo: `UsuarioController`, `CursoController`, `PagoController`, `ReembolsoController`, `ForoController`... |
| `dto` | Objetos de entrada/salida de la API (nunca se exponen las entidades JPA directamente) |
| `service` / `service.impl` | Interfaces de negocio y su implementación |
| `repository` | Interfaces `JpaRepository` por entidad |
| `model` | Entidades JPA (`@Entity`) |
| `enums` | `RolUsuario`, `EstadoCurso`, `EstadoPago`, `EstadoInvitacion`... |
| `exception` | Excepciones propias + `GlobalExceptionHandler` |
| `util` | Mappers entidad↔DTO, constantes |

---

## 3. Diagrama de clases

Cubre las entidades principales de los 5 módulos de rol. Se simplificaron algunas tablas de detalle (opciones de pregunta, destinatarios de mensaje) para legibilidad; quedan documentadas en el diccionario de datos que hacemos en el siguiente paso.

**Usuario y sus roles:** en vez de un único `Usuario` con un campo `rol`, se modela como **generalización**: `Usuario` es la clase base abstracta con los datos comunes (credenciales, nombre, estado), y `Administrador`, `Docente`, `Contador`, `Supervisor` y `Estudiante` son subclases que heredan de ella y agregan solo lo que les es propio (ej. `Estudiante` tiene `tipoEstudiante`, `Supervisor` está ligado a una `Institucion`). En el `schema.sql` definitivo esto se traduce con una **tabla de perfil por rol** (`perfil_docente`, `perfil_estudiante`, `perfil_contador`, `perfil_admin`, `supervisor_institucion`), cada una con su **propio ID autoincremental** y una columna `id_usuario` con restricción `UNIQUE` que la enlaza 1 a 1 con `usuario` (en JPA/Hibernate, una relación `@OneToOne` con llave foránea única, en vez de la estrategia `JOINED` de PK compartida). La ventaja frente a una sola tabla plana se mantiene igual: la base de datos garantiza por diseño que, por ejemplo, solo un estudiante real pueda aparecer en una `inscripcion`.

```mermaid
classDiagram
    class Usuario {
        <<abstract>>
        +Long id
        +String nombre
        +String email
        +String passwordHash
        +EstadoUsuario estado
        +LocalDate fechaRegistro
    }
    class Administrador {
    }
    class Docente {
        +String especialidad
        +String biografia
    }
    class Contador {
    }
    class Supervisor {
    }
    class Estudiante {
        +TipoEstudiante tipoEstudiante
    }
    Usuario <|-- Administrador
    Usuario <|-- Docente
    Usuario <|-- Contador
    Usuario <|-- Supervisor
    Usuario <|-- Estudiante
    Estudiante "*" --> "0..1" Institucion : afiliado a
    Supervisor "*" --> "1" Institucion : representa
    class Institucion {
        +Long id
        +String nombre
        +String nit
        +String rector
        +String logoUrl
        +String codigoAfiliacion
        +Integer cuposContratados
        +Integer cuposUsados
        +EstadoInstitucion estado
    }
    class AreaAcademica {
        +Long id
        +String codigo
        +String nombre
    }
    class Curso {
        +Long id
        +String codigo
        +String nombre
        +String grado
        +BigDecimal precio
        +Integer creditos
        +Integer horas
        +EstadoCurso estado
    }
    class Leccion {
        +Long id
        +String nombre
        +Integer orden
        +EstadoLeccion estado
    }
    class Material {
        +Long id
        +TipoMaterial tipo
        +String nombre
        +String contenidoUrl
    }
    class Actividad {
        +Long id
        +String nombre
        +String instrucciones
        +LocalDateTime fechaLimite
        +BigDecimal puntajeMax
    }
    class EntregaActividad {
        +Long id
        +String archivoUrl
        +LocalDateTime fecha
        +EstadoEntrega estado
        +BigDecimal calificacion
    }
    class Evaluacion {
        +Long id
        +String nombre
        +Integer intentosPermitidos
        +Integer tiempoLimiteMin
    }
    class IntentoEvaluacion {
        +Long id
        +BigDecimal puntaje
        +LocalDateTime fecha
    }
    class Foro {
        +Long id
        +String nombre
        +String tema
        +EstadoForo estado
        +LocalDate fechaCierre
    }
    class AporteForo {
        +Long id
        +String contenido
        +LocalDateTime fecha
        +Integer likes
    }
    class SesionEnLinea {
        +Long id
        +String nombre
        +LocalDateTime fechaInicio
        +String plataforma
        +String codigoAsistencia
    }
    class EventoVirtual {
        +Long id
        +String nombre
        +LocalDateTime fechaInicio
        +Boolean emiteCertificado
    }
    class Inscripcion {
        +Long id
        +LocalDate fechaInscripcion
    }
    class ProgresoLeccion {
        +Long id
        +EstadoProgreso estado
        +Integer avancePct
    }
    class Asistencia {
        +Long id
        +Boolean asistio
        +String codigoIngresado
    }
    class Certificado {
        +Long id
        +String codigo
        +String origen
        +TipoCertificado tipo
        +LocalDate emitidoEn
    }
    class PlantillaCertificado {
        +Long id
        +String colorAcento
        +String logoUrl
        +String firmaRepresentanteNombre
        +String textoReconocimiento
    }
    class DatosFacturacion {
        +Long id
        +String razonSocial
        +String nit
        +String logoUrl
    }
    class Pago {
        +Long id
        +BigDecimal monto
        +String metodo
        +String referencia
        +EstadoPago estado
    }
    class Factura {
        +Long id
        +String numero
        +BigDecimal monto
        +BigDecimal pagado
        +EstadoFactura estadoPago
    }
    class Reembolso {
        +Long id
        +BigDecimal monto
        +String motivo
        +EstadoReembolso estado
        +String justificacion
    }
    class Mensaje {
        +Long id
        +String asunto
        +String cuerpo
        +LocalDateTime fecha
    }
    class Notificacion {
        +Long id
        +String texto
        +Boolean leido
    }

    Curso "*" --> "1" AreaAcademica
    Curso "1" --> "*" Leccion
    Curso "1" --> "1" Docente
    Leccion "1" --> "*" Material
    Leccion "1" --> "*" Actividad
    Leccion "1" --> "0..1" Evaluacion
    Leccion "1" --> "0..1" Foro
    Leccion "1" --> "0..1" SesionEnLinea
    Actividad "1" --> "*" EntregaActividad
    EntregaActividad "*" --> "1" Estudiante
    Evaluacion "1" --> "*" IntentoEvaluacion
    IntentoEvaluacion "*" --> "1" Estudiante
    Foro "1" --> "*" AporteForo
    AporteForo "*" --> "1" Usuario : autor
    AporteForo "0..1" --> "*" AporteForo : respuestas
    Estudiante "1" --> "*" Inscripcion
    Inscripcion "*" --> "1" Curso
    Inscripcion "1" --> "*" ProgresoLeccion
    ProgresoLeccion "*" --> "1" Leccion
    Estudiante "1" --> "*" Certificado
    Certificado "*" --> "0..1" PlantillaCertificado
    Estudiante "1" --> "*" Pago
    Pago "*" --> "1" Curso
    Pago "1" --> "0..1" Factura
    Factura "*" --> "0..1" DatosFacturacion
    Pago "1" --> "0..1" Reembolso
    Reembolso "*" --> "0..1" Contador : resuelto por
    Docente "1" --> "*" EventoVirtual : organiza
    Leccion "1" --> "0..*" EventoVirtual
    EventoVirtual "1" --> "*" Certificado : origina
    Estudiante "1" --> "*" Asistencia
    SesionEnLinea "1" --> "*" Asistencia
    EventoVirtual "1" --> "*" Asistencia
    Usuario "1" --> "*" Mensaje : envía
    Usuario "1" --> "*" Notificacion
    class Reporte {
        +Long id
        +TipoReporte tipo
        +FormatoArchivo formato
        +LocalDateTime fechaCorte
        +String motivo
        +EstadoReporte estado
    }
    Usuario "1" --> "*" Reporte : solicita
    Reporte "*" --> "0..1" Institucion
    Reporte "*" --> "0..1" DatosFacturacion
    Reporte "*" --> "0..1" Curso
    Reporte "*" --> "0..1" Factura
    Reporte "*" --> "*" Certificado
```

---

## 3.1 Diagrama Entidad-Relación (diseño físico)

El diagrama anterior es UML (conceptual, sin FK explícitas). Para el diseño físico de la base de datos, con llaves primarias y foráneas visibles en cada tabla —igual que se documentó en el diccionario de datos—, se generó un **diagrama ER completo y verificado, cubriendo las 45 tablas sin omitir ninguna**, construido directamente a partir de `schema.sql` para garantizar que coincida al 100% con el script real. Cada uno de los 8 dominios se validó renderizándolo, así que no es solo texto de Mermaid sin probar.

📄 Ver documento completo: **BioLearn — Diagrama ER completo (45 tablas)**

---

## 4. Diagrama de componentes

Cómo se comunican las piezas grandes del sistema (frontend, backend por capas, base de datos, y los servicios externos que en los prototipos estaban *simulados*):

```mermaid
graph LR
    subgraph Cliente
        FE[React SPA<br/>un módulo por rol]
    end
    subgraph "Backend Spring Boot"
        API[Capa API<br/>Controllers + DTOs]
        SEC[Spring Security<br/>JWT]
        SRV[Capa de Servicio<br/>Reglas de negocio]
        REPO[Capa de Acceso a Datos<br/>Spring Data JPA]
    end
    DB[(MySQL)]
    MAIL[Servicio de correo<br/>ej. SMTP / SendGrid]
    PAGO[Pasarela de pagos<br/>ej. Wompi / PSE]

    FE -->|HTTPS + JWT| API
    API --> SEC
    SEC --> SRV
    SRV --> REPO
    REPO --> DB
    SRV -.envía correos.-> MAIL
    SRV -.confirma pagos.-> PAGO

    style FE fill:#E6EDE9
    style API fill:#F3E6CE
    style SRV fill:#F3E6CE
    style REPO fill:#F3E6CE
    style DB fill:#2C4A42,color:#fff
```

**Nota:** `MAIL` y `PAGO` son justo lo que en los prototipos aparecía simulado con un `toast()` ("Factura enviada al correo...", "Pago confirmado..."). En el sistema real son integraciones externas reales que la capa de Servicio invoca.

---

## 5. Patrones de diseño que aplican (adelanto del punto 18 de tu lista)

| Patrón | Dónde se usa |
|---|---|
| **MVC** (a nivel general) | Controller (API) → Service (lógica) → Repository/Model (datos) |
| **Repository** | Toda la capa `repository`, vía Spring Data JPA |
| **DTO (Data Transfer Object)** | Para no exponer las entidades JPA directamente en la API |
| **Factory** | Creación de notificaciones/mensajes según el tipo de evento |
| **Strategy** | Cálculo de estado de lección (bloqueada/en progreso/completada) según reglas distintas por tipo de curso |
| **Observer** (vía eventos de Spring) | Ej: al aprobar un pago → emitir factura → notificar al estudiante, sin acoplar esos tres pasos directamente |
| **Singleton** | Beans de Spring por defecto (`@Service`, `@Component` son singleton) |

---

## 6. Próximos pasos sugeridos

1. **Diccionario de datos** completo (todas las columnas, tipos, tamaños, llaves foráneas) — a partir del diagrama de clases de arriba.
2. **Mecanismos de seguridad** en detalle (JWT, hash de contraseñas, control de acceso por rol, HTTPS).
3. **Mapa de navegación formal** (ya está implícito en los menús de cada prototipo — lo paso a diagrama).
4. Configuración del **entorno de desarrollo**: IDE, Git, servidor local, MySQL.
5. Empezar a codificar por el módulo más pequeño (Auth) para validar la arquitectura de punta a punta antes de escalar a los demás.
