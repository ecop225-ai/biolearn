# BioLearn — Matriz de trazabilidad: historias de usuario ↔ dominios de datos

Este documento conecta las dos clasificaciones usadas en la documentación del sistema, que usan ejes distintos a propósito:

- **Historias de usuario** → organizadas por **rol** (quién realiza la acción), como corresponde a la metodología ágil.
- **Diagrama ER / Diccionario de datos** → organizado por **dominio de datos** (de qué trata la información), como corresponde al diseño de base de datos.

Un mismo rol usa varios dominios de datos, y un mismo dominio lo usan varios roles — por eso las dos clasificaciones no coinciden entre sí, y esta matriz es la que las conecta explícitamente.

---

| Módulo de historias de usuario (rol) | Dominios de datos que involucra |
|---|---|
| **Administrador** | 1. Usuarios, seguridad y perfiles · 2. Cursos y contenido académico (catálogo/áreas) · 7. Finanzas (datos de facturación) |
| **Docente** | 2. Cursos y contenido académico · 3. Actividades y evaluaciones · 4. Foros · 5. Sesiones, eventos y certificados · 8. Comunicación y reportes |
| **Contador** | 1. Usuarios (activación de su propio perfil) · 7. Finanzas |
| **Representante institucional (Supervisor)** | 1. Usuarios (solo su institución) · 2, 3, 4. Cursos/Actividades/Foros (solo lectura) · 6. Inscripciones y progreso |
| **Docente institucional** | Igual que Docente, acotado a los cursos de su institución |
| **Estudiante** | 2 a 7 casi por completo: Cursos, Actividades, Foros, Sesiones/eventos/certificados, Inscripciones y progreso, Finanzas |
| **Mensajería (todos los roles)** | 8. Comunicación y reportes |
| **Retroalimentación** | 3. Actividades y evaluaciones · 4. Foros · 8. Comunicación y reportes (notificaciones) |
| **Perfil y Seguridad (todos los roles)** | 1. Usuarios, seguridad y perfiles por rol |

---

**Nota:** ningún módulo de historias de usuario mapea 1 a 1 con un solo dominio de datos, y eso es esperado — confirma que el modelo de datos está bien normalizado (no duplica estructuras por rol) y que las historias de usuario capturan bien la perspectiva de cada actor sobre un sistema compartido.