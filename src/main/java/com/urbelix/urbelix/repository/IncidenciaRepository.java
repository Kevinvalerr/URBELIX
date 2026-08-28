package com.urbelix.urbelix.repository;

import com.urbelix.urbelix.model.Incidencia;
import com.urbelix.urbelix.model.enums.EstadoIncidencia;
import com.urbelix.urbelix.model.enums.PrioridadIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    @Query("select i from Incidencia i join fetch i.residente r left join fetch i.apartamento a " +
           "where (:texto is null or lower(i.titulo) like lower(concat('%', :texto, '%')) or lower(i.descripcion) like lower(concat('%', :texto, '%')) or lower(r.nombre) like lower(concat('%', :texto, '%')) or r.documento like concat('%', :texto, '%')) " +
           "and (:estado is null or i.estado = :estado) and (:prioridad is null or i.prioridad = :prioridad) " +
           "and (:torre is null or a.torre = :torre) and (:apartamento is null or a.numero = :apartamento) " +
           "and (:residenteId is null or r.id = :residenteId) and (:desde is null or i.fechaCreacion >= :desde) and (:hasta is null or i.fechaCreacion < :hasta) " +
           "order by i.fechaCreacion desc")
    List<Incidencia> buscar(@Param("texto") String texto, @Param("estado") EstadoIncidencia estado, @Param("prioridad") PrioridadIncidencia prioridad, @Param("torre") String torre, @Param("apartamento") String apartamento, @Param("residenteId") Long residenteId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
    List<Incidencia> findByResidenteIdOrderByFechaCreacionDesc(Long residenteId);
    long countByEstado(EstadoIncidencia estado);
    long count();
}
