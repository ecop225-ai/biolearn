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
 * Pruebas de extremo a extremo (E2E) con Selenium WebDriver sobre el portal del docente (Fase 2 del módulo de Cursos):
 * interfaz original del prototipo (tarjetas, formularios por tipo de material, editor enriquecido y vistas previas)
 * conectada al backend: cursos propuestos, lecciones, materiales, subida de archivos y control de acceso.
 *
 * Requisitos: MySQL, backend en localhost:8080, frontend en 127.0.0.1:5173, Chrome, y la cuenta de docente de pruebas
 * activa (biolearn.docente.usuario / biolearn.docente.clave; por defecto docente.pruebas@biolearn.co / Docente2026).
 * Si la cuenta no existe, los casos se omiten.
 *
 * Datos que crean las pruebas: cursos «Curso Selenium D<marca>» con sus lecciones (se borran en cascada con el
 * script limpieza_datos_selenium.sql) y archivos en la carpeta uploads del backend.
 * Capturas en backend/target/capturas-selenium/ con el prefijo CP-DC##.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Selenium — Portal del docente: contenido de los cursos (Fase 2)")
class DocenteContenidoSeleniumTest {

    private static final String URL_AUTH = System.getProperty("biolearn.frontend.url",
            "http://127.0.0.1:5173/prototipo-auth/biolearn_auth_prototipo.html");
    private static final String URL_DOCENTE = URL_AUTH.replace("prototipo-auth/biolearn_auth_prototipo.html", "docente/biolearn_docente_prototipo.html");
    private static final String URL_API = System.getProperty("biolearn.backend.url", "http://localhost:8080");
    private static final String DOCENTE_USUARIO = System.getProperty("biolearn.docente.usuario", "docente.pruebas@biolearn.co");
    private static final String DOCENTE_CLAVE = System.getProperty("biolearn.docente.clave", "Docente2026");
    private static final String ADMIN_USUARIO = System.getProperty("biolearn.admin.usuario", "admin@biolearn.co");
    private static final String ADMIN_CLAVE = System.getProperty("biolearn.admin.clave", "Admin2026");
    private static final boolean SIN_VENTANA = Boolean.parseBoolean(System.getProperty("biolearn.headless", "false"));
    private static final String VIDEO = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

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

    @Test @Order(1)
    @DisplayName("CP-DC01: el docente entra a su portal y ve «Mis cursos»")
    void cpDC01_misCursos() {
        entrarComoDocente();
        abrirSeccion("Cursos");
        espera.until(d -> texto("doc-cursos-resumen").startsWith("Mostrando")
                || !d.findElements(By.cssSelector("#doc-cursos-lista .empty-state")).isEmpty());
        assertThat(driver.findElement(By.cssSelector(".sb-user-info .nombre")).getText()).isNotEqualTo("Laura Restrepo");
        capturar("CP-DC01_mis_cursos");
    }

