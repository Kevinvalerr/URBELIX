package com.nexur.nexur.repository;

import com.nexur.nexur.model.Parqueadero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface ParqueaderoRepository extends JpaRepository<Parqueadero, Long> {
    Optional<Parqueadero> findByNumero(String numero);
    @EntityGraph(attributePaths = {"apartamento", "vehiculo"})
    List<Parqueadero> findByApartamentoIdOrderByNumeroAsc(Long apartamentoId);
    @EntityGraph(attributePaths = {"apartamento", "vehiculo"})
    List<Parqueadero> findAllByOrderByNumeroAsc();
    List<Parqueadero> findByEstadoOrderByNumeroAsc(com.nexur.nexur.model.EstadoParqueadero estado);
    Optional<Parqueadero> findByVehiculoId(Long vehiculoId);
    long countByEstado(com.nexur.nexur.model.EstadoParqueadero estado);
    boolean existsByNumero(String numero);
    boolean existsByNumeroIgnoreCaseAndIdNot(String numero, Long id);
}
