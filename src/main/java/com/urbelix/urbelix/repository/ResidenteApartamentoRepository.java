package com.urbelix.urbelix.repository;

import com.urbelix.urbelix.model.ResidenteApartamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResidenteApartamentoRepository extends JpaRepository<ResidenteApartamento, Long> {

    boolean existsByResidenteIdAndApartamentoIdAndActivoTrue(Long residenteId, Long apartamentoId);

    long countByApartamentoIdAndActivoTrue(Long apartamentoId);

    List<ResidenteApartamento> findByResidenteIdAndActivoTrue(Long residenteId);
}