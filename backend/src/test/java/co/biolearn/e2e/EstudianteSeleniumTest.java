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
 * Pruebas de extremo a extremo (E2E) con Selenium del portal del estudiante (Fase 3 del módulo de Cursos):
 * catálogo, código de inscripción del colegio, inscripción gratuita y con precio, carrito, lecciones con
 * prerrequisito, progreso por material, completación del curso, retiro y seguimiento del docente.
 *
 * Cada caso prepara sus datos por la API (como lo harían el docente y el Administrador): un curso
 * «Curso Selenium E<marca>» con dos lecciones (la segunda con prerrequisito), materiales y un recurso de apoyo.
 * Requiere: backend y frontend en ejecución; las cuentas de Administrador, de docente de pruebas y de un
 * estudiante con institución (biolearn.estudiante.usuario / biolearn.estudiante.clave; por defecto
 * estudiante.pruebas@biolearn.co / Estudiante2026). Si una cuenta no existe, los casos se omiten.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Selenium — Portal del estudiante: inscripciones y aprendizaje (Fase 3)")
class EstudianteSeleniumTest {

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

    // ------------------------------------------------------------------ casos

    @Test @Order(1)
    @DisplayName("CP-ES01: el catálogo muestra el curso publicado como gratuito")
    void cpES01_catalogo() {
        String curso = prepararCurso(0, false).nombre;
        entrarComoEstudiante();
        abrirCatalogo();
        WebElement tarjeta = tarjetaCatalogo(curso);
        assertThat(tarjeta.getText()).contains("Gratuito").contains("2 lecciones");
        capturar("CP-ES01_catalogo");
    }

