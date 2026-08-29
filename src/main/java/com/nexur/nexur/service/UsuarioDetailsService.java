package com.nexur.nexur.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.nexur.nexur.repository.UsuarioRepository;
import java.util.Locale;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        String emailNormalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario no encontrado"));
    }     
}
