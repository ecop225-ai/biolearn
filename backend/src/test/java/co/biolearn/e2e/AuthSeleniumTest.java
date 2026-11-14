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
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pruebas de extremo a extremo (E2E) con Selenium WebDriver sobre el prototipo
 * de autenticación de BioLearn (frontend/prototipo-auth).
 *
 * Requisitos antes de ejecutar:
 *   1. MySQL encendido con biolearn_db y los datos de sql/datos_iniciales.sql.
 *   2. BiolearnBackendApplication corriendo en http://localhost:8080.
 *   3. El frontend servido en http://127.0.0.1:5173 (Live Server o Vite).
 *   4. Google Chrome instalado (Selenium Manager descarga el driver solo).
 *
 * Si el frontend o el backend no responden, las pruebas se OMITEN (no fallan),
 * para que "mvn test" siga funcionando aunque no estén encendidos.
 *
 * Las capturas de pantalla se guardan en backend/target/capturas-selenium/.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Selenium — Módulo de autenticación de BioLearn")
class AuthSeleniumTest {

    // Se pueden cambiar con -Dpropiedad=valor en las VM options de IntelliJ.
    private static final String URL_AUTH = System.getProperty("biolearn.frontend.url",
            "http://127.0.0.1:5173/prototipo-auth/biolearn_auth_prototipo.html");
    private static final String URL_API = System.getProperty("biolearn.backend.url",
            "http://localhost:8080");
    private static final String ADMIN_USUARIO = System.getProperty("biolearn.admin.usuario",
            "admin@biolearn.co");
    private static final String ADMIN_CLAVE = System.getProperty("biolearn.admin.clave",
            "Admin2026");
    private static final boolean SIN_VENTANA = Boolean.parseBoolean(
            System.getProperty("biolearn.headless", "false"));

    private static final Path CARPETA_CAPTURAS = Paths.get("target", "capturas-selenium");
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(10);

    private WebDriver driver;
    private WebDriverWait espera;

    // ------------------------------------------------------------------ ciclo de vida

    @BeforeAll
    static void verificarEntorno() {
        assumeTrue(responde(URL_AUTH),
                "Frontend no disponible en " + URL_AUTH + ". Ábrelo con Live Server o Vite.");
        assumeTrue(responde(URL_API + "/api/auth/me"),
                "Backend no disponible en " + URL_API + ". Ejecuta BiolearnBackendApplication.");
    }

    @BeforeEach
    void abrirNavegador() {
        ChromeOptions opciones = new ChromeOptions();
        opciones.addArguments("--lang=es");
        // Con ventana, Chrome se abre maximizado; sin ventana (headless) se usa un tamaño fijo, porque no hay pantalla que maximizar
        opciones.addArguments(SIN_VENTANA ? "--window-size=1366,900" : "--start-maximized");
        if (SIN_VENTANA) {
            opciones.addArguments("--headless=new");
        }
        driver = new ChromeDriver(opciones);   // Selenium Manager resuelve chromedriver
        if (!SIN_VENTANA) driver.manage().window().maximize();
        espera = new WebDriverWait(driver, ESPERA_MAXIMA);

        driver.get(URL_AUTH);
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-user")));
    }

    @AfterEach
    void cerrarNavegador() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ------------------------------------------------------------------ casos de prueba

    @Test
    @Order(1)
    @DisplayName("CP-S01: la pantalla de inicio de sesión carga con sus campos")
    void cp01_cargaPantallaLogin() {
        assertThat(driver.getTitle()).isEqualTo("BioLearn — Iniciar sesión");
        assertThat(driver.findElement(By.id("login-user")).isDisplayed()).isTrue();
        assertThat(driver.findElement(By.id("login-pass")).isDisplayed()).isTrue();
        assertThat(boton("Ingresar").isDisplayed()).isTrue();
        assertThat(driver.findElement(By.id("login-pass")).getAttribute("type")).isEqualTo("password");

        capturar("CP-S01_pantalla_login");
    }

