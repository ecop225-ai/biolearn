# BioLearn — Mapa de navegación

Basado directamente en los menús (`NAV`) que ya implementamos y probamos en cada uno de los 6 prototipos.

## Punto de entrada único

```mermaid
graph TD
    START([Usuario abre BioLearn]) --> LOGIN[Iniciar sesión]
    LOGIN -->|Olvidé contraseña| REC1[Recuperar: solicitar correo]
    REC1 --> REC2[Correo enviado]
    REC2 --> REC3[Crear nueva contraseña]
    REC3 --> LOGIN
    LOGIN -->|Crear usuario| SIGNUP[Registro de estudiante]
    SIGNUP --> ACT[Activar cuenta]
    LOGIN -->|Activar cuenta| ACT
    LOGIN -.->|Cuenta pendiente| ACT
    ACT -->|Estudiante: token de verificación| LOGIN
    ACT -->|Personal: token de invitación + contraseña| LOGIN
    ACT -.->|Enlace expirado| REENV[Reenviar enlace de verificación]
    REENV --> ACT
    LOGIN -->|Credenciales válidas| ROUTER{Redirige según rol}
    ROUTER -->|ADMIN| ADM[Panel Administrador]
    ROUTER -->|DOCENTE| DOC[Panel Docente]
    ROUTER -->|CONTADOR| CON[Panel Contador]
    ROUTER -->|SUPERVISOR| SUP[Panel Representante]
    ROUTER -->|ESTUDIANTE| EST[Panel Estudiante]
```

---

## Administrador
```mermaid
graph LR
    ADM[Panel Admin] --> A1[Inicio]
    ADM --> A2[Usuarios]
    ADM --> A3[Instituciones]
    ADM --> A4[Catálogos académicos]
    ADM --> A5[Cursos]
    ADM --> A6[Reportes]
    ADM --> A7[Mensajería]
    ADM --> A8[Mi perfil]
    A2 -.-> A2a[Crear/editar usuario]
    A2 -.-> A2b[Invitaciones pendientes]
    A3 -.-> A3a[Registrar institución]
    A3 -.-> A3b[Asignar representante]
    A5 -.-> A5a[Editar / publicar / archivar curso]
    A8 -.-> A8a[Datos de facturación]
```

## Docente
```mermaid
graph LR
    DOC[Panel Docente] --> D1[Inicio]
    DOC --> D2[Cursos]
    DOC --> D3[Lecciones]
    DOC --> D4[Materiales]
    DOC --> D5[Actividades]
    DOC --> D6[Evaluaciones]
    DOC --> D7[Foros]
    DOC --> D8[Eventos]
    DOC --> D9[Sesiones en línea]
    DOC --> D10[Estudiantes]
    DOC --> D11[Certificados]
    DOC --> D12[Reportes]
    DOC --> D13[Mensajería]
    DOC --> D14[Mi perfil]
    D2 --> D2a[Detalle del curso → Lecciones]
    D2a --> D3
```

## Contador
```mermaid
graph LR
    CON[Panel Contador] --> C1[Inicio]
    CON --> C2[Pagos]
    CON --> C3[Facturas]
    CON --> C4[Reembolsos]
    CON --> C5[Reportes]
    CON --> C6[Mensajería]
    CON --> C7[Mi perfil]
    C2 -.confirma pago.-> C3
    C4 -.aprueba.-> C3
```

## Representante institucional (Supervisor)
```mermaid
graph LR
    SUP[Panel Representante] --> S1[Inicio]
    SUP --> S2[Cursos inscritos · solo lectura]
    SUP --> S3[Estudiantes]
    SUP --> S4[Foros]
    SUP --> S5[Reportes]
    SUP --> S6[Mi institución]
    SUP --> S7[Mensajería]
    SUP --> S8[Mi perfil]
    S3 --> S3a[Detalle del estudiante]
```

## Estudiante
```mermaid
graph LR
    EST[Panel Estudiante] --> E1[Inicio]
    EST --> E2[Mis cursos]
    EST --> E3[Actividades]
    EST --> E4[Evaluaciones]
    EST --> E5[Foros]
    EST --> E6[Sesiones en línea]
    EST --> E7[Eventos]
    EST --> E8[Certificados]
    EST --> E9[Mis notas]
    EST --> E10[Mi reporte]
    EST --> E11[Mensajería]
    EST --> E12[Mis pagos]
    EST --> E13[Mi perfil]
    EST --> E14[Catálogo]
    E2 --> E2a[Curso → Lección]
    E2a --> E2b[Materiales / Actividad / Evaluación / Sesión / Foro]
    E14 --> E14a[Carrito → Checkout → Pago]
    E14a --> E2
```

---

## Reglas de navegación transversales (ya validadas en los prototipos)

- Una **lección se bloquea** hasta completar la anterior (Estudiante).
- Un **estudiante independiente** no ve `Mi institución`; solo aparece si está afiliado.
- **Docente, Contador y Supervisor** llegan al sistema únicamente por invitación — no pasan por la pantalla de registro público.
- Cerrar sesión desde cualquier módulo regresa siempre a `Iniciar sesión`.
