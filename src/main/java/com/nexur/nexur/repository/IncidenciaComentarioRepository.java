package com.nexur.nexur.repository;

import com.nexur.nexur.model.IncidenciaComentario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidenciaComentarioRepository extends JpaRepository<IncidenciaComentario, Long> {
    List<IncidenciaComentario> findByIncidenciaIdOrderByCreadoEnAsc(Long incidenciaId);
}
