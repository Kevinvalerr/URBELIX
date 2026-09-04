package com.nexur.nexur.service;

import com.nexur.nexur.model.Aviso;
import com.nexur.nexur.repository.AvisoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AvisoService {

    private final AvisoRepository avisoRepository;

    public AvisoService(AvisoRepository avisoRepository) {
        this.avisoRepository = avisoRepository;
    }

    public List<Aviso> listarParaAdministracion() {
        return avisoRepository.findAllByOrderByPublicadoEnDesc();
    }

    public List<Aviso> listarVisibles() {
        return avisoRepository.findVisibles(LocalDateTime.now());
    }

    @Transactional
    public Aviso publicar(Aviso aviso) {
        if (aviso == null || !StringUtils.hasText(aviso.getTitulo())) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        if (!StringUtils.hasText(aviso.getContenido())) {
            throw new IllegalArgumentException("El contenido es obligatorio");
        }
        if (aviso.getVenceEn() != null && !aviso.getVenceEn().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha de vencimiento debe ser futura");
        }
        aviso.setTitulo(aviso.getTitulo().trim());
        aviso.setContenido(aviso.getContenido().trim());
        aviso.setPublicadoEn(LocalDateTime.now());
        aviso.setActivo(true);
        return avisoRepository.save(aviso);
    }

    @Transactional
    public void cambiarEstado(Long id, boolean activo) {
        Aviso aviso = avisoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aviso no encontrado"));
        aviso.setActivo(activo);
        avisoRepository.save(aviso);
    }
}
