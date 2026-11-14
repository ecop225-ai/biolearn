package co.biolearn.service.impl;

import co.biolearn.dto.ActualizarEstadoUsuarioRequest;
import co.biolearn.dto.ActualizarUsuarioRequest;
import co.biolearn.dto.CrearUsuarioRequest;
import co.biolearn.dto.MensajeResponse;
import co.biolearn.dto.UsuarioAdminResponse;
import co.biolearn.enums.EstadoPerfilAmplio;
import co.biolearn.enums.EstadoUsuario;
import co.biolearn.enums.RolContador;
import co.biolearn.enums.RolUsuario;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.Institucion;
import co.biolearn.model.PerfilContador;
import co.biolearn.model.PerfilDocente;
import co.biolearn.model.SupervisorInstitucion;
import co.biolearn.model.Usuario;
import co.biolearn.repository.InstitucionRepository;
import co.biolearn.repository.PerfilContadorRepository;
import co.biolearn.repository.PerfilDocenteRepository;
import co.biolearn.repository.PerfilEstudianteRepository;
import co.biolearn.repository.SupervisorInstitucionRepository;
import co.biolearn.repository.UsuarioRepository;
import co.biolearn.service.TokenVerificacionService;
import co.biolearn.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilDocenteRepository perfilDocenteRepository;
    private final PerfilContadorRepository perfilContadorRepository;
    private final PerfilEstudianteRepository perfilEstudianteRepository;
    private final SupervisorInstitucionRepository supervisorRepository;
    private final InstitucionRepository institucionRepository;
    private final TokenVerificacionService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UsuarioAdminResponse crearUsuario(CrearUsuarioRequest request) {
        RolUsuario rol = parsearRolPermitido(request.getRol());

        if (usuarioRepository.existsByCorreo(request.getCorreo().trim().toLowerCase())) {
            throw new SolicitudInvalidaException("Ya existe una cuenta registrada con ese correo");
        }
        if (usuarioRepository.existsByNombreUsuario(request.getNombreUsuario())) {
            throw new SolicitudInvalidaException("Ese nombre de usuario ya está en uso, elige otro");
        }

        Institucion institucion = null;
        if (rol == RolUsuario.supervisor) {
            if (request.getIdInstitucion() == null) {
                throw new SolicitudInvalidaException("Debes indicar la institución que representará el supervisor");
            }
            institucion = institucionRepository.findById(request.getIdInstitucion())
                    .orElseThrow(() -> new RecursoNoEncontradoException("La institución indicada no existe"));
            if (!"activa".equalsIgnoreCase(institucion.getEstado())) {
                throw new SolicitudInvalidaException("No se puede asignar un representante a una institución que no está activa");
            }
            // Regla: máximo UN representante activo por institución. Si el anterior fue desactivado,
            // su registro queda en estado inactivo y el cupo de representante se libera.
            if (supervisorRepository.existsByInstitucion_IdAndEstado(institucion.getId(), EstadoPerfilAmplio.activo)) {
                throw new SolicitudInvalidaException("Esta institución ya tiene un representante activo asignado");
            }
        }

        String contrasenaProvisional = passwordEncoder.encode(UUID.randomUUID().toString());

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre().trim())
                .apellido(request.getApellido().trim())
                .nombreUsuario(request.getNombreUsuario().trim())
                .correo(request.getCorreo().trim().toLowerCase())
                .contrasena(contrasenaProvisional)
                .tipoUsuario(rol)
                .estado(EstadoUsuario.pendiente)
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .ciudad(request.getCiudad())
                .docIdentidad(request.getDocIdentidad())
                .build();
        usuario = usuarioRepository.save(usuario);

        switch (rol) {
            case docente -> perfilDocenteRepository.save(PerfilDocente.builder()
                    .usuario(usuario).especialidad(request.getEspecialidad()).build());
            case contador -> {
                RolContador rolContador = request.getRolContador() != null
                        ? parsearRolContador(request.getRolContador())
                        : RolContador.auxiliar;
                perfilContadorRepository.save(PerfilContador.builder()
                        .usuario(usuario).rolContador(rolContador).build());
            }
            case supervisor -> supervisorRepository.save(SupervisorInstitucion.builder()
                    .usuario(usuario).institucion(institucion).cargo(request.getCargo()).build());
            default -> throw new SolicitudInvalidaException("Rol no soportado para creación administrativa");
        }

        tokenService.crearTokenActivacion(usuario, TokenVerificacionService.HORAS_INVITACION_PERSONAL,
                "invitación (" + usuario.getTipoUsuario() + ")");
        return aUsuarioAdminResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true) // lee relaciones LAZY (institución del perfil) al armar la respuesta
    public List<UsuarioAdminResponse> listarUsuarios(String rol, String estado, Integer idInstitucion) {
        List<Usuario> usuarios;
        RolUsuario rolFiltro = rol != null && !rol.isBlank() ? RolUsuario.valueOf(rol.trim().toLowerCase()) : null;
        EstadoUsuario estadoFiltro = estado != null && !estado.isBlank() ? EstadoUsuario.valueOf(estado.trim().toLowerCase()) : null;

        if (rolFiltro != null && estadoFiltro != null) {
            usuarios = usuarioRepository.findByTipoUsuarioAndEstado(rolFiltro, estadoFiltro);
        } else if (rolFiltro != null) {
            usuarios = usuarioRepository.findByTipoUsuario(rolFiltro);
        } else if (estadoFiltro != null) {
            usuarios = usuarioRepository.findByEstado(estadoFiltro);
        } else {
            usuarios = usuarioRepository.findAll();
        }
        if (idInstitucion != null && !institucionRepository.existsById(idInstitucion)) {
            throw new RecursoNoEncontradoException("La institución indicada no existe");
        }
        // La institución vive en el perfil (supervisor_institucion / perfil_estudiante), por eso se filtra
        // sobre la respuesta ya armada. Docentes y contadores no tienen institución y quedan fuera del filtro.
        return usuarios.stream()
                .map(this::aUsuarioAdminResponse)
                .filter(u -> idInstitucion == null || idInstitucion.equals(u.getIdInstitucion()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioAdminResponse obtenerUsuario(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario indicado no existe"));
        return aUsuarioAdminResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioAdminResponse actualizarEstado(Integer id, ActualizarEstadoUsuarioRequest request, Integer idAdminActual) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario indicado no existe"));

        // Evita que el administrador se bloquee a sí mismo y deje el sistema sin nadie que pueda reactivarlo.
        if (usuario.getId().equals(idAdminActual)) {
            throw new SolicitudInvalidaException("No puedes cambiar el estado de tu propia cuenta");
        }

        EstadoUsuario nuevoEstado;
        try {
            nuevoEstado = EstadoUsuario.valueOf(request.getEstado().trim().toLowerCase());
        } catch (IllegalArgumentException ex) {
            throw new SolicitudInvalidaException("Estado no válido. Usa: activo, inactivo o suspendido");
        }
        // 'pendiente' lo asigna solo el sistema (cuenta creada sin activar); no es un estado que el admin pueda elegir.
        if (nuevoEstado == EstadoUsuario.pendiente) {
            throw new SolicitudInvalidaException("Estado no válido. Usa: activo, inactivo o suspendido");
        }

        if (usuario.getTipoUsuario() == RolUsuario.supervisor) {
            sincronizarEstadoSupervisor(usuario, nuevoEstado);
        }

        usuario.setEstado(nuevoEstado);
        usuarioRepository.save(usuario);
        return aUsuarioAdminResponse(usuario);
    }

    /**
     * Asigna o cambia la institución de un representante (supervisor) existente. Cubre los casos reales en que un
     * representante cambia de institución, su institución se da de baja o se asignó por error.
     * La base admite UN solo registro de supervisor_institucion por usuario (uq_supervisor_usuario), por eso
     * se actualiza el registro existente en lugar de crear otro.
     */
    @Override
    @Transactional
    public UsuarioAdminResponse asignarInstitucion(Integer id, co.biolearn.dto.AsignarInstitucionRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (usuario.getTipoUsuario() != RolUsuario.supervisor) {
            throw new SolicitudInvalidaException("Solo se puede asignar una institución a un representante institucional");
        }
        Institucion institucion = institucionRepository.findById(request.getIdInstitucion())
                .orElseThrow(() -> new RecursoNoEncontradoException("La institución indicada no existe"));
        if (!"activa".equalsIgnoreCase(institucion.getEstado())) {
            throw new SolicitudInvalidaException("No se puede asignar un representante a una institución que no está activa");
        }
        SupervisorInstitucion perfil = supervisorRepository.findByUsuario_Id(id).orElse(null);
        if (perfil != null && perfil.getInstitucion().getId().equals(institucion.getId())) {
            throw new SolicitudInvalidaException("El representante ya está asignado a esa institución");
        }
        // Regla RF-004: máximo UN representante activo por institución (sin contar a este mismo usuario)
        if (supervisorRepository.existsByInstitucion_IdAndEstadoAndUsuario_IdNot(institucion.getId(), EstadoPerfilAmplio.activo, id)) {
            throw new SolicitudInvalidaException("Esta institución ya tiene un representante activo asignado");
        }
        if (perfil == null) {
            perfil = SupervisorInstitucion.builder().usuario(usuario).build();
        }
        perfil.setInstitucion(institucion);
        if (request.getCargo() != null && !request.getCargo().isBlank()) perfil.setCargo(request.getCargo().trim());
        // La representación sigue el estado de la cuenta: una cuenta inactiva o suspendida no ocupa el cupo de representante
        perfil.setEstado(switch (usuario.getEstado()) {
            case inactivo -> EstadoPerfilAmplio.inactivo;
            case suspendido -> EstadoPerfilAmplio.suspendido;
            default -> EstadoPerfilAmplio.activo;
        });
        supervisorRepository.save(perfil);
        return aUsuarioAdminResponse(usuario);
    }

    /** Mantiene supervisor_institucion.estado alineado con usuario.estado. Desactivar a un representante
     *  libera la institución; reactivarlo solo es posible si nadie más la representa ya. */
    private void sincronizarEstadoSupervisor(Usuario usuario, EstadoUsuario nuevoEstado) {
        supervisorRepository.findByUsuario_Id(usuario.getId()).ifPresent(perfil -> {
            EstadoPerfilAmplio estadoPerfil = switch (nuevoEstado) {
                case activo -> EstadoPerfilAmplio.activo;
                case suspendido -> EstadoPerfilAmplio.suspendido;
                default -> EstadoPerfilAmplio.inactivo;
            };
            if (estadoPerfil == EstadoPerfilAmplio.activo && perfil.getEstado() != EstadoPerfilAmplio.activo
                    && supervisorRepository.existsByInstitucion_IdAndEstadoAndUsuario_IdNot(
                            perfil.getInstitucion().getId(), EstadoPerfilAmplio.activo, usuario.getId())) {
                throw new SolicitudInvalidaException(
                        "No se puede reactivar: la institución ya tiene otro representante activo");
            }
            perfil.setEstado(estadoPerfil);
            supervisorRepository.save(perfil);
        });
    }

    @Override
    @Transactional
    public MensajeResponse reenviarInvitacion(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario indicado no existe"));
        if (usuario.getTipoUsuario() == RolUsuario.estudiante || usuario.getTipoUsuario() == RolUsuario.admin) {
            throw new SolicitudInvalidaException("Solo se reenvían invitaciones a cuentas de Docente, Contador o Supervisor");
        }
        if (usuario.getEstado() != EstadoUsuario.pendiente) {
            throw new SolicitudInvalidaException("Esta cuenta ya fue activada; no necesita una nueva invitación");
        }
        tokenService.crearTokenActivacion(usuario, TokenVerificacionService.HORAS_INVITACION_PERSONAL,
                "invitación (" + usuario.getTipoUsuario() + ", reenvío)");
        return MensajeResponse.builder()
                .mensaje("Se generó una nueva invitación para " + usuario.getCorreo() + ". La anterior dejó de ser válida.")
                .build();
    }

    @Override
    @Transactional
    public UsuarioAdminResponse actualizarUsuario(Integer id, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario indicado no existe"));

        if (request.getNombre() != null && !request.getNombre().isBlank()) usuario.setNombre(request.getNombre().trim());
        if (request.getApellido() != null && !request.getApellido().isBlank()) usuario.setApellido(request.getApellido().trim());
        if (request.getTelefono() != null) usuario.setTelefono(request.getTelefono().trim());
        if (request.getDireccion() != null) usuario.setDireccion(request.getDireccion().trim());
        if (request.getCiudad() != null) usuario.setCiudad(request.getCiudad().trim());
        if (request.getDocIdentidad() != null) usuario.setDocIdentidad(request.getDocIdentidad().trim());
        usuarioRepository.save(usuario);

        // Campos específicos según el rol real del usuario — se ignoran silenciosamente
        // si no aplican (ej. mandar "especialidad" para un Contador no hace nada).
        switch (usuario.getTipoUsuario()) {
            case docente -> {
                if (request.getEspecialidad() != null) {
                    PerfilDocente perfil = perfilDocenteRepository.findByUsuario_Id(id)
                            .orElseThrow(() -> new RecursoNoEncontradoException("Perfil de docente no encontrado"));
                    perfil.setEspecialidad(request.getEspecialidad().trim());
                    perfilDocenteRepository.save(perfil);
                }
            }
            case contador -> {
                if (request.getRolContador() != null) {
                    PerfilContador perfil = perfilContadorRepository.findByUsuario_Id(id)
                            .orElseThrow(() -> new RecursoNoEncontradoException("Perfil de contador no encontrado"));
                    perfil.setRolContador(parsearRolContador(request.getRolContador()));
                    perfilContadorRepository.save(perfil);
                }
            }
            case supervisor -> {
                if (request.getCargo() != null) {
                    SupervisorInstitucion perfil = supervisorRepository.findByUsuario_Id(id)
                            .orElseThrow(() -> new RecursoNoEncontradoException("Perfil de supervisor no encontrado"));
                    perfil.setCargo(request.getCargo().trim());
                    supervisorRepository.save(perfil);
                }
            }
            default -> { /* admin/estudiante: sin campos de perfil editables aquí */ }
        }

        return aUsuarioAdminResponse(usuario);
    }

    private RolUsuario parsearRolPermitido(String rolTexto) {
        RolUsuario rol;
        try {
            rol = RolUsuario.valueOf(rolTexto.trim().toLowerCase());
        } catch (IllegalArgumentException ex) {
            throw new SolicitudInvalidaException("Rol no reconocido");
        }
        if (rol != RolUsuario.docente && rol != RolUsuario.contador && rol != RolUsuario.supervisor) {
            throw new SolicitudInvalidaException(
                    "Solo se pueden crear cuentas de Docente, Contador o Supervisor desde este endpoint. "
                    + "Los estudiantes se registran por su cuenta.");
        }
        return rol;
    }

    /** Misma validación y mismo error (400) al crear y al editar un contador. */
    private RolContador parsearRolContador(String valor) {
        try {
            return RolContador.valueOf(valor.trim().toLowerCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("rolContador no válido. Usa: administrador_financiero, auxiliar o auditor");
        }
    }

    private UsuarioAdminResponse aUsuarioAdminResponse(Usuario usuario) {
        UsuarioAdminResponse.UsuarioAdminResponseBuilder builder = UsuarioAdminResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .nombreUsuario(usuario.getNombreUsuario())
                .correo(usuario.getCorreo())
                .rol(usuario.getTipoUsuario())
                .estado(usuario.getEstado())
                .telefono(usuario.getTelefono())
                .direccion(usuario.getDireccion())
                .ciudad(usuario.getCiudad())
                .docIdentidad(usuario.getDocIdentidad())
                .fechaRegistro(usuario.getFechaRegistro() != null ? usuario.getFechaRegistro().toLocalDate() : null);

        // El dato específico del rol vive en su propia tabla de perfil, no en `usuario`.
        switch (usuario.getTipoUsuario()) {
            case docente -> perfilDocenteRepository.findByUsuario_Id(usuario.getId())
                    .ifPresent(p -> builder.especialidad(p.getEspecialidad()));
            case contador -> perfilContadorRepository.findByUsuario_Id(usuario.getId())
                    .ifPresent(p -> builder.rolContador(p.getRolContador() != null ? p.getRolContador().name() : null));
            case supervisor -> supervisorRepository.findByUsuario_Id(usuario.getId())
                    .ifPresent(p -> builder.cargo(p.getCargo())
                            .idInstitucion(p.getInstitucion().getId())
                            .institucion(p.getInstitucion().getNombre())
                            .estadoRepresentacion(p.getEstado().name()));
            case estudiante -> perfilEstudianteRepository.findByUsuario_Id(usuario.getId())
                    .filter(p -> p.getInstitucion() != null)
                    .ifPresent(p -> builder.idInstitucion(p.getInstitucion().getId())
                            .institucion(p.getInstitucion().getNombre()));
            default -> { /* admin: sin campo de perfil adicional que mostrar aquí */ }
        }
        return builder.build();
    }
}
