package com.urbelix.urbelix.service;

import com.urbelix.urbelix.model.*;
import com.urbelix.urbelix.model.enums.*;
import com.urbelix.urbelix.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDate;
import java.util.List;

@Service
public class IncidenciaService {
    private final IncidenciaRepository repository;
    private final ResidenteRepository residenteRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final ResidenteApartamentoRepository asociacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final AuditoriaService auditoriaService;
    private final IncidenciaFastApiService fastApiService;

    public IncidenciaService(IncidenciaRepository repository, ResidenteRepository residenteRepository,
            ApartamentoRepository apartamentoRepository, ResidenteApartamentoRepository asociacionRepository,
            UsuarioRepository usuarioRepository, EmailService emailService, AuditoriaService auditoriaService,
            IncidenciaFastApiService fastApiService) {
        this.repository = repository;
        this.residenteRepository = residenteRepository;
        this.apartamentoRepository = apartamentoRepository;
        this.asociacionRepository = asociacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.auditoriaService = auditoriaService;
        this.fastApiService = fastApiService;
    }

    public List<Incidencia> buscar(String texto, EstadoIncidencia estado, PrioridadIncidencia prioridad,
            String torre, String apartamento, Long residenteId, LocalDate desde, LocalDate hasta) {
        return repository.buscar(limpiar(texto), estado, prioridad, limpiar(torre), limpiar(apartamento), residenteId,
                desde == null ? null : desde.atStartOfDay(), hasta == null ? null : hasta.plusDays(1).atStartOfDay());
    }

    public List<Incidencia> buscarPropias(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow(() -> new SecurityException("Usuario no encontrado"));
        if (usuario.getResidente() == null) return List.of();
        return repository.findByResidenteIdOrderByFechaCreacionDesc(usuario.getResidente().getId());
    }

    @Transactional
    public Incidencia crear(Incidencia incidencia, Long residenteId, Long apartamentoId) {
        Residente residente = residenteRepository.findById(residenteId).orElseThrow(() -> new IllegalArgumentException("El residente no existe"));
        validarPropietario(residente);
        Apartamento apartamento = resolverApartamento(residente, apartamentoId);
        incidencia.setResidente(residente);
        incidencia.setApartamento(apartamento);
        incidencia.setEstado(EstadoIncidencia.PENDIENTE);
        Incidencia guardada = repository.save(incidencia);
        registrarEstado(guardada, null, EstadoIncidencia.PENDIENTE, null, "CREAR_INCIDENCIA", "Incidencia creada");
        fastApiService.analizar(guardada);
        notificar(guardada, EstadoIncidencia.PENDIENTE, null);
        return guardada;
    }

