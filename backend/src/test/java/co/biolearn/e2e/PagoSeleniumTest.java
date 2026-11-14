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
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pruebas de extremo a extremo (E2E) con Selenium del Módulo de Pagos, Fase 1: el estudiante paga sus cursos en la
 * pasarela de pruebas o por transferencia, y el contador confirma o rechaza la transferencia.
 *
 * Cada caso prepara por la API un curso con precio «Curso Selenium P<marca>» y la inscripción pendiente de pago del
 * estudiante. Requiere backend y frontend en ejecución y las cuentas de Administrador, docente y estudiante de
 * pruebas; los casos del contador necesitan además una cuenta de contador administrador financiero o auxiliar
 * (biolearn.contador.usuario / biolearn.contador.clave; por defecto contador.pruebas@biolearn.co / Contador2026).
 * Si una cuenta no existe, sus casos se omiten.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Selenium — Pagos: carrito, pasarela, transferencia y contador (Pagos, Fase 1)")
class PagoSeleniumTest {

    private static final String URL_AUTH = System.getProperty("biolearn.frontend.url",
            "http://127.0.0.1:5173/prototipo-auth/biolearn_auth_prototipo.html");
    private static final String URL_ESTUDIANTE = URL_AUTH.replace("prototipo-auth/biolearn_auth_prototipo.html", "prototipo-estudiante/biolearn_estudiante_prototipo.html");
    private static final String URL_CONTADOR = URL_AUTH.replace("prototipo-auth/biolearn_auth_prototipo.html", "contador/biolearn_contador_prototipo.html");
    private static final String URL_API = System.getProperty("biolearn.backend.url", "http://localhost:8080");
    private static final String ESTUDIANTE_USUARIO = System.getProperty("biolearn.estudiante.usuario", "estudiante.pruebas@biolearn.co");
    private static final String ESTUDIANTE_CLAVE = System.getProperty("biolearn.estudiante.clave", "Estudiante2026");
    private static final String DOCENTE_USUARIO = System.getProperty("biolearn.docente.usuario", "docente.pruebas@biolearn.co");
    private static final String DOCENTE_CLAVE = System.getProperty("biolearn.docente.clave", "Docente2026");
    private static final String ADMIN_USUARIO = System.getProperty("biolearn.admin.usuario", "admin@biolearn.co");
    private static final String ADMIN_CLAVE = System.getProperty("biolearn.admin.clave", "Admin2026");
    private static final String CONTADOR_USUARIO = System.getProperty("biolearn.contador.usuario", "contador.pruebas@biolearn.co");
    private static final String CONTADOR_CLAVE = System.getProperty("biolearn.contador.clave", "Contador2026");
    private static final boolean SIN_VENTANA = Boolean.parseBoolean(System.getProperty("biolearn.headless", "false"));

    private static final Path CARPETA_CAPTURAS = Paths.get("target", "capturas-selenium");
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(10);
    /** PNG de 1×1 px para el comprobante de la transferencia. */
    private static final String PNG_1X1 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    private WebDriver driver;
    private WebDriverWait espera;

    @BeforeAll
    static void verificarEntorno() {
        assumeTrue(responde(URL_AUTH), "Frontend no disponible en " + URL_AUTH);
        assumeTrue(responde(URL_API + "/api/cursos/catalogo"), "Backend no disponible en " + URL_API);
    }

    @BeforeEach
    void abrirNavegador() {
        ChromeOptions opciones = new ChromeOptions();
        opciones.addArguments("--lang=es");
        opciones.addArguments(SIN_VENTANA ? "--window-size=1366,900" : "--start-maximized");
        driver = new ChromeDriver(opciones);
        if (!SIN_VENTANA) driver.manage().window().maximize();
        espera = new WebDriverWait(driver, ESPERA_MAXIMA);
    }

    @AfterEach
    void cerrarNavegador() { if (driver != null) driver.quit(); }

    // ------------------------------------------------------------------ estudiante

