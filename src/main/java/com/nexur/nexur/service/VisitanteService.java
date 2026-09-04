package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.EstadoVisitante;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.Visitante;
import com.nexur.nexur.model.Rol;
import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.repository.UsuarioRepository;
import com.nexur.nexur.repository.VisitanteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VisitanteService {

    private static final Logger log = LoggerFactory.getLogger(VisitanteService.class);

    private final VisitanteRepository visitanteRepository;
    private final ResidenteRepository residenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    @Autowired
    public VisitanteService(VisitanteRepository visitanteRepository,
                            ResidenteRepository residenteRepository,
                            UsuarioRepository usuarioRepository,
                            NotificacionService notificacionService) {
        this.visitanteRepository = visitanteRepository;
        this.residenteRepository = residenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    public VisitanteService(VisitanteRepository visitanteRepository,
                            ResidenteRepository residenteRepository) {
        this(visitanteRepository, residenteRepository, null, null);
    }

    public List<Visitante> listarVisitantes() {
        return visitanteRepository.findAll();
    }

    public List<Visitante> listarVisitantesActivos() {
        return visitanteRepository.findByEstadoOrderByFechaEntradaDesc(EstadoVisitante.DENTRO);
    }

    public List<Visitante> listarVisitantesActivosPorApartamento(Long apartamentoId) {
        return visitanteRepository.findByApartamentoIdAndEstadoOrderByFechaEntradaDesc(
                apartamentoId, EstadoVisitante.DENTRO);
    }

    public List<Visitante> listarSolicitudesPendientes() {
        return visitanteRepository.findByEstadoOrderByFechaEntradaDesc(EstadoVisitante.PENDIENTE);
    }

    public List<Visitante> buscarPorApartamento(Long apartamentoId) {
        return visitanteRepository.findByApartamentoId(apartamentoId);
    }

    @Transactional
    public Visitante solicitar(Visitante visitante, String emailResidente) {
        validarDatosSolicitud(visitante);
        Residente residente = residenteRepository.findByUsuarioEmail(emailResidente)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta no tiene un perfil de residente"));
        Apartamento apartamento = residente.getApartamento();
        if (apartamento == null || apartamento.getId() == null) {
            throw new IllegalArgumentException(
                    "El residente no tiene un apartamento asignado");
        }

        visitante.setNombre(visitante.getNombre().trim());
        visitante.setDocumento(visitante.getDocumento().trim());
        // El apartamento siempre sale del residente autenticado, no del formulario.
        visitante.setApartamento(apartamento);
        visitante.setEstado(EstadoVisitante.PENDIENTE);
        visitante.setFechaEntrada(null);
        visitante.setFechaSalida(null);
        visitante.setMotivoRechazo(null);
        Visitante guardado = visitanteRepository.save(visitante);
        notificar(residente.getUsuario(), "Solicitud de visitante recibida",
                "La solicitud para " + guardado.getNombre()
                        + " fue registrada y está pendiente de aprobación.");
        notificarRoles(Rol.PORTERIA, "Nueva solicitud de visitante",
                "Hay una solicitud de acceso pendiente para el apartamento "
                        + apartamento.getNumero() + ".");
        return guardado;
    }

    @Transactional
    public void aprobarSolicitud(Long id) {
        Visitante visitante = buscar(id);
        exigirEstado(visitante, EstadoVisitante.PENDIENTE,
                "Solo se pueden aprobar solicitudes pendientes");
        visitante.setEstado(EstadoVisitante.APROBADA);
        visitante.setMotivoRechazo(null);
        visitanteRepository.save(visitante);
        notificarResidente(visitante, "Solicitud de visitante aprobada",
                "Portería aprobó el acceso de " + visitante.getNombre() + ".");
    }

    @Transactional
    public void rechazarSolicitud(Long id, String motivo) {
        Visitante visitante = buscar(id);
        exigirEstado(visitante, EstadoVisitante.PENDIENTE,
                "Solo se pueden rechazar solicitudes pendientes");
        String motivoNormalizado = StringUtils.hasText(motivo)
                ? motivo.trim() : "Solicitud rechazada por portería";
        if (motivoNormalizado.length() > 500) {
            throw new IllegalArgumentException("El motivo no puede superar 500 caracteres");
        }
        visitante.setEstado(EstadoVisitante.RECHAZADA);
        visitante.setMotivoRechazo(motivoNormalizado);
        visitanteRepository.save(visitante);
        notificarResidente(visitante, "Solicitud de visitante rechazada",
                "La solicitud de " + visitante.getNombre() + " fue rechazada. Motivo: "
                        + motivoNormalizado);
    }

    @Transactional
    public void registrarEntrada(Long id) {
        Visitante visitante = buscar(id);
        exigirEstado(visitante, EstadoVisitante.APROBADA,
                "La solicitud debe estar aprobada antes de registrar la entrada");
        visitante.setEstado(EstadoVisitante.DENTRO);
        visitante.setFechaEntrada(LocalDateTime.now());
        visitanteRepository.save(visitante);
        notificarResidente(visitante, "Entrada de visitante registrada",
                "Portería registró la entrada de " + visitante.getNombre() + ".");
    }

    @Transactional
    public void registrarSalida(Long id) {
        Visitante visitante = buscar(id);
        exigirEstado(visitante, EstadoVisitante.DENTRO,
                "Solo se puede registrar la salida de un visitante dentro del conjunto");
        visitante.setEstado(EstadoVisitante.FINALIZADA);
        visitante.setFechaSalida(LocalDateTime.now());
        visitanteRepository.save(visitante);
        notificarResidente(visitante, "Salida de visitante registrada",
                "Portería registró la salida de " + visitante.getNombre() + ".");
    }

    private Visitante buscar(Long id) {
        return visitanteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Visitante no encontrado"));
    }

    private void exigirEstado(Visitante visitante, EstadoVisitante esperado, String mensaje) {
        if (visitante.getEstado() != esperado) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private void validarDatosSolicitud(Visitante visitante) {
        if (visitante == null || !StringUtils.hasText(visitante.getNombre())) {
            throw new IllegalArgumentException("El nombre del visitante es obligatorio");
        }
        if (!StringUtils.hasText(visitante.getDocumento())
                || !visitante.getDocumento().trim().matches("\\d{8,}")) {
            throw new IllegalArgumentException(
                    "El documento debe tener al menos 8 dígitos y solo números");
        }
    }

    private void notificarResidente(Visitante visitante, String titulo, String mensaje) {
        if (visitante == null || visitante.getApartamento() == null
                || visitante.getApartamento().getId() == null) {
            return;
        }
        residenteRepository.findFirstByApartamentoId(visitante.getApartamento().getId())
                .ifPresent(residente -> notificar(residente.getUsuario(), titulo, mensaje));
    }

    private void notificar(Usuario usuario, String titulo, String mensaje) {
        if (notificacionService == null || usuario == null) {
            return;
        }
        try {
            notificacionService.crear(usuario, titulo, mensaje, "/visitantes");
        } catch (RuntimeException exception) {
            log.warn("No se pudo crear la notificacion del visitante", exception);
        }
    }

    private void notificarRoles(Rol rol, String titulo, String mensaje) {
        if (usuarioRepository == null || rol == null) {
            return;
        }
        usuarioRepository.findAll().stream()
                .filter(usuario -> usuario.isActivo() && usuario.getRol() == rol)
                .forEach(usuario -> notificar(usuario, titulo, mensaje));
    }
}
