package com.nexur.nexur.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.Rol;
import java.util.Optional;
import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRolAndActivoTrue(Rol rol);

    List<Usuario> findByRolAndActivoTrue(Rol rol);
}