    @Test @Order(1)
    @DisplayName("CP-PG01: pagar con la tarjeta de prueba aprobada activa el curso y emite la factura")
    void cpPG01_tarjetaAprobada() {
        String curso = prepararCursoPendiente();
        entrarComoEstudiante();
        irAMisPagos();
        seleccionarSolo(curso);
        elegirMedio("tarjeta");
        pagarEnPasarela("pas-tarjeta", "4242 4242 4242 4242");
        WebElement estado = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("pas-estado")));
        assertThat(estado.getAttribute("data-estado")).isEqualTo("aprobado");
        assertThat(estado.getText()).contains("Pago aprobado").contains("BIO-FAC-");
        capturar("CP-PG01a_pasarela_aprobada");
        cerrarModal("modal-pasarela");
        assertThat(filaHistorial(curso).getAttribute("data-estado")).isEqualTo("aprobado");
        capturar("CP-PG01b_historial_con_factura");
        irAMisCursos();
        assertThat(tarjetaMiCurso(curso).getText()).contains("Inscrito").contains("0% completado");
        capturar("CP-PG01c_curso_activo");
    }

    @Test @Order(2)
    @DisplayName("CP-PG02: la tarjeta de prueba rechazada muestra el motivo y el curso sigue por pagar")
    void cpPG02_tarjetaRechazada() {
        String curso = prepararCursoPendiente();
        entrarComoEstudiante();
        irAMisPagos();
        seleccionarSolo(curso);
        elegirMedio("tarjeta");
        pagarEnPasarela("pas-tarjeta", "4111 1111 1111 1111");
        WebElement estado = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("pas-estado")));
        assertThat(estado.getAttribute("data-estado")).isEqualTo("rechazado");
        assertThat(estado.getText()).contains("rechazó la tarjeta");
        capturar("CP-PG02a_pasarela_rechazada");
        cerrarModal("modal-pasarela");
        espera.until(ExpectedConditions.visibilityOfElementLocated(itemPorPagar(curso)));
        assertThat(filaHistorial(curso).getText()).contains("Rechazado").contains("Motivo");
        capturar("CP-PG02b_sigue_por_pagar");
    }

    @Test @Order(3)
    @DisplayName("CP-PG03: un dato que no es de prueba muestra cuáles usar")
    void cpPG03_datoInvalido() {
        String curso = prepararCursoPendiente();
        entrarComoEstudiante();
        irAMisPagos();
        seleccionarSolo(curso);
        elegirMedio("nequi");
        pagarEnPasarela("pas-telefono", "3000000000");
        esperarMensaje("3991111111");
        capturar("CP-PG03_dato_no_valido");
    }

    @Test @Order(4)
    @DisplayName("CP-PG04: PSE con el banco que aprueba")
    void cpPG04_pse() {
        String curso = prepararCursoPendiente();
        entrarComoEstudiante();
        irAMisPagos();
        seleccionarSolo(curso);
        elegirMedio("pse");
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("pag-btn-pagar"))));
        new Select(espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("pas-banco")))).selectByValue("1");
        driver.findElement(By.id("pas-btn")).click();
        assertThat(espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("pas-estado"))).getAttribute("data-estado")).isEqualTo("aprobado");
        capturar("CP-PG04_pse_aprobado");
    }

    @Test @Order(5)
    @DisplayName("CP-PG05: una transferencia con comprobante queda esperando la confirmación del contador")
    void cpPG05_transferencia() {
        String curso = prepararCursoPendiente();
        entrarComoEstudiante();
        transferirConComprobante(curso);
        assertThat(filaHistorial(curso).getText()).contains("Pendiente").contains("Esperando confirmación");
        capturar("CP-PG05_transferencia_pendiente");
    }

    @Test @Order(6)
    @DisplayName("CP-PG06: cancelar un pago pendiente devuelve el curso a «Por pagar»")
    void cpPG06_cancelar() {
        String curso = prepararCursoPendiente();
        entrarComoEstudiante();
        irAMisPagos();
        seleccionarSolo(curso);
        elegirMedio("transferencia");
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("pag-btn-pagar"))));
        cerrarModal("modal-comprobante");
        clicSeguro(filaHistorial(curso).findElement(By.xpath(".//button[normalize-space()='Cancelar']")));
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("confirm-btn"))));
        esperarMensaje("Pago cancelado");
        espera.until(ExpectedConditions.visibilityOfElementLocated(itemPorPagar(curso)));
        espera.until(d -> "cancelado".equals(filaHistorial(curso).getAttribute("data-estado")));
        capturar("CP-PG06_pago_cancelado");
    }

    @Test @Order(7)
    @DisplayName("CP-PG07: la factura del pago aprobado se abre lista para imprimir")
    void cpPG07_factura() {
        String curso = prepararCursoPendiente();
        entrarComoEstudiante();
        irAMisPagos();
        seleccionarSolo(curso);
        pagarEnPasarela("pas-tarjeta", "4242424242424242");
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("pas-estado")));
        cerrarModal("modal-pasarela");
        String principal = driver.getWindowHandle();
        clicSeguro(filaHistorial(curso).findElement(By.xpath(".//button[contains(.,'BIO-FAC-')]")));
        espera.until(d -> d.getWindowHandles().size() > 1);
        driver.getWindowHandles().stream().filter(h -> !h.equals(principal)).findFirst().ifPresent(h -> driver.switchTo().window(h));
        espera.until(d -> d.findElement(By.tagName("body")).getText().contains("FACTURA DE VENTA"));
        assertThat(driver.findElement(By.tagName("body")).getText()).contains(curso).contains("BioLearn S.A.S.");
        capturar("CP-PG07_factura");
        driver.close();
        driver.switchTo().window(principal);
    }

    // ------------------------------------------------------------------ contador

    @Test @Order(8)
    @DisplayName("CP-PG08: el contador confirma la transferencia y el curso queda activo para el estudiante")
    void cpPG08_contadorConfirma() {
        String curso = prepararCursoPendiente();
        entrarComoEstudiante();
        transferirConComprobante(curso);
        cerrarSesionNavegador();
        entrarComoContador();
        WebElement fila = filaContador(curso);
        capturar("CP-PG08a_contador_pendiente");
        clicSeguro(fila.findElement(By.cssSelector(".cp-confirmar")));
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("cpc-btn"))));
        esperarMensaje("confirmado");
        capturar("CP-PG08b_contador_confirmado");
        cerrarSesionNavegador();
        entrarComoEstudiante();
        irAMisCursos();
        assertThat(tarjetaMiCurso(curso).getText()).contains("Inscrito");
        capturar("CP-PG08c_curso_activo");
    }

    @Test @Order(9)
    @DisplayName("CP-PG09: el contador rechaza la transferencia con un motivo que ve el estudiante")
    void cpPG09_contadorRechaza() {
        String curso = prepararCursoPendiente();
        entrarComoEstudiante();
        transferirConComprobante(curso);
        cerrarSesionNavegador();
        entrarComoContador();
        clicSeguro(filaContador(curso).findElement(By.cssSelector(".cp-rechazar")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("cpr-motivo"))).sendKeys("El comprobante no corresponde al valor");
        clicSeguro(driver.findElement(By.id("cpr-btn")));
        esperarMensaje("Pago rechazado");
        capturar("CP-PG09a_contador_rechazo");
        cerrarSesionNavegador();
        entrarComoEstudiante();
        irAMisPagos();
        assertThat(filaHistorial(curso).getText()).contains("El comprobante no corresponde al valor");
        espera.until(ExpectedConditions.visibilityOfElementLocated(itemPorPagar(curso)));
        capturar("CP-PG09b_estudiante_ve_motivo");
    }

    @Test @Order(10)
    @DisplayName("CP-PG10: un estudiante no ve el portal del contador")
    void cpPG10_accesoDenegado() {
        entrarComoEstudiante();
        driver.get(URL_CONTADOR);
        assertThat(espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("acceso-denegado"))).getText()).contains("Acceso solo para contadores");
        capturar("CP-PG10_acceso_denegado");
    }

    // ------------------------------------------------------------------ preparación por la API

    /** El docente crea el curso con una lección y un material; el Administrador fija el precio y lo publica; el estudiante se inscribe (pendiente de pago). */
    private String prepararCursoPendiente() {
        driver.get(URL_AUTH);
        String marca = marcaCorta();
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
            "const [api, marca, au, ac, du, dc, eu, ec] = arguments; const listo = arguments[arguments.length - 1];" +
            "const json = (h) => ({'Content-Type':'application/json', ...(h ? {'Authorization':'Bearer ' + h} : {})});" +
            "const pedir = async (m, ruta, token, cuerpo) => { const x = await fetch(api + ruta, {method: m, headers: json(token), body: cuerpo ? JSON.stringify(cuerpo) : undefined}); const d = await x.json().catch(() => ({})); if (!x.ok) throw new Error(ruta + ' ' + x.status + ' ' + (d.mensaje || '')); return d; };" +
            "const login = async (u, c) => (await pedir('POST', '/api/auth/login', null, {usuario: u, contrasena: c}));" +
            "(async () => { try {" +
            " let A, D, E; try { A = await login(au, ac); D = await login(du, dc); E = await login(eu, ec); } catch (e) { return listo('SIN_CUENTAS ' + e.message); }" +
            " const areas = await pedir('GET', '/api/catalogos/areas'); const grados = await pedir('GET', '/api/catalogos/grados');" +
            " const nombre = 'Curso Selenium P' + marca;" +
            " const c = await pedir('POST', '/api/docente/cursos', D.token, {nombre, idArea: areas[0].id, idGrado: grados[0].id, codigo: 'SP-' + marca, descripcion: 'Curso de prueba de pagos'});" +
            " const l1 = await pedir('POST', '/api/cursos/' + c.id + '/lecciones', D.token, {nombre: 'Lección Pago 1'});" +
            " await pedir('POST', '/api/lecciones/' + l1.id + '/materiales', D.token, {nombre: 'Lectura Pago 1', tipo: 'lectura', contenido: '<p>Texto</p>'});" +
            " await pedir('PATCH', '/api/lecciones/' + l1.id + '/estado', D.token, {estado: 'activa'});" +
            " await pedir('PATCH', '/api/cursos/' + c.id, A.token, {precio: 85000});" +
            " await pedir('PATCH', '/api/cursos/' + c.id + '/estado', A.token, {estado: 'activo'});" +
            " const i = await pedir('POST', '/api/estudiante/inscripciones', E.token, {idCurso: c.id});" +
            " if (i.estado !== 'pendiente_pago') return listo('ERROR estado ' + i.estado);" +
            " listo('OK|' + nombre);" +
            "} catch (e) { listo('ERROR ' + e.message); } })();",
            URL_API, marca, ADMIN_USUARIO, ADMIN_CLAVE, DOCENTE_USUARIO, DOCENTE_CLAVE, ESTUDIANTE_USUARIO, ESTUDIANTE_CLAVE);
        String res = String.valueOf(r);
        assumeTrue(!res.startsWith("SIN_CUENTAS"), "Se necesitan las cuentas de Administrador, docente y estudiante de pruebas: " + res);
        assertThat(res).as("Preparación de datos por la API").startsWith("OK|");
        return res.split("\\|", -1)[1];
    }

    // ------------------------------------------------------------------ pasos reutilizables

    private void iniciarSesion(String usuario, String clave, String destino) {
        driver.get(URL_AUTH);
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-user"))).sendKeys(usuario);
        driver.findElement(By.id("login-pass")).sendKeys(clave);
        boton("Ingresar").click();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(6)).until(ExpectedConditions.urlContains(destino));
        } catch (TimeoutException e) {
            assumeTrue(false, "No se pudo iniciar sesión con " + usuario + "; revisa que la cuenta exista y esté activa.");
        }
    }

    private void entrarComoEstudiante() {
        iniciarSesion(ESTUDIANTE_USUARIO, ESTUDIANTE_CLAVE, "biolearn_estudiante_prototipo.html");
        espera.until(ExpectedConditions.titleIs("BioLearn — Estudiante"));
    }

    /** Inicia la sesión del contador por la API y abre su portal (no depende de la redirección del login). */
    private void entrarComoContador() {
        driver.get(URL_AUTH);
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
            "const [api, u, c] = arguments; const listo = arguments[arguments.length - 1];" +
            "fetch(api + '/api/auth/login', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({usuario: u, contrasena: c})})" +
            ".then(x => x.ok ? x.json() : Promise.reject(x.status))" +
            ".then(d => { sessionStorage.setItem('biolearn_token', d.token); sessionStorage.setItem('biolearn_usuario', JSON.stringify(d.usuario)); listo('OK'); })" +
            ".catch(e => listo('SIN_CONTADOR ' + e));",
            URL_API, CONTADOR_USUARIO, CONTADOR_CLAVE);
        assumeTrue("OK".equals(String.valueOf(r)), "Se necesita la cuenta de contador de pruebas (" + CONTADOR_USUARIO + "): " + r);
        driver.get(URL_CONTADOR);
        espera.until(ExpectedConditions.titleIs("BioLearn — Contador"));
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class,'sb-item')][.//span[normalize-space()='Pagos']]"))));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("cp-tabla")));
    }

    private void cerrarSesionNavegador() {
        ((JavascriptExecutor) driver).executeScript("sessionStorage.clear();");
    }

    private void irAMisCursos() {
        esperarSinMensajes();
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class,'sb-item')][.//span[normalize-space()='Mis cursos']]"))));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("est-mis-cursos")));
    }

    private void irAMisPagos() {
        esperarSinMensajes();
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class,'sb-item')][.//span[normalize-space()='Mis pagos']]"))));
        espera.until(d -> !d.findElements(By.id("pag-items")).isEmpty() || !d.findElements(By.id("pag-sin-pendientes")).isEmpty());
    }

    /** Deja marcado solo el curso indicado (el estudiante de pruebas puede tener otros cursos por pagar). */
    private void seleccionarSolo(String curso) {
        espera.until(ExpectedConditions.visibilityOfElementLocated(itemPorPagar(curso)));
        for (int k = 0; k < 50; k++) {
            WebElement marcado = driver.findElements(By.cssSelector("#pag-items .pag-item")).stream()
                    .filter(e -> !curso.equals(e.getAttribute("data-curso")) && e.findElement(By.tagName("input")).isSelected())
                    .findFirst().orElse(null);
            if (marcado == null) break;
            clicCentrado(marcado.findElement(By.tagName("input")));
        }
        WebElement propio = driver.findElement(itemPorPagar(curso)).findElement(By.tagName("input"));
        if (!propio.isSelected()) clicCentrado(propio);
        espera.until(d -> d.findElement(By.id("pag-total")).getText().contains("85.000"));
    }

    private void elegirMedio(String medio) {
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".pag-medio[data-medio='" + medio + "']"))));
    }

    private void pagarEnPasarela(String campo, String valor) {
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("pag-btn-pagar"))));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id(campo))).sendKeys(valor);
        driver.findElement(By.id("pas-btn")).click();
    }

    private void transferirConComprobante(String curso) {
        irAMisPagos();
        seleccionarSolo(curso);
        elegirMedio("transferencia");
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("pag-btn-pagar"))));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("comp-resumen")));
        driver.findElement(By.id("comp-archivo")).sendKeys(comprobante().toAbsolutePath().toString());
        driver.findElement(By.id("comp-btn")).click();
        esperarMensaje("Comprobante enviado");
        espera.until(d -> filaHistorial(curso).getText().contains("Esperando confirmación"));
    }

    private Path comprobante() {
        try {
            Path p = Files.createTempFile("comprobante-", ".png");
            Files.write(p, Base64.getDecoder().decode(PNG_1X1));
            p.toFile().deleteOnExit();
            return p;
        } catch (IOException e) { throw new IllegalStateException(e); }
    }

    private void cerrarModal(String id) {
        driver.findElement(By.cssSelector("#" + id + " .modal-close")).click();
        espera.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("#" + id + ".open")));
    }

    private By itemPorPagar(String curso) {
        return By.cssSelector("#pag-items .pag-item[data-curso='" + curso + "']");
    }

    /** Fila más reciente del historial del estudiante con ese curso. */
    private WebElement filaHistorial(String curso) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//*[@id='pag-tabla']//tbody/tr[td[contains(.,'" + curso + "')]])[1]")));
    }

    private WebElement filaContador(String curso) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//*[@id='cp-tabla']//tr[contains(@class,'cp-fila')][td[contains(.,'" + curso + "')]])[1]")));
    }

    private WebElement tarjetaMiCurso(String nombre) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='est-mis-cursos']//div[contains(@class,'est-mi-curso')][@data-nombre='" + nombre + "']")));
    }

    private static String marcaCorta() {
        String m = String.valueOf(System.currentTimeMillis());
        return m.substring(m.length() - 7);
    }

    private WebElement boton(String texto) {
        return espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space()='" + texto + "']")));
    }

    private WebElement esperarMensaje(String textoParcial) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//*[@id='toast-wrap' or @id='toast-cnt-box']/*[contains(normalize-space(.), '" + textoParcial.replace("'", "") + "')]")));
    }

    private void esperarSinMensajes() {
        espera.until(d -> d.findElements(By.cssSelector("#toast-wrap .toast-item, #toast-cnt-box .toast-item")).isEmpty());
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
