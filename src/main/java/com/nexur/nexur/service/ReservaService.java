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
        if (reserva.getEstado() == null) {
            reserva.setEstado(EstadoReserva.PENDIENTE);
        }

        List<Reserva> conflictos = reservaRepository
            .findByTipoEspacioAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                    reserva.getTipoEspacio(),
                    reserva.getFechaFin(),
                    reserva.getFechaInicio()
            );

           if (conflictos.stream().anyMatch(r -> r.getEstado() == EstadoReserva.APROBADA)) {
          throw new RuntimeException("Ya existe una reserva aprobada en ese horario para este espacio");
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

   public void aprobarReserva(Long id) {
    aprobarReserva(id, null);
}

public void aprobarReserva(Long id, String comentario) {
    Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

    reserva.setEstado(EstadoReserva.APROBADA);
    agregarObservacionAdmin(reserva, comentario, "APROBADA");
    reservaRepository.save(reserva);
}

public void rechazarReserva(Long id) {
    rechazarReserva(id, null);
}

public void rechazarReserva(Long id, String comentario) {
    Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

    reserva.setEstado(EstadoReserva.RECHAZADA);
    agregarObservacionAdmin(reserva, comentario, "RECHAZADA");
    reservaRepository.save(reserva);
}

private void agregarObservacionAdmin(Reserva reserva, String comentario, String estado) {
    if (comentario == null || comentario.isBlank()) {
        return;
    }
    String textoComentario = "Observación administrativa (" + estado + "): " + comentario.trim();
    String observacionesExistentes = reserva.getObservaciones();
    if (observacionesExistentes == null || observacionesExistentes.isBlank()) {
        reserva.setObservaciones(textoComentario);
    } else {
        reserva.setObservaciones(observacionesExistentes + "\n" + textoComentario);
    }
}

}
