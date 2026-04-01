package com.nexur.nexur.repository;

import com.nexur.nexur.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findTop4ByOrderByIdDesc();
    List<Pago> findByFechaBetween(LocalDate fechaInicio, LocalDate fechaFin);
    List<Pago> findByMetodo(String metodo);
}
