package co.biolearn.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pruebas de extremo a extremo (E2E) con Selenium del Módulo de Pagos, Fase 3: Inicio del contador con datos reales,
 * reportes financieros (vista para imprimir o guardar en PDF y descarga en Excel), datos de facturación y cuenta para
 * transferencias (editables por el Administrador y el administrador financiero; el auditor solo consulta), la sección
 * «Finanzas» del Administrador y la cuenta que ve el estudiante al pagar por transferencia.
 *
 * Requiere backend y frontend en ejecución, la migración 010 y las mismas cuentas de prueba de ReembolsoSeleniumTest.
 * Los cambios de datos se deshacen al final de cada caso (queda una versión nueva con los valores originales).
 * Las descargas de Excel quedan en target/descargas-selenium.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Selenium — Reportes financieros y datos de facturación y recaudo (Pagos, Fase 3)")
class FinanzasSeleniumTest {

    private static final String URL_AUTH = System.getProperty("biolearn.frontend.url",
            "http://127.0.0.1:5173/prototipo-auth/biolearn_auth_prototipo.html");
    private static final String URL_CONTADOR = URL_AUTH.replace("prototipo-auth/biolearn_auth_prototipo.html", "contador/biolearn_contador_prototipo.html");
    private static final String URL_ADMIN = URL_AUTH.replace("prototipo-auth/biolearn_auth_prototipo.html", "administrador/biolearn_admin_prototipo.html");
    private static final String URL_ESTUDIANTE = URL_AUTH.replace("prototipo-auth/biolearn_auth_prototipo.html", "prototipo-estudiante/biolearn_estudiante_prototipo.html");
    private static final String URL_API = System.getProperty("biolearn.backend.url", "http://localhost:8080");
    private static final String ESTUDIANTE_USUARIO = System.getProperty("biolearn.estudiante.usuario", "estudiante.pruebas@biolearn.co");
    private static final String ESTUDIANTE_CLAVE = System.getProperty("biolearn.estudiante.clave", "Estudiante2026");
    private static final String ADMIN_USUARIO = System.getProperty("biolearn.admin.usuario", "admin@biolearn.co");
    private static final String ADMIN_CLAVE = System.getProperty("biolearn.admin.clave", "Admin2026");
    private static final String CONTADOR_USUARIO = System.getProperty("biolearn.contador.usuario", "contador.pruebas@biolearn.co");
    private static final String CONTADOR_CLAVE = System.getProperty("biolearn.contador.clave", "Contador2026");
    private static final String AUDITOR_USUARIO = System.getProperty("biolearn.auditor.usuario", "auditor.pruebas@biolearn.co");
    private static final String AUDITOR_CLAVE = System.getProperty("biolearn.auditor.clave", "Contador2026");
    private static final boolean SIN_VENTANA = Boolean.parseBoolean(System.getProperty("biolearn.headless", "false"));

    private static final Path CARPETA_CAPTURAS = Paths.get("target", "capturas-selenium");
    private static final Path CARPETA_DESCARGAS = Paths.get("target", "descargas-selenium").toAbsolutePath();
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(10);

    private WebDriver driver;
    private WebDriverWait espera;

    @BeforeAll
    static void verificarEntorno() throws IOException {
        assumeTrue(responde(URL_AUTH), "Frontend no disponible en " + URL_AUTH);
        assumeTrue(responde(URL_API + "/api/cursos/catalogo"), "Backend no disponible en " + URL_API);
        Files.createDirectories(CARPETA_DESCARGAS);
    }

    @BeforeEach
    void abrirNavegador() {
        ChromeOptions opciones = new ChromeOptions();
        opciones.addArguments("--lang=es");
        opciones.addArguments(SIN_VENTANA ? "--window-size=1366,900" : "--start-maximized");
        Map<String, Object> preferencias = new HashMap<>();
        preferencias.put("download.default_directory", CARPETA_DESCARGAS.toString());
        preferencias.put("download.prompt_for_download", false);
        opciones.setExperimentalOption("prefs", preferencias);
        driver = new ChromeDriver(opciones);
        if (!SIN_VENTANA) driver.manage().window().maximize();
        espera = new WebDriverWait(driver, ESPERA_MAXIMA);
    }

