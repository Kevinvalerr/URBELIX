package com.urbelix.urbelix.service;

import com.urbelix.urbelix.model.Apartamento;
import com.urbelix.urbelix.model.Residente;
import com.urbelix.urbelix.model.Rol;
import com.urbelix.urbelix.model.Usuario;
import com.urbelix.urbelix.model.EstadoApartamento;
import com.urbelix.urbelix.model.ResidenteApartamento;
import com.urbelix.urbelix.repository.ResidenteRepository;
import com.urbelix.urbelix.repository.ApartamentoRepository;
import com.urbelix.urbelix.repository.ResidenteApartamentoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/*
Esta clase contiene la lógica de negocio relacionada con los residentes.
El Service actúa como intermedio entre el controller y el Repository.
*/

@Service
public class ResidenteService {

    private final ApartamentoRepository apartamentoRepository;
    private final ResidenteRepository residenteRepository;
    private final UsuarioService usuarioService;
    private final ResidenteApartamentoRepository residenteApartamentoRepository;
    private final EmailService emailService;
    private final AuditoriaService auditoriaService;

    public ResidenteService(ResidenteRepository residenteRepository,
                            ApartamentoRepository apartamentoRepository,
                            UsuarioService usuarioService,
                            ResidenteApartamentoRepository residenteApartamentoRepository,
                            EmailService emailService,
                            AuditoriaService auditoriaService) {
        this.residenteRepository = residenteRepository;
        this.apartamentoRepository = apartamentoRepository;
        this.usuarioService = usuarioService;
        this.residenteApartamentoRepository = residenteApartamentoRepository;
        this.emailService = emailService;
        this.auditoriaService = auditoriaService;
    }

    public List<Residente> obtenerTodos() {
        return residenteRepository.findAll();
    }

    @Transactional
    public Residente guardar(Residente residente, Long apartamentoId) {
        Apartamento apartamento = apartamentoRepository
                .findById(apartamentoId)
                .orElseThrow(() -> new RuntimeException("Apartamento no encontrado"));

        residente.setApartamento(apartamento);

        Usuario usuario = residente.getUsuario();

        if (usuario != null && StringUtils.hasText(usuario.getEmail())) {
            if (usuario.getId() == null) {
                if (usuario.getRol() == null) {
                    usuario.setRol(Rol.RESIDENTE);
                }
                usuario = usuarioService.guardarUsuario(usuario);
            } else {
                usuario = usuarioService.guardarUsuarioActualizado(usuario);
            }
            residente.setUsuario(usuario);
        } else if (residente.getId() != null) {
            Residente residenteExistente = buscarPorId(residente.getId());
            residente.setUsuario(residenteExistente.getUsuario());
        }

        return residenteRepository.save(residente);
    }

    @Transactional
    public Residente crearConCuenta(Residente residente, Collection<Long> apartamentoIds) {
        if (residente == null || !StringUtils.hasText(residente.getCorreo())) {
            throw new IllegalArgumentException("El correo del residente es obligatorio");
        }
        if (residenteRepository.existsByDocumento(residente.getDocumento())) {
            throw new IllegalArgumentException("El documento ya pertenece a un residente");
        }
        String correo = residente.getCorreo().trim().toLowerCase(java.util.Locale.ROOT);
        if (usuarioService.existePorEmail(correo)) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        Set<Long> ids = new HashSet<>(apartamentoIds == null ? List.of() : apartamentoIds);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un apartamento");
        }

        List<Apartamento> apartamentos = ids.stream()
                .map(id -> apartamentoRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Apartamento no encontrado: " + id)))
                .toList();

        residente.setCorreo(correo);
        residente.setApartamento(apartamentos.get(0));
        Residente guardado = residenteRepository.save(residente);

        for (Apartamento apartamento : apartamentos) {
            if (residenteApartamentoRepository.existsByResidenteIdAndApartamentoIdAndActivoTrue(
                    guardado.getId(), apartamento.getId())) {
                throw new IllegalArgumentException("La asociación residente-apartamento ya existe");
            }
            ResidenteApartamento asociacion = new ResidenteApartamento();
            asociacion.setResidente(guardado);
            asociacion.setApartamento(apartamento);
            asociacion.setFechaAsignacion(java.time.LocalDate.now());
            asociacion.setActivo(true);
            residenteApartamentoRepository.save(asociacion);
            if (!EstadoApartamento.MANTENIMIENTO.name().equalsIgnoreCase(apartamento.getEstado())) {
                apartamento.setEstado(EstadoApartamento.OCUPADO.name());
                apartamentoRepository.save(apartamento);
            }
        }

        String passwordTemporal = usuarioService.crearCuentaResidente(guardado);
        emailService.enviarCredencialesIniciales(guardado.getCorreo(), guardado.getNombre(), passwordTemporal);
        auditoriaService.registrar("CREAR_RESIDENTE", "RESIDENTES", "Residente", guardado.getId(),
            "EXITO", "Residente creado con asociaciones de apartamento");
        return guardado;
    }

    public void eliminar(Long id) {
        residenteRepository.deleteById(id);
    }

    public Residente buscarPorId(Long id) {
        return residenteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Residente no encontrado"));
    }

    public Residente buscarPorUsuarioEmail(String email) {
        return residenteRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new RuntimeException("Residente no encontrado"));
    }
}

