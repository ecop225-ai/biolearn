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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pruebas de extremo a extremo (E2E) con Selenium del acceso a los cursos por tiempo (migración 011):
 * meses para terminar y de repaso definidos por el Administrador, plazo visible en el catálogo y en Mis cursos,
 * curso completado en repaso (solo lectura) y fechas en la lista de inscritos.
 *
 * Cada ejecución prepara por la API un curso gratuito «Curso Selenium P<marca>» con una lección de una lectura,
 * 18 meses para terminar y 3 de repaso. Los vencimientos se prueban en la guía manual (cambiando las fechas en
 * MySQL), porque no se pueden esperar meses en una prueba automática.
 * Requiere las mismas cuentas de EstudianteSeleniumTest; sin ellas, los casos se omiten.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Selenium — Acceso a los cursos por tiempo: plazo, repaso y renovación")
class AccesoSeleniumTest {

    private static final String URL_AUTH = System.getProperty("biolearn.frontend.url",
            "http://127.0.0.1:5173/prototipo-auth/biolearn_auth_prototipo.html");
    private static final String URL_ESTUDIANTE = URL_AUTH.replace("prototipo-auth/biolearn_auth_prototipo.html", "prototipo-estudiante/biolearn_estudiante_prototipo.html");
    private static final String URL_API = System.getProperty("biolearn.backend.url", "http://localhost:8080");
    private static final String ESTUDIANTE_USUARIO = System.getProperty("biolearn.estudiante.usuario", "estudiante.pruebas@biolearn.co");
    private static final String ESTUDIANTE_CLAVE = System.getProperty("biolearn.estudiante.clave", "Estudiante2026");
    private static final String DOCENTE_USUARIO = System.getProperty("biolearn.docente.usuario", "docente.pruebas@biolearn.co");
    private static final String DOCENTE_CLAVE = System.getProperty("biolearn.docente.clave", "Docente2026");
    private static final String ADMIN_USUARIO = System.getProperty("biolearn.admin.usuario", "admin@biolearn.co");
    private static final String ADMIN_CLAVE = System.getProperty("biolearn.admin.clave", "Admin2026");
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

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static String cursoCompartido;

    // ------------------------------------------------------------------ casos

