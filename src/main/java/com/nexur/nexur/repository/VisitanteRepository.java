package com.nexur.nexur.repository;

import com.nexur.nexur.model.Visitante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitanteRepository extends JpaRepository<Visitante,Long>{
    /*Esto habilita automaticamente métodos CRUD
     save() , finALl(),findBYId(),delteById() 
    */

    List<Visitante> findByFechaSalidaIsNull();

    /*Esto permite filtatrar visitas de un apartamento especifico.*/
    List<Visitante> findByApartamentoId(Long apartamentoId);

    /*Muy útil para saber si un visitante aún no ha registrado salida.*/
    List<Visitante> findByApartamentoIdAndFechaSalidaIsNull(Long apartamentoId);

    List<Visitante> findByFechaEntradaBetween(LocalDateTime inicio, LocalDateTime fin);
}
