package com.nexur.nexur.repository;

import com.nexur.nexur.model.Visitante;
import com.nexur.nexur.model.EstadoVisitante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitanteRepository extends JpaRepository<Visitante,Long>{
    /*Esto habilita automaticamente métodos CRUD
     save() , finALl(),findBYId(),delteById() 
    */

    List<Visitante> findByEstadoOrderByFechaEntradaDesc(EstadoVisitante estado);

    List<Visitante> findByApartamentoIdAndEstadoOrderByFechaEntradaDesc(
            Long apartamentoId, EstadoVisitante estado);

    /*Esto permite filtatrar visitas de un apartamento especifico.*/
    List<Visitante> findByApartamentoId(Long apartamentoId);

    List<Visitante> findByFechaEntradaBetween(LocalDateTime inicio, LocalDateTime fin);
}
