package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.repository.ApartamentoRepository;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/*
Esta clase contiene la lógica de negocio relacionada con los residentes.
El Service actúa como intermedio entre el controller y el Repository.
*/

@Service
public class ResidenteService {

    private final ApartamentoRepository apartamentoRepository;
    private final ResidenteRepository residenteRepository;
    private final UsuarioService usuarioService;

    public ResidenteService(ResidenteRepository residenteRepository,
                            ApartamentoRepository apartamentoRepository,
                            UsuarioService usuarioService) {
        this.residenteRepository = residenteRepository;
        this.apartamentoRepository = apartamentoRepository;
        this.usuarioService = usuarioService;
    }

    public List<Residente> obtenerTodos() {
        return residenteRepository.findAll();
    }

    public Residente guardar(Residente residente, Long apartamentoId) {
        Apartamento apartamento = apartamentoRepository
                .findById(apartamentoId)
                .orElseThrow(() -> new RuntimeException("Apartamento no encontrado"));

        residente.setApartamento(apartamento);

        Usuario usuario = residente.getUsuario();

        if (usuario != null && StringUtils.hasText(usuario.getEmail())) {
            if (usuario.getId() == null) {
                if (usuario.getRol() == null) {
                    usuario.setRol(Rol.RESIDENTE);
                }
                usuario = usuarioService.guardarUsuario(usuario);
            } else {
                usuario = usuarioService.guardarUsuarioActualizado(usuario);
            }
            residente.setUsuario(usuario);
        } else if (residente.getId() != null) {
            Residente residenteExistente = buscarPorId(residente.getId());
            residente.setUsuario(residenteExistente.getUsuario());
        }

        return residenteRepository.save(residente);
    }

    public void eliminar(Long id) {
        residenteRepository.deleteById(id);
    }

    public Residente buscarPorId(Long id) {
        return residenteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Residente no encontrado"));
    }

    public Residente buscarPorUsuarioEmail(String email) {
        return residenteRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new RuntimeException("Residente no encontrado"));
    }
}

