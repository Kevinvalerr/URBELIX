package com.nexur.nexur.config;

import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void permaneceInactivoPorDefecto() throws Exception {
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                usuarioRepository, passwordEncoder, false, "", "");

        runner.run();

        verify(usuarioRepository, never()).findByEmail(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizaAdminYObligaCambioDeClave() throws Exception {
        Usuario admin = new Usuario();
        admin.setId(4L);
        admin.setEmail("admin@nexur.com");
        admin.setRol(Rol.ADMIN);
        admin.setActivo(false);
        admin.setDebeCambiarPassword(false);
        when(usuarioRepository.findByEmail("admin@nexur.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("AdminTemporal123!")).thenReturn("hash");

        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                usuarioRepository, passwordEncoder, true,
                " ADMIN@NEXUR.COM ", "AdminTemporal123!");

        runner.run();

        verify(usuarioRepository).save(admin);
        org.junit.jupiter.api.Assertions.assertEquals("hash", admin.getPassword());
        org.junit.jupiter.api.Assertions.assertTrue(admin.isActivo());
        org.junit.jupiter.api.Assertions.assertTrue(admin.isDebeCambiarPassword());
        org.junit.jupiter.api.Assertions.assertEquals(Rol.ADMIN, admin.getRol());
    }

    @Test
    void rechazaUsarCuentaExistenteConOtroRol() {
        Usuario residente = new Usuario();
        residente.setId(8L);
        residente.setEmail("persona@nexur.com");
        residente.setRol(Rol.RESIDENTE);
        when(usuarioRepository.findByEmail("persona@nexur.com")).thenReturn(Optional.of(residente));

        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                usuarioRepository, passwordEncoder, true,
                "persona@nexur.com", "AdminTemporal123!");

        assertThrows(IllegalStateException.class, runner::run);
        verify(usuarioRepository, never()).save(any());
    }
}
