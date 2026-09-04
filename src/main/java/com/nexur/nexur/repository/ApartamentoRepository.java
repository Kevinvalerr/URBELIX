package com.nexur.nexur.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nexur.nexur.model.Apartamento;

@Repository
public interface ApartamentoRepository extends JpaRepository<Apartamento, Long> {

    // 🔹 Buscar por número (CLAVE para Excel y creación de residentes)
    Optional<Apartamento> findByNumero(String numero);

    // 🔹 Validar si existe (útil para validaciones)
    boolean existsByNumero(String numero);

    // 🔹 Buscar por torre (opcional, útil para filtros)
    List<Apartamento> findByTorre(String torre);

    // 🔹 Buscar por estado (opcional: disponible, ocupado, etc.)
    List<Apartamento> findByEstado(String estado);

    @Query("SELECT DISTINCT a.torre FROM Apartamento a")
List<String> findDistinctTorres();
}