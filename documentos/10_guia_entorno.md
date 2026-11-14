# BioLearn — Guía de configuración del entorno (Paso 4)

Esto se hace en tu máquina — te indico exactamente qué instalar y en qué orden. Ya adelanté lo que sí puedo hacer por ti: el **proyecto Spring Boot inicial** (descárgalo abajo) y el **script SQL completo**.

---

## 1. Instalar el JDK (Java Development Kit)

Necesitas **JDK 17** o superior (usamos JDK 17 en el `pom.xml`).

- Descarga: [https://adoptium.net](https://adoptium.net) → elige "Temurin 17 (LTS)" para tu sistema operativo.
- Instala normalmente (siguiente, siguiente, finalizar).
- Verifica que quedó instalado abriendo una terminal y escribiendo:
  ```
  java -version
  ```
  Debe mostrar algo como `openjdk version "17..."`.

## 2. Instalar el IDE — IntelliJ IDEA

Recomendado sobre otros IDEs porque tiene el mejor soporte para Spring Boot (autocompletado de anotaciones, ejecución con un clic, integración con Maven sin configurar nada aparte).

- Descarga la edición **Community** (gratuita, alcanza de sobra): [https://www.jetbrains.com/idea/download](https://www.jetbrains.com/idea/download)
- Instálalo normalmente.
- Al abrirlo por primera vez, puedes saltar la creación de cuenta (no es obligatoria para la edición Community).

## 3. Instalar Git

- Windows: [https://git-scm.com/download/win](https://git-scm.com/download/win) (deja todas las opciones por defecto durante la instalación).
- Mac: si tienes Homebrew, `brew install git`; si no, [https://git-scm.com/download/mac](https://git-scm.com/download/mac).
- Verifica: `git --version` en una terminal.

Configura tu identidad (una sola vez, en cualquier proyecto):
```bash
git config --global user.name "Tu Nombre"
git config --global user.email "tu_correo@ejemplo.com"
```

## 4. Crear el repositorio (GitHub)

1. Crea una cuenta en [github.com](https://github.com) si no tienes.
2. Clic en **New repository** → nómbralo `biolearn-backend` → **no** marques "Add a README" (ya traemos uno) → **Create repository**.
3. GitHub te mostrará una URL como `https://github.com/tu-usuario/biolearn-backend.git` — la usas en el paso 6.

## 5. Descargar y abrir el proyecto inicial

Descarga el archivo `biolearn-backend-starter.zip` de este chat y descomprímelo donde quieras trabajar.

Contiene:
```
biolearn-backend/
 ├── pom.xml                          → dependencias (Spring Web, JPA, Security, JWT, MySQL...)
 ├── schema.sql                       → script completo de las 51 tablas (diccionario de datos)
 ├── README.md                        → instrucciones del proyecto
 ├── .gitignore                       → ya configurado para no subir claves ni carpetas generadas
 └── src/main/java/co/biolearn/
      ├── BiolearnBackendApplication.java   → clase principal, ya lista
      └── config, security, controller, dto, service, service/impl,
          repository, model, enums, exception, util   → paquetes vacíos, listos para tu código
```

Abre IntelliJ → **File → Open** → selecciona la carpeta `biolearn-backend` (la que tiene el `pom.xml` adentro). IntelliJ va a reconocerlo como proyecto Maven automáticamente y empezará a descargar las dependencias — espera a que termine (barra de progreso abajo a la derecha).

## 6. Conectar el proyecto a Git

Desde una terminal, dentro de la carpeta `biolearn-backend`:
```bash
git init
git add .
git commit -m "chore: estructura inicial del proyecto Spring Boot"
git branch -M main
git remote add origin https://github.com/tu-usuario/biolearn-backend.git
git push -u origin main
```

**Recomendación de flujo de trabajo** (encaja con que ya tienes historias de usuario en formato ágil): crea una rama por cada historia de usuario que vayas a implementar, ej. `git checkout -b feature/HU-login`, y solo une (`merge` o *pull request*) a `main` cuando esa funcionalidad esté probada.

## 7. Instalar MySQL

- Descarga **MySQL Community Server**: [https://dev.mysql.com/downloads/mysql](https://dev.mysql.com/downloads/mysql)
- Durante la instalación te pedirá una contraseña para el usuario `root` — anótala, la necesitas ahora.
- Instala también **MySQL Workbench** (viene junto en el instalador de Windows/Mac) — es la interfaz visual para ver tus tablas y datos.

## 8. Crear la base de datos con los scripts del proyecto

`schema.sql` solo crea las tablas: la base de datos y el usuario se crean antes. Con MySQL corriendo, desde la carpeta `backend/`:

```bash
# 1. Base de datos y usuario de la aplicación (cambia la clave por una tuya)
mysql -u root -p -e "CREATE DATABASE biolearn_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE USER 'biolearn_user'@'localhost' IDENTIFIED BY 'tu_clave';
  GRANT ALL PRIVILEGES ON biolearn_db.* TO 'biolearn_user'@'localhost';"

# 2. Las 51 tablas del modelo de datos
mysql -u root -p biolearn_db < schema.sql

# 3. Datos iniciales: grados, áreas, administrador inicial e instituciones de prueba
mysql -u root -p < sql/datos_iniciales.sql
```

El administrador inicial queda como `admin@biolearn.co` / `Admin2026`: **cámbiale la contraseña** después de ingresar (desde "Olvidé contraseña").

**Si ya tenías la base creada con una versión anterior de `schema.sql`**, no la borres: aplica solo las migraciones pendientes de `sql/migraciones/`, en orden, y luego `datos_iniciales.sql` (no duplica nada de lo que ya exista):
```bash
mysql -u root -p < sql/migraciones/001_cupos_institucion.sql
mysql -u root -p < sql/datos_iniciales.sql
```

## 9. Conectar el proyecto a tu base de datos

1. Dentro de `src/main/resources/`, copia `application.properties.example` y renombra la copia a `application.properties`.
2. Ábrelo y reemplaza `biolearn_user` / `CAMBIA_ESTA_CLAVE` por tus datos reales, y `biolearn.jwt.secret` por una clave larga inventada por ti.
3. Este archivo **no se sube a Git** (ya está en `.gitignore`) porque tiene tus credenciales.

## 10. Primera ejecución

En IntelliJ, abre `BiolearnBackendApplication.java` y presiona el botón ▶ verde junto a la clase (o `Ctrl+Shift+F10`). Si todo quedó bien conectado, en la consola verás algo como:
```
Tomcat started on port 8080
Started BiolearnBackendApplication in X seconds
```

Abre `http://localhost:8080/swagger-ui.html` en el navegador — aunque todavía no hay endpoints propios, confirma que el servidor está vivo.

---

## Checklist de este paso

- [ ] JDK 17 instalado (`java -version` funciona)
- [ ] IntelliJ IDEA instalado
- [ ] Git instalado y configurado
- [ ] Repositorio creado en GitHub
- [ ] Proyecto descargado, abierto en IntelliJ, dependencias descargadas
- [ ] Primer commit y push realizados
- [ ] MySQL Server + Workbench instalados
- [ ] `schema.sql` y `sql/datos_iniciales.sql` ejecutados sin errores
- [ ] `application.properties` creado con tus credenciales reales
- [ ] La aplicación arranca sin errores en el puerto 8080

Cuando tengas todo esto listo, seguimos con el **paso 5: codificar el módulo de Auth** (login, registro, JWT) para probar la arquitectura completa de punta a punta.
