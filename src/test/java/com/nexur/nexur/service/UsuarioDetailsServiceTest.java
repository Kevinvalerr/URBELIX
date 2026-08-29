package com.nexur.nexur.service;

import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock private UsuarioRepository usuarioRepository;

    @Test
    void buscaElCorreoNormalizadoParaPermitirLoginSinProblemasDeMayusculas() {
        Usuario usuario = new Usuario();
        usuario.setEmail("admin@example.com");
        when(usuarioRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(usuario));

        UsuarioDetailsService service = new UsuarioDetailsService(usuarioRepository);

        assertSame(usuario, service.loadUserByUsername(" Admin@Example.COM "));
        verify(usuarioRepository).findByEmail("admin@example.com");
    }

    @Test
    void rechazaCorreoNoRegistrado() {
        when(usuarioRepository.findByEmail("no@example.com")).thenReturn(Optional.empty());

        UsuarioDetailsService service = new UsuarioDetailsService(usuarioRepository);

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("no@example.com"));
    }
}
