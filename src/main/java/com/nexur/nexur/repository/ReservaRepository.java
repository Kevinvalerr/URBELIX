package com.nexur.nexur.repository;

import com.nexur.nexur.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.nexur.nexur.model.enums.EstadoReserva;

import java.time.LocalDateTime;
import com.nexur.nexur.model.enums.TipoEspacio;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findTop4ByOrderByIdDesc();
    long countByEstado(String estado);
    
    Long countByEstado(EstadoReserva estado);
    boolean existsByApartamentoId(Long apartamentoId);
    long countByApartamentoId(Long apartamentoId);
    List<Reserva> findByFechaInicioBetween(LocalDateTime inicio, LocalDateTime fin);
    List<Reserva> findByTipoEspacio(TipoEspacio tipoEspacio);
    List<Reserva> findByTipoEspacioAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
       TipoEspacio tipoEspacio,
       LocalDateTime fechaFin,
       LocalDateTime fechaInicio
    );
}
