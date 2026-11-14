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
 * Pruebas de extremo a extremo (E2E) con Selenium WebDriver sobre las secciones "Instituciones" y
 * "Catálogos" del prototipo del Administrador, conectadas al backend real.
 *
 * Requisitos antes de ejecutar (los mismos de AdminUsuariosSeleniumTest):
 *   1. MySQL encendido con biolearn_db.
 *   2. BiolearnBackendApplication corriendo en http://localhost:8080 (con el módulo de Instituciones y Catálogos).
 *   3. El frontend servido en http://127.0.0.1:5173.
 *   4. Google Chrome instalado.
 *
 * Datos que crean las pruebas: instituciones "Colegio Selenium <marca>" y grados/áreas "Grado Selenium <marca>" /
 * "Área Selenium <marca>", donde la marca es la hora en milisegundos; así la suite se puede repetir sin chocar.
 * Limpieza sugerida al cerrar el ciclo (MySQL Workbench):
 *   UPDATE institucion SET estado = 'inactiva' WHERE nombre LIKE 'Colegio Selenium %';
 *   UPDATE grado SET estado = 'inactivo' WHERE nombre LIKE 'Grado Selenium %';
 *   UPDATE area_academica SET estado = 'inactivo' WHERE nombre LIKE 'Área Selenium %';
 *
 * Buenas prácticas heredadas de la suite de Usuarios: localizadores por id (//*[@id='...']), esperar a que se
 * cierren los mensajes emergentes antes de hacer clic y comparar valores exactos (no "contiene").
 *
 * Las capturas se guardan en backend/target/capturas-selenium/ con el prefijo CP-IC##.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Selenium — Instituciones y Catálogos del Administrador")
class InstitucionesCatalogosSeleniumTest {

    private static final String URL_AUTH = System.getProperty("biolearn.frontend.url",
            "http://127.0.0.1:5173/prototipo-auth/biolearn_auth_prototipo.html");
    private static final String URL_API = System.getProperty("biolearn.backend.url", "http://localhost:8080");
    private static final String ADMIN_USUARIO = System.getProperty("biolearn.admin.usuario", "admin@biolearn.co");
    private static final String ADMIN_CLAVE = System.getProperty("biolearn.admin.clave", "Admin2026");
    private static final boolean SIN_VENTANA = Boolean.parseBoolean(System.getProperty("biolearn.headless", "false"));

    private static final Path CARPETA_CAPTURAS = Paths.get("target", "capturas-selenium");
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(10);

    private WebDriver driver;
    private WebDriverWait espera;

    // ------------------------------------------------------------------ ciclo de vida

    @BeforeAll
    static void verificarEntorno() {
        assumeTrue(responde(URL_AUTH), "Frontend no disponible en " + URL_AUTH);
        assumeTrue(responde(URL_API + "/api/catalogos/grados"), "Backend no disponible en " + URL_API);
    }

    @BeforeEach
    void abrirNavegador() {
        ChromeOptions opciones = new ChromeOptions();
        opciones.addArguments("--lang=es");
        // Con ventana, Chrome se abre maximizado; sin ventana (headless) se usa un tamaño fijo, porque no hay pantalla que maximizar
        opciones.addArguments(SIN_VENTANA ? "--window-size=1366,900" : "--start-maximized");
        if (SIN_VENTANA) opciones.addArguments("--headless=new");
        driver = new ChromeDriver(opciones);   // ENT-02: new FirefoxDriver()  ·  ENT-03: new EdgeDriver()
        if (!SIN_VENTANA) driver.manage().window().maximize();
        espera = new WebDriverWait(driver, ESPERA_MAXIMA);
    }

    @AfterEach
    void cerrarNavegador() {
        if (driver != null) driver.quit();
    }

    // ================================================================== INSTITUCIONES

    @Test
    @Order(1)
    @DisplayName("CP-IC01: la sección Instituciones carga el listado en tabla")
    void cpIC01_listarInstituciones() {
        iniciarSesionComoAdmin();
        abrirSeccion("Instituciones", "inst-resumen");

        assertThat(texto("inst-resumen")).startsWith("Mostrando");
        assertThat(filas("inst-lista")).isNotEmpty();
        assertThat(driver.findElement(By.xpath("//*[@id='inst-lista']//thead")).getText())
                .contains("INSTITUCIÓN").contains("CÓDIGO").contains("CUPOS").contains("REPRESENTANTE");
        capturar("CP-IC01_listado_instituciones");
    }

    @Test
    @Order(2)
    @DisplayName("CP-IC02: registrar una institución genera su código de afiliación de 8 caracteres")
    void cpIC02_registrarInstitucion() {
        iniciarSesionComoAdmin();
        abrirSeccion("Instituciones", "inst-resumen");
        String nombre = registrarInstitucion();

        WebElement fila = filaInstitucion(nombre);
        String codigo = fila.findElements(By.tagName("td")).get(1).getText().trim();
        assertThat(codigo).matches("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$");
        assertThat(new Select(fila.findElement(By.tagName("select"))).getFirstSelectedOption().getText()).isEqualTo("activa");
        capturar("CP-IC02_institucion_registrada");
    }

    @Test
    @Order(3)
    @DisplayName("CP-IC03: registrar sin los campos obligatorios muestra un aviso")
    void cpIC03_camposObligatorios() {
        iniciarSesionComoAdmin();
        abrirSeccion("Instituciones", "inst-resumen");

        boton("+ Registrar institución").click();
        WebElement cupos = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("inst-cupos")));
        cupos.clear();
        driver.findElement(By.id("inst-modal-btn")).click();

        assertThat(esperarMensaje("Nombre, NIT y cupos son obligatorios").getText()).startsWith("⚠️");
        assertThat(driver.findElement(By.id("modal-institucion")).getAttribute("class")).contains("open");
        capturar("CP-IC03_campos_obligatorios");
    }

    @Test
    @Order(4)
    @DisplayName("CP-IC04: registrar con un NIT repetido muestra el error 409")
    void cpIC04_nitRepetido() {
        iniciarSesionComoAdmin();
        abrirSeccion("Instituciones", "inst-resumen");
        String nit = nitUnico();
        registrarInstitucion("Colegio Selenium " + System.currentTimeMillis(), nit);

        llenarFormularioInstitucion("Colegio Selenium copia " + System.currentTimeMillis(), nit);
        driver.findElement(By.id("inst-modal-btn")).click();

        assertThat(esperarMensaje("Ya existe una institución registrada con ese NIT").getText()).startsWith("⚠️");
        capturar("CP-IC04_nit_repetido");
    }

    @Test
    @Order(5)
    @DisplayName("CP-IC05: los filtros muestran el resumen de filtros activos y se pueden limpiar")
    void cpIC05_filtrosInstituciones() {
        iniciarSesionComoAdmin();
        abrirSeccion("Instituciones", "inst-resumen");

        new Select(driver.findElement(By.id("inst-f-estado"))).selectByValue("activa");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("inst-resumen"), "Estado: activa"));
        new Select(driver.findElement(By.id("inst-f-rep"))).selectByValue("sin");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("inst-resumen"), "Representante: sin representante"));
        filas("inst-lista").forEach(f -> assertThat(f.getText()).contains("Sin asignar"));
        capturar("CP-IC05a_filtros_activos");

        boton("✕ Limpiar filtros").click();
        espera.until(d -> !texto("inst-resumen").contains("Filtros activos"));
        assertThat(new Select(driver.findElement(By.id("inst-f-estado"))).getFirstSelectedOption().getAttribute("value")).isEqualTo("todas");
        capturar("CP-IC05b_filtros_limpios");
    }

    @Test
    @Order(6)
    @DisplayName("CP-IC06: la búsqueda ignora tildes y conserva el cursor al escribir")
    void cpIC06_busquedaInstituciones() {
        iniciarSesionComoAdmin();
        abrirSeccion("Instituciones", "inst-resumen");

        WebElement buscador = driver.findElement(By.id("inst-buscar"));
        buscador.click();
        buscador.sendKeys("selenium");

        assertThat(driver.switchTo().activeElement().getAttribute("id")).isEqualTo("inst-buscar");
        List<WebElement> filas = filas("inst-lista");
        assumeTrue(!filas.isEmpty(), "No hay instituciones de Selenium; ejecuta primero CP-IC02.");
        filas.forEach(f -> assertThat(normalizar(f.getText())).contains("selenium"));
        capturar("CP-IC06_busqueda");
    }

    @Test
    @Order(7)
    @DisplayName("CP-IC07: la ventana de detalle muestra los datos frescos de la institución")
    void cpIC07_detalleInstitucion() {
        iniciarSesionComoAdmin();
        abrirSeccion("Instituciones", "inst-resumen");
        String nombre = registrarInstitucion();

        clicSeguro(filaInstitucion(nombre).findElement(By.xpath(".//button[contains(.,'Ver')]")));
        WebElement cuerpo = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("inst-detalle-body")));
        espera.until(ExpectedConditions.textToBePresentInElement(cuerpo, nombre));

        assertThat(cuerpo.getText()).contains("Código de afiliación", "Cupos contratados", "Estudiantes afiliados", "Representante");
        capturar("CP-IC07_detalle_institucion");
    }

    @Test
    @Order(8)
    @DisplayName("CP-IC08: editar los cupos de una institución actualiza la tabla")
    void cpIC08_editarInstitucion() {
        iniciarSesionComoAdmin();
        abrirSeccion("Instituciones", "inst-resumen");
        String nombre = registrarInstitucion();

        clicSeguro(filaInstitucion(nombre).findElement(By.xpath(".//button[contains(.,'Editar')]")));
        WebElement cupos = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("inst-cupos")));
        cupos.clear();
        cupos.sendKeys("75");
        capturar("CP-IC08a_formulario_edicion");
        driver.findElement(By.id("inst-modal-btn")).click();

        esperarMensaje("Institución " + nombre + " actualizada");
        espera.until(d -> filaInstitucion(nombre).getText().contains("0 / 75"));
        capturar("CP-IC08b_cupos_actualizados");
    }

    @Test
    @Order(9)
    @DisplayName("CP-IC09: generar un nuevo código reemplaza el anterior")
    void cpIC09_nuevoCodigo() {
        iniciarSesionComoAdmin();
        abrirSeccion("Instituciones", "inst-resumen");
        String nombre = registrarInstitucion();
        String anterior = filaInstitucion(nombre).findElements(By.tagName("td")).get(1).getText().trim();

        clicSeguro(filaInstitucion(nombre).findElement(By.xpath(".//button[contains(.,'Ver')]")));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("inst-detalle-body"), nombre));
        clicSeguro(driver.findElement(By.xpath("//*[@id='inst-detalle-btns']//button[contains(.,'Nuevo código')]")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("confirm-btn")));
        capturar("CP-IC09a_confirmacion");
        driver.findElement(By.id("confirm-btn")).click();

        esperarMensaje("Nuevo código de " + nombre);
        espera.until(d -> !filaInstitucion(nombre).findElements(By.tagName("td")).get(1).getText().trim().equals(anterior));
        capturar("CP-IC09b_codigo_nuevo");
    }

    @Test
    @Order(10)
    @DisplayName("CP-IC10: suspender una institución avisa que no admite nuevas afiliaciones")
    void cpIC10_suspenderInstitucion() {
        iniciarSesionComoAdmin();
        abrirSeccion("Instituciones", "inst-resumen");
        String nombre = registrarInstitucion();

        esperarSinMensajes();
        new Select(filaInstitucion(nombre).findElement(By.tagName("select"))).selectByValue("suspendida");

        esperarMensaje(nombre + " quedó suspendida: no admite nuevas afiliaciones");
        espera.until(d -> "suspendida".equals(new Select(filaInstitucion(nombre).findElement(By.tagName("select")))
                .getFirstSelectedOption().getText()));
        capturar("CP-IC10_institucion_suspendida");
    }

    // ================================================================== CATÁLOGOS

    @Test
    @Order(11)
    @DisplayName("CP-IC11: crear un grado lo agrega activo a la tabla de grados")
    void cpIC11_crearGrado() {
        iniciarSesionComoAdmin();
        abrirSeccion("Catálogos", "cat-resumen");
        String nombre = crearGrado();

        WebElement fila = filaCatalogo(nombre);
        assertThat(fila.getText()).contains("primaria");
        assertThat(new Select(fila.findElement(By.tagName("select"))).getFirstSelectedOption().getText()).isEqualTo("activo");
        capturar("CP-IC11_grado_creado");
    }

    @Test
    @Order(12)
    @DisplayName("CP-IC12: crear un grado con un nombre repetido muestra el error 409")
    void cpIC12_gradoRepetido() {
        iniciarSesionComoAdmin();
        abrirSeccion("Catálogos", "cat-resumen");
        String nombre = crearGrado();

        llenarFormularioGrado(nombre);
        driver.findElement(By.id("cat-modal-btn")).click();

        assertThat(esperarMensaje("Ya existe un grado con ese nombre").getText()).startsWith("⚠️");
        capturar("CP-IC12_grado_repetido");
    }

    @Test
    @Order(13)
    @DisplayName("CP-IC13: inactivar un grado lo marca como inactivo y aparece en el filtro de inactivos")
    void cpIC13_inactivarGrado() {
        iniciarSesionComoAdmin();
        abrirSeccion("Catálogos", "cat-resumen");
        String nombre = crearGrado();

        esperarSinMensajes();
        new Select(filaCatalogo(nombre).findElement(By.tagName("select"))).selectByValue("inactivo");
        esperarMensaje("\"" + nombre + "\" quedó inactivo");

        new Select(driver.findElement(By.id("cat-f-estado"))).selectByValue("inactivo");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("cat-resumen"), "Estado: inactivo"));
        assertThat(filaCatalogo(nombre).isDisplayed()).isTrue();
        capturar("CP-IC13_grado_inactivo");
    }

    @Test
    @Order(14)
    @DisplayName("CP-IC14: la ventana de detalle de un grado muestra los estudiantes asociados")
    void cpIC14_detalleGrado() {
        iniciarSesionComoAdmin();
        abrirSeccion("Catálogos", "cat-resumen");
        String nombre = crearGrado();

        clicSeguro(filaCatalogo(nombre).findElement(By.xpath(".//button[contains(.,'Ver')]")));
        WebElement cuerpo = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("cat-detalle-body")));
        espera.until(ExpectedConditions.textToBePresentInElement(cuerpo, nombre));

        assertThat(cuerpo.getText()).contains("Nivel", "Orden en la lista", "Estado", "Estudiantes asociados");
        capturar("CP-IC14_detalle_grado");
    }

    @Test
    @Order(15)
    @DisplayName("CP-IC15: el registro de estudiantes no ofrece un grado inactivo")
    void cpIC15_registroSinGradoInactivo() {
        iniciarSesionComoAdmin();
        abrirSeccion("Catálogos", "cat-resumen");
        String nombre = crearGrado();
        esperarSinMensajes();
        new Select(filaCatalogo(nombre).findElement(By.tagName("select"))).selectByValue("inactivo");
        esperarMensaje("\"" + nombre + "\" quedó inactivo");

        // El formulario de registro consulta el listado público (solo registros activos)
        ((JavascriptExecutor) driver).executeScript("sessionStorage.clear();");
        driver.get(URL_AUTH);
        espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[self::a or self::button][normalize-space()='Crear usuario']"))).click();
        WebElement grado = espera.until(ExpectedConditions.presenceOfElementLocated(By.id("signup-grado")));
        espera.until(d -> new Select(grado).getOptions().size() > 1);

        assertThat(new Select(grado).getOptions()).extracting(WebElement::getText).doesNotContain(nombre);
        capturar("CP-IC15_registro_sin_grado_inactivo");
    }

    @Test
    @Order(16)
    @DisplayName("CP-IC16: en la pestaña de áreas se puede crear un área académica")
    void cpIC16_crearArea() {
        iniciarSesionComoAdmin();
        abrirSeccion("Catálogos", "cat-resumen");
        clicSeguro(driver.findElement(By.id("cat-tab-areas")));
        espera.until(ExpectedConditions.textToBe(By.id("cat-btn-agregar"), "+ Agregar área"));
        assertThat(driver.findElement(By.id("cat-f-nivel")).isDisplayed()).isFalse();   // el nivel solo aplica a grados

        String nombre = "Área Selenium " + marcaCorta();
        clicSeguro(driver.findElement(By.id("cat-btn-agregar")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("cat-nombre"))).sendKeys(nombre);
        driver.findElement(By.id("cat-descripcion")).sendKeys("Creada por la suite de Selenium");
        driver.findElement(By.id("cat-modal-btn")).click();

        esperarMensaje("Área \"" + nombre + "\"");
        assertThat(filaCatalogo(nombre).isDisplayed()).isTrue();
        capturar("CP-IC16_area_creada");
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

    private void abrirSeccion(String etiqueta, String idResumen) {
        espera.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//*[@id='sidebar']//button[.//span[normalize-space()='" + etiqueta + "']]"))).click();
        espera.until(d -> texto(idResumen).startsWith("Mostrando"));
    }

    private String registrarInstitucion() {
        return registrarInstitucion("Colegio Selenium " + System.currentTimeMillis(), nitUnico());
    }

    private String registrarInstitucion(String nombre, String nit) {
        llenarFormularioInstitucion(nombre, nit);
        driver.findElement(By.id("inst-modal-btn")).click();
        esperarMensaje("Institución registrada. Código de afiliación:");
        filaInstitucion(nombre);
        esperarSinMensajes();
        return nombre;
    }

    private void llenarFormularioInstitucion(String nombre, String nit) {
        esperarSinMensajes();
        boton("+ Registrar institución").click();
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("inst-nombre"))).sendKeys(nombre);
        driver.findElement(By.id("inst-nit")).sendKeys(nit);
        WebElement cupos = driver.findElement(By.id("inst-cupos"));
        cupos.clear();
        cupos.sendKeys("50");
        driver.findElement(By.id("inst-rector")).sendKeys("Rector de prueba");
        driver.findElement(By.id("inst-email")).sendKeys("rectoria." + nit.replaceAll("\\D", "") + "@colegio.edu.co");
    }

    private String crearGrado() {
        String nombre = "Grado Selenium " + marcaCorta();
        llenarFormularioGrado(nombre);
        driver.findElement(By.id("cat-modal-btn")).click();
        esperarMensaje("Grado \"" + nombre + "\" creado");
        filaCatalogo(nombre);
        esperarSinMensajes();
        return nombre;
    }

    private void llenarFormularioGrado(String nombre) {
        esperarSinMensajes();
        clicSeguro(driver.findElement(By.id("cat-btn-agregar")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("cat-nombre"))).sendKeys(nombre);
        new Select(driver.findElement(By.id("cat-nivel"))).selectByValue("primaria");
    }

    // ------------------------------------------------------------------ utilidades

    private static String nitUnico() {
        String m = String.valueOf(System.currentTimeMillis());
        return "9" + m.substring(m.length() - 8);
    }

    private static String marcaCorta() {
        String m = String.valueOf(System.currentTimeMillis());
        return m.substring(m.length() - 7);
    }

    private List<WebElement> filas(String idLista) {
        return driver.findElements(By.xpath("//*[@id='" + idLista + "']//tbody/tr[not(td[contains(@class,'empty-state')])]"));
    }

    /** Fila de institución cuyo nombre es exactamente ese (el nombre va en el primer div de la celda). */
    private WebElement filaInstitucion(String nombre) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//*[@id='inst-lista']//tbody/tr[td[1]/div[normalize-space()='" + nombre + "']]")));
    }

    /** Fila de grado o área cuyo nombre es exactamente ese. */
    private WebElement filaCatalogo(String nombre) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//*[@id='cat-lista']//tbody/tr[td[normalize-space()='" + nombre + "']]")));
    }

    private WebElement boton(String texto) {
        return espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space()='" + texto + "']")));
    }

    private WebElement esperarMensaje(String textoParcial) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//*[@id='toast-cnt-box']/*[contains(normalize-space(.), '" + textoParcial.replace("'", "") + "')]")));
    }

    private void esperarSinMensajes() {
        espera.until(d -> d.findElements(By.cssSelector("#toast-cnt-box .toast-item")).isEmpty());
    }

    private void clicSeguro(WebElement elemento) {
        esperarSinMensajes();
        elemento.click();
    }

    private String texto(String id) {
        return driver.findElement(By.id(id)).getText().trim();
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
