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
 * Pruebas de extremo a extremo (E2E) con Selenium WebDriver sobre la sección "Cursos" del prototipo del
 * Administrador (Fase 1 del módulo de Cursos), conectada al backend real.
 *
 * Requisitos: MySQL con biolearn_db, backend en localhost:8080, frontend en 127.0.0.1:5173, Chrome instalado,
 * y al menos un docente activo, un segundo docente activo (CP-CU09) y una institución activa (CP-CU12).
 * Desde la Fase 2, CP-CU06 y CP-CU14 publican cursos y por eso necesitan la cuenta de docente de pruebas
 * (biolearn.docente.usuario / biolearn.docente.clave; por defecto docente.pruebas@biolearn.co / Docente2026).
 *
 * Datos que crean las pruebas: cursos "Curso Selenium <marca>". Limpieza sugerida (MySQL Workbench, con
 * SET SQL_SAFE_UPDATES = 0 en la misma ejecución):
 *   DELETE hc FROM horario_curso hc JOIN curso c ON c.id_curso = hc.id_curso WHERE c.nombre LIKE 'Curso Selenium %';
 *   DELETE cd FROM curso_docente cd JOIN curso c ON c.id_curso = cd.id_curso WHERE c.nombre LIKE 'Curso Selenium %';
 *   DELETE ic FROM institucion_curso_contratado ic JOIN curso c ON c.id_curso = ic.id_curso WHERE c.nombre LIKE 'Curso Selenium %';
 *   DELETE FROM curso WHERE nombre LIKE 'Curso Selenium %';
 *
 * Buenas prácticas de las suites anteriores: localizadores por id, esperar a que se cierren los mensajes
 * emergentes antes de hacer clic y comparar valores exactos. Capturas en backend/target/capturas-selenium/ (CP-CU##).
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Selenium — Cursos del Administrador (Fase 1)")
class CursosSeleniumTest {

    private static final String URL_AUTH = System.getProperty("biolearn.frontend.url",
            "http://127.0.0.1:5173/prototipo-auth/biolearn_auth_prototipo.html");
    private static final String URL_API = System.getProperty("biolearn.backend.url", "http://localhost:8080");
    private static final String ADMIN_USUARIO = System.getProperty("biolearn.admin.usuario", "admin@biolearn.co");
    private static final String ADMIN_CLAVE = System.getProperty("biolearn.admin.clave", "Admin2026");
    /** Cuenta de docente para preparar el contenido que exige la publicación (Fase 2). */
    private static final String DOCENTE_USUARIO = System.getProperty("biolearn.docente.usuario", "docente.pruebas@biolearn.co");
    private static final String DOCENTE_CLAVE = System.getProperty("biolearn.docente.clave", "Docente2026");
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

    @Test
    @Order(1)
    @DisplayName("CP-CU01: la sección Cursos carga el listado en tabla")
    void cpCU01_listarCursos() {
        entrarACursos();
        assertThat(texto("cur-resumen")).startsWith("Mostrando");
        assertThat(driver.findElement(By.xpath("//*[@id='cur-lista']//thead")).getText())
                .contains("CURSO").contains("DOCENTE TITULAR").contains("INSTITUCIONES").contains("ESTADO");
        capturar("CP-CU01_listado_cursos");
    }

    @Test
    @Order(2)
    @DisplayName("CP-CU02: crear un curso lo deja en borrador")
    void cpCU02_crearCurso() {
        entrarACursos();
        String nombre = crearCurso(false, false);
        assertThat(estadoDeFila(nombre)).isEqualTo("borrador");
        capturar("CP-CU02_curso_creado");
    }

    @Test
    @Order(3)
    @DisplayName("CP-CU03: crear sin los campos obligatorios muestra un aviso")
    void cpCU03_camposObligatorios() {
        entrarACursos();
        clicCentrado(boton("+ Crear curso"));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("cur-nombre")));
        driver.findElement(By.id("cur-modal-btn")).click();
        assertThat(esperarMensaje("Nombre, área y grado son obligatorios").getText()).startsWith("⚠️");
        assertThat(driver.findElement(By.id("modal-curso")).getAttribute("class")).contains("open");
        capturar("CP-CU03_campos_obligatorios");
    }

    @Test
    @Order(4)
    @DisplayName("CP-CU04: crear un curso con un nombre repetido muestra el error 409")
    void cpCU04_nombreRepetido() {
        entrarACursos();
        String nombre = crearCurso(false, false);
        llenarFormulario(nombre, false, false);
        driver.findElement(By.id("cur-modal-btn")).click();
        assertThat(esperarMensaje("Ya existe un curso con ese nombre").getText()).startsWith("⚠️");
        capturar("CP-CU04_nombre_repetido");
    }

    @Test
    @Order(5)
    @DisplayName("CP-CU05: publicar un curso incompleto muestra qué le falta y sigue en borrador")
    void cpCU05_publicarIncompleto() {
        entrarACursos();
        String nombre = crearCurso(false, false);
        cambiarEstado(nombre, "activo");
        WebElement aviso = esperarMensaje("El curso no se puede publicar todavía");
        assertThat(aviso.getText()).contains("asigna un docente titular").contains("agrega una descripción");
        espera.until(d -> "borrador".equals(estadoDeFila(nombre)));
        capturar("CP-CU05_publicar_incompleto");
    }

    @Test
    @Order(6)
    @DisplayName("CP-CU06: un curso con docente titular y descripción se publica")
    void cpCU06_publicarCompleto() {
        entrarACursos();
        String nombre = crearCurso(true, true);
        prepararLeccionActiva(nombre);
        cambiarEstado(nombre, "activo");
        esperarMensaje("\"" + nombre + "\" quedó publicado");
        espera.until(d -> "activo".equals(estadoDeFila(nombre)));
        capturar("CP-CU06_curso_publicado");
    }

    @Test
    @Order(7)
    @DisplayName("CP-CU07: los filtros muestran el resumen de filtros activos y se pueden limpiar")
    void cpCU07_filtros() {
        entrarACursos();
        new Select(driver.findElement(By.id("cur-f-estado"))).selectByValue("borrador");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("cur-resumen"), "Estado: borrador"));
        filas().forEach(f -> assertThat(new Select(f.findElement(By.tagName("select"))).getFirstSelectedOption().getText()).isEqualTo("borrador"));
        WebElement buscador = driver.findElement(By.id("cur-buscar"));
        buscador.click();
        buscador.sendKeys("selenium");
        assertThat(driver.switchTo().activeElement().getAttribute("id")).isEqualTo("cur-buscar");
        capturar("CP-CU07a_filtros_activos");
        boton("✕ Limpiar filtros").click();
        espera.until(d -> !texto("cur-resumen").contains("Filtros activos"));
        capturar("CP-CU07b_filtros_limpios");
    }

    @Test
    @Order(8)
    @DisplayName("CP-CU08: el detalle de un borrador indica lo que falta para publicarlo")
    void cpCU08_detallePendientes() {
        entrarACursos();
        String nombre = crearCurso(false, false);
        abrirDetalle(nombre);
        WebElement pendientes = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("curd-pendientes")));
        assertThat(pendientes.getText()).contains("Para publicarlo falta").contains("asigna un docente titular");
        capturar("CP-CU08_detalle_pendientes");
    }

    @Test
    @Order(9)
    @DisplayName("CP-CU09: agregar un docente colaborador desde el detalle")
    void cpCU09_agregarColaborador() {
        entrarACursos();
        String nombre = crearCurso(true, true);
        abrirDetalle(nombre);
        Select docentes = new Select(espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("curd-doc-sel"))));
        assumeTrue(docentes.getOptions().size() > 1, "Se necesita un segundo docente activo para este caso.");
        String elegido = docentes.getOptions().get(1).getText();
        docentes.selectByIndex(1);
        new Select(driver.findElement(By.id("curd-doc-rol"))).selectByValue("colaborador");
        clicSeguro(driver.findElement(By.id("curd-doc-btn")));
        esperarMensaje("Docente agregado al curso");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("curd-docentes"), elegido));
        assertThat(texto("curd-docentes")).contains("colaborador");
        capturar("CP-CU09_colaborador_agregado");
    }

    @Test
    @Order(10)
    @DisplayName("CP-CU10: definir un horario y guardarlo")
    void cpCU10_definirHorario() {
        entrarACursos();
        String nombre = crearCurso(false, false);
        abrirDetalle(nombre);
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("curd-hor-add"))));
        new Select(driver.findElement(By.cssSelector("#curd-horarios .curd-hor-fila .curd-hor-dia"))).selectByValue("miercoles");
        clicSeguro(driver.findElement(By.id("curd-hor-guardar")));
        esperarMensaje("Horarios guardados (1)");
        capturar("CP-CU10_horario_guardado");
    }

    @Test
    @Order(11)
    @DisplayName("CP-CU11: un horario con la hora de fin antes del inicio se rechaza")
    void cpCU11_horarioInvalido() {
        entrarACursos();
        String nombre = crearCurso(false, false);
        abrirDetalle(nombre);
        clicSeguro(espera.until(ExpectedConditions.elementToBeClickable(By.id("curd-hor-add"))));
        WebElement fila = driver.findElement(By.cssSelector("#curd-horarios .curd-hor-fila"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].querySelector('.curd-hor-ini').value='10:00'; arguments[0].querySelector('.curd-hor-fin').value='08:00';", fila);
        clicSeguro(driver.findElement(By.id("curd-hor-guardar")));
        assertThat(esperarMensaje("posterior a la de inicio").getText()).startsWith("⚠️");
        capturar("CP-CU11_horario_invalido");
    }

    @Test
    @Order(12)
    @DisplayName("CP-CU12: una institución activa contrata el curso desde el detalle")
    void cpCU12_contratarInstitucion() {
        entrarACursos();
        String nombre = crearCurso(false, false);
        abrirDetalle(nombre);
        Select instituciones = new Select(espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("curd-inst-sel"))));
        assumeTrue(instituciones.getOptions().size() > 1, "Se necesita una institución activa para este caso.");
        String elegida = instituciones.getOptions().get(1).getText();
        instituciones.selectByIndex(1);
        clicSeguro(driver.findElement(By.id("curd-inst-btn")));
        esperarMensaje("Contratación registrada");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("curd-inst"), elegida));
        capturar("CP-CU12_institucion_contratada");
    }

    @Test
    @Order(13)
    @DisplayName("CP-CU13: archivar un curso pide confirmación y deja de permitir editarlo")
    void cpCU13_archivarCurso() {
        entrarACursos();
        String nombre = crearCurso(false, false);
        cambiarEstado(nombre, "archivado");
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("confirm-btn")));
        capturar("CP-CU13a_confirmacion");
        driver.findElement(By.id("confirm-btn")).click();
        esperarMensaje("\"" + nombre + "\" quedó archivado");
        espera.until(d -> "archivado".equals(estadoDeFila(nombre)));
        assertThat(filaCurso(nombre).findElements(By.xpath(".//button[contains(.,'Editar')]"))).isEmpty();
        capturar("CP-CU13b_curso_archivado");
    }

    @Test
    @Order(14)
    @DisplayName("CP-CU14: el catálogo público muestra el curso publicado y no el borrador")
    void cpCU14_catalogoPublico() {
        entrarACursos();
        String publicado = crearCurso(true, true);
        prepararLeccionActiva(publicado);
        cambiarEstado(publicado, "activo");
        esperarMensaje("quedó publicado");
        esperarSinMensajes();
        String borrador = crearCurso(false, false);

        // Consulta el catálogo SIN token, como lo haría un visitante
        @SuppressWarnings("unchecked")
        List<String> nombres = (List<String>) ((JavascriptExecutor) driver).executeAsyncScript(
                "const listo = arguments[arguments.length - 1];" +
                "fetch('" + URL_API + "/api/cursos/catalogo').then(r => r.json()).then(l => listo(l.map(c => c.nombre))).catch(() => listo([]));");
        assertThat(nombres).contains(publicado).doesNotContain(borrador);
        capturar("CP-CU14_catalogo_publico");
    }

    // ------------------------------------------------------------------ pasos reutilizables

    private void entrarACursos() {
        driver.get(URL_AUTH);
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-user"))).sendKeys(ADMIN_USUARIO);
        driver.findElement(By.id("login-pass")).sendKeys(ADMIN_CLAVE);
        boton("Ingresar").click();
        espera.until(ExpectedConditions.urlContains("biolearn_admin_prototipo.html"));
        espera.until(ExpectedConditions.titleIs("BioLearn — Administrador"));
        espera.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//*[@id='sidebar']//button[.//span[normalize-space()='Cursos']]"))).click();
        espera.until(d -> texto("cur-resumen").startsWith("Mostrando"));
    }

    /** Crea "Curso Selenium <marca>" y devuelve su nombre. */
    private String crearCurso(boolean conDocente, boolean conDescripcion) {
        String nombre = "Curso Selenium " + marcaCorta();
        llenarFormulario(nombre, conDocente, conDescripcion);
        driver.findElement(By.id("cur-modal-btn")).click();
        esperarMensaje("Curso \"" + nombre + "\" creado en borrador");
        filaCurso(nombre);
        esperarSinMensajes();
        return nombre;
    }

    private void llenarFormulario(String nombre, boolean conDocente, boolean conDescripcion) {
        esperarSinMensajes();
        clicCentrado(boton("+ Crear curso"));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("cur-nombre"))).sendKeys(nombre);
        new Select(driver.findElement(By.id("cur-area"))).selectByIndex(1);
        new Select(driver.findElement(By.id("cur-grado"))).selectByIndex(1);
        if (conDocente) {
            Select docente = new Select(driver.findElement(By.id("cur-docente")));
            assumeTrue(docente.getOptions().size() > 1, "Se necesita al menos un docente activo para este caso.");
            docente.selectByIndex(1);
        }
        if (conDescripcion) driver.findElement(By.id("cur-descripcion")).sendKeys("Curso creado por la suite de Selenium");
    }

    /**
     * Fase 2: para publicar, el curso necesita al menos una lección activa, y el contenido solo lo crean los docentes.
     * Por la API: el administrador agrega como colaborador a la cuenta de docente de pruebas, y esta crea una lección
     * con un material y la activa. Si la cuenta de docente no existe, el caso se omite.
     */
    private void prepararLeccionActiva(String nombreCurso) {
        String resultado = (String) ((JavascriptExecutor) driver).executeAsyncScript(
                "const [api, curso, usuario, clave] = arguments; const listo = arguments[arguments.length - 1];" +
                "(async () => { try {" +
                " const ah = {'Content-Type':'application/json','Authorization':'Bearer ' + sessionStorage.getItem('biolearn_token')};" +
                " const cursos = await (await fetch(api + '/api/cursos?busqueda=' + encodeURIComponent(curso), {headers: ah})).json();" +
                " const c = cursos.find(x => x.nombre === curso); if (!c) return listo('sin curso');" +
                " const login = await fetch(api + '/api/auth/login', {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({usuario, contrasena: clave})});" +
                " if (!login.ok) return listo('sin docente');" +
                " const s = await login.json(); const dh = {'Content-Type':'application/json','Authorization':'Bearer ' + s.token};" +
                " await fetch(api + '/api/cursos/' + c.id + '/docentes', {method:'POST', headers: ah, body: JSON.stringify({idDocente: s.usuario.id, rol:'colaborador'})});" +
                " const l = await (await fetch(api + '/api/cursos/' + c.id + '/lecciones', {method:'POST', headers: dh, body: JSON.stringify({nombre:'Lección Selenium', tipo:'teoria'})})).json();" +
                " await fetch(api + '/api/lecciones/' + l.id + '/materiales', {method:'POST', headers: dh, body: JSON.stringify({nombre:'Lectura Selenium', tipo:'lectura'})});" +
                " const r = await fetch(api + '/api/lecciones/' + l.id + '/estado', {method:'PATCH', headers: dh, body: JSON.stringify({estado:'activa'})});" +
                " listo(r.ok ? 'ok' : 'error ' + r.status);" +
                "} catch (e) { listo('error ' + e); } })();", URL_API, nombreCurso, DOCENTE_USUARIO, DOCENTE_CLAVE);
        assumeTrue(!"sin docente".equals(resultado), "Se necesita la cuenta de docente de pruebas (" + DOCENTE_USUARIO + ") activa.");
        assertThat(resultado).as("Preparación del contenido por la API").isEqualTo("ok");
    }

    private void cambiarEstado(String nombre, String estado) {
        esperarSinMensajes();
        WebElement selector = filaCurso(nombre).findElement(By.tagName("select"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", selector);
        new Select(selector).selectByValue(estado);
    }

    private void abrirDetalle(String nombre) {
        clicSeguro(filaCurso(nombre).findElement(By.xpath(".//button[contains(.,'Ver')]")));
        espera.until(ExpectedConditions.textToBe(By.id("curd-titulo"), nombre));
    }

    private String estadoDeFila(String nombre) {
        return new Select(filaCurso(nombre).findElement(By.tagName("select"))).getFirstSelectedOption().getText();
    }

    private List<WebElement> filas() {
        return driver.findElements(By.xpath("//*[@id='cur-lista']//tbody/tr[not(td[contains(@class,'empty-state')])]"));
    }

    /** Fila del curso cuyo nombre es exactamente ese (el nombre va en el primer div de la celda). */
    private WebElement filaCurso(String nombre) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//*[@id='cur-lista']//tbody/tr[td[1]/div[normalize-space()='" + nombre + "']]")));
    }

    // ------------------------------------------------------------------ utilidades


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
                "//*[@id='toast-cnt-box']/*[contains(normalize-space(.), '" + textoParcial.replace("'", "") + "')]")));
    }

    private void esperarSinMensajes() {
        espera.until(d -> d.findElements(By.cssSelector("#toast-cnt-box .toast-item")).isEmpty());
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
