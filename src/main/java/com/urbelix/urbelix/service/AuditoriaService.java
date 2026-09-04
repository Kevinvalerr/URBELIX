package com.urbelix.urbelix.service;

import com.urbelix.urbelix.model.AuditoriaEvento;
import com.urbelix.urbelix.model.Usuario;
import com.urbelix.urbelix.repository.AuditoriaEventoRepository;
import com.urbelix.urbelix.repository.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

    private final AuditoriaEventoRepository repository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(AuditoriaEventoRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registrar(String accion, String modulo, String entidad, Long entidadId,
                          String resultado, String descripcion) {
        Usuario actor = null;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            actor = usuarioRepository.findByEmail(authentication.getName()).orElse(null);
        }
        repository.save(new AuditoriaEvento(actor, accion, modulo, entidad, entidadId, resultado, descripcion));
    }
}
