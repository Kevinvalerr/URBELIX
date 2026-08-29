package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.EstadoVisitante;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Visitante;
import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.repository.VisitanteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VisitanteService {

    private final VisitanteRepository visitanteRepository;
    private final ResidenteRepository residenteRepository;

    public VisitanteService(VisitanteRepository visitanteRepository,
                            ResidenteRepository residenteRepository) {
        this.visitanteRepository = visitanteRepository;
        this.residenteRepository = residenteRepository;
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
        return visitanteRepository.save(visitante);
    }

    @Transactional
    public void aprobarSolicitud(Long id) {
        Visitante visitante = buscar(id);
        exigirEstado(visitante, EstadoVisitante.PENDIENTE,
                "Solo se pueden aprobar solicitudes pendientes");
        visitante.setEstado(EstadoVisitante.APROBADA);
        visitante.setMotivoRechazo(null);
        visitanteRepository.save(visitante);
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
    }

    @Transactional
    public void registrarEntrada(Long id) {
        Visitante visitante = buscar(id);
        exigirEstado(visitante, EstadoVisitante.APROBADA,
                "La solicitud debe estar aprobada antes de registrar la entrada");
        visitante.setEstado(EstadoVisitante.DENTRO);
        visitante.setFechaEntrada(LocalDateTime.now());
        visitanteRepository.save(visitante);
    }

    @Transactional
    public void registrarSalida(Long id) {
        Visitante visitante = buscar(id);
        exigirEstado(visitante, EstadoVisitante.DENTRO,
                "Solo se puede registrar la salida de un visitante dentro del conjunto");
        visitante.setEstado(EstadoVisitante.FINALIZADA);
        visitante.setFechaSalida(LocalDateTime.now());
        visitanteRepository.save(visitante);
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
}
