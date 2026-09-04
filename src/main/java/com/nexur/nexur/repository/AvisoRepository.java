package com.nexur.nexur.repository;

import com.nexur.nexur.model.Aviso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {

    List<Aviso> findAllByOrderByPublicadoEnDesc();

    @Query("select a from Aviso a where a.activo = true and (a.venceEn is null or a.venceEn >= :ahora) order by a.publicadoEn desc")
    List<Aviso> findVisibles(LocalDateTime ahora);
}
