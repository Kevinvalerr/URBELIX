package com.nexur.nexur.repository;

import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.TipoPago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    @Override
    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Pago> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {"residente", "apartamento"})
    Optional<Pago> findById(Long id);

    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Pago> findTop4ByOrderByIdDesc();

    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Pago> findByFechaBetween(LocalDate fechaInicio, LocalDate fechaFin);
    List<Pago> findByMetodo(String metodo);

    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Pago> findByResidenteUsuarioEmail(String email);
    boolean existsByApartamentoId(Long apartamentoId);
    long countByApartamentoId(Long apartamentoId);
    
    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Pago> findByEstadoPago(EstadoPago estadoPago);

    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Pago> findByEstadoPagoAndFechaVencimientoBefore(EstadoPago estadoPago, LocalDate fecha);

    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Pago> findByEstadoPagoAndFechaPagoIsNull(EstadoPago estadoPago);

    @EntityGraph(attributePaths = {"residente", "apartamento"})
    List<Pago> findByResidenteUsuarioEmailAndEstadoPago(String email, EstadoPago estadoPago);

    Optional<Pago> findByReferenciaPago(String referenciaPago);

    boolean existsByResidenteIdAndTipoPagoAndFechaBetween(Long residenteId, TipoPago tipoPago,
                                                           LocalDate fechaInicio, LocalDate fechaFin);
}
