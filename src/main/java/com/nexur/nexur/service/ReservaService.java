package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Reserva;
import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.enums.EstadoReserva;
import com.nexur.nexur.model.enums.TipoEspacio;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.ReservaRepository;
import com.nexur.nexur.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

    private final ReservaRepository reservaRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    @Autowired
    public ReservaService(ReservaRepository reservaRepository, ApartamentoRepository apartamentoRepository,
                          UsuarioRepository usuarioRepository,
                          NotificacionService notificacionService) {
        this.reservaRepository = reservaRepository;
        this.apartamentoRepository = apartamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    public ReservaService(ReservaRepository reservaRepository, ApartamentoRepository apartamentoRepository) {
        this(reservaRepository, apartamentoRepository, null, null);
    }

     

    public List<Reserva> listarReservas() {
        return reservaRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public List<Reserva> listarReservasPorResidente(Long residenteId) {
        return reservaRepository.findByResidenteIdOrderByIdDesc(residenteId);
    }

    public List<Reserva> obtenerUltimasReservas() {
        return reservaRepository.findTop4ByOrderByIdDesc();
    }

    @Transactional
    public Reserva guardar(Reserva reserva, Long apartamentoId) {
        if (reserva == null) {
            throw new IllegalArgumentException("La reserva es obligatoria");
        }
        if (reserva.getTipoEspacio() == null) {
            throw new IllegalArgumentException("Debe seleccionar el área a reservar");
        }
        if (apartamentoId == null) {
            throw new IllegalArgumentException("Debe seleccionar un apartamento");
        }
        if (reserva.getFechaInicio() == null || reserva.getFechaFin() == null
                || !reserva.getFechaFin().isAfter(reserva.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha final debe ser posterior a la fecha inicial");
        }
        if (reserva.getFechaInicio().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La reserva debe comenzar en el futuro");
        }
        Apartamento apartamento = apartamentoRepository.findById(apartamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Apartamento no encontrado"));
        if (reserva.getResidente() != null
                && (reserva.getResidente().getApartamento() == null
                || !apartamentoId.equals(reserva.getResidente().getApartamento().getId()))) {
            throw new IllegalArgumentException("El apartamento no pertenece al residente que solicita la reserva");
        }
        reserva.setApartamento(apartamento);

        List<Reserva> conflictos = reservaRepository
            .findByTipoEspacioAndEstadoInAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                    reserva.getTipoEspacio(),
                List.of(EstadoReserva.PENDIENTE, EstadoReserva.APROBADA),
                    reserva.getFechaFin(),
                    reserva.getFechaInicio()
            );

           if (!conflictos.isEmpty()) {
          throw new IllegalArgumentException("Ya existe una reserva en ese horario para este espacio");
         }

         if (reserva.getObservaciones() == null || reserva.getObservaciones().isBlank()) {
    reserva.setObservaciones(generarObservaciones(reserva.getTipoEspacio()));
}
          Reserva guardada = reservaRepository.save(reserva);
          notificar(reserva.getResidente() == null ? null : reserva.getResidente().getUsuario(),
                  "Solicitud de reserva recibida",
                  "Tu solicitud para " + guardada.getTipoEspacio()
                          + " fue registrada y está pendiente de aprobación.");
          notificarRoles(Rol.ADMIN, "Nueva solicitud de reserva",
                  "Hay una solicitud de reserva pendiente para " + guardada.getTipoEspacio() + ".");
          return guardada;
          }

    public long contarReservas() {
        return reservaRepository.count();
    }

    public long contarReservasPendientes() {
        return reservaRepository.countByEstado(
             com.nexur.nexur.model.enums.EstadoReserva.PENDIENTE
        );
    }
    

    private String generarObservaciones(TipoEspacio tipoEspacio) {
    return switch (tipoEspacio) {
        case PISCINA -> "Debe usar gorro y traje adecuado para piscina y llevar gorro ";
        case BBQ -> "Debe limpiar el área después de usarla ";
        case GIMNASIO -> "Usar toalla y desinfectar equipos por favor no tirar material al suelo";
        case SALON_SOCIAL -> "Respetar horarios y normas de ruido";
    };
}

public void aprobarReserva(Long id, String comentario) {
    Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

    if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
        throw new IllegalArgumentException("Solo se pueden aprobar reservas pendientes");
    }

    reserva.setEstado(EstadoReserva.APROBADA);

    if (comentario != null && !comentario.isBlank()) {
        reserva.setObservaciones(comentario);
    }

    reservaRepository.save(reserva);
    notificar(reserva.getResidente() == null ? null : reserva.getResidente().getUsuario(),
            "Reserva aprobada", "Tu reserva para " + reserva.getTipoEspacio() + " fue aprobada.");
}

public void rechazarReserva(Long id, String comentario) {
    Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

    if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
        throw new IllegalArgumentException("Solo se pueden rechazar reservas pendientes");
    }

    reserva.setEstado(EstadoReserva.RECHAZADA);

    if (comentario != null && !comentario.isBlank()) {
        reserva.setObservaciones(comentario);
    }

    reservaRepository.save(reserva);
    notificar(reserva.getResidente() == null ? null : reserva.getResidente().getUsuario(),
            "Reserva rechazada", "Tu reserva para " + reserva.getTipoEspacio()
                    + " fue rechazada." + (comentario == null || comentario.isBlank()
                    ? "" : " Motivo: " + comentario.trim()));
}

private void notificar(Usuario usuario, String titulo, String mensaje) {
    if (notificacionService == null || usuario == null) {
        return;
    }
    try {
        notificacionService.crear(usuario, titulo, mensaje, "/reservas");
    } catch (RuntimeException exception) {
        log.warn("No se pudo crear la notificacion de la reserva", exception);
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
