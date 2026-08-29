package com.nexur.nexur.repository;

import com.nexur.nexur.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    Optional<Vehiculo> findByPlacaIgnoreCase(String placa);
    boolean existsByPlacaIgnoreCase(String placa);
    boolean existsByPlacaIgnoreCaseAndIdNot(String placa, Long id);
    @EntityGraph(attributePaths = {"residente", "parqueadero"})
    List<Vehiculo> findAllByOrderByPlacaAsc();
    List<Vehiculo> findByResidenteIdOrderByPlacaAsc(Long residenteId);
}