    @Transactional(readOnly = true)
    public Incidencia obtener(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("La incidencia no existe")); }

    @Transactional
    public Incidencia actualizar(Incidencia cambios, Long apartamentoId) {
        Incidencia actual = obtener(cambios.getId());
        validarGestion();
        if (actual.getEstado() != EstadoIncidencia.PENDIENTE) throw new IllegalStateException("Solo se pueden editar incidencias pendientes");
        actual.setTitulo(cambios.getTitulo());
        actual.setDescripcion(cambios.getDescripcion());
        actual.setCategoria(cambios.getCategoria());
        actual.setPrioridad(cambios.getPrioridad());
        actual.setApartamento(resolverApartamento(actual.getResidente(), apartamentoId));
        Incidencia guardada = repository.save(actual);
        auditoriaService.registrar("MODIFICAR_INCIDENCIA", "INCIDENCIAS", "Incidencia", guardada.getId(), "EXITO", "Datos actualizados");
        return guardada;
    }

    @Transactional
    public void cambiarEstado(Long id, EstadoIncidencia nuevoEstado, String comentario) {
        Incidencia incidencia = obtener(id);
        validarGestion();
        EstadoIncidencia anterior = incidencia.getEstado();
        if (!transicionValida(anterior, nuevoEstado)) throw new IllegalStateException("La transición de estado no es válida");
        if (nuevoEstado == EstadoIncidencia.RECHAZADA && !StringUtils.hasText(comentario)) throw new IllegalArgumentException("El motivo del rechazo es obligatorio");
        if (nuevoEstado == EstadoIncidencia.RESUELTA && !StringUtils.hasText(comentario)) throw new IllegalArgumentException("La observación de resolución es obligatoria");
        if (nuevoEstado == EstadoIncidencia.RECHAZADA) incidencia.setMotivoRechazo(comentario.trim());
        if (nuevoEstado == EstadoIncidencia.RESUELTA) incidencia.setObservacionResolucion(comentario.trim());
        repository.save(incidencia);
        String accion = nuevoEstado == EstadoIncidencia.RECHAZADA ? "RECHAZAR_INCIDENCIA" : nuevoEstado == EstadoIncidencia.RESUELTA ? "RESOLVER_INCIDENCIA" : "ACEPTAR_INCIDENCIA";
        registrarEstado(incidencia, anterior, nuevoEstado, comentario, accion, "Estado cambiado de " + anterior + " a " + nuevoEstado);
        notificar(incidencia, nuevoEstado, comentario);
    }

    @Transactional
    public void eliminar(Long id) {
        Incidencia incidencia = obtener(id);
        validarGestion();
        if (incidencia.getEstado() != EstadoIncidencia.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden eliminar incidencias pendientes");
        }
        repository.delete(incidencia);
        auditoriaService.registrar("ELIMINAR_INCIDENCIA", "INCIDENCIAS", "Incidencia", id, "EXITO", "Incidencia eliminada; auditoría conservada");
    }

    public List<Residente> residentes() { return residenteRepository.findAll(); }
    public List<Apartamento> apartamentosDe(Residente residente) { return asociacionRepository.findByResidenteIdAndActivoTrue(residente.getId()).stream().map(ResidenteApartamento::getApartamento).toList(); }
    public List<String> torres() { return apartamentoRepository.findDistinctTorres(); }
    public long contar(EstadoIncidencia estado) { return repository.countByEstado(estado); }
    public long total() { return repository.count(); }

    public void validarConsulta(Incidencia incidencia, String email) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow(() -> new SecurityException("Usuario no encontrado"));
        if (usuario.getRol() != Rol.ADMIN && usuario.getRol() != Rol.PORTERIA && (usuario.getResidente() == null || !usuario.getResidente().getId().equals(incidencia.getResidente().getId()))) throw new SecurityException("No tiene permisos para consultar esta incidencia");
    }

    private Apartamento resolverApartamento(Residente residente, Long apartamentoId) {
        if (apartamentoId == null) return residente.getApartamento();
        Apartamento apartamento = apartamentoRepository.findById(apartamentoId).orElseThrow(() -> new IllegalArgumentException("El apartamento no existe"));
        Usuario usuario = usuarioActual();
        if (usuario.getRol() != Rol.ADMIN && !asociacionRepository.existsByResidenteIdAndApartamentoIdAndActivoTrue(residente.getId(), apartamentoId)) throw new SecurityException("El apartamento no pertenece al residente");
        return apartamento;
    }
    private void validarPropietario(Residente residente) { Usuario usuario = usuarioActual(); if (usuario.getRol() != Rol.ADMIN && (usuario.getResidente() == null || !usuario.getResidente().getId().equals(residente.getId()))) throw new SecurityException("El residente no corresponde a la cuenta activa"); }
    private void validarGestion() { if (usuarioActual().getRol() != Rol.ADMIN) throw new SecurityException("Solo el administrador puede realizar esta acción"); }
    private Usuario usuarioActual() { Authentication auth = SecurityContextHolder.getContext().getAuthentication(); if (auth == null || !auth.isAuthenticated()) throw new SecurityException("Usuario no autenticado"); return usuarioRepository.findByEmail(auth.getName()).orElseThrow(() -> new SecurityException("Usuario no encontrado")); }
    private boolean transicionValida(EstadoIncidencia anterior, EstadoIncidencia nuevo) { return anterior == EstadoIncidencia.PENDIENTE && (nuevo == EstadoIncidencia.EN_PROCESO || nuevo == EstadoIncidencia.RECHAZADA) || anterior == EstadoIncidencia.EN_PROCESO && nuevo == EstadoIncidencia.RESUELTA; }
    private void registrarEstado(Incidencia incidencia, EstadoIncidencia anterior, EstadoIncidencia nuevo, String comentario, String accion, String descripcion) { incidencia.getHistorial().add(new IncidenciaHistorial(incidencia, usuarioActual(), anterior, nuevo, comentario)); auditoriaService.registrar(accion, "INCIDENCIAS", "Incidencia", incidencia.getId(), "EXITO", descripcion); }
    private void notificar(Incidencia incidencia, EstadoIncidencia estado, String comentario) { if (StringUtils.hasText(incidencia.getResidente().getCorreo())) emailService.enviarActualizacionIncidencia(incidencia.getResidente().getCorreo(), incidencia.getResidente().getNombre(), incidencia.getTitulo(), incidencia.getApartamento(), estado, comentario); }
    private String limpiar(String valor) { return StringUtils.hasText(valor) ? valor.trim() : null; }
}