    @AfterEach
    void cerrarNavegador() { if (driver != null) driver.quit(); }

    // ------------------------------------------------------------------ Inicio del contador

    @Test @Order(1)
    @DisplayName("CP-FI01: el Inicio del contador muestra los indicadores del mes, la gráfica de 6 meses y los últimos movimientos")
    void cpFI01_inicioConDatosReales() {
        entrarComo(CONTADOR_USUARIO, CONTADOR_CLAVE, URL_CONTADOR, "BioLearn — Contador");
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("fin-t-ingresos")));
        assertThat(texto("fin-t-ingresos")).containsIgnoringCase("Ingresos del mes").contains("$");   // la etiqueta se muestra en mayúsculas (CSS)
        assertThat(texto("fin-t-transferencias")).containsPattern("\\d");
        assertThat(texto("fin-t-revisar")).containsPattern("\\d");
        assertThat(texto("fin-t-devolver")).containsPattern("\\d");
        assertThat(driver.findElements(By.cssSelector("#fin-barras .fin-barra-mes"))).hasSize(6);
        capturar("CP-FI01_inicio_contador");
    }

    // ------------------------------------------------------------------ reportes

    @Test @Order(2)
    @DisplayName("CP-FI02: sin motivo o con la fecha inicial posterior a la final, el reporte no se genera y se explica el error")
    void cpFI02_validaciones() {
        entrarComo(CONTADOR_USUARIO, CONTADOR_CLAVE, URL_CONTADOR, "BioLearn — Contador");
        irA("Reportes");
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("fin-btn-ver")));
        clicSeguro(driver.findElement(By.id("fin-btn-ver")));
        esperarMensaje("Escribe el motivo del reporte");
        capturar("CP-FI02a_sin_motivo");
        driver.findElement(By.id("fin-motivo")).sendKeys("Prueba de período invertido");
        ponerFechas(LocalDate.now(), LocalDate.now().minusDays(10));
        clicSeguro(driver.findElement(By.id("fin-btn-ver")));
        esperarMensaje("La fecha inicial no puede ser posterior a la final");
        capturar("CP-FI02b_periodo_invertido");
        ponerFechas(LocalDate.now().minusYears(2), LocalDate.now());
        clicSeguro(driver.findElement(By.id("fin-btn-ver")));
        esperarMensaje("máximo de un reporte es de un año");
        capturar("CP-FI02c_periodo_mayor_a_un_anio");
    }

    @Test @Order(3)
    @DisplayName("CP-FI03: el reporte general se abre en la vista para imprimir o guardar en PDF y queda en el historial con el motivo")
    void cpFI03_reporteGeneralPdf() {
        entrarComo(CONTADOR_USUARIO, CONTADOR_CLAVE, URL_CONTADOR, "BioLearn — Contador");
        irA("Reportes");
        String motivo = "Revisión general Selenium " + marcaCorta();
        elegirTipo("general");
        clicCentrado(driver.findElement(By.xpath("//button[normalize-space()='Este año']")));
        driver.findElement(By.id("fin-motivo")).sendKeys(motivo);
        clicSeguro(driver.findElement(By.id("fin-btn-ver")));
        WebElement documento = espera.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#modal-fin-reporte.open #fin-documento")));
        assertThat(documento.getText()).contains("Reporte financiero general").contains("Mes a mes").contains(motivo);
        assertThat(driver.findElement(By.id("fin-btn-imprimir")).isDisplayed()).isTrue();
        capturar("CP-FI03a_vista_pdf_general");
        ((JavascriptExecutor) driver).executeScript("cerrarModal('modal-fin-reporte');");
        espera.until(d -> d.findElement(By.id("fin-historial")).getText().contains(motivo));
        assertThat(primerHistorial().getText()).contains("PDF").contains("Reporte financiero general");
        capturar("CP-FI03b_historial_pdf");
    }

    @Test @Order(4)
    @DisplayName("CP-FI04: el reporte de ingresos se descarga en Excel (.xlsx) y queda en el historial como Excel")
    void cpFI04_reporteIngresosExcel() throws IOException {
        entrarComo(CONTADOR_USUARIO, CONTADOR_CLAVE, URL_CONTADOR, "BioLearn — Contador");
        irA("Reportes");
        String motivo = "Cierre Selenium " + marcaCorta();
        elegirTipo("ingresos");
        String desde = valor("fin-desde"), hasta = valor("fin-hasta");
        Path archivo = CARPETA_DESCARGAS.resolve("BioLearn_ingresos_" + desde + "_" + hasta + ".xlsx");
        Files.deleteIfExists(archivo);
        driver.findElement(By.id("fin-motivo")).sendKeys(motivo);
        clicSeguro(driver.findElement(By.id("fin-btn-excel")));
        esperarMensaje("Reporte en Excel descargado");
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> Files.exists(archivo) && sinDescargasPendientes());
        byte[] inicio = Files.readAllBytes(archivo);
        assertThat(inicio.length).isGreaterThan(1000);
        assertThat(new String(inicio, 0, 2)).isEqualTo("PK");                 // un .xlsx es un ZIP
        espera.until(d -> d.findElement(By.id("fin-historial")).getText().contains(motivo));
        assertThat(primerHistorial().getText()).contains("Excel").contains("Reporte de ingresos");
        capturar("CP-FI04_excel_descargado");
    }

    // ------------------------------------------------------------------ datos de facturación y recaudo

    @Test @Order(5)
    @DisplayName("CP-FI05: el administrador financiero cambia el teléfono de facturación; queda como versión vigente con su nombre y se restaura")
    void cpFI05_cambiarFacturacion() {
        entrarComo(CONTADOR_USUARIO, CONTADOR_CLAVE, URL_CONTADOR, "BioLearn — Contador");
        irA("Facturación y recaudo");
        WebElement telefono = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("fin-f-telefono")));
        assumeTrue(driver.findElements(By.id("fin-solo-consulta")).isEmpty(), "La cuenta " + CONTADOR_USUARIO + " no es administrador financiero");
        String original = telefono.getAttribute("value");
        String nuevo = "604 " + marcaCorta();
        guardarFacturacion(nuevo);
        esperarMensaje("Datos de facturación guardados");
        assertThat(driver.findElement(By.id("fin-datos")).getText()).contains("Vigente desde");
        capturar("CP-FI05a_facturacion_guardada");
        clicSeguro(driver.findElement(By.xpath("(//button[normalize-space()='Ver versiones anteriores'])[1]")));
        WebElement modal = espera.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#modal-fin-historial.open")));
        WebElement vigente = modal.findElement(By.xpath(".//tr[.//span[normalize-space()='Vigente']]"));
        assertThat(vigente.getText()).doesNotContain("Registro inicial");
        capturar("CP-FI05b_versiones_facturacion");
        ((JavascriptExecutor) driver).executeScript("cerrarModal('modal-fin-historial');");
        guardarFacturacion(original);                                       // deja los datos como estaban
        esperarMensaje("Datos de facturación guardados");
    }

    @Test @Order(6)
    @DisplayName("CP-FI06: guardar la cuenta sin cambios o con un número de cuenta inválido muestra el error y no crea versión")
    void cpFI06_recaudoSinCambiosEInvalido() {
        entrarComo(CONTADOR_USUARIO, CONTADOR_CLAVE, URL_CONTADOR, "BioLearn — Contador");
        irA("Facturación y recaudo");
        WebElement numero = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("fin-r-numero")));
        assumeTrue(driver.findElements(By.id("fin-solo-consulta")).isEmpty(), "La cuenta " + CONTADOR_USUARIO + " no es administrador financiero");
        clicSeguro(driver.findElement(By.id("fin-r-guardar")));
        confirmar();
        esperarMensaje("No hay cambios para guardar");
        capturar("CP-FI06a_sin_cambios");
        numero.clear();
        numero.sendKeys("12AB");
        clicSeguro(driver.findElement(By.id("fin-r-guardar")));
        confirmar();
        esperarMensaje("6 y 20 dígitos");
        capturar("CP-FI06b_cuenta_invalida");
    }

    @Test @Order(7)
    @DisplayName("CP-FI07: el auditor consulta los datos sin poder cambiarlos, pero sí genera reportes")
    void cpFI07_auditorSoloConsulta() {
        entrarComo(AUDITOR_USUARIO, AUDITOR_CLAVE, URL_CONTADOR, "BioLearn — Contador");
        irA("Facturación y recaudo");
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("fin-solo-consulta")));
        assertThat(driver.findElement(By.id("fin-f-razon")).isEnabled()).isFalse();
        assertThat(driver.findElement(By.id("fin-r-numero")).isEnabled()).isFalse();
        assertThat(driver.findElements(By.id("fin-f-guardar"))).isEmpty();
        assertThat(driver.findElements(By.id("fin-r-guardar"))).isEmpty();
        capturar("CP-FI07a_auditor_solo_consulta");
        irA("Reportes");
        elegirTipo("reembolsos");
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("fin-motivo"))).sendKeys("Auditoría de reembolsos Selenium");
        clicSeguro(driver.findElement(By.id("fin-btn-ver")));
        WebElement documento = espera.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#modal-fin-reporte.open #fin-documento")));
        assertThat(documento.getText()).contains("Reembolsos").contains("enmascarad");
        capturar("CP-FI07b_auditor_reporte_reembolsos");
    }

    // ------------------------------------------------------------------ Administrador

    @Test @Order(8)
    @DisplayName("CP-FI08: el Administrador entra a Finanzas, genera el reporte de facturas y ve los datos de facturación y recaudo editables")
    void cpFI08_administradorFinanzas() {
        entrarComo(ADMIN_USUARIO, ADMIN_CLAVE, URL_ADMIN, "BioLearn — Administrador");
        irA("Finanzas");
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("fin-p-reportes")));
        elegirTipo("facturas");
        driver.findElement(By.id("fin-motivo")).sendKeys("Revisión de facturas Selenium");
        clicSeguro(driver.findElement(By.id("fin-btn-ver")));
        WebElement documento = espera.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#modal-fin-reporte.open #fin-documento")));
        assertThat(documento.getText()).contains("Estado de facturas");
        capturar("CP-FI08a_admin_reporte_facturas");
        ((JavascriptExecutor) driver).executeScript("cerrarModal('modal-fin-reporte');");
        clicSeguro(driver.findElement(By.id("fin-p-datos")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("fin-f-guardar")));
        assertThat(driver.findElements(By.id("fin-solo-consulta"))).isEmpty();
        assertThat(driver.findElement(By.id("fin-r-guardar")).isDisplayed()).isTrue();
        capturar("CP-FI08b_admin_facturacion_recaudo");
    }

    // ------------------------------------------------------------------ Estudiante

    @Test @Order(9)
    @DisplayName("CP-FI09: al pagar por transferencia, el estudiante ve la cuenta vigente que definió BioLearn")
    void cpFI09_estudianteVeCuentaVigente() {
        entrarComo(CONTADOR_USUARIO, CONTADOR_CLAVE, URL_CONTADOR, "BioLearn — Contador");
        irA("Facturación y recaudo");
        String numero = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("fin-r-numero"))).getAttribute("value");
        String banco = driver.findElement(By.id("fin-r-banco")).getAttribute("value");
        ((JavascriptExecutor) driver).executeScript("sessionStorage.clear();");
        entrarComo(ESTUDIANTE_USUARIO, ESTUDIANTE_CLAVE, URL_ESTUDIANTE, "BioLearn — Estudiante");
        // Abre el mismo modal que aparece al elegir «Transferencia» en el carrito, con un pago de ejemplo
        ((JavascriptExecutor) driver).executeScript("renderModalesPagoEst(); abrirComprobanteEst({id: 0, monto: 65000, items: [{curso: 'Curso de ejemplo'}]});");
        WebElement cuenta = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("comp-cuenta")));
        assertThat(cuenta.getText()).contains(banco).contains(numero);
        capturar("CP-FI09_estudiante_cuenta_vigente");
    }

    // ------------------------------------------------------------------ pasos reutilizables

    /** Inicia la sesión por la API (para no depender del formulario de cada rol) y abre el portal. */
    private void entrarComo(String usuario, String clave, String urlPortal, String titulo) {
        driver.get(URL_AUTH);
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
            "const [api, u, c] = arguments; const listo = arguments[arguments.length - 1];" +
            "fetch(api + '/api/auth/login', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({usuario: u, contrasena: c})})" +
            ".then(x => x.ok ? x.json() : Promise.reject(x.status))" +
            ".then(d => { sessionStorage.setItem('biolearn_token', d.token); sessionStorage.setItem('biolearn_usuario', JSON.stringify(d.usuario)); listo('OK'); })" +
            ".catch(e => listo('SIN_CUENTA ' + e));",
            URL_API, usuario, clave);
        assumeTrue("OK".equals(String.valueOf(r)), "Se necesita la cuenta de pruebas " + usuario + ": " + r);
        driver.get(urlPortal);
        espera.until(ExpectedConditions.titleIs(titulo));
    }

    private void irA(String opcion) {
        esperarSinMensajes();
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class,'sb-item')][.//span[normalize-space()='" + opcion + "']]"))));
    }

    private void elegirTipo(String tipo) {
        clicCentrado(espera.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[name='fin-tipo'][value='" + tipo + "']"))));
    }

    private void ponerFechas(LocalDate desde, LocalDate hasta) {
        ((JavascriptExecutor) driver).executeScript(
            "document.getElementById('fin-desde').value = arguments[0]; document.getElementById('fin-hasta').value = arguments[1];",
            desde.toString(), hasta.toString());
    }

    private void guardarFacturacion(String telefono) {
        WebElement campo = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("fin-f-telefono")));
        campo.clear();
        campo.sendKeys(telefono);
        clicSeguro(driver.findElement(By.id("fin-f-guardar")));
        confirmar();
    }

    private void confirmar() {
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#modal-confirmar.open")));
        clicCentrado(driver.findElement(By.id("confirm-btn")));
    }

    private WebElement primerHistorial() {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#fin-historial .fin-hist")));
    }

    private boolean sinDescargasPendientes() {
        try (Stream<Path> archivos = Files.list(CARPETA_DESCARGAS)) {
            List<Path> lista = archivos.toList();
            return lista.stream().noneMatch(p -> p.getFileName().toString().endsWith(".crdownload"));
        } catch (IOException e) {
            return false;
        }
    }

    private String texto(String id) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.id(id))).getText().trim();
    }

    private String valor(String id) {
        return espera.until(ExpectedConditions.presenceOfElementLocated(By.id(id))).getAttribute("value");
    }

    private static String marcaCorta() {
        String m = String.valueOf(System.currentTimeMillis());
        return m.substring(m.length() - 7);
    }

    private WebElement esperarMensaje(String textoParcial) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//*[@id='toast-wrap' or @id='toast-cnt-box']/*[contains(normalize-space(.), '" + textoParcial.replace("'", "") + "')]")));
    }

    private void esperarSinMensajes() {
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> d.findElements(By.cssSelector("#toast-wrap .toast-item, #toast-cnt-box .toast-item")).isEmpty());
    }

    private void clicSeguro(WebElement elemento) {
        esperarSinMensajes();
        clicCentrado(elemento);
    }

    private void clicCentrado(WebElement elemento) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", elemento);
        espera.until(ExpectedConditions.elementToBeClickable(elemento)).click();
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
