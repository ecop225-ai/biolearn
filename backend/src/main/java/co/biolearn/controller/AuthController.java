package co.biolearn.controller;

import co.biolearn.dto.CompletarInvitacionRequest;
import co.biolearn.dto.InstitucionPublicaResponse;
import co.biolearn.dto.LoginRequest;
import co.biolearn.dto.LoginResponse;
import co.biolearn.dto.MensajeResponse;
import co.biolearn.dto.RecuperarPasswordRequest;
import co.biolearn.dto.ReenviarVerificacionRequest;
import co.biolearn.dto.RegistroEstudianteRequest;
import co.biolearn.dto.RestablecerPasswordRequest;
import co.biolearn.dto.UsuarioResponse;
import co.biolearn.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login, registro, recuperación de contraseña y activación de cuentas")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y devuelve un token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro")
    @Operation(summary = "Registro público de un estudiante (independiente o afiliado a una institución)")
    public ResponseEntity<UsuarioResponse> registrarEstudiante(@Valid @RequestBody RegistroEstudianteRequest request) {
        UsuarioResponse creado = authService.registrarEstudiante(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PostMapping("/recuperar-password")
    @Operation(summary = "Solicita el enlace para restablecer la contraseña")
    public ResponseEntity<MensajeResponse> recuperarPassword(@Valid @RequestBody RecuperarPasswordRequest request,
                                                               HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.solicitarRecuperacion(request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/restablecer-password")
    @Operation(summary = "Confirma la nueva contraseña usando el token recibido por correo")
    public ResponseEntity<MensajeResponse> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request) {
        return ResponseEntity.ok(authService.restablecerPassword(request));
    }

    @GetMapping("/verificar-cuenta")
    @Operation(summary = "Activa la cuenta a partir del enlace de verificación enviado al registrarse (estudiantes)")
    public ResponseEntity<MensajeResponse> verificarCuenta(@RequestParam String token) {
        return ResponseEntity.ok(authService.verificarCuenta(token));
    }

    @PostMapping("/reenviar-verificacion")
    @Operation(summary = "Genera un nuevo enlace de verificación para un estudiante cuya cuenta sigue pendiente (el anterior deja de ser válido)")
    public ResponseEntity<MensajeResponse> reenviarVerificacion(@Valid @RequestBody ReenviarVerificacionRequest request) {
        return ResponseEntity.ok(authService.reenviarVerificacion(request));
    }

    @GetMapping("/validar-codigo-institucion")
    @Operation(summary = "Valida un código de afiliación durante el registro (existe, institución activa y con cupos) y devuelve el nombre de la institución")
    public ResponseEntity<InstitucionPublicaResponse> validarCodigoInstitucion(@RequestParam String codigo) {
        return ResponseEntity.ok(authService.validarCodigoInstitucion(codigo));
    }

    @PostMapping("/completar-invitacion")
    @Operation(summary = "Activa la cuenta de un Docente, Contador o Supervisor definiendo su contraseña a partir del token de invitación")
    public ResponseEntity<MensajeResponse> completarInvitacion(@Valid @RequestBody CompletarInvitacionRequest request) {
        return ResponseEntity.ok(authService.completarInvitacion(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Devuelve los datos del usuario autenticado (requiere token JWT)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UsuarioResponse> obtenerUsuarioActual(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.obtenerUsuarioActual(userDetails.getUsername()));
    }
}
