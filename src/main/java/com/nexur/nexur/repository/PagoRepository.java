package com.nexur.nexur.repository;

import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.enums.EstadoPago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findTop4ByOrderByIdDesc();
    List<Pago> findByFechaBetween(LocalDate fechaInicio, LocalDate fechaFin);
    List<Pago> findByMetodo(String metodo);

    List<Pago> findByResidenteUsuarioEmail(String email);
    
    List<Pago> findByEstadoPago(EstadoPago estadoPago);

    List<Pago> findByResidenteUsuarioEmailAndEstadoPago(String email, EstadoPago estadoPago);
}
