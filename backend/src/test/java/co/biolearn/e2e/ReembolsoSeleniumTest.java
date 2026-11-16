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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pruebas de extremo a extremo (E2E) con Selenium del Módulo de Pagos, Fase 2 (reembolsos con nota crédito) y de
 * «Mi institución» en Mi perfil.
 *
 * Cada caso prepara por la API uno o varios cursos con precio «Curso Selenium R<marca>», la inscripción del estudiante
 * y un pago aprobado (tarjeta de prueba, o transferencia confirmada por el contador). Requiere backend y frontend en
 * ejecución, la migración 008 y las mismas cuentas de prueba de PagoSeleniumTest (Administrador, docente, estudiante,
 * contador administrador financiero o auxiliar y contador auditor). Si una cuenta no existe, sus casos se omiten.
 * Desactiva el envío de correos (biolearn.correo.habilitado=false) antes de ejecutarla.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Selenium — Reembolsos y notas crédito (Pagos, Fase 2) y Mi institución")
class ReembolsoSeleniumTest {

    private static final String URL_AUTH = System.getProperty("biolearn.frontend.url",
            "http://127.0.0.1:5173/prototipo-auth/biolearn_auth_prototipo.html");
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
    private static final String AUDITOR_USUARIO = System.getProperty("biolearn.auditor.usuario", "auditor.pruebas@biolearn.co");
    private static final String AUDITOR_CLAVE = System.getProperty("biolearn.auditor.clave", "Contador2026");
    private static final boolean SIN_VENTANA = Boolean.parseBoolean(System.getProperty("biolearn.headless", "false"));

    private static final Path CARPETA_CAPTURAS = Paths.get("target", "capturas-selenium");
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(10);

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
    @DisplayName("CP-RE01: el estudiante elige 2 de 3 cursos de un pago con tarjeta; la solicitud queda en revisión y esos cursos en pausa")
    void cpRE01_solicitarDosDeTres() {
        String[] c = prepararPagoAprobado("tarjeta", 3, POCO);          // el primer curso con 10 %: lo revisa un contador
        entrarComoEstudiante();
        irAMisPagos();
        abrirReembolso(c[0]);
        WebElement aviso = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ree-mismo-medio")));
        assertThat(aviso.getText()).contains("Tarjeta");
        marcarCurso(c[0]); marcarCurso(c[1]);
        espera.until(d -> d.findElement(By.id("ree-total")).getText().contains("170.000"));
        driver.findElement(By.id("ree-motivo")).sendKeys("Me inscribí en cursos que ya había visto");
        capturar("CP-RE01a_seleccion_dos_de_tres");
        clicSeguro(driver.findElement(By.id("ree-btn-enviar")));
        esperarMensaje("Solicitud enviada");
        WebElement fila = filaReembolso(c[0]);
        assertThat(fila.getAttribute("data-estado")).isEqualTo("solicitado");
        assertThat(fila.getText()).contains(c[1]).doesNotContain(c[2]).contains("170.000");
        capturar("CP-RE01b_en_revision");
        irAMisCursos();
        assertThat(tarjetaMiCurso(c[0]).getText()).contains("Reembolso en trámite");
        assertThat(tarjetaMiCurso(c[2]).getText()).contains("Inscrito");
        capturar("CP-RE01c_cursos_en_pausa");
    }

    @Test @Order(2)
    @DisplayName("CP-RE02: un curso con 20 % o más de avance aparece deshabilitado con el motivo")
    void cpRE02_avanceMayor() {
        String[] c = prepararPagoAprobado("tarjeta", 2, MUCHO);       // el primer curso queda al 50 %
        entrarComoEstudiante();
        irAMisPagos();
        abrirReembolso(c[0]);
        WebElement item = espera.until(ExpectedConditions.visibilityOfElementLocated(itemReembolso(c[0])));
        assertThat(item.findElement(By.tagName("input")).isEnabled()).isFalse();
        assertThat(item.getText()).contains("menos del 20 %");
        assertThat(driver.findElement(itemReembolso(c[1])).findElement(By.tagName("input")).isEnabled()).isTrue();
        capturar("CP-RE02_avance_mayor");
    }

