package com.nexur.nexur.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario guardarUsuario(Usuario usuario) {
        // Encriptamos la contraseña antes de guardar
        usuario.setPassword(
            passwordEncoder.encode(usuario.getPassword())
        );

        if (usuario.getRol() == null) {
            usuario.setRol(Rol.RESIDENTE);
        }
        return usuarioRepository.save(usuario);
    }

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