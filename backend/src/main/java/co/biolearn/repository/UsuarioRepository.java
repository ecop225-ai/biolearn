package co.biolearn.repository;

import co.biolearn.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    boolean existsByNombreUsuario(String nombreUsuario);

    /**
     * Login por correo o por nombre de usuario indistintamente,
     * tal como se muestra en el prototipo de Auth ("Correo o nombre de usuario").
     */
    Optional<Usuario> findByCorreoOrNombreUsuario(String correo, String nombreUsuario);
}
