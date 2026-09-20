package co.biolearn.security;

import co.biolearn.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Usuario usuario;

    public CustomUserDetails(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security espera el prefijo "ROLE_" para usar hasRole("ESTUDIANTE") en los controllers.
        String rol = "ROLE_" + usuario.getTipoUsuario().name().toUpperCase();
        return List.of(new SimpleGrantedAuthority(rol));
    }

    @Override
    public String getPassword() {
        return usuario.getContrasena();
    }

    @Override
    public String getUsername() {
        return usuario.getCorreo();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return usuario.getEstado() != co.biolearn.enums.EstadoUsuario.suspendido;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 'pendiente' puede iniciar sesión (aún no construimos el flujo de verificación de correo);
        // solo bloqueamos cuentas explícitamente inactivas o suspendidas.
        var estado = usuario.getEstado();
        return estado != co.biolearn.enums.EstadoUsuario.inactivo
                && estado != co.biolearn.enums.EstadoUsuario.suspendido;
    }
}
