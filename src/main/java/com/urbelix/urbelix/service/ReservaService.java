package com.urbelix.urbelix.service;

import com.urbelix.urbelix.model.Apartamento;
import com.urbelix.urbelix.model.Reserva;
import com.urbelix.urbelix.model.enums.EstadoReserva;
import com.urbelix.urbelix.model.enums.TipoEspacio;
import com.urbelix.urbelix.repository.ApartamentoRepository;
import com.urbelix.urbelix.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.util.List;

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

    public List<Reserva> obtenerUltimasReservas() {
        return reservaRepository.findTop4ByOrderByIdDesc();
    }

    public Reserva guardar(Reserva reserva, Long apartamentoId) {
        Apartamento apartamento = apartamentoRepository.findById(apartamentoId)
                .orElseThrow(() -> new RuntimeException("Apartamento no encontrado"));
        reserva.setApartamento(apartamento);

        List<Reserva> conflictos = reservaRepository
            .findByTipoEspacioAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                    reserva.getTipoEspacio(),
                    reserva.getFechaFin(),
                    reserva.getFechaInicio()
            );

           if (!conflictos.isEmpty()) {
          throw new RuntimeException("Ya existe una reserva en ese horario para este espacio");
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
             com.urbelix.urbelix.model.enums.EstadoReserva.PENDIENTE
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
            .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

    reserva.setEstado(EstadoReserva.APROBADA);

    if (comentario != null && !comentario.isBlank()) {
        reserva.setObservaciones(comentario);
    }

    reservaRepository.save(reserva);
}

public void rechazarReserva(Long id, String comentario) {
    Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

    reserva.setEstado(EstadoReserva.RECHAZADA);

    if (comentario != null && !comentario.isBlank()) {
        reserva.setObservaciones(comentario);
    }

    reservaRepository.save(reserva);
}
}