    @Test
    @Order(2)
    @DisplayName("CP-S02: el login avisa cuando faltan usuario o contraseña")
    void cp02_loginConCamposVacios() {
        boton("Ingresar").click();
        WebElement aviso1 = esperarToast("Escribe tu usuario o correo");
        assertThat(aviso1.getText()).contains("Escribe tu usuario o correo");
        capturar("CP-S02a_falta_usuario");

        driver.findElement(By.id("login-user")).sendKeys(ADMIN_USUARIO);
        boton("Ingresar").click();
        WebElement aviso2 = esperarToast("Escribe tu contraseña");
        assertThat(aviso2.getText()).contains("Escribe tu contraseña");
        capturar("CP-S02b_falta_contrasena");
    }

    @Test
    @Order(3)
    @DisplayName("CP-S03: con una contraseña incorrecta no se inicia sesión")
    void cp03_loginConContrasenaIncorrecta() {
        driver.findElement(By.id("login-user")).sendKeys(ADMIN_USUARIO);
        driver.findElement(By.id("login-pass")).sendKeys("ClaveErrada9");
        boton("Ingresar").click();

        WebElement aviso = esperarToast("⚠️");
        assertThat(aviso.getText()).startsWith("⚠️");
        assertThat(driver.getCurrentUrl()).contains("prototipo-auth");
        assertThat(leerSessionStorage("biolearn_token")).isNull();

        capturar("CP-S03_credenciales_incorrectas");
    }

    @Test
    @Order(4)
    @DisplayName("CP-S04: el administrador inicia sesión y llega a su panel")
    void cp04_loginExitosoAdministrador() {
        driver.findElement(By.id("login-user")).sendKeys(ADMIN_USUARIO);
        driver.findElement(By.id("login-pass")).sendKeys(ADMIN_CLAVE);
        boton("Ingresar").click();

        WebElement aviso = esperarToast("Sesión iniciada como");
        assertThat(aviso.getText()).contains("admin");
        capturar("CP-S04a_sesion_iniciada");

        espera.until(ExpectedConditions.urlContains("biolearn_admin_prototipo.html"));
        espera.until(ExpectedConditions.titleIs("BioLearn — Administrador"));
        String token = leerSessionStorage("biolearn_token");
        assertThat(token).isNotBlank().startsWith("eyJ");   // un JWT siempre empieza así

        capturar("CP-S04b_panel_administrador");
    }

