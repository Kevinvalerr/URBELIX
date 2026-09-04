package com.nexur.nexur.repository;

import com.nexur.nexur.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import com.nexur.nexur.model.enums.EstadoReserva;

import java.time.LocalDateTime;
import com.nexur.nexur.model.enums.TipoEspacio;
import java.time.LocalDate;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    @Override
    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Reserva> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {"residente", "apartamento"})
    Optional<Reserva> findById(Long id);

    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Reserva> findTop4ByOrderByIdDesc();
    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Reserva> findByResidenteIdOrderByIdDesc(Long residenteId);
    long countByEstado(String estado);

    Long countByEstado(EstadoReserva estado);
    boolean existsByApartamentoId(Long apartamentoId);
    long countByApartamentoId(Long apartamentoId);
    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Reserva> findByFechaInicioBetween(LocalDateTime inicio, LocalDateTime fin);
    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Reserva> findByTipoEspacio(TipoEspacio tipoEspacio);
    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Reserva> findByTipoEspacioAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
       TipoEspacio tipoEspacio,
       LocalDateTime fechaFin,
       LocalDateTime fechaInicio
    );
     @EntityGraph(attributePaths = {"residente", "apartamento"})
     List<Reserva> findByTipoEspacioAndEstadoInAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
         TipoEspacio tipoEspacio,
         Collection<EstadoReserva> estados,
         LocalDateTime fechaFin,
         LocalDateTime fechaInicio
     );
}
