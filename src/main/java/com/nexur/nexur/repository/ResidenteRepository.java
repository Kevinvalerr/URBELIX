package com.nexur.nexur.repository;

import com.nexur.nexur.model.Residente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
/*
Este repository permite interactuar con la tabla "residentes"
sin escribir consultas SQL manualmente.
Sprind Data JPA generará atumáticamente las operaciones CRUD.
*/

@Repository
public interface ResidenteRepository extends JpaRepository<Residente, Long> {

    Optional<Residente> findByUsuarioEmail(String email);

}
