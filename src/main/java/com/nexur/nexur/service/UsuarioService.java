package com.nexur.nexur.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ResidenteRepository residenteRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ResidenteRepository residenteRepository,
                          ApartamentoRepository apartamentoRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.residenteRepository = residenteRepository;
        this.apartamentoRepository = apartamentoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    //  MÉTODO NUEVO (CLAVE)
    @Transactional
    public Usuario crearUsuarioConResidente(
            String nombre,
            String email,
            String password,
            String documento,
            String telefono,
            String numeroApartamento
    ) {

        // Validar email único
        if (usuarioRepository.existsByEmail(email)) {
            throw new RuntimeException("El email ya está en uso");
        }

        // Validar documento único
        if (residenteRepository.existsByDocumento(documento)) {
            throw new RuntimeException("El documento ya está registrado");
        }

        // Buscar apartamento
        Apartamento apartamento = apartamentoRepository
                .findByNumero(numeroApartamento)
                .orElseThrow(() -> new RuntimeException("Apartamento no encontrado"));

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol(Rol.RESIDENTE);

        // Crear residente
        Residente residente = new Residente();
        residente.setNombre(nombre);
        residente.setDocumento(documento);
        residente.setTelefono(telefono);
        residente.setApartamento(apartamento);

        // Relación bidireccional
        residente.setUsuario(usuario);
        usuario.setResidente(residente);

        // Guardar todo (cascade)
        return usuarioRepository.save(usuario);
    }

    //  MÉTODO ORIGINAL (NO TOCAR)
    public Usuario guardarUsuario(Usuario usuario) {
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        if (usuario.getRol() == null) {
            usuario.setRol(Rol.RESIDENTE);
        }

        return usuarioRepository.save(usuario);
    }

    //  ACTUALIZAR USUARIO
    public Usuario guardarUsuarioActualizado(Usuario usuario) {

        if (usuario == null) {
            return null;
        }

        if (usuario.getId() == null) {
            return guardarUsuario(usuario);
        }

        Usuario existente = usuarioRepository.findById(usuario.getId()).orElse(usuario);

        if (StringUtils.hasText(usuario.getPassword())) {
            existente.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }

        if (StringUtils.hasText(usuario.getEmail())) {
            existente.setEmail(usuario.getEmail());
        }

        if (StringUtils.hasText(usuario.getNombre())) {
            existente.setNombre(usuario.getNombre());
        }

        if (usuario.getRol() != null) {
            existente.setRol(usuario.getRol());
        }

        return usuarioRepository.save(existente);
    }

    //  OTROS MÉTODOS

    public boolean existePorEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    public void eliminarUsuario(Long id) {
        usuarioRepository.deleteById(id);
    }
}