    @Test @Order(2)
    @DisplayName("CP-ES02: el detalle muestra el temario y el botón para inscribirse")
    void cpES02_detalle() {
        String curso = prepararCurso(0, false).nombre;
        entrarComoEstudiante();
        abrirCatalogo();
        clicSeguro(tarjetaCatalogo(curso).findElement(By.xpath(".//button[contains(.,'Ver detalle')]")));
        WebElement det = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("est-btn-inscribir")));
        assertThat(texto("est-detalle")).contains("Lección Selenium 1").contains("Lección Selenium 2");
        assertThat(det.getText()).isEqualTo("Inscribirme gratis");
        capturar("CP-ES02_detalle");
    }

    @Test @Order(3)
    @DisplayName("CP-ES03: inscribirse en un curso gratuito lo deja activo en «Mis cursos»")
    void cpES03_inscripcionGratuita() {
        String curso = prepararCurso(0, false).nombre;
        entrarComoEstudiante();
        inscribirseDesdeDetalle(curso);
        esperarMensaje("Inscripción exitosa");
        assertThat(tarjetaMiCurso(curso).getText()).contains("Inscrito").contains("0% completado");
        capturar("CP-ES03_inscripcion_gratuita");
    }

    @Test @Order(4)
    @DisplayName("CP-ES04: un curso en el que ya está inscrito no se puede volver a agregar")
    void cpES04_yaInscrito() {
        String curso = prepararCurso(0, false).nombre;
        entrarComoEstudiante();
        inscribirseDesdeDetalle(curso);
        esperarMensaje("Inscripción exitosa");
        abrirCatalogo();
        // el catálogo abre en «No inscritos»: el curso inscrito se ve en «Ya inscritos»
        espera.until(d -> d.findElements(By.xpath("//*[@id='est-catalogo']//div[contains(@class,'est-curso-catalogo')][@data-nombre='" + curso + "']")).isEmpty());
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("est-ver-inscritos"))));
        WebElement boton = tarjetaCatalogo(curso).findElement(By.xpath(".//button[contains(.,'Inscrito')]"));
        assertThat(boton.isEnabled()).isFalse();
        capturar("CP-ES04_ya_inscrito");
    }

    @Test @Order(5)
    @DisplayName("CP-ES05: un curso con precio queda pendiente de pago y sin acceso a las lecciones")
    void cpES05_pendientePago() {
        String curso = prepararCurso(95000, false).nombre;
        entrarComoEstudiante();
        inscribirseDesdeDetalle(curso);
        esperarMensaje("pendiente de pago");
        WebElement tarjeta = tarjetaMiCurso(curso);
        assertThat(tarjeta.getText()).contains("Pendiente de pago").contains("cuando se confirme el pago");
        capturar("CP-ES05_pendiente_pago");
    }

    @Test @Order(6)
    @DisplayName("CP-ES06: con el código de su colegio, la inscripción queda vinculada a la institución")
    void cpES06_codigoColegio() {
        Curso c = prepararCurso(0, true);
        entrarComoEstudiante();
        abrirCatalogo();
        driver.findElement(By.id("est-codigo")).sendKeys(c.codigo);
        clicSeguro(driver.findElement(By.id("est-btn-codigo")));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("est-detalle"), "Curso asignado por"));
        capturar("CP-ES06a_detalle_por_codigo");
        clicSeguro(driver.findElement(By.id("est-btn-inscribir")));
        esperarMensaje("Inscripción exitosa");
        assertThat(tarjetaMiCurso(c.nombre).getText()).contains("🏫");
        capturar("CP-ES06b_inscripcion_vinculada");
    }

    @Test @Order(7)
    @DisplayName("CP-ES07: un código de inscripción inexistente muestra un aviso")
    void cpES07_codigoInexistente() {
        entrarComoEstudiante();
        abrirCatalogo();
        driver.findElement(By.id("est-codigo")).sendKeys("ZZZZZZZZ");
        clicSeguro(driver.findElement(By.id("est-btn-codigo")));
        assertThat(esperarMensaje("no existe").getText()).startsWith("⚠️");
        capturar("CP-ES07_codigo_inexistente");
    }

    @Test @Order(8)
    @DisplayName("CP-ES08: inscribirse desde el carrito")
    void cpES08_carrito() {
        String curso = prepararCurso(0, false).nombre;
        entrarComoEstudiante();
        abrirCatalogo();
        clicSeguro(tarjetaCatalogo(curso).findElement(By.xpath(".//button[contains(.,'+ Agregar')]")));
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("est-btn-carrito"))));
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("est-btn-confirmar"))));
        esperarMensaje("inscripción(es) activa(s)");
        tarjetaMiCurso(curso);
        capturar("CP-ES08_carrito");
    }

    @Test @Order(9)
    @DisplayName("CP-ES09: la lección con prerrequisito aparece bloqueada")
    void cpES09_bloqueo() {
        String curso = prepararCurso(0, false).nombre;
        entrarComoEstudiante();
        inscribirseDesdeDetalle(curso);
        abrirCursoInscrito(curso);
        assertThat(filaLeccion("Lección Selenium 2").getText()).contains("Bloqueada").contains("Lección Selenium 1");
        capturar("CP-ES09_leccion_bloqueada");
    }

    @Test @Order(10)
    @DisplayName("CP-ES10: la lección muestra sus materiales y los recursos de apoyo")
    void cpES10_leccion() {
        String curso = prepararCurso(0, false).nombre;
        entrarComoEstudiante();
        inscribirseDesdeDetalle(curso);
        abrirCursoInscrito(curso);
        abrirLeccion("Lección Selenium 1");
        assertThat(texto("est-leccion-progreso")).contains("0 de 2 materiales vistos");
        clicSeguro(material("Lectura Selenium 1").findElement(By.xpath(".//div[contains(@onclick,'toggleMaterial')]")));
        espera.until(d -> material("Lectura Selenium 1").getText().contains("Recurso de apoyo Selenium"));
        capturar("CP-ES10_leccion_materiales");
    }

    @Test @Order(11)
    @DisplayName("CP-ES11: revisar los materiales los marca solos, completa la lección y desbloquea la siguiente")
    void cpES11_progreso() {
        String curso = prepararCurso(0, false).nombre;
        entrarComoEstudiante();
        inscribirseDesdeDetalle(curso);
        abrirCursoInscrito(curso);
        abrirLeccion("Lección Selenium 1");
        revisarMaterial("Lectura Selenium 1");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("est-leccion-progreso"), "50%"));
        capturar("CP-ES11a_mitad");
        revisarMaterial("Lectura Selenium 2");
        esperarMensaje("Completaste la lección");
        assertThat(texto("est-leccion-progreso")).contains("Lección completada");
        clicSeguro(boton("← Volver a lecciones"));
        espera.until(d -> !filaLeccion("Lección Selenium 2").getText().contains("Bloqueada"));
        capturar("CP-ES11b_siguiente_desbloqueada");
    }

    @Test @Order(12)
    @DisplayName("CP-ES12: completar todas las lecciones completa el curso")
    void cpES12_cursoCompletado() {
        String curso = prepararCurso(0, false).nombre;
        entrarComoEstudiante();
        inscribirseDesdeDetalle(curso);
        abrirCursoInscrito(curso);
        abrirLeccion("Lección Selenium 1");
        revisarMaterial("Lectura Selenium 1");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("est-leccion-progreso"), "50%"));
        revisarMaterial("Lectura Selenium 2");
        esperarMensaje("Completaste la lección");
        clicSeguro(boton("← Volver a lecciones"));
        abrirLeccion("Lección Selenium 2");
        revisarMaterial("Lectura Selenium 3");
        esperarMensaje("Completaste el curso");
        clicSeguro(boton("← Volver a lecciones"));
        clicSeguro(boton("← Volver a mis cursos"));
        assertThat(tarjetaMiCurso(curso).getText()).contains("Completado").contains("100% completado");
        capturar("CP-ES12_curso_completado");
    }

    @Test @Order(13)
    @DisplayName("CP-ES13: una lectura no se marca antes del tiempo mínimo y luego se marca sola")
    void cpES13_marcadoAutomatico() {
        String curso = prepararCurso(0, false).nombre;
        entrarComoEstudiante();
        inscribirseDesdeDetalle(curso);
        abrirCursoInscrito(curso);
        abrirLeccion("Lección Selenium 1");
        assertThat(driver.findElements(By.cssSelector(".est-visto-check"))).as("ya no hay casilla manual").isEmpty();
        abrirMaterialHastaElFinal("Lectura Selenium 1");
        espera.until(d -> material("Lectura Selenium 1").getText().contains("En revisión"));
        assertThat(texto("est-leccion-progreso")).contains("0 de 2");
        capturar("CP-ES13a_en_revision");
        esperaLarga().until(d -> material("Lectura Selenium 1").getText().contains("✓ Visto"));
        assertThat(texto("est-leccion-progreso")).contains("1 de 2");
        capturar("CP-ES13b_marcado_automatico");
    }

    @Test @Order(14)
    @DisplayName("CP-ES14: retirarse del curso y volver a inscribirse conserva el progreso")
    void cpES14_retiro() {
        String curso = prepararCurso(0, false).nombre;
        entrarComoEstudiante();
        inscribirseDesdeDetalle(curso);
        abrirCursoInscrito(curso);
        abrirLeccion("Lección Selenium 1");
        revisarMaterial("Lectura Selenium 1");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("est-leccion-progreso"), "50%"));
        clicSeguro(boton("← Volver a lecciones"));
        clicSeguro(boton("← Volver a mis cursos"));
        clicSeguro(tarjetaMiCurso(curso).findElement(By.xpath(".//button[contains(.,'Retirarme')]")));
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("confirm-btn"))));
        esperarMensaje("Te retiraste");
        espera.until(d -> d.findElements(By.xpath("//*[@id='est-mis-cursos']//div[contains(@class,'est-mi-curso')][@data-nombre='" + curso + "']")).isEmpty());
        capturar("CP-ES14a_retirado");
        inscribirseDesdeDetalle(curso);
        assertThat(tarjetaMiCurso(curso).getText()).contains("25% completado");
        capturar("CP-ES14b_reinscrito_con_progreso");
    }

    @Test @Order(15)
    @DisplayName("CP-ES15: un usuario que no es estudiante no ve el portal del estudiante")
    void cpES15_accesoDenegado() {
        iniciarSesion(ADMIN_USUARIO, ADMIN_CLAVE, "biolearn_admin_prototipo.html");
        driver.get(URL_ESTUDIANTE);
        assertThat(espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("acceso-denegado"))).getText()).contains("Acceso solo para estudiantes");
        capturar("CP-ES15_acceso_denegado");
    }

    @Test @Order(16)
    @DisplayName("CP-ES16: el docente ve al estudiante inscrito con su avance")
    void cpES16_inscritosDocente() {
        String curso = prepararCurso(0, false).nombre;
        entrarComoEstudiante();
        inscribirseDesdeDetalle(curso);
        cerrarSesionNavegador();
        iniciarSesion(DOCENTE_USUARIO, DOCENTE_CLAVE, "biolearn_docente_prototipo.html");
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class,'sb-item')][.//span[normalize-space()='Cursos']]"))));
        WebElement fila = espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='doc-cursos-lista']//tr[contains(@class,'doc-curso')][@data-nombre='" + curso + "']")));
        clicSeguro(fila.findElement(By.xpath(".//button[contains(.,'Inscritos')]")));
        espera.until(d -> !d.findElements(By.cssSelector(".dins-fila[data-correo='" + ESTUDIANTE_USUARIO + "']")).isEmpty());
        capturar("CP-ES16_inscritos_docente");
    }

    // ------------------------------------------------------------------ preparación de datos por la API

    private record Curso(String nombre, String codigo) { }

    /** El docente crea el curso con dos lecciones (la segunda con prerrequisito); el Administrador fija el precio, lo publica y, si se pide, lo contrata para el colegio del estudiante. */
    private Curso prepararCurso(int precio, boolean contratar) {
        driver.get(URL_AUTH);
        String marca = marcaCorta();
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
            "const [api, marca, precio, contratar, au, ac, du, dc, eu, ec] = arguments; const listo = arguments[arguments.length - 1];" +
            "const json = (h) => ({'Content-Type':'application/json', ...(h ? {'Authorization':'Bearer ' + h} : {})});" +
            "const pedir = async (m, ruta, token, cuerpo) => { const x = await fetch(api + ruta, {method: m, headers: json(token), body: cuerpo ? JSON.stringify(cuerpo) : undefined}); const d = await x.json().catch(() => ({})); if (!x.ok) throw new Error(ruta + ' ' + x.status + ' ' + (d.mensaje || '')); return d; };" +
            "const login = async (u, c) => (await pedir('POST', '/api/auth/login', null, {usuario: u, contrasena: c}));" +
            "(async () => { try {" +
            " let A, D, E; try { A = await login(au, ac); D = await login(du, dc); E = await login(eu, ec); } catch (e) { return listo('SIN_CUENTAS ' + e.message); }" +
            " const areas = await pedir('GET', '/api/catalogos/areas'); const grados = await pedir('GET', '/api/catalogos/grados');" +
            " const nombre = 'Curso Selenium E' + marca;" +
            " const c = await pedir('POST', '/api/docente/cursos', D.token, {nombre, idArea: areas[0].id, idGrado: grados[0].id, codigo: 'SE-' + marca, descripcion: 'Curso de prueba del portal del estudiante'});" +
            " const l1 = await pedir('POST', '/api/cursos/' + c.id + '/lecciones', D.token, {nombre: 'Lección Selenium 1'});" +
            " let det = await pedir('POST', '/api/lecciones/' + l1.id + '/materiales', D.token, {nombre: 'Lectura Selenium 1', tipo: 'lectura', contenido: '<p>Texto de la lectura 1</p>'});" +
            " const m1 = det.materiales[det.materiales.length - 1].id;" +
            " await pedir('POST', '/api/materiales/' + m1 + '/recursos', D.token, {nombre: 'Recurso de apoyo Selenium', tipoRecurso: 'enlace', url: 'https://es.wikipedia.org/wiki/Manglar'});" +
            " await pedir('POST', '/api/lecciones/' + l1.id + '/materiales', D.token, {nombre: 'Lectura Selenium 2', tipo: 'lectura', contenido: '<p>Texto de la lectura 2</p>'});" +
            " await pedir('PATCH', '/api/lecciones/' + l1.id + '/estado', D.token, {estado: 'activa'});" +
            " const l2 = await pedir('POST', '/api/cursos/' + c.id + '/lecciones', D.token, {nombre: 'Lección Selenium 2', idPrerequisito: l1.id});" +
            " await pedir('POST', '/api/lecciones/' + l2.id + '/materiales', D.token, {nombre: 'Lectura Selenium 3', tipo: 'lectura', contenido: '<p>Texto de la lectura 3</p>'});" +
            " await pedir('PATCH', '/api/lecciones/' + l2.id + '/estado', D.token, {estado: 'activa'});" +
            " if (precio > 0) await pedir('PATCH', '/api/cursos/' + c.id, A.token, {precio});" +
            " await pedir('PATCH', '/api/cursos/' + c.id + '/estado', A.token, {estado: 'activo'});" +
            " let codigo = '';" +
            " if (contratar) { const p = await pedir('GET', '/api/estudiante/perfil', E.token); if (!p.idInstitucion) return listo('SIN_INSTITUCION');" +
            "   const cur = await pedir('POST', '/api/cursos/' + c.id + '/instituciones', A.token, {idInstitucion: p.idInstitucion});" +
            "   codigo = cur.instituciones.find(i => i.idInstitucion === p.idInstitucion).codigoInscripcion; }" +
            " listo('OK|' + nombre + '|' + codigo);" +
            "} catch (e) { listo('ERROR ' + e.message); } })();",
            URL_API, marca, precio, contratar, ADMIN_USUARIO, ADMIN_CLAVE, DOCENTE_USUARIO, DOCENTE_CLAVE, ESTUDIANTE_USUARIO, ESTUDIANTE_CLAVE);
        String res = String.valueOf(r);
        assumeTrue(!res.startsWith("SIN_CUENTAS"), "Se necesitan las cuentas de Administrador, docente y estudiante de pruebas: " + res);
        assumeTrue(!res.startsWith("SIN_INSTITUCION"), "El estudiante de pruebas debe pertenecer a una institución.");
        assertThat(res).as("Preparación de datos por la API").startsWith("OK|");
        String[] p = res.split("\\|", -1);
        return new Curso(p[1], p[2]);
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
