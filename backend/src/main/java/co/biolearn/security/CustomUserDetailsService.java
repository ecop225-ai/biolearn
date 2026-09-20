package co.biolearn.security;

import co.biolearn.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String correoOUsuario) throws UsernameNotFoundException {
        var usuario = usuarioRepository.findByCorreoOrNombreUsuario(correoOUsuario, correoOUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("No existe un usuario con ese correo o nombre de usuario"));
        return new CustomUserDetails(usuario);
    }
}
