package com.nexur.nexur.repository;

import com.nexur.nexur.model.Incidencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Incidencia> findByResidenteIdOrderByCreadoEnDesc(Long residenteId);

    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Incidencia> findAllByOrderByCreadoEnDesc();

    @Query("select count(i) from Incidencia i where i.estado = com.nexur.nexur.model.EstadoIncidencia.ABIERTA")
    long countAbiertas();
}