    @Test @Order(1)
    @DisplayName("CP-AC01: el Administrador ve y cambia los meses para terminar y de repaso del curso")
    void cpAC01_mesesDelCurso() {
        String curso = curso();
        iniciarSesion(ADMIN_USUARIO, ADMIN_CLAVE, "biolearn_admin_prototipo.html");
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class,'sb-item')][.//span[normalize-space()='Cursos']]"))));
        WebElement buscar = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("cur-buscar")));
        buscar.sendKeys(curso);
        WebElement fila = espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//tr[.//*[normalize-space()='" + curso + "']]")));
        clicSeguro(fila.findElement(By.xpath(".//button[contains(@onclick,'verDetalleCurso')]")));
        espera.until(d -> d.findElement(By.tagName("body")).getText().contains("18 meses para terminar · 3 de repaso"));
        capturar("CP-AC01a_detalle_plazo");
        ((JavascriptExecutor) driver).executeScript("cerrarDetalleCurso && cerrarDetalleCurso();");
        ((JavascriptExecutor) driver).executeScript("abrirModalCurso(arguments[0]);", idCurso(curso));
        WebElement meses = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("cur-meses")));
        assertThat(meses.getAttribute("value")).isEqualTo("18");
        assertThat(driver.findElement(By.id("cur-repaso")).getAttribute("value")).isEqualTo("3");
        capturar("CP-AC01b_formulario_meses");
        meses.clear(); meses.sendKeys("40");
        clicSeguro(driver.findElement(By.id("cur-modal-btn")));
        esperarMensaje("Los meses para terminar deben estar entre 1 y 36");
        capturar("CP-AC01c_meses_fuera_de_rango");
    }

    @Test @Order(2)
    @DisplayName("CP-AC02: el catálogo muestra los meses para terminar y de repaso antes de inscribirse")
    void cpAC02_plazoEnCatalogo() {
        String curso = curso();
        entrarComoEstudiante();
        abrirCatalogo();
        clicSeguro(tarjetaCatalogo(curso).findElement(By.xpath(".//button[contains(.,'Ver detalle')]")));
        WebElement plazo = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("est-plazo-catalogo")));
        assertThat(plazo.getText()).contains("18 meses").contains("3 meses");
        capturar("CP-AC02_plazo_catalogo");
    }

    @Test @Order(3)
    @DisplayName("CP-AC03: al inscribirse el plazo empieza y Mis cursos muestra la fecha límite")
    void cpAC03_fechaLimite() {
        String curso = curso();
        entrarComoEstudiante();
        inscribirseDesdeDetalle(curso);
        String limite = LocalDate.now().plusMonths(18).format(DMY);
        WebElement tarjeta = tarjetaMiCurso(curso);
        espera.until(d -> tarjeta.getText().contains("Termina antes del " + limite));
        assertThat(tarjeta.findElement(By.cssSelector(".est-plazo")).getText()).contains("quedan");
        capturar("CP-AC03a_fecha_limite_mis_cursos");
        abrirCursoInscrito(curso);
        assertThat(texto("est-plazo-curso")).contains(limite);
        capturar("CP-AC03b_fecha_limite_curso");
    }

    @Test @Order(4)
    @DisplayName("CP-AC04: al completar el curso queda en repaso: solo lectura con la fecha en que termina")
    void cpAC04_repaso() {
        String curso = curso();
        entrarComoEstudiante();
        irAMisCursos();
        abrirCursoInscrito(curso);
        abrirLeccion("Lección Selenium 1");
        revisarMaterial("Lectura Selenium 1");
        esperarMensaje("Completaste el curso");
        String finRepaso = LocalDate.now().plusMonths(3).format(DMY);
        irAMisCursos();
        WebElement tarjeta = tarjetaMiCurso(curso);
        espera.until(d -> tarjeta.getText().contains("Repaso hasta el " + finRepaso));
        capturar("CP-AC04a_repaso_mis_cursos");
        abrirCursoInscrito(curso);
        assertThat(texto("est-modo-repaso")).contains("Modo repaso hasta el " + finRepaso);
        abrirLeccion("Lección Selenium 1");
        assertThat(texto("est-modo-repaso")).contains("tu avance no cambia");
        capturar("CP-AC04b_leccion_en_repaso");
    }

    @Test @Order(5)
    @DisplayName("CP-AC05: el docente ve hasta cuándo puede repasar el estudiante")
    void cpAC05_inscritosConPlazo() {
        String curso = curso();
        iniciarSesion(DOCENTE_USUARIO, DOCENTE_CLAVE, "biolearn_docente_prototipo.html");
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class,'sb-item')][.//span[normalize-space()='Cursos']]"))));
        WebElement fila = espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='doc-cursos-lista']//tr[contains(@class,'doc-curso')][@data-nombre='" + curso + "']")));
        clicSeguro(fila.findElement(By.xpath(".//button[contains(.,'Inscritos')]")));
        WebElement inscrito = espera.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".dins-fila[data-correo='" + ESTUDIANTE_USUARIO + "']")));
        assertThat(inscrito.getText()).contains("Completada (en repaso)").contains("repaso hasta el " + LocalDate.now().plusMonths(3).format(DMY));
        capturar("CP-AC05_inscritos_docente");
    }

    // ------------------------------------------------------------------ preparación de datos por la API

    /** El curso de la ejecución: se crea la primera vez y lo usan los casos siguientes (el orden importa). */
    private String curso() {
        if (cursoCompartido == null) cursoCompartido = prepararCurso();
        return cursoCompartido;
    }

    /** El docente crea el curso con una lección de una lectura; el Administrador fija 18 meses y 3 de repaso y lo publica (gratuito). */
    private String prepararCurso() {
        driver.get(URL_AUTH);
        String marca = marcaCorta();
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
            "const [api, marca, au, ac, du, dc] = arguments; const listo = arguments[arguments.length - 1];" +
            "const json = (h) => ({'Content-Type':'application/json', ...(h ? {'Authorization':'Bearer ' + h} : {})});" +
            "const pedir = async (m, ruta, token, cuerpo) => { const x = await fetch(api + ruta, {method: m, headers: json(token), body: cuerpo ? JSON.stringify(cuerpo) : undefined}); const d = await x.json().catch(() => ({})); if (!x.ok) throw new Error(ruta + ' ' + x.status + ' ' + (d.mensaje || '')); return d; };" +
            "const login = async (u, c) => (await pedir('POST', '/api/auth/login', null, {usuario: u, contrasena: c}));" +
            "(async () => { try {" +
            " let A, D; try { A = await login(au, ac); D = await login(du, dc); } catch (e) { return listo('SIN_CUENTAS ' + e.message); }" +
            " const areas = await pedir('GET', '/api/catalogos/areas'); const grados = await pedir('GET', '/api/catalogos/grados');" +
            " const nombre = 'Curso Selenium P' + marca;" +
            " const c = await pedir('POST', '/api/docente/cursos', D.token, {nombre, idArea: areas[0].id, idGrado: grados[0].id, codigo: 'SP-' + marca, descripcion: 'Curso de prueba del acceso por tiempo'});" +
            " const l1 = await pedir('POST', '/api/cursos/' + c.id + '/lecciones', D.token, {nombre: 'Lección Selenium 1'});" +
            " await pedir('POST', '/api/lecciones/' + l1.id + '/materiales', D.token, {nombre: 'Lectura Selenium 1', tipo: 'lectura', contenido: '<p>Texto de la lectura</p>'});" +
            " await pedir('PATCH', '/api/lecciones/' + l1.id + '/estado', D.token, {estado: 'activa'});" +
            " await pedir('PATCH', '/api/cursos/' + c.id, A.token, {mesesParaTerminar: 18, mesesRepaso: 3});" +
            " await pedir('PATCH', '/api/cursos/' + c.id + '/estado', A.token, {estado: 'activo'});" +
            " listo('OK|' + nombre);" +
            "} catch (e) { listo('ERROR ' + e.message); } })();",
            URL_API, marca, ADMIN_USUARIO, ADMIN_CLAVE, DOCENTE_USUARIO, DOCENTE_CLAVE);
        String res = String.valueOf(r);
        assumeTrue(!res.startsWith("SIN_CUENTAS"), "Se necesitan las cuentas de Administrador y docente de pruebas: " + res);
        assertThat(res).as("Preparación de datos por la API").startsWith("OK|");
        return res.substring(3);
    }

    /** Id del curso por su nombre, con la sesión que tenga el navegador (Administrador). */
    private Integer idCurso(String nombre) {
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
            "const [api, nombre] = arguments; const listo = arguments[arguments.length - 1];" +
            "fetch(api + '/api/cursos?busqueda=' + encodeURIComponent(nombre), {headers: {'Authorization': 'Bearer ' + sessionStorage.getItem('biolearn_token')}})" +
            ".then(x => x.json()).then(l => listo(String((l.find(c => c.nombre === nombre) || {}).id))).catch(() => listo('null'));",
            URL_API, nombre);
        return Integer.valueOf(String.valueOf(r));
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

    private void cerrarSesionNavegador() {
        ((JavascriptExecutor) driver).executeScript("sessionStorage.clear();");
    }

    private void irAMisCursos() {
        esperarSinMensajes();
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class,'sb-item')][.//span[normalize-space()='Mis cursos']]"))));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("est-mis-cursos")));
    }

    private void abrirCatalogo() {
        irAMisCursos();
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='est-mis-cursos']//span[normalize-space()='Inscribirse a un curso']"))));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("est-catalogo")));
    }

    private void inscribirseDesdeDetalle(String curso) {
        abrirCatalogo();
        clicSeguro(tarjetaCatalogo(curso).findElement(By.xpath(".//button[contains(.,'Ver detalle')]")));
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("est-btn-inscribir"))));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("est-mis-cursos")));
    }

    private void abrirCursoInscrito(String curso) {
        clicSeguro(tarjetaMiCurso(curso));
        espera.until(d -> !d.findElements(By.cssSelector("#est-curso .est-leccion")).isEmpty());
    }

    private void abrirLeccion(String leccion) {
        clicSeguro(filaLeccion(leccion));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("est-leccion-progreso")));
    }

    /** Abre el material y se desplaza hasta el final de su contenido (criterio de una lectura). */
    private void abrirMaterialHastaElFinal(String material) {
        esperarSinMensajes();
        WebElement cabecera = material(material).findElement(By.cssSelector(".est-material-cabecera"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", cabecera);
        cabecera.click();
        WebElement fin = espera.until(d -> material(material).findElement(By.cssSelector(".est-fin-lectura")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", fin);
    }

    /** Revisa el material como lo haría el estudiante y espera a que el portal lo marque solo (tiempo mínimo de 20 s). */
    private void revisarMaterial(String material) {
        abrirMaterialHastaElFinal(material);
        esperaLarga().until(d -> material(material).getText().contains("✓ Visto"));
    }

    private WebDriverWait esperaLarga() { return new WebDriverWait(driver, Duration.ofSeconds(60)); }

    private WebElement tarjetaCatalogo(String nombre) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='est-catalogo']//div[contains(@class,'est-curso-catalogo')][@data-nombre='" + nombre + "']")));
    }

    private WebElement tarjetaMiCurso(String nombre) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='est-mis-cursos']//div[contains(@class,'est-mi-curso')][@data-nombre='" + nombre + "']")));
    }

    private WebElement filaLeccion(String nombre) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='est-curso']//div[contains(@class,'est-leccion')][@data-nombre='" + nombre + "']")));
    }

    private WebElement material(String nombre) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='est-leccion']//div[contains(@class,'est-material')][@data-nombre='" + nombre + "']")));
    }

    private static String marcaCorta() {
        String m = String.valueOf(System.currentTimeMillis());
        return m.substring(m.length() - 7);
    }

    private List<WebElement> filas(String idLista) {
        return driver.findElements(By.xpath("//*[@id='" + idLista + "']//tbody/tr[not(td[contains(@class,'empty-state')])]"));
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

    /** Centra el elemento en la pantalla antes de hacer clic: la barra superior del prototipo es fija y, si
     *  Selenium deja el elemento pegado al borde superior, el clic lo recibe la barra (ElementClickIntercepted). */
    private void clicCentrado(WebElement elemento) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", elemento);
        espera.until(ExpectedConditions.elementToBeClickable(elemento)).click();
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
