package com.nexur.nexur.repository;

import com.nexur.nexur.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findTop4ByOrderByIdDesc();
    long countByEstado(String estado);
    List<Reserva> findByFechaBetween(LocalDate fechaInicio, LocalDate fechaFin);
    List<Reserva> findByArea(String area);
}
