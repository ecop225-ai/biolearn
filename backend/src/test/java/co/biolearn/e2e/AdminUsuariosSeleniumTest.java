package co.biolearn.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pruebas de extremo a extremo (E2E) con Selenium WebDriver sobre la sección "Gestión de usuarios"
 * del prototipo del Administrador (frontend/administrador), conectada al backend real.
 * Implementa los 17 casos del plan de pruebas EV02 (CP-U01 a CP-U17).
 *
 * Requisitos antes de ejecutar (los mismos de AuthSeleniumTest):
 *   1. MySQL encendido con biolearn_db y los datos de sql/datos_iniciales.sql.
 *   2. BiolearnBackendApplication corriendo en http://localhost:8080.
 *   3. El frontend servido en http://127.0.0.1:5173 (Live Server o Vite).
 *   4. Google Chrome instalado (Selenium Manager descarga el driver solo).
 *
 * Datos que crean las pruebas: cada ejecución crea docentes de prueba con una marca de tiempo en el
 * correo (ej. laura.prueba.1696358400123@biolearn.co), así nunca chocan con datos anteriores.
 *
 * CP-U16 necesita una cuenta ACTIVA que no sea de administrador. Por defecto usa la estudiante
 * maceron@test.com / Clave1234; se cambia con -Dbiolearn.noadmin.usuario=... y -Dbiolearn.noadmin.clave=...
 * Si esa cuenta no puede iniciar sesión, el caso se OMITE (no falla).
 *
 * Para cambiar de navegador (entornos ENT-02 y ENT-03) basta reemplazar ChromeDriver por
 * FirefoxDriver o EdgeDriver en crearDriver().
 *
 * Las capturas de pantalla se guardan en backend/target/capturas-selenium/ (prefijo CP-U##).
 *
 * Nota de mantenimiento: los localizadores usan //*[@id='...'] (cualquier etiqueta) y no //div[@id='...'],
 * porque el menú lateral del prototipo es un <aside>. La primera ejecución (07/10/2026) falló en 16 de 17
 * casos por ese localizador; fue un error del script de prueba, no del sistema. La segunda ejecución
 * (12/17) reveló otros dos ajustes del script: esperar a que se cierren los mensajes emergentes antes de
 * hacer clic en los botones de una fila, y ubicar las filas por el correo exacto y no por "contiene".
 * Con el módulo de Instituciones, CP-U05 verifica el selector de institución (opción deshabilitada)
 * en lugar del mensaje 409, que sigue cubierto por la prueba unitaria UsuarioServiceImplTest.
 * Con la corrección de DEF-003, CP-U16 verifica el aviso de acceso denegado y la redirección al login.
 * CP-U18 prepara sus propios datos por la API (dos instituciones y un representante) y verifica el cambio de institución desde Editar.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Selenium — Gestión de usuarios del Administrador")
class AdminUsuariosSeleniumTest {

    private static final String URL_AUTH = System.getProperty("biolearn.frontend.url",
            "http://127.0.0.1:5173/prototipo-auth/biolearn_auth_prototipo.html");
    private static final String URL_ADMIN = System.getProperty("biolearn.admin.url",
            "http://127.0.0.1:5173/administrador/biolearn_admin_prototipo.html");
    private static final String URL_API = System.getProperty("biolearn.backend.url", "http://localhost:8080");
    private static final String ADMIN_USUARIO = System.getProperty("biolearn.admin.usuario", "admin@biolearn.co");
    private static final String ADMIN_CLAVE = System.getProperty("biolearn.admin.clave", "Admin2026");
    private static final String NOADMIN_USUARIO = System.getProperty("biolearn.noadmin.usuario", "maceron@test.com");
    private static final String NOADMIN_CLAVE = System.getProperty("biolearn.noadmin.clave", "Clave1234");
    private static final boolean SIN_VENTANA = Boolean.parseBoolean(System.getProperty("biolearn.headless", "false"));

    private static final Path CARPETA_CAPTURAS = Paths.get("target", "capturas-selenium");
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(10);

    private WebDriver driver;
    private WebDriverWait espera;

    // ------------------------------------------------------------------ ciclo de vida

    @BeforeAll
    static void verificarEntorno() {
        assumeTrue(responde(URL_AUTH), "Frontend no disponible en " + URL_AUTH + ". Ábrelo con Live Server o Vite.");
        assumeTrue(responde(URL_API + "/api/auth/me"), "Backend no disponible en " + URL_API + ". Ejecuta BiolearnBackendApplication.");
    }

    @BeforeEach
    void abrirNavegador() {
        driver = crearDriver();
        espera = new WebDriverWait(driver, ESPERA_MAXIMA);
    }

    @AfterEach
    void cerrarNavegador() {
        if (driver != null) driver.quit();
    }

    private WebDriver crearDriver() {
        ChromeOptions opciones = new ChromeOptions();
        opciones.addArguments("--lang=es");
        // Con ventana, Chrome se abre maximizado; sin ventana (headless) se usa un tamaño fijo, porque no hay pantalla que maximizar
        opciones.addArguments(SIN_VENTANA ? "--window-size=1366,900" : "--start-maximized");
        if (SIN_VENTANA) opciones.addArguments("--headless=new");
        WebDriver navegador = new ChromeDriver(opciones);
        if (!SIN_VENTANA) navegador.manage().window().maximize();
        return navegador;   // ENT-02: new FirefoxDriver()  ·  ENT-03: new EdgeDriver()
    }

    // ------------------------------------------------------------------ casos de prueba

    @Test
    @Order(1)
    @DisplayName("CP-U01: la pantalla Gestión de usuarios carga el listado de la API")
    void cpU01_abrirGestionUsuarios() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();

        assertThat(driver.findElement(By.xpath("//h2[normalize-space()='Gestión de usuarios']")).isDisplayed()).isTrue();
        assertThat(texto("usr-resumen")).startsWith("Mostrando");
        assertThat(filas()).isNotEmpty();
        capturar("CP-U01_listado_usuarios");
    }

    @Test
    @Order(2)
    @DisplayName("CP-U02: crear un docente válido lo deja pendiente con invitación")
    void cpU02_crearDocenteValido() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();
        String correo = crearDocente("Laura", "Prueba");

        WebElement fila = esperarFilaConTexto(correo);
        assertThat(fila.getText()).contains("Laura Prueba").contains("Docente");
        assertThat(new Select(fila.findElement(By.tagName("select"))).getFirstSelectedOption().getText()).isEqualTo("pendiente");
        capturar("CP-U02_docente_creado");
    }

    @Test
    @Order(3)
    @DisplayName("CP-U03: crear con campos obligatorios vacíos muestra un aviso y no crea nada")
    void cpU03_crearConCamposVacios() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();
        int antes = filas().size();

        abrirModalCrear();
        driver.findElement(By.id("usr-modal-btn")).click();

        assertThat(esperarToast("Completa nombre, apellido, usuario y correo").getText()).startsWith("⚠️");
        assertThat(driver.findElement(By.id("modal-usuario")).getAttribute("class")).contains("open");
        capturar("CP-U03_campos_vacios");
        cerrarModal("modal-usuario");
        assertThat(filas()).hasSize(antes);
    }

    @Test
    @Order(4)
    @DisplayName("CP-U04: crear con un correo ya registrado muestra el error del backend (409)")
    void cpU04_crearConCorreoRegistrado() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();

        abrirModalCrear();
        llenarDatosBasicos("Correo", "Repetido", "repetido" + System.currentTimeMillis(), ADMIN_USUARIO);
        new Select(driver.findElement(By.id("usr-rol"))).selectByValue("docente");
        driver.findElement(By.id("usr-modal-btn")).click();

        WebElement aviso = esperarToast("Ya existe una cuenta registrada con ese correo");
        assertThat(aviso.getText()).startsWith("⚠️");
        capturar("CP-U04_correo_repetido");
    }

    @Test
    @Order(5)
    @DisplayName("CP-U05: una institución que ya tiene representante activo no se puede elegir para otro")
    void cpU05_segundoRepresentante() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();
        Long idInstitucion = institucionConRepresentanteActivo();
        assumeTrue(idInstitucion != null, "No hay ninguna institución con representante activo para probar la regla.");

        abrirModalCrear();
        long marca = System.currentTimeMillis();
        llenarDatosBasicos("Segundo", "Representante", "rep" + marca, "rep." + marca + "@biolearn.co");
        new Select(driver.findElement(By.id("usr-rol"))).selectByValue("supervisor");
        // Desde el módulo de Instituciones el campo es un selector que solo ofrece instituciones activas
        // y deshabilita las que ya tienen representante (el backend además responde 409 si se intenta por API).
        By opcion = By.cssSelector("#usr-institucion-id option[value='" + idInstitucion + "']");
        WebElement opt = espera.until(ExpectedConditions.presenceOfElementLocated(opcion));
        assertThat(opt.isEnabled()).isFalse();
        assertThat(opt.getText()).contains("ya tiene representante");
        capturar("CP-U05_representante_duplicado");
    }

    @Test
    @Order(6)
    @DisplayName("CP-U06: el formulario muestra solo los campos propios de cada rol")
    void cpU06_camposDinamicosPorRol() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();
        abrirModalCrear();
        Select rol = new Select(driver.findElement(By.id("usr-rol")));

        rol.selectByValue("docente");
        assertThat(visible("usr-campo-especialidad")).isTrue();
        assertThat(visible("usr-campo-rolcontador")).isFalse();
        assertThat(visible("usr-campo-institucion")).isFalse();
        capturar("CP-U06a_rol_docente");

        rol.selectByValue("contador");
        assertThat(visible("usr-campo-rolcontador")).isTrue();
        assertThat(visible("usr-campo-especialidad")).isFalse();
        capturar("CP-U06b_rol_contador");

        rol.selectByValue("supervisor");
        assertThat(visible("usr-campo-institucion")).isTrue();
        assertThat(visible("usr-campo-cargo")).isTrue();
        assertThat(visible("usr-campo-rolcontador")).isFalse();
        capturar("CP-U06c_rol_representante");
    }

    @Test
    @Order(7)
    @DisplayName("CP-U07: el filtro por rol muestra solo ese rol e informa el filtro activo")
    void cpU07_filtrarPorRol() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();

        new Select(driver.findElement(By.id("usr-f-rol"))).selectByValue("docente");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("usr-resumen"), "Rol: Docente"));

        List<WebElement> filas = filas();
        assertThat(filas).isNotEmpty();
        filas.forEach(f -> assertThat(f.findElements(By.tagName("td")).get(2).getText()).contains("Docente"));
        capturar("CP-U07_filtro_rol_docente");
    }

    @Test
    @Order(8)
    @DisplayName("CP-U08: el filtro por estado muestra solo usuarios pendientes")
    void cpU08_filtrarPorEstado() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();
        crearDocente("Pendiente", "Filtro");   // garantiza al menos un usuario pendiente

        new Select(driver.findElement(By.id("usr-f-estado"))).selectByValue("pendiente");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("usr-resumen"), "Estado: pendiente"));

        List<WebElement> filas = filas();
        assertThat(filas).isNotEmpty();
        filas.forEach(f -> assertThat(new Select(f.findElement(By.tagName("select"))).getFirstSelectedOption().getText())
                .isEqualTo("pendiente"));
        capturar("CP-U08_filtro_estado_pendiente");
    }

    @Test
    @Order(9)
    @DisplayName("CP-U09: la búsqueda ignora tildes y mayúsculas y conserva el cursor al escribir")
    void cpU09_buscarSinTildes() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();

        WebElement buscador = driver.findElement(By.id("usr-buscar"));
        buscador.click();
        buscador.sendKeys("andres");   // se escribe seguido, letra por letra, sin volver a hacer clic

        assertThat(buscador.getAttribute("value")).isEqualTo("andres");
        assertThat(driver.switchTo().activeElement().getAttribute("id")).isEqualTo("usr-buscar");
        List<WebElement> filas = filas();
        assertThat(filas).isNotEmpty();
        filas.forEach(f -> assertThat(normalizar(f.getText())).contains("andres"));
        assertThat(filas.stream().anyMatch(f -> f.getText().contains("Andrés"))).isTrue();   // encontró un nombre con tilde
        capturar("CP-U09_busqueda_sin_tildes");
    }

    @Test
    @Order(10)
    @DisplayName("CP-U10: limpiar filtros devuelve el listado completo")
    void cpU10_limpiarFiltros() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();
        int total = filas().size();

        new Select(driver.findElement(By.id("usr-f-rol"))).selectByValue("docente");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("usr-resumen"), "Filtros activos"));
        capturar("CP-U10a_con_filtro");

        boton("✕ Limpiar filtros").click();
        espera.until(d -> !texto("usr-resumen").contains("Filtros activos"));
        espera.until(d -> filas().size() == total);

        assertThat(new Select(driver.findElement(By.id("usr-f-rol"))).getFirstSelectedOption().getAttribute("value")).isEqualTo("todos");
        assertThat(driver.findElement(By.id("usr-buscar")).getAttribute("value")).isEmpty();
        capturar("CP-U10b_filtros_limpios");
    }

    @Test
    @Order(11)
    @DisplayName("CP-U11: la ventana de detalle muestra todos los datos del usuario")
    void cpU11_verDetalle() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();
        String correo = crearDocente("Detalle", "Prueba");

        clicSeguro(esperarFilaConTexto(correo).findElement(By.xpath(".//button[contains(.,'Ver')]")));
        WebElement cuerpo = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("detalle-usuario-body")));
        espera.until(ExpectedConditions.textToBePresentInElement(cuerpo, correo));

        assertThat(cuerpo.getText()).contains("Nombre completo", "Detalle Prueba", "Correo", correo,
                "Rol", "docente", "Estado", "pendiente", "Especialidad", "Registrado");
        capturar("CP-U11_detalle_usuario");
    }

    @Test
    @Order(12)
    @DisplayName("CP-U12: editar el teléfono de un docente guarda el cambio y bloquea correo, usuario y rol")
    void cpU12_editarTelefonoDocente() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();
        String correo = crearDocente("Editar", "Telefono");

        clicSeguro(esperarFilaConTexto(correo).findElement(By.xpath(".//button[contains(.,'Editar')]")));
        WebElement telefono = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("edit-telefono")));
        // El formulario de edición no ofrece correo, nombre de usuario ni rol (RF-US-04)
        assertThat(driver.findElements(By.id("edit-correo"))).isEmpty();
        assertThat(driver.findElements(By.id("edit-usuario"))).isEmpty();
        assertThat(driver.findElements(By.id("edit-rol"))).isEmpty();
        telefono.clear();
        telefono.sendKeys("3001234567");
        capturar("CP-U12a_formulario_edicion");
        driver.findElement(By.id("edit-modal-btn")).click();
        esperarToast("Usuario actualizado correctamente");

        clicSeguro(esperarFilaConTexto(correo).findElement(By.xpath(".//button[contains(.,'Ver')]")));
        WebElement cuerpo = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("detalle-usuario-body")));
        espera.until(ExpectedConditions.textToBePresentInElement(cuerpo, "3001234567"));
        capturar("CP-U12b_telefono_guardado");
    }

    @Test
    @Order(13)
    @DisplayName("CP-U13: desactivar un usuario actualiza su estado a inactivo")
    void cpU13_desactivarUsuario() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();
        String correo = crearDocente("Desactivar", "Prueba");

        new Select(esperarFilaConTexto(correo).findElement(By.tagName("select"))).selectByValue("inactivo");
        esperarToast("Estado actualizado a \"inactivo\"");

        espera.until(d -> "inactivo".equals(new Select(esperarFilaConTexto(correo).findElement(By.tagName("select")))
                .getFirstSelectedOption().getText()));
        // Confirmación en el backend: el detalle también dice inactivo (una cuenta inactiva recibe 403 al iniciar sesión)
        clicSeguro(esperarFilaConTexto(correo).findElement(By.xpath(".//button[contains(.,'Ver')]")));
        WebElement cuerpo = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("detalle-usuario-body")));
        espera.until(ExpectedConditions.textToBePresentInElement(cuerpo, "inactivo"));
        capturar("CP-U13_usuario_desactivado");
    }

    @Test
    @Order(14)
    @DisplayName("CP-U14: el administrador no puede cambiar su propio estado (409)")
    void cpU14_cambiarPropioEstado() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();

        esperarSinMensajes();
        new Select(esperarFilaConTexto(ADMIN_USUARIO).findElement(By.tagName("select"))).selectByValue("inactivo");

        assertThat(esperarToast("No puedes cambiar el estado de tu propia cuenta").getText()).startsWith("⚠️");
        capturar("CP-U14_propio_estado_rechazado");
        // La tabla se recarga y el selector vuelve a "activo"
        espera.until(d -> "activo".equals(new Select(esperarFilaConTexto(ADMIN_USUARIO).findElement(By.tagName("select")))
                .getFirstSelectedOption().getText()));
    }

    @Test
    @Order(15)
    @DisplayName("CP-U15: reenviar la invitación de un usuario pendiente genera una nueva")
    void cpU15_reenviarInvitacion() {
        iniciarSesionComoAdmin();
        abrirSeccionUsuarios();
        String correo = crearDocente("Reenvio", "Invitacion");

        clicSeguro(esperarFilaConTexto(correo).findElement(By.xpath(".//button[contains(.,'Reenviar invitación')]")));

        WebElement aviso = esperarToast("Se generó una nueva invitación");
        assertThat(aviso.getText()).contains(correo).contains("La anterior dejó de ser válida");
        capturar("CP-U15_invitacion_reenviada");
    }

    @Test
    @Order(16)
    @DisplayName("CP-U16: un usuario sin rol de administrador no puede abrir el prototipo del Administrador (DEF-003)")
    void cpU16_accesoConRolDistinto() {
        driver.get(URL_AUTH);
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-user"))).sendKeys(NOADMIN_USUARIO);
        driver.findElement(By.id("login-pass")).sendKeys(NOADMIN_CLAVE);
        boton("Ingresar").click();
        boolean entro;
        try {
            espera.until(d -> leerSessionStorage("biolearn_token") != null);
            entro = true;
        } catch (TimeoutException e) {
            entro = false;
        }
        assumeTrue(entro, "La cuenta " + NOADMIN_USUARIO + " no pudo iniciar sesión; indica otra con -Dbiolearn.noadmin.usuario.");
        assertThat(rolEnSesion()).isNotEqualTo("admin");

        // Intenta abrir directamente el prototipo del Administrador con su sesión.
        // Desde la corrección de DEF-003 el prototipo valida el rol: no dibuja el menú, avisa y redirige
        // al inicio de sesión. (El backend, además, responde 403 si se llama a la API con ese token.)
        driver.get(URL_ADMIN);
        WebElement aviso = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("acceso-denegado")));
        assertThat(aviso.getText()).contains("exclusiva del Administrador");
        assertThat(driver.findElements(By.id("sidebar"))).isEmpty();
        capturar("CP-U16_acceso_denegado");
        espera.until(ExpectedConditions.urlContains("prototipo-auth"));
    }

    @Test
    @Order(17)
    @DisplayName("CP-U17: el perfil muestra el nombre del administrador que inició sesión (DEF-002)")
    void cpU17_nombreDelAdministrador() {
        iniciarSesionComoAdmin();
        String nombreSesion = (String) ((JavascriptExecutor) driver).executeScript(
                "const u = JSON.parse(sessionStorage.getItem('biolearn_usuario')); return (u.nombre + ' ' + (u.apellido || '')).trim();");

        WebElement nombreBarra = espera.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("#sidebar .sb-user-info .nombre")));
        assertThat(nombreBarra.getText()).isEqualTo(nombreSesion).isNotEqualTo("Ana Bernal");
        assertThat(driver.findElement(By.id("topbar")).getText()).contains(nombreSesion).doesNotContain("Ana Bernal");
        capturar("CP-U17_nombre_administrador");
    }

    @Test
    @Order(18)
    @DisplayName("CP-U18: cambiar la institución de un representante desde Editar")
    void cpU18_cambiarInstitucionDeRepresentante() {
        iniciarSesionComoAdmin();
        // La prueba prepara sus propios datos por la API (con la sesión del administrador), para no depender
        // de que exista un representante sin institución: dos instituciones nuevas y un representante en la primera.
        long marca = System.currentTimeMillis();
        String sufijo = String.valueOf(marca).substring(6);
        String institucionA = "Colegio Selenium A " + sufijo;
        String institucionB = "Colegio Selenium B " + sufijo;
        long idA = crearPorApi("/api/instituciones", "{\"nombre\":\"" + institucionA + "\",\"nit\":\"8" + sufijo + "1\",\"cuposContratados\":10}");
        long idB = crearPorApi("/api/instituciones", "{\"nombre\":\"" + institucionB + "\",\"nit\":\"8" + sufijo + "2\",\"cuposContratados\":10}");
        String correo = "rep.selenium." + marca + "@biolearn.co";
        crearPorApi("/api/usuarios", "{\"nombre\":\"Representante\",\"apellido\":\"Selenium\",\"nombreUsuario\":\"repsel" + sufijo
                + "\",\"correo\":\"" + correo + "\",\"rol\":\"supervisor\",\"idInstitucion\":" + idA + ",\"cargo\":\"Coordinador\"}");

        abrirSeccionUsuarios();
        clicSeguro(esperarFilaConTexto(correo).findElement(By.xpath(".//button[contains(.,'Editar')]")));
        WebElement selector = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("edit-institucion")));
        espera.until(d -> new Select(selector).getOptions().size() > 1);
        assertThat(new Select(selector).getFirstSelectedOption().getAttribute("value")).isEqualTo(String.valueOf(idA));
        assertThat(texto("edit-inst-hint")).contains("Cambiarla mueve al representante");
        new Select(selector).selectByValue(String.valueOf(idB));
        capturar("CP-U18a_cambio_institucion");

        clicSeguro(driver.findElement(By.id("edit-modal-btn")));
        esperarToast("Institución asignada: " + institucionB);
        espera.until(d -> esperarFilaConTexto(correo).getText().contains(institucionB));
        capturar("CP-U18b_institucion_cambiada");
    }

    /** POST autenticado a la API desde el navegador (usa el token de la sesión del administrador); devuelve el id creado. */
    private long crearPorApi(String ruta, String json) {
        String respuesta = (String) ((JavascriptExecutor) driver).executeAsyncScript(
                "const ruta = arguments[0], cuerpo = arguments[1], listo = arguments[arguments.length - 1];" +
                "fetch('" + URL_API + "' + ruta, { method: 'POST', body: cuerpo, headers: { 'Content-Type': 'application/json'," +
                " 'Authorization': 'Bearer ' + sessionStorage.getItem('biolearn_token') } })" +
                ".then(r => r.text().then(t => listo(r.status + '|' + t))).catch(e => listo('0|' + e));", ruta, json);
        String[] partes = respuesta.split("\\|", 2);
        assertThat(partes[0]).as("Respuesta de POST " + ruta + ": " + respuesta).isEqualTo("201");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(partes[1]);
        assertThat(m.find()).as("La respuesta de " + ruta + " no trae el id").isTrue();
        return Long.parseLong(m.group(1));
    }

    // ------------------------------------------------------------------ pasos reutilizables

    private void iniciarSesionComoAdmin() {
        driver.get(URL_AUTH);
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-user"))).sendKeys(ADMIN_USUARIO);
        driver.findElement(By.id("login-pass")).sendKeys(ADMIN_CLAVE);
        boton("Ingresar").click();
        espera.until(ExpectedConditions.urlContains("biolearn_admin_prototipo.html"));
        espera.until(ExpectedConditions.titleIs("BioLearn — Administrador"));
    }

    private void abrirSeccionUsuarios() {
        abrirSeccionUsuariosSinEsperarListado();
        espera.until(d -> texto("usr-resumen").startsWith("Mostrando"));
    }

    private void abrirSeccionUsuariosSinEsperarListado() {
        espera.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//*[@id='sidebar']//button[.//span[normalize-space()='Usuarios']]"))).click();
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("usr-buscar")));
    }

    private void abrirModalCrear() {
        boton("+ Crear usuario").click();
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("usr-nombre")));
    }

    private void llenarDatosBasicos(String nombre, String apellido, String usuario, String correo) {
        driver.findElement(By.id("usr-nombre")).sendKeys(nombre);
        driver.findElement(By.id("usr-apellido")).sendKeys(apellido);
        driver.findElement(By.id("usr-usuario")).sendKeys(usuario);
        driver.findElement(By.id("usr-email")).sendKeys(correo);
    }

    /** Crea un docente con datos únicos desde el formulario y devuelve su correo. */
    private String crearDocente(String nombre, String apellido) {
        long marca = System.currentTimeMillis();
        String correo = normalizar(nombre) + "." + normalizar(apellido) + "." + marca + "@biolearn.co";
        abrirModalCrear();
        llenarDatosBasicos(nombre, apellido, normalizar(nombre) + marca, correo);
        new Select(driver.findElement(By.id("usr-rol"))).selectByValue("docente");
        driver.findElement(By.id("usr-especialidad")).sendKeys("Genética");
        driver.findElement(By.id("usr-modal-btn")).click();
        esperarToast("Usuario " + nombre + " creado");
        esperarFilaConTexto(correo);
        esperarSinMensajes();
        return correo;
    }

    /** Busca, con la sesión del navegador, una institución que ya tenga representante activo. */
    private Long institucionConRepresentanteActivo() {
        Object id = ((JavascriptExecutor) driver).executeAsyncScript(
                "const listo = arguments[arguments.length - 1];" +
                "fetch(arguments[0] + '/api/usuarios?rol=supervisor', { headers: { 'Authorization': 'Bearer ' + sessionStorage.getItem('biolearn_token') } })" +
                "  .then(r => r.json())" +
                "  .then(l => { const s = l.find(u => u.estadoRepresentacion === 'activo' && u.idInstitucion); listo(s ? s.idInstitucion : null); })" +
                "  .catch(() => listo(null));", URL_API);
        return id == null ? null : ((Number) id).longValue();
    }

    // ------------------------------------------------------------------ utilidades

    private List<WebElement> filas() {
        return driver.findElements(By.xpath("//*[@id='usr-lista']//tbody/tr[not(td[contains(@class,'empty-state')])]"));
    }

    /** Fila cuya celda de correo es EXACTAMENTE ese correo. (Con "contiene", admin@biolearn.co también
     *  coincidía con admin@biolearn.com y la prueba actuaba sobre otra cuenta.) */
    private WebElement esperarFilaConTexto(String correo) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//*[@id='usr-lista']//tbody/tr[td[normalize-space(.)='" + correo + "']]")));
    }

    /** Los mensajes emergentes duran ~3 s en la esquina inferior derecha y tapan los botones de la última
     *  fila; se espera a que desaparezcan antes de hacer clic, como haría una persona. */
    private void esperarSinMensajes() {
        espera.until(d -> d.findElements(By.cssSelector("#toast-cnt-box .toast-item")).isEmpty());
    }

    private void clicSeguro(WebElement elemento) {
        esperarSinMensajes();
        elemento.click();
    }

    private WebElement boton(String texto) {
        return espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space()='" + texto + "']")));
    }

    private WebElement esperarToast(String textoParcial) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//*[@id='toast-cnt-box']/*[contains(normalize-space(.), '" + textoParcial.replace("'", "") + "')]")));
    }

    private void cerrarModal(String id) {
        driver.findElement(By.cssSelector("#" + id + " .modal-close")).click();
        espera.until(d -> !d.findElement(By.id(id)).getAttribute("class").contains("open"));
    }

    private boolean visible(String id) {
        return driver.findElement(By.id(id)).isDisplayed();
    }

    private String texto(String id) {
        return driver.findElement(By.id(id)).getText().trim();
    }

    private String leerSessionStorage(String clave) {
        return (String) ((JavascriptExecutor) driver).executeScript("return window.sessionStorage.getItem(arguments[0]);", clave);
    }

    private String rolEnSesion() {
        return (String) ((JavascriptExecutor) driver).executeScript(
                "const u = JSON.parse(sessionStorage.getItem('biolearn_usuario') || '{}'); return u.rol || '';");
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private void capturar(String nombre) {
        try {
            Files.createDirectories(CARPETA_CAPTURAS);
            File origen = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(origen.toPath(), CARPETA_CAPTURAS.resolve(nombre + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("No se pudo guardar la captura " + nombre + ": " + e.getMessage());
        }
    }

    /** true si el servidor contesta (cualquier código HTTP, incluso 401/403). */
    private static boolean responde(String url) {
        try {
            HttpURLConnection conexion = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conexion.setConnectTimeout(2000);
            conexion.setReadTimeout(2000);
            conexion.getResponseCode();
            conexion.disconnect();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
