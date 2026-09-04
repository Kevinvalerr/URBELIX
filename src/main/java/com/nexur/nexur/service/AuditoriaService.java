package com.nexur.nexur.service;

import com.nexur.nexur.model.Auditoria;
import com.nexur.nexur.repository.AuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String actorEmail, String accion, String entidad, Long entidadId, String detalle) {
        Auditoria auditoria = new Auditoria();
        auditoria.setActorEmail(limpiar(actorEmail, "SISTEMA", 180));
        auditoria.setAccion(limpiar(accion, "MUTACION", 120));
        auditoria.setEntidad(limpiar(entidad, "HTTP", 255));
        auditoria.setEntidadId(entidadId);
        auditoria.setDetalle(limpiar(detalle, null, 500));
        auditoria.setCreadoEn(LocalDateTime.now());
        auditoriaRepository.save(auditoria);
    }

    @Transactional(readOnly = true)
    public List<Auditoria> listarRecientes() {
        return auditoriaRepository.findTop200ByOrderByCreadoEnDesc();
    }

    private String limpiar(String valor, String valorPorDefecto, int longitudMaxima) {
        String resultado = StringUtils.hasText(valor) ? valor.trim() : valorPorDefecto;
        if (resultado == null) {
            return null;
        }
        return resultado.length() <= longitudMaxima
                ? resultado
                : resultado.substring(0, longitudMaxima);
    }
}