    @Test @Order(2)
    @DisplayName("CP-DC02: proponer un curso lo deja en borrador, con lo que falta para publicarlo, y el filtro lo encuentra")
    void cpDC02_proponerCurso() {
        entrarComoDocente();
        String curso = proponerCurso();
        WebElement fila = tarjetaCurso(curso);
        assertThat(fila.getText()).containsIgnoringCase("titular").containsIgnoringCase("borrador").contains("Falta:");
        assertThat(fila.findElements(By.xpath(".//button[contains(.,'Editar')]"))).hasSize(1); // es el titular
        capturar("CP-DC02a_curso_propuesto");
        WebElement buscador = driver.findElement(By.id("doc-cur-buscar"));
        buscador.sendKeys(curso.substring(curso.length() - 7));   // la marca única del curso
        new Select(driver.findElement(By.id("doc-cur-estado"))).selectByValue("borrador");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("doc-cursos-resumen"), "Filtros activos"));
        assertThat(driver.findElements(By.cssSelector("#doc-cursos-lista tr.doc-curso"))).hasSize(1);
        capturar("CP-DC02b_filtros");
    }

    @Test @Order(3)
    @DisplayName("CP-DC03: crear sin nombre muestra un aviso")
    void cpDC03_camposObligatorios() {
        entrarComoDocente();
        abrirSeccion("Cursos");
        clicSeguro(boton("+ Proponer curso"));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("curso-nombre")));
        driver.findElement(By.id("curso-modal-btn")).click();
        assertThat(esperarMensaje("Escribe el nombre del curso").getText()).startsWith("⚠️");
        capturar("CP-DC03_campos_obligatorios");
    }

    @Test @Order(4)
    @DisplayName("CP-DC04: el titular edita los datos de su curso, con el editor enriquecido")
    void cpDC04_editarCurso() {
        entrarComoDocente();
        String curso = proponerCurso();
        clicSeguro(tarjetaCurso(curso).findElement(By.xpath(".//button[contains(.,'Editar')]")));
        WebElement descripcion = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("curso-descripcion")));
        descripcion.clear();
        descripcion.sendKeys("Descripción editada por la suite de Selenium");
        WebElement info = driver.findElement(By.id("curso-info-estudiante"));
        clicCentrado(info);
        info.sendKeys("Requisitos: biología básica");
        driver.findElement(By.id("curso-modal-btn")).click();
        esperarMensaje("Curso \"" + curso + "\" actualizado");
        capturar("CP-DC04_curso_editado");
    }

    @Test @Order(5)
    @DisplayName("CP-DC05: crear una lección la deja en borrador")
    void cpDC05_crearLeccion() {
        entrarComoDocente();
        abrirLecciones(proponerCurso());
        crearLeccion("Lección Selenium 1");
        assertThat(tarjetaLeccion("Lección Selenium 1").getText()).containsIgnoringCase("borrador").contains("Materiales (0 de 0)");
        capturar("CP-DC05_leccion_creada");
    }

    @Test @Order(6)
    @DisplayName("CP-DC06: activar una lección sin material muestra el motivo")
    void cpDC06_activarSinMaterial() {
        entrarComoDocente();
        abrirLecciones(proponerCurso());
        crearLeccion("Lección Selenium 1");
        clicSeguro(tarjetaLeccion("Lección Selenium 1").findElement(By.xpath(".//button[normalize-space()='Activar']")));
        assertThat(esperarMensaje("sin al menos un material activo").getText()).startsWith("⚠️");
        capturar("CP-DC06_activar_sin_material");
    }

    @Test @Order(7)
    @DisplayName("CP-DC07: agregar una lectura escrita con el editor enriquecido")
    void cpDC07_agregarMaterial() {
        entrarComoDocente();
        abrirLecciones(proponerCurso());
        crearLeccion("Lección Selenium 1");
        abrirMateriales("Lección Selenium 1");
        agregarLectura("Lectura Selenium");
        assertThat(driver.findElements(By.cssSelector("#doc-mat-lista .doc-material"))).hasSize(1);
        clicSeguro(filaMaterial("Lectura Selenium").findElement(By.xpath(".//button[contains(.,'Ver como estudiante')]")));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("preview-lectura-body"), "Texto de la lectura"));
        capturar("CP-DC07_lectura_vista_previa");
    }

    @Test @Order(8)
    @DisplayName("CP-DC08: un video de YouTube se guarda en formato para embeber y se ve como estudiante")
    void cpDC08_videoYoutube() {
        entrarComoDocente();
        abrirLecciones(proponerCurso());
        crearLeccion("Lección Selenium 1");
        abrirMateriales("Lección Selenium 1");
        agregarVideo("Video Selenium", VIDEO);
        esperarMensaje("Material \"Video Selenium\" agregado");
        clicSeguro(filaMaterial("Video Selenium").findElement(By.xpath(".//button[contains(.,'Ver como estudiante')]")));
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("preview-inter-launch-btn"))));
        WebElement iframe = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("preview-inter-iframe")));
        assertThat(iframe.getAttribute("src")).isEqualTo("https://www.youtube.com/embed/dQw4w9WgXcQ");
        capturar("CP-DC08_video_embebido");
    }

    @Test @Order(9)
    @DisplayName("CP-DC09: un video que no es de YouTube se rechaza")
    void cpDC09_videoNoYoutube() {
        entrarComoDocente();
        abrirLecciones(proponerCurso());
        crearLeccion("Lección Selenium 1");
        abrirMateriales("Lección Selenium 1");
        agregarVideo("Video externo", "https://vimeo.com/123456");
        assertThat(esperarMensaje("YouTube").getText()).startsWith("⚠️");
        capturar("CP-DC09_video_no_youtube");
    }

    @Test @Order(10)
    @DisplayName("CP-DC10: subir un PDF como material de tipo documento")
    void cpDC10_subirPdf() throws IOException {
        entrarComoDocente();
        abrirLecciones(proponerCurso());
        crearLeccion("Lección Selenium 1");
        abrirMateriales("Lección Selenium 1");
        abrirNuevoMaterial("documento");
        driver.findElement(By.id("mat-archivo-input")).sendKeys(archivoTemporal("guia.pdf", "%PDF-1.4 Guía de prueba de BioLearn".getBytes()).toString());
        esperarMensaje("Archivo subido");
        assertThat(texto("mat-archivo-label")).contains("guia.pdf (subido)");
        capturar("CP-DC10a_pdf_subido");
        clicSeguro(driver.findElement(By.id("mat-modal-btn")));
        esperarMensaje("agregado");
        filaMaterial("guia");
        capturar("CP-DC10b_material_documento");
    }

    @Test @Order(11)
    @DisplayName("CP-DC11: un archivo con extensión .pdf pero contenido de otro tipo se rechaza")
    void cpDC11_archivoFalso() throws IOException {
        entrarComoDocente();
        abrirLecciones(proponerCurso());
        crearLeccion("Lección Selenium 1");
        abrirMateriales("Lección Selenium 1");
        abrirNuevoMaterial("documento");
        driver.findElement(By.id("mat-archivo-input")).sendKeys(archivoTemporal("tarea.pdf", new byte[]{'M', 'Z', (byte) 0x90, 0, 3, 0}).toString());
        assertThat(esperarMensaje("solo PDF, PNG o JPG").getText()).startsWith("⚠️");
        assertThat(texto("mat-archivo-label")).isEmpty();
        capturar("CP-DC11_archivo_rechazado");
    }

    @Test @Order(12)
    @DisplayName("CP-DC12: una lección con material activo se activa")
    void cpDC12_activarLeccion() {
        entrarComoDocente();
        abrirLecciones(proponerCurso());
        crearLeccion("Lección Selenium 1");
        abrirMateriales("Lección Selenium 1");
        agregarLectura("Lectura Selenium");
        abrirSeccion("Lecciones");
        clicSeguro(tarjetaLeccion("Lección Selenium 1").findElement(By.xpath(".//button[normalize-space()='Activar']")));
        esperarMensaje("\"Lección Selenium 1\" quedó activa");
        capturar("CP-DC12_leccion_activa");
    }

    @Test @Order(13)
    @DisplayName("CP-DC13: reordenar las lecciones con las flechas")
    void cpDC13_reordenar() {
        entrarComoDocente();
        abrirLecciones(proponerCurso());
        crearLeccion("Lección Selenium 1");
        crearLeccion("Lección Selenium 2");
        clicSeguro(tarjetaLeccion("Lección Selenium 2").findElement(By.xpath(".//button[@title='Subir']")));
        esperarMensaje("Orden actualizado");
        espera.until(d -> primeraLeccion().equals("Lección Selenium 2"));
        capturar("CP-DC13_lecciones_reordenadas");
    }

    @Test @Order(14)
    @DisplayName("CP-DC14: el orden no puede dejar una lección antes de su prerrequisito")
    void cpDC14_prerrequisito() {
        entrarComoDocente();
        abrirLecciones(proponerCurso());
        crearLeccion("Lección Selenium 1");
        crearLeccion("Lección Selenium 2");
        clicSeguro(tarjetaLeccion("Lección Selenium 2").findElement(By.xpath(".//button[@title='Editar']")));
        new Select(espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("leccion-prereq")))).selectByVisibleText("Lección Selenium 1");
        driver.findElement(By.id("leccion-modal-btn")).click();
        esperarMensaje("actualizada");
        espera.until(d -> tarjetaLeccion("Lección Selenium 2").getText().contains("Requiere: Lección Selenium 1"));
        clicSeguro(tarjetaLeccion("Lección Selenium 2").findElement(By.xpath(".//button[@title='Subir']")));
        assertThat(esperarMensaje("antes de su prerrequisito").getText()).startsWith("⚠️");
        capturar("CP-DC14_orden_prerrequisito");
    }

    @Test @Order(15)
    @DisplayName("CP-DC15: un usuario que no es docente no ve el portal del docente")
    void cpDC15_accesoDenegado() {
        iniciarSesion(ADMIN_USUARIO, ADMIN_CLAVE, "biolearn_admin_prototipo.html");
        driver.get(URL_DOCENTE);
        WebElement aviso = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("acceso-denegado")));
        assertThat(aviso.getText()).contains("Acceso solo para docentes");
        capturar("CP-DC15_acceso_denegado");
        espera.until(ExpectedConditions.urlContains("biolearn_auth_prototipo.html"));
    }

    @Test @Order(16)
    @DisplayName("CP-DC16: un recurso de apoyo agregado al material aparece en la vista del estudiante")
    void cpDC16_recursoDeApoyo() {
        entrarComoDocente();
        abrirLecciones(proponerCurso());
        crearLeccion("Lección Selenium 1");
        abrirMateriales("Lección Selenium 1");
        agregarLectura("Lectura Selenium");
        clicSeguro(filaMaterial("Lectura Selenium").findElement(By.xpath(".//button[starts-with(@title,'Recursos de apoyo')]")));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("apoyo-nombre"))).sendKeys("Wikipedia: manglar");
        driver.findElement(By.id("apoyo-url")).sendKeys("https://es.wikipedia.org/wiki/Manglar");
        clicSeguro(driver.findElement(By.id("apoyo-guardar")));
        esperarMensaje("Recurso \"Wikipedia: manglar\" agregado");
        espera.until(d -> !d.findElements(By.cssSelector(".apoyo-item[data-nombre='Wikipedia: manglar']")).isEmpty());
        capturar("CP-DC16a_recurso_apoyo_agregado");
        clicSeguro(driver.findElement(By.cssSelector("#modal-doc-apoyo .modal-close")));
        clicSeguro(filaMaterial("Lectura Selenium").findElement(By.xpath(".//button[contains(.,'Ver como estudiante')]")));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("preview-lectura-apoyo"), "Wikipedia: manglar"));
        capturar("CP-DC16b_apoyo_vista_estudiante");
    }

    @Test @Order(17)
    @DisplayName("CP-DC17: el artículo científico tiene su vista «como estudiante»")
    void cpDC17_vistaArticulo() {
        entrarComoDocente();
        abrirLecciones(proponerCurso());
        crearLeccion("Lección Selenium 1");
        abrirMateriales("Lección Selenium 1");
        abrirNuevoMaterial("articulo");
        driver.findElement(By.id("mat-nombre")).sendKeys("La doble hélice");
        driver.findElement(By.id("mat-autores")).sendKeys("Watson, J.; Crick, F.");
        driver.findElement(By.id("mat-revista")).sendKeys("Nature");
        driver.findElement(By.id("mat-doi")).sendKeys("10.1038/171737a0");
        clicSeguro(driver.findElement(By.id("mat-modal-btn")));
        esperarMensaje("Material \"La doble hélice\" agregado");
        clicSeguro(filaMaterial("La doble hélice").findElement(By.xpath(".//button[contains(.,'Ver como estudiante')]")));
        WebElement cuerpo = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("preview-simple-body")));
        assertThat(cuerpo.getText()).contains("Watson, J.; Crick, F.").contains("Nature").contains("10.1038/171737a0");
        capturar("CP-DC17_vista_articulo");
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

    private void entrarComoDocente() {
        iniciarSesion(DOCENTE_USUARIO, DOCENTE_CLAVE, "biolearn_docente_prototipo.html");
        espera.until(ExpectedConditions.titleIs("BioLearn — Prototipo Docente"));
    }

    private void abrirSeccion(String etiqueta) {
        esperarSinMensajes();
        clicCentrado(espera.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class,'sb-item')][.//span[normalize-space()='" + etiqueta + "']]"))));
    }

    /** Crea «Curso Selenium D<marca>» desde «+ Proponer curso» y devuelve su nombre. */
    private String proponerCurso() {
        abrirSeccion("Cursos");
        String nombre = "Curso Selenium D" + marcaCorta();
        clicSeguro(boton("+ Proponer curso"));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("curso-nombre"))).sendKeys(nombre);
        espera.until(d -> new Select(d.findElement(By.id("curso-area"))).getOptions().size() > 1
                && new Select(d.findElement(By.id("curso-grado"))).getOptions().size() > 1);
        new Select(driver.findElement(By.id("curso-area"))).selectByIndex(1);
        new Select(driver.findElement(By.id("curso-grado"))).selectByIndex(1);
        driver.findElement(By.id("curso-codigo")).clear(); // el código se genera único a partir de la marca
        driver.findElement(By.id("curso-codigo")).sendKeys("SEL-" + nombre.substring(nombre.length() - 7));
        driver.findElement(By.id("curso-modal-btn")).click();
        esperarMensaje("Curso \"" + nombre + "\" creado como borrador");
        tarjetaCurso(nombre);
        return nombre;
    }

    private void abrirLecciones(String curso) {
        clicSeguro(tarjetaCurso(curso).findElement(By.xpath(".//button[contains(.,'Contenido')]")));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("doc-lec-info"), "Tu rol"));
    }

    private void crearLeccion(String nombre) {
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("doc-lec-nueva"))));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("leccion-nombre"))).sendKeys(nombre);
        driver.findElement(By.id("leccion-modal-btn")).click();
        esperarMensaje("Lección \"" + nombre + "\" creada en borrador");
        tarjetaLeccion(nombre);
    }

    private void abrirMateriales(String leccion) {
        clicSeguro(tarjetaLeccion(leccion).findElement(By.xpath(".//button[contains(.,'Materiales')]")));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("doc-mat-info"), "Lección"));
    }

    private void abrirNuevoMaterial(String tipo) {
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("doc-mat-nuevo"))));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("mat-nombre")));
        clicSeguro(driver.findElement(By.cssSelector("#modal-material .tipo-btn[data-tipo='" + tipo + "']")));
    }

    private void agregarLectura(String nombre) {
        abrirNuevoMaterial("lectura");
        driver.findElement(By.id("mat-nombre")).sendKeys(nombre);
        WebElement editor = driver.findElement(By.id("mat-editor"));
        clicCentrado(editor);
        editor.sendKeys("Texto de la lectura escrito con el editor enriquecido");
        clicSeguro(driver.findElement(By.id("mat-modal-btn")));
        esperarMensaje("Material \"" + nombre + "\" agregado");
    }

    private void agregarVideo(String nombre, String url) {
        abrirNuevoMaterial("video");
        driver.findElement(By.id("mat-nombre")).sendKeys(nombre);
        driver.findElement(By.id("mat-url-video")).sendKeys(url);
        clicSeguro(driver.findElement(By.id("mat-modal-btn")));
    }

    private Path archivoTemporal(String nombre, byte[] contenido) throws IOException {
        Path carpeta = Files.createTempDirectory("biolearn-selenium");
        Path archivo = carpeta.resolve(nombre);
        Files.write(archivo, contenido);
        return archivo.toAbsolutePath();
    }

    private String primeraLeccion() {
        return driver.findElements(By.cssSelector("#doc-lec-lista .doc-leccion")).get(0).getAttribute("data-nombre");
    }

    private WebElement tarjetaCurso(String nombre) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//*[@id='doc-cursos-lista']//tr[contains(@class,'doc-curso')][@data-nombre='" + nombre + "']")));
    }

    private WebElement tarjetaLeccion(String nombre) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//*[@id='doc-lec-lista']//div[contains(@class,'doc-leccion')][@data-nombre='" + nombre + "']")));
    }

    private WebElement filaMaterial(String nombre) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//*[@id='doc-mat-lista']//div[contains(@class,'doc-material')][@data-nombre='" + nombre + "']")));
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
                "//*[@id='toast-wrap']/*[contains(normalize-space(.), '" + textoParcial.replace("'", "") + "')]")));
    }

    private void esperarSinMensajes() {
        espera.until(d -> d.findElements(By.cssSelector("#toast-wrap .toast-item")).isEmpty());
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
