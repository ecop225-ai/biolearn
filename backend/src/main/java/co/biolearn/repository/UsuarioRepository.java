package co.biolearn.repository;

import co.biolearn.enums.EstadoUsuario;
import co.biolearn.enums.RolUsuario;
import co.biolearn.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    boolean existsByNombreUsuario(String nombreUsuario);

    Optional<Usuario> findByCorreoOrNombreUsuario(String correo, String nombreUsuario);

    List<Usuario> findByTipoUsuario(RolUsuario rol);

    List<Usuario> findByEstado(EstadoUsuario estado);

    List<Usuario> findByTipoUsuarioAndEstado(RolUsuario rol, EstadoUsuario estado);
}