    @Test @Order(3)
    @DisplayName("CP-RE03: con transferencia se piden los datos de la devolución; datos incompletos muestran el error y con Nequi se envía")
    void cpRE03_transferenciaPideDatos() {
        String[] c = prepararPagoAprobado("transferencia", 1, POCO);
        entrarComoEstudiante();
        irAMisPagos();
        abrirReembolso(c[0]);
        marcarCurso(c[0]);
        driver.findElement(By.id("ree-motivo")).sendKeys("Cambié de horario y no podré tomarlo");
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ree-numero"))).sendKeys("12");
        clicSeguro(driver.findElement(By.id("ree-btn-enviar")));
        esperarMensaje("6 y 20 dígitos");
        capturar("CP-RE03a_datos_incompletos");
        clicCentrado(driver.findElement(By.cssSelector("input[name='ree-destino'][value='nequi']")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ree-nequi"))).sendKeys("3001234567");
        clicSeguro(driver.findElement(By.id("ree-btn-enviar")));
        esperarMensaje("Solicitud enviada");
        WebElement fila = filaReembolso(c[0]);
        assertThat(fila.getText()).contains("Nequi").contains("••••4567");
        capturar("CP-RE03b_solicitud_con_nequi");
    }

    @Test @Order(4)
    @DisplayName("CP-RE04: cancelar la solicitud devuelve el curso a «Inscrito»")
    void cpRE04_cancelar() {
        String[] c = prepararPagoAprobado("tarjeta", 1, POCO);
        entrarComoEstudiante();
        solicitar(c[0]);
        clicSeguro(filaReembolso(c[0]).findElement(By.cssSelector(".ree-cancelar")));
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("confirm-btn"))));
        esperarMensaje("Solicitud cancelada");
        espera.until(d -> "cancelado".equals(filaReembolso(c[0]).getAttribute("data-estado")));
        capturar("CP-RE04a_cancelada");
        irAMisCursos();
        assertThat(tarjetaMiCurso(c[0]).getText()).contains("Inscrito");
        capturar("CP-RE04b_curso_disponible");
    }

    // ------------------------------------------------------------------ contador

    @Test @Order(5)
    @DisplayName("CP-RE05: el contador aprueba un reembolso con tarjeta: se devuelve solo y el estudiante ve la nota crédito")
    void cpRE05_aprobarTarjeta() {
        String[] c = prepararPagoAprobado("tarjeta", 1, POCO);
        entrarComoEstudiante();
        solicitar(c[0]);
        cerrarSesionNavegador();
        entrarComoContador();
        WebElement tarjeta = tarjetaContador(c[0]);
        capturar("CP-RE05a_por_revisar");
        clicSeguro(tarjeta.findElement(By.cssSelector(".cr-aprobar")));
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("cra-btn"))));
        esperarMensaje("dinero devuelto");
        verTodosContador();
        assertThat(tarjetaContador(c[0]).getAttribute("data-estado")).isEqualTo("devuelto");
        assertThat(tarjetaContador(c[0]).getText()).contains("PRUEBA-REV-").contains("BIO-NC-");
        capturar("CP-RE05b_devuelto");
        cerrarSesionNavegador();
        entrarComoEstudiante();
        irAMisPagos();
        WebElement fila = filaReembolso(c[0]);
        assertThat(fila.getAttribute("data-estado")).isEqualTo("devuelto");
        String principal = driver.getWindowHandle();
        clicSeguro(fila.findElement(By.cssSelector(".ree-nota")));
        espera.until(d -> d.getWindowHandles().size() > 1);
        driver.getWindowHandles().stream().filter(h -> !h.equals(principal)).findFirst().ifPresent(h -> driver.switchTo().window(h));
        espera.until(d -> d.findElement(By.tagName("body")).getText().contains("NOTA CRÉDITO"));
        assertThat(driver.findElement(By.tagName("body")).getText()).contains(c[0]).contains("BIO-FAC-").contains("Total a devolver");
        capturar("CP-RE05c_nota_credito");
        driver.close();
        driver.switchTo().window(principal);
    }

    @Test @Order(6)
    @DisplayName("CP-RE06: transferencia: el contador aprueba, queda por devolver y registra la devolución con su referencia")
    void cpRE06_devolucionManual() {
        String[] c = prepararPagoAprobado("transferencia", 1, POCO);
        entrarComoEstudiante();
        irAMisPagos();
        abrirReembolso(c[0]);
        marcarCurso(c[0]);
        driver.findElement(By.id("ree-motivo")).sendKeys("Cambié de horario y no podré tomarlo");
        clicCentrado(driver.findElement(By.cssSelector("input[name='ree-destino'][value='nequi']")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ree-nequi"))).sendKeys("3001234567");
        clicSeguro(driver.findElement(By.id("ree-btn-enviar")));
        esperarMensaje("Solicitud enviada");
        cerrarSesionNavegador();
        entrarComoContador();
        WebElement tarjeta = tarjetaContador(c[0]);
        assertThat(tarjeta.getText()).contains("3001234567");                // completo para quien hace la devolución
        clicSeguro(tarjeta.findElement(By.cssSelector(".cr-aprobar")));
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("cra-btn"))));
        esperarMensaje("falta registrar la devolución");
        verTodosContador();
        clicSeguro(tarjetaContador(c[0]).findElement(By.cssSelector(".cr-devolucion")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("crd-referencia"))).sendKeys("NEQ-SEL-" + marcaCorta());
        capturar("CP-RE06a_registrar_devolucion");
        clicSeguro(driver.findElement(By.id("crd-btn")));
        esperarMensaje("Devolución registrada");
        espera.until(d -> "devuelto".equals(tarjetaContador(c[0]).getAttribute("data-estado")));
        assertThat(tarjetaContador(c[0]).getText()).contains("NEQ-SEL-");
        capturar("CP-RE06b_devuelto");
    }

    @Test @Order(7)
    @DisplayName("CP-RE07: el contador rechaza con un motivo; el estudiante lo ve y recupera el curso")
    void cpRE07_rechazar() {
        String[] c = prepararPagoAprobado("tarjeta", 1, POCO);
        entrarComoEstudiante();
        solicitar(c[0]);
        cerrarSesionNavegador();
        entrarComoContador();
        clicSeguro(tarjetaContador(c[0]).findElement(By.cssSelector(".cr-rechazar")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("crr-motivo"))).sendKeys("La solicitud no corresponde a un retiro voluntario");
        clicSeguro(driver.findElement(By.id("crr-btn")));
        esperarMensaje("Solicitud rechazada");
        capturar("CP-RE07a_rechazo");
        cerrarSesionNavegador();
        entrarComoEstudiante();
        irAMisPagos();
        WebElement fila = filaReembolso(c[0]);
        assertThat(fila.getAttribute("data-estado")).isEqualTo("rechazado");
        assertThat(fila.getText()).contains("no corresponde a un retiro voluntario");
        capturar("CP-RE07b_estudiante_ve_motivo");
        irAMisCursos();
        assertThat(tarjetaMiCurso(c[0]).getText()).contains("Inscrito");
    }

    @Test @Order(8)
    @DisplayName("CP-RE08: el auditor consulta las solicitudes sin Aprobar ni Rechazar y con la cuenta enmascarada")
    void cpRE08_auditor() {
        String[] c = prepararPagoAprobado("transferencia", 1, POCO);
        entrarComoEstudiante();
        irAMisPagos();
        abrirReembolso(c[0]);
        marcarCurso(c[0]);
        driver.findElement(By.id("ree-motivo")).sendKeys("Cambié de horario y no podré tomarlo");
        clicCentrado(driver.findElement(By.cssSelector("input[name='ree-destino'][value='nequi']")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ree-nequi"))).sendKeys("3001234567");
        clicSeguro(driver.findElement(By.id("ree-btn-enviar")));
        esperarMensaje("Solicitud enviada");
        cerrarSesionNavegador();
        entrarComoContador(AUDITOR_USUARIO, AUDITOR_CLAVE);
        WebElement tarjeta = tarjetaContador(c[0]);
        assertThat(texto("cont-aviso-auditor")).contains("Rol auditor");
        assertThat(tarjeta.findElements(By.cssSelector(".cr-aprobar, .cr-rechazar, .cr-devolucion"))).isEmpty();
        assertThat(tarjeta.getText()).contains("••••4567").doesNotContain("3001234567");
        capturar("CP-RE08_auditor");
    }

    // ------------------------------------------------------------------ aprobación automática

    @Test @Order(9)
    @DisplayName("CP-RE09: sin avance y con tarjeta, la solicitud se aprueba sola: dinero devuelto y nota crédito sin pasar por el contador")
    void cpRE09_automaticaTarjeta() {
        String[] c = prepararPagoAprobado("tarjeta", 1, NADA);
        entrarComoEstudiante();
        irAMisPagos();
        abrirReembolso(c[0]);
        marcarCurso(c[0]);
        assertThat(espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ree-auto"))).getText()).contains("se aprueba automáticamente");
        driver.findElement(By.id("ree-motivo")).sendKeys("Me inscribí en el curso equivocado");
        capturar("CP-RE09a_aviso_aprobacion_automatica");
        clicSeguro(driver.findElement(By.id("ree-btn-enviar")));
        esperarMensaje("aprobado automáticamente");
        WebElement fila = filaReembolso(c[0]);
        assertThat(fila.getAttribute("data-estado")).isEqualTo("devuelto");
        assertThat(fila.getText()).contains("Aprobado automáticamente").contains("BIO-NC-").contains("PRUEBA-REV-");
        capturar("CP-RE09b_devuelto_automatico");
        cerrarSesionNavegador();
        entrarComoContador();
        verTodosContador();
        WebElement tarjeta = tarjetaContador(c[0]);
        assertThat(tarjeta.getAttribute("data-estado")).isEqualTo("devuelto");
        assertThat(tarjeta.getText()).contains("Automática").contains("Aprobación automática");
        assertThat(tarjeta.findElements(By.cssSelector(".cr-aprobar, .cr-rechazar"))).isEmpty();
        capturar("CP-RE09c_contador_ve_automatica");
    }

    @Test @Order(10)
    @DisplayName("CP-RE10: sin avance y con transferencia, se aprueba sola y queda por devolver; el contador registra la devolución")
    void cpRE10_automaticaTransferencia() {
        String[] c = prepararPagoAprobado("transferencia", 1, NADA);
        entrarComoEstudiante();
        irAMisPagos();
        abrirReembolso(c[0]);
        marcarCurso(c[0]);
        driver.findElement(By.id("ree-motivo")).sendKeys("Cambié de horario y no podré tomarlo");
        clicCentrado(driver.findElement(By.cssSelector("input[name='ree-destino'][value='nequi']")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ree-nequi"))).sendKeys("3001234567");
        clicSeguro(driver.findElement(By.id("ree-btn-enviar")));
        esperarMensaje("aprobado automáticamente");
        assertThat(filaReembolso(c[0]).getAttribute("data-estado")).isEqualTo("aprobado");
        capturar("CP-RE10a_aprobado_por_devolver");
        cerrarSesionNavegador();
        entrarComoContador();
        verTodosContador();
        WebElement tarjeta = tarjetaContador(c[0]);
        assertThat(tarjeta.getAttribute("data-estado")).isEqualTo("aprobado");
        assertThat(tarjeta.getText()).contains("Automática");
        clicSeguro(tarjeta.findElement(By.cssSelector(".cr-devolucion")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("crd-referencia"))).sendKeys("NEQ-AUT-" + marcaCorta());
        clicSeguro(driver.findElement(By.id("crd-btn")));
        esperarMensaje("Devolución registrada");
        espera.until(d -> "devuelto".equals(tarjetaContador(c[0]).getAttribute("data-estado")));
        assertThat(tarjetaContador(c[0]).getText()).contains("Devolución registrada por");
        capturar("CP-RE10b_devolucion_registrada");
    }

    // ------------------------------------------------------------------ Mi institución

    @Test @Order(11)
    @DisplayName("CP-AF01: Mi perfil muestra la institución con «Cambiar de colegio» y «Salir de mi colegio» (o «Unirme» si es independiente)")
    void cpAF01_miInstitucion() {
        entrarComoEstudiante();
        esperarSinMensajes();
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class,'sb-item')][.//span[normalize-space()='Mi perfil']]"))));
        WebElement tarjeta = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("perfil-institucion")));
        espera.until(d -> !d.findElements(By.id("perfil-inst-vinculado")).isEmpty() || !d.findElements(By.id("perfil-inst-independiente")).isEmpty());
        if (!driver.findElements(By.id("perfil-inst-vinculado")).isEmpty()) {
            assertThat(texto("perfil-inst-nombre")).isNotBlank();
            assertThat(driver.findElement(By.id("perfil-btn-salir")).getText()).isEqualTo("Salir de mi colegio");
            assertThat(tarjeta.getText()).contains("¿Cambiaste de colegio?");
        }
        else
            assertThat(tarjeta.getText()).contains("Unirme a mi colegio");
        capturar("CP-AF01_mi_institucion");
    }

    // ------------------------------------------------------------------ preparación por la API

    /**
     * Crea n cursos con precio, inscribe al estudiante y los paga en un solo pago aprobado: con la tarjeta de prueba
     * o por transferencia (comprobante y confirmación del contador). Cada curso tiene dos lecciones. Con avance MUCHO el
     * estudiante revisa la única lectura de la primera lección del primer curso (apertura, 21 s y visto): queda al 50 %.
     * Con POCO esa lección tiene 5 lecturas y revisa una: la lección queda al 20 % y el curso al 10 %. Devuelve los nombres de los cursos.
     */
    /** Avance del primer curso: NADA = 0 % (aprobación automática), POCO = 10 % (la revisa un contador), MUCHO = 50 % (no reembolsable). */
    private static final String NADA = "nada", POCO = "poco", MUCHO = "mucho";

    private String[] prepararPagoAprobado(String medio, int n, String avance) {
        driver.get(URL_AUTH);
        String marca = marcaCorta();
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(90));
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
            "const [api, marca, medio, n, avance, au, ac, du, dc, eu, ec, cu, cc] = arguments; const listo = arguments[arguments.length - 1];" +
            "const json = (h) => ({'Content-Type':'application/json', ...(h ? {'Authorization':'Bearer ' + h} : {})});" +
            "const pedir = async (m, ruta, token, cuerpo) => { const x = await fetch(api + ruta, {method: m, headers: json(token), body: cuerpo ? JSON.stringify(cuerpo) : undefined}); const d = await x.json().catch(() => ({})); if (!x.ok) throw new Error(ruta + ' ' + x.status + ' ' + (d.mensaje || '')); return d; };" +
            "const login = async (u, c) => (await pedir('POST', '/api/auth/login', null, {usuario: u, contrasena: c}));" +
            "(async () => { try {" +
            " let A, D, E, C; try { A = await login(au, ac); D = await login(du, dc); E = await login(eu, ec); } catch (e) { return listo('SIN_CUENTAS ' + e.message); }" +
            " if (medio === 'transferencia') { try { C = await login(cu, cc); } catch (e) { return listo('SIN_CONTADOR ' + e.message); } }" +
            " const areas = await pedir('GET', '/api/catalogos/areas'); const grados = await pedir('GET', '/api/catalogos/grados');" +
            " const nombres = [], ids = []; let material = null;" +
            " for (let k = 1; k <= n; k++) {" +
            "  const nombre = 'Curso Selenium R' + marca + '-' + k;" +
            "  const c = await pedir('POST', '/api/docente/cursos', D.token, {nombre, idArea: areas[0].id, idGrado: grados[0].id, codigo: 'SR' + k + '-' + marca, descripcion: 'Curso de prueba de reembolsos'});" +
            "  const l1 = await pedir('POST', '/api/cursos/' + c.id + '/lecciones', D.token, {nombre: 'Lección Reembolso'});" +
            "  const lr = await pedir('POST', '/api/lecciones/' + l1.id + '/materiales', D.token, {nombre: 'Lectura Reembolso', tipo: 'lectura', contenido: '<p>Texto</p>'});" +
            "  if (k === 1) material = lr.materiales[lr.materiales.length - 1].id;" +
            "  if (k === 1 && avance === 'poco') for (let m = 2; m <= 5; m++) await pedir('POST', '/api/lecciones/' + l1.id + '/materiales', D.token, {nombre: 'Lectura Reembolso ' + m, tipo: 'lectura', contenido: '<p>Texto</p>'});" +
            "  await pedir('PATCH', '/api/lecciones/' + l1.id + '/estado', D.token, {estado: 'activa'});" +
            "  const l2 = await pedir('POST', '/api/cursos/' + c.id + '/lecciones', D.token, {nombre: 'Lección Reembolso 2'});" +
            "  await pedir('POST', '/api/lecciones/' + l2.id + '/materiales', D.token, {nombre: 'Lectura Reembolso 2', tipo: 'lectura', contenido: '<p>Texto</p>'});" +
            "  await pedir('PATCH', '/api/lecciones/' + l2.id + '/estado', D.token, {estado: 'activa'});" +
            "  await pedir('PATCH', '/api/cursos/' + c.id, A.token, {precio: 85000});" +
            "  await pedir('PATCH', '/api/cursos/' + c.id + '/estado', A.token, {estado: 'activo'});" +
            "  const i = await pedir('POST', '/api/estudiante/inscripciones', E.token, {idCurso: c.id});" +
            "  nombres.push(nombre); ids.push(i.id);" +
            " }" +
            " const p = await pedir('POST', '/api/estudiante/pagos', E.token, {idsInscripcion: ids, medio});" +
            " if (medio === 'tarjeta') { const r = await pedir('POST', '/api/estudiante/pagos/' + p.id + '/pasarela', E.token, {numeroTarjeta: '4242424242424242'}); if (r.estado !== 'aprobado') return listo('ERROR pago ' + r.estado); }" +
            " else {" +
            "  const png = Uint8Array.from(atob('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=='), ch => ch.charCodeAt(0));" +
            "  const fd = new FormData(); fd.append('archivo', new Blob([png], {type: 'image/png'}), 'comprobante.png');" +
            "  const sub = await fetch(api + '/api/archivos', {method: 'POST', headers: {'Authorization': 'Bearer ' + E.token}, body: fd}); const arch = await sub.json();" +
            "  if (!sub.ok) return listo('ERROR archivo ' + (arch.mensaje || sub.status));" +
            "  await pedir('PUT', '/api/estudiante/pagos/' + p.id + '/comprobante', E.token, {url: arch.url});" +
            "  await pedir('PATCH', '/api/pagos/' + p.id + '/confirmacion', C.token);" +
            " }" +
            " if (avance !== 'nada') { await pedir('POST', '/api/estudiante/materiales/' + material + '/apertura', E.token); await new Promise(ok => setTimeout(ok, 21000)); await pedir('PUT', '/api/estudiante/materiales/' + material + '/visto', E.token); }" +
            " listo('OK|' + nombres.join('|'));" +
            "} catch (e) { listo('ERROR ' + e.message); } })();",
            URL_API, marca, medio, n, avance, ADMIN_USUARIO, ADMIN_CLAVE, DOCENTE_USUARIO, DOCENTE_CLAVE, ESTUDIANTE_USUARIO, ESTUDIANTE_CLAVE,
            CONTADOR_USUARIO, CONTADOR_CLAVE);
        String res = String.valueOf(r);
        assumeTrue(!res.startsWith("SIN_CUENTAS") && !res.startsWith("SIN_CONTADOR"), "Faltan cuentas de prueba: " + res);
        assertThat(res).as("Preparación de datos por la API").startsWith("OK|");
        String[] partes = res.split("\\|", -1);
        String[] nombres = new String[partes.length - 1];
        System.arraycopy(partes, 1, nombres, 0, nombres.length);
        return nombres;
    }

    // ------------------------------------------------------------------ pasos reutilizables

    private void entrarComoEstudiante() {
        driver.get(URL_AUTH);
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-user"))).sendKeys(ESTUDIANTE_USUARIO);
        driver.findElement(By.id("login-pass")).sendKeys(ESTUDIANTE_CLAVE);
        espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space()='Ingresar']"))).click();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(6)).until(ExpectedConditions.urlContains("biolearn_estudiante_prototipo.html"));
        } catch (TimeoutException e) {
            assumeTrue(false, "No se pudo iniciar sesión con " + ESTUDIANTE_USUARIO);
        }
        espera.until(ExpectedConditions.titleIs("BioLearn — Estudiante"));
    }

    private void entrarComoContador() { entrarComoContador(CONTADOR_USUARIO, CONTADOR_CLAVE); }

    /** Inicia la sesión del contador por la API y abre Reembolsos con el filtro «Por revisar». */
    private void entrarComoContador(String usuario, String clave) {
        driver.get(URL_AUTH);
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
            "const [api, u, c] = arguments; const listo = arguments[arguments.length - 1];" +
            "fetch(api + '/api/auth/login', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({usuario: u, contrasena: c})})" +
            ".then(x => x.ok ? x.json() : Promise.reject(x.status))" +
            ".then(d => { sessionStorage.setItem('biolearn_token', d.token); sessionStorage.setItem('biolearn_usuario', JSON.stringify(d.usuario)); listo('OK'); })" +
            ".catch(e => listo('SIN_CONTADOR ' + e));",
            URL_API, usuario, clave);
        assumeTrue("OK".equals(String.valueOf(r)), "Se necesita la cuenta de contador de pruebas (" + usuario + "): " + r);
        driver.get(URL_CONTADOR);
        espera.until(ExpectedConditions.titleIs("BioLearn — Contador"));
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class,'sb-item')][.//span[normalize-space()='Reembolsos']]"))));
        espera.until(d -> !d.findElements(By.cssSelector("#cr-lista .cr-tarjeta")).isEmpty() || !d.findElements(By.id("cr-vacio")).isEmpty());
    }

    private void verTodosContador() {
        esperarSinMensajes();
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".cr-chip[data-estado='']"))));
        espera.until(d -> !d.findElements(By.cssSelector("#cr-lista .cr-tarjeta")).isEmpty());
    }

    private void cerrarSesionNavegador() { ((JavascriptExecutor) driver).executeScript("sessionStorage.clear();"); }

    private void irAMisCursos() {
        esperarSinMensajes();
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class,'sb-item')][.//span[normalize-space()='Mis cursos']]"))));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("est-mis-cursos")));
    }

    private void irAMisPagos() {
        esperarSinMensajes();
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class,'sb-item')][.//span[normalize-space()='Mis pagos']]"))));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("pag-tabla")));
        espera.until(d -> !d.findElements(By.id("ree-tabla")).isEmpty() || !d.findElements(By.id("ree-vacio")).isEmpty());
    }

    /** Abre «Solicitar reembolso» en la fila del historial que tiene ese curso. */
    private void abrirReembolso(String curso) {
        WebElement fila = espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//*[@id='pag-tabla']//tbody/tr[td[contains(.,'" + curso + "')]])[1]")));
        clicSeguro(fila.findElement(By.cssSelector(".pag-btn-reembolso")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#modal-reembolso.open")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(itemReembolso(curso)));
    }

    private void marcarCurso(String curso) {
        WebElement input = driver.findElement(itemReembolso(curso)).findElement(By.tagName("input"));
        if (!input.isSelected()) clicCentrado(input);
    }

    /** Solicita el reembolso de un curso pagado con tarjeta. */
    private void solicitar(String curso) {
        irAMisPagos();
        abrirReembolso(curso);
        marcarCurso(curso);
        driver.findElement(By.id("ree-motivo")).sendKeys("Me inscribí en el curso equivocado");
        clicSeguro(driver.findElement(By.id("ree-btn-enviar")));
        esperarMensaje("Solicitud enviada");
        filaReembolso(curso);
    }

    private By itemReembolso(String curso) { return By.cssSelector("#ree-cursos .ree-item[data-curso='" + curso + "']"); }

    private WebElement filaReembolso(String curso) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//*[@id='ree-tabla']//tr[contains(@class,'ree-fila')][td[contains(.,'" + curso + "')]])[1]")));
    }

    private WebElement tarjetaContador(String curso) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//*[@id='cr-lista']//div[contains(@class,'cr-tarjeta')][contains(.,'" + curso + "')])[1]")));
    }

    private WebElement tarjetaMiCurso(String nombre) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='est-mis-cursos']//div[contains(@class,'est-mi-curso')][@data-nombre='" + nombre + "']")));
    }

    private String texto(String id) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.id(id))).getText().trim();
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