    @Test
    @Order(5)
    @DisplayName("CP-S05: recuperar contraseña solo se habilita con un correo válido")
    void cp05_recuperarContrasenaValidaCorreo() {
        boton("Olvidé contraseña").click();
        WebElement correo = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("rec-email")));
        WebElement enviar = driver.findElement(By.id("rec-btn-enviar"));

        assertThat(enviar.isEnabled()).isFalse();
        correo.sendKeys("admin");
        assertThat(enviar.isEnabled()).isFalse();
        capturar("CP-S05a_correo_incompleto");

        correo.sendKeys("@biolearn.co");
        assertThat(enviar.isEnabled()).isTrue();
        capturar("CP-S05b_correo_valido");
    }

    @Test
    @Order(6)
    @DisplayName("CP-S06: el registro aplica la política de contraseñas")
    void cp06_registroPoliticaContrasena() {
        boton("Crear usuario").click();
        WebElement pass = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("signup-pass")));
        WebElement confirmar = driver.findElement(By.id("signup-confirm"));

        assertThat(driver.findElement(By.id("signup-btn-crear")).isEnabled()).isFalse();
        assertThat(driver.findElement(By.id("signup-faltantes")).getText()).startsWith("Falta:");

        pass.sendKeys("abc");
        confirmar.sendKeys("abc");
        assertThat(cumpleRegla("signup-req-len")).isFalse();
        assertThat(cumpleRegla("signup-req-upper")).isFalse();
        assertThat(cumpleRegla("signup-req-num")).isFalse();
        capturar("CP-S06a_contrasena_debil");

        pass.clear();
        confirmar.clear();
        pass.sendKeys("Biolearn2026");
        confirmar.sendKeys("Biolearn2026");
        assertThat(cumpleRegla("signup-req-len")).isTrue();
        assertThat(cumpleRegla("signup-req-upper")).isTrue();
        assertThat(cumpleRegla("signup-req-num")).isTrue();
        assertThat(cumpleRegla("signup-req-match")).isTrue();
        capturar("CP-S06b_contrasena_valida");
    }

    @Test
    @Order(7)
    @DisplayName("CP-S07: un código de institución válido muestra el nombre del colegio")
    void cp07_codigoInstitucionValido() {
        abrirRegistroConInstitucion();
        driver.findElement(By.id("signup-codigo")).sendKeys("SR7K2MQX");

        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("signup-codigo-hint"), "✓"));
        String pista = driver.findElement(By.id("signup-codigo-hint")).getText();
        assertThat(pista).startsWith("✓").contains("San Rafael");

        capturar("CP-S07_codigo_valido");
    }

    @Test
    @Order(8)
    @DisplayName("CP-S08: un código de institución inválido muestra un error")
    void cp08_codigoInstitucionInvalido() {
        abrirRegistroConInstitucion();
        WebElement codigo = driver.findElement(By.id("signup-codigo"));

        codigo.sendKeys("ABC");
        WebElement pista = driver.findElement(By.id("signup-codigo-hint"));
        assertThat(pista.getText()).isEqualTo("El código debe tener 8 caracteres");
        capturar("CP-S08a_codigo_corto");

        borrarCampo(codigo);
        codigo.sendKeys("ZZZZ9999");
        try {
            // Espera la respuesta del backend: que el mensaje deje de ser "Validando..."
            espera.until(d -> {
                String texto = d.findElement(By.id("signup-codigo-hint")).getText();
                return !texto.isBlank()
                        && !texto.startsWith("Validando")
                        && !texto.startsWith("El código debe tener")
                        && !texto.startsWith("Código de");
            });
        } catch (TimeoutException e) {
            capturar("CP-S08b_codigo_inexistente_FALLO");
            throw new AssertionError("El mensaje del código no cambió en 10 segundos. Texto actual: \""
                    + driver.findElement(By.id("signup-codigo-hint")).getText() + "\"", e);
        }

        WebElement resultado = driver.findElement(By.id("signup-codigo-hint"));
        assertThat(resultado.getText()).doesNotStartWith("✓");
        assertThat(tieneClase(resultado, "error")).isTrue();
        capturar("CP-S08b_codigo_inexistente");
    }

    // ------------------------------------------------------------------ utilidades

    private WebElement boton(String texto) {
        return driver.findElement(By.xpath("//button[normalize-space()='" + texto + "']"));
    }

    /** Borra el campo como lo haría el usuario (Ctrl+A y Retroceso), para que la página reciba el evento. */
    private void borrarCampo(WebElement campo) {
        campo.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
    }

    private void abrirRegistroConInstitucion() {
        boton("Crear usuario").click();
        espera.until(ExpectedConditions.elementToBeClickable(By.xpath(
                        "//button[contains(@class,'tipo-cuenta-btn')][.//div[normalize-space()='Con mi institución']]")))
                .click();
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("signup-codigo")));
    }

    private WebElement esperarToast(String textoParcial) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//div[@id='toast-wrap']/div[contains(normalize-space(.), \"" + textoParcial + "\")]")));
    }

    private boolean cumpleRegla(String id) {
        return tieneClase(driver.findElement(By.id(id)), "ok");
    }

    private static boolean tieneClase(WebElement elemento, String clase) {
        String clases = elemento.getAttribute("class");
        return clases != null && Arrays.asList(clases.split("\\s+")).contains(clase);
    }

    private String leerSessionStorage(String clave) {
        return (String) ((JavascriptExecutor) driver)
                .executeScript("return window.sessionStorage.getItem(arguments[0]);", clave);
    }

    private void capturar(String nombre) {
        try {
            Files.createDirectories(CARPETA_CAPTURAS);
            File origen = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(origen.toPath(), CARPETA_CAPTURAS.resolve(nombre + ".png"),
                    StandardCopyOption.REPLACE_EXISTING);
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
