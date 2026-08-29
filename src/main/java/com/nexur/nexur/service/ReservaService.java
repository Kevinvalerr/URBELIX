package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Reserva;
import com.nexur.nexur.model.enums.EstadoReserva;
import com.nexur.nexur.model.enums.TipoEspacio;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ApartamentoRepository apartamentoRepository;

    public ReservaService(ReservaRepository reservaRepository, ApartamentoRepository apartamentoRepository) {
        this.reservaRepository = reservaRepository;
        this.apartamentoRepository = apartamentoRepository;
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
          return reservaRepository.save(reserva);
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
}
}
