package com.nexur.nexur.repository;

import com.nexur.nexur.model.IncidenciaAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncidenciaAdjuntoRepository extends JpaRepository<IncidenciaAdjunto, Long> {
    Optional<IncidenciaAdjunto> findByIdAndIncidenciaId(Long id, Long incidenciaId);
}
