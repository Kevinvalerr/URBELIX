package com.nexur.nexur.repository;

import com.nexur.nexur.model.EstadoMovimientoParqueadero;
import com.nexur.nexur.model.MovimientoParqueadero;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MovimientoParqueaderoRepository extends JpaRepository<MovimientoParqueadero, Long> {
    Optional<MovimientoParqueadero> findByVehiculoIdAndEstado(Long vehiculoId, EstadoMovimientoParqueadero estado);
    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.residente", "parqueadero"})
    List<MovimientoParqueadero> findByEstadoOrderByFechaHoraIngresoDesc(EstadoMovimientoParqueadero estado);
    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.residente", "parqueadero"})
    List<MovimientoParqueadero> findAllByOrderByFechaHoraIngresoDesc();
    long countByEstado(EstadoMovimientoParqueadero estado);
    boolean existsByVehiculoId(Long vehiculoId);
    boolean existsByParqueaderoId(Long parqueaderoId);
}
