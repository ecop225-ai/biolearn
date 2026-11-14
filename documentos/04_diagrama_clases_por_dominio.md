# BioLearn — Diagrama de clases dividido por dominio

El mismo diagrama de clases del documento de arquitectura, separado en los mismos 8 dominios usados en el diccionario de datos y el diagrama ER — para que sea más fácil de leer que la versión única con las 30 clases juntas. Las clases marcadas aparecen también en otro dominio porque son el punto de conexión entre ambos (se resalta en el texto).

## Dominio 1: Usuarios Seguridad Perfiles

*Se conecta con: `AporteForo`, `Asistencia`, `Certificado`, `Curso`, `EntregaActividad`, `EventoVirtual`, `Inscripcion`, `IntentoEvaluacion`, `Mensaje`, `Notificacion`, `Pago`, `Reembolso` (de otro dominio).*

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
    class Institucion {
        +Long id
        +String nombre
        +String nit
        +String rector
        +String codigoAfiliacion
        +Integer cuposContratados
        +Integer cuposUsados
        +EstadoInstitucion estado
    }
    class AporteForo {
        +Long id
        +String contenido
        +LocalDateTime fecha
        +Integer likes
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
    class EntregaActividad {
        +Long id
        +String archivoUrl
        +LocalDateTime fecha
        +EstadoEntrega estado
        +BigDecimal calificacion
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
    class IntentoEvaluacion {
        +Long id
        +BigDecimal puntaje
        +LocalDateTime fecha
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
    class Pago {
        +Long id
        +BigDecimal monto
        +String metodo
        +String referencia
        +EstadoPago estado
    }
    class Reembolso {
        +Long id
        +BigDecimal monto
        +String motivo
        +EstadoReembolso estado
        +String justificacion
    }
    Usuario <|-- Administrador
    Usuario <|-- Docente
    Usuario <|-- Contador
    Usuario <|-- Supervisor
    Usuario <|-- Estudiante
    Estudiante "*" --> "0..1" Institucion : afiliado a
    Supervisor "*" --> "1" Institucion : representa
    Curso "1" --> "1" Docente
    EntregaActividad "*" --> "1" Estudiante
    IntentoEvaluacion "*" --> "1" Estudiante
    AporteForo "*" --> "1" Usuario : autor
    Estudiante "1" --> "*" Inscripcion
    Estudiante "1" --> "*" Certificado
    Estudiante "1" --> "*" Pago
    Reembolso "*" --> "0..1" Contador : resuelto por
    Docente "1" --> "*" EventoVirtual : organiza
    Estudiante "1" --> "*" Asistencia
    Usuario "1" --> "*" Mensaje : envía
    Usuario "1" --> "*" Notificacion
```

## Dominio 2: Cursos Contenido Academico

*Se conecta con: `Actividad`, `Docente`, `Evaluacion`, `EventoVirtual`, `Foro`, `Inscripcion`, `Pago`, `ProgresoLeccion`, `SesionEnLinea` (de otro dominio).*

```mermaid
classDiagram
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
    class AreaAcademica {
        +Long id
        +String codigo
        +String nombre
    }
    class Actividad {
        +Long id
        +String nombre
        +String instrucciones
        +LocalDateTime fechaLimite
        +BigDecimal puntajeMax
    }
    class Docente {
        +String especialidad
        +String biografia
    }
    class Evaluacion {
        +Long id
        +String nombre
        +Integer intentosPermitidos
        +Integer tiempoLimiteMin
    }
    class EventoVirtual {
        +Long id
        +String nombre
        +LocalDateTime fechaInicio
        +Boolean emiteCertificado
    }
    class Foro {
        +Long id
        +String nombre
        +String tema
        +EstadoForo estado
        +LocalDate fechaCierre
    }
    class Inscripcion {
        +Long id
        +LocalDate fechaInscripcion
    }
    class Pago {
        +Long id
        +BigDecimal monto
        +String metodo
        +String referencia
        +EstadoPago estado
    }
    class ProgresoLeccion {
        +Long id
        +EstadoProgreso estado
        +Integer avancePct
    }
    class SesionEnLinea {
        +Long id
        +String nombre
        +LocalDateTime fechaInicio
        +String plataforma
        +String codigoAsistencia
    }
    Curso "*" --> "1" AreaAcademica
    Curso "1" --> "*" Leccion
    Curso "1" --> "1" Docente
    Leccion "1" --> "*" Material
    Leccion "1" --> "*" Actividad
    Leccion "1" --> "0..1" Evaluacion
    Leccion "1" --> "0..1" Foro
    Leccion "1" --> "0..1" SesionEnLinea
    Inscripcion "*" --> "1" Curso
    ProgresoLeccion "*" --> "1" Leccion
    Pago "*" --> "1" Curso
    Leccion "1" --> "0..*" EventoVirtual
```

## Dominio 3: Actividades Evaluaciones

*Se conecta con: `Estudiante`, `Leccion` (de otro dominio).*

```mermaid
classDiagram
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
    class Estudiante {
        +TipoEstudiante tipoEstudiante
    }
    class Leccion {
        +Long id
        +String nombre
        +Integer orden
        +EstadoLeccion estado
    }
    Leccion "1" --> "*" Actividad
    Leccion "1" --> "0..1" Evaluacion
    Actividad "1" --> "*" EntregaActividad
    EntregaActividad "*" --> "1" Estudiante
    Evaluacion "1" --> "*" IntentoEvaluacion
    IntentoEvaluacion "*" --> "1" Estudiante
```

## Dominio 4: Foros

*Se conecta con: `Leccion`, `Usuario` (de otro dominio).*

```mermaid
classDiagram
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
    class Leccion {
        +Long id
        +String nombre
        +Integer orden
        +EstadoLeccion estado
    }
    class Usuario {
        <<abstract>>
        +Long id
        +String nombre
        +String email
        +String passwordHash
        +EstadoUsuario estado
        +LocalDate fechaRegistro
    }
    Leccion "1" --> "0..1" Foro
    Foro "1" --> "*" AporteForo
    AporteForo "*" --> "1" Usuario : autor
    AporteForo "0..1" --> "*" AporteForo : respuestas
```

## Dominio 5: Sesiones Eventos Certificados

*Se conecta con: `Docente`, `Estudiante`, `Leccion` (de otro dominio).*

```mermaid
classDiagram
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
    class Docente {
        +String especialidad
        +String biografia
    }
    class Estudiante {
        +TipoEstudiante tipoEstudiante
    }
    class Leccion {
        +Long id
        +String nombre
        +Integer orden
        +EstadoLeccion estado
    }
    Leccion "1" --> "0..1" SesionEnLinea
    Estudiante "1" --> "*" Certificado
    Certificado "*" --> "0..1" PlantillaCertificado
    Docente "1" --> "*" EventoVirtual : organiza
    Leccion "1" --> "0..*" EventoVirtual
    EventoVirtual "1" --> "*" Certificado : origina
    Estudiante "1" --> "*" Asistencia
    SesionEnLinea "1" --> "*" Asistencia
    EventoVirtual "1" --> "*" Asistencia
```

## Dominio 6: Inscripciones Progreso

*Se conecta con: `Curso`, `Estudiante`, `Leccion` (de otro dominio).*

```mermaid
classDiagram
    class Inscripcion {
        +Long id
        +LocalDate fechaInscripcion
    }
    class ProgresoLeccion {
        +Long id
        +EstadoProgreso estado
        +Integer avancePct
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
    class Estudiante {
        +TipoEstudiante tipoEstudiante
    }
    class Leccion {
        +Long id
        +String nombre
        +Integer orden
        +EstadoLeccion estado
    }
    Estudiante "1" --> "*" Inscripcion
    Inscripcion "*" --> "1" Curso
    Inscripcion "1" --> "*" ProgresoLeccion
    ProgresoLeccion "*" --> "1" Leccion
```

## Dominio 7: Finanzas

*Se conecta con: `Contador`, `Curso`, `Estudiante` (de otro dominio).*

```mermaid
classDiagram
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
    class DatosFacturacion {
        +Long id
        +String razonSocial
        +String nit
        +String logoUrl
    }
    class Contador {
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
    class Estudiante {
        +TipoEstudiante tipoEstudiante
    }
    Estudiante "1" --> "*" Pago
    Pago "*" --> "1" Curso
    Pago "1" --> "0..1" Factura
    Factura "*" --> "0..1" DatosFacturacion
    Pago "1" --> "0..1" Reembolso
    Reembolso "*" --> "0..1" Contador : resuelto por
```

## Dominio 8: Comunicacion Reportes

*Se conecta con: `Usuario` (de otro dominio).*

```mermaid
classDiagram
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
    class Usuario {
        <<abstract>>
        +Long id
        +String nombre
        +String email
        +String passwordHash
        +EstadoUsuario estado
        +LocalDate fechaRegistro
    }
    Usuario "1" --> "*" Mensaje : envía
    Usuario "1" --> "*" Notificacion
```
