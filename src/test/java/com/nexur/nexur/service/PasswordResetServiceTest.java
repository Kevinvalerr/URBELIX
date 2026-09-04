package com.nexur.nexur.service;

import com.nexur.nexur.model.PasswordResetToken;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UsuarioService usuarioService;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;

    @Test
    void generaTokenYEnviaEnlaceParaCorreoExistente() {
        Usuario usuario = usuario("ana@example.com");
        when(usuarioService.buscarPorEmail("ana@example.com")).thenReturn(usuario);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(
                new MimeMessage(Session.getInstance(new Properties())));

        PasswordResetService service = service();
        service.solicitar(" Ana@Example.com ");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).deleteByUsuarioId(7L);
        verify(tokenRepository).save(tokenCaptor.capture());
        verify(tokenRepository, org.mockito.Mockito.times(2)).flush();
        verify(mailSender).send(any(MimeMessage.class));

        PasswordResetToken token = tokenCaptor.getValue();
        assertNotNull(token.getToken());
        assertEquals(usuario, token.getUsuario());
        assertTrue(token.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void reemplazaTokenAnteriorAntesDeInsertarElSiguiente() {
        Usuario usuario = usuario("ana@example.com");
        when(usuarioService.buscarPorEmail("ana@example.com")).thenReturn(usuario);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(
                new MimeMessage(Session.getInstance(new Properties())));

        PasswordResetService service = service();
        service.solicitar("ana@example.com");
        service.solicitar("ana@example.com");

        InOrder orden = inOrder(tokenRepository);
        orden.verify(tokenRepository).deleteByUsuarioId(7L);
        orden.verify(tokenRepository).flush();
        orden.verify(tokenRepository).save(any(PasswordResetToken.class));
        orden.verify(tokenRepository).flush();
        orden.verify(tokenRepository).deleteByUsuarioId(7L);
        orden.verify(tokenRepository).flush();
        orden.verify(tokenRepository).save(any(PasswordResetToken.class));
        orden.verify(tokenRepository).flush();
    }

    @Test
    void rechazaSolicitudSiNoHayCorreoConfigurado() {
        Usuario usuario = usuario("ana@example.com");
        when(usuarioService.buscarPorEmail("ana@example.com")).thenReturn(usuario);
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        PasswordResetService service = service();

        assertThrows(IllegalStateException.class, () -> service.solicitar("ana@example.com"));
        verify(tokenRepository, never()).deleteByUsuarioId(7L);
        verify(tokenRepository, never()).save(any(PasswordResetToken.class));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void restableceContrasenaYConsumeToken() {
        Usuario usuario = usuario("ana@example.com");
        PasswordResetToken token = new PasswordResetToken(
                "token-valido", usuario, LocalDateTime.now().plusMinutes(30));
        when(tokenRepository.findByToken("token-valido")).thenReturn(Optional.of(token));

        service().restablecer("token-valido", "Nueva123!", "Nueva123!");

        verify(usuarioService).cambiarPassword(usuario, "Nueva123!");
        verify(tokenRepository).delete(token);
    }

    @Test
    void rechazaTokenExpiradoYNoCambiaContrasena() {
        Usuario usuario = usuario("ana@example.com");
        PasswordResetToken token = new PasswordResetToken(
                "token-expirado", usuario, LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByToken("token-expirado")).thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class,
                () -> service().restablecer("token-expirado", "Nueva123!", "Nueva123!"));

        verify(tokenRepository).delete(token);
        verify(usuarioService, never()).cambiarPassword(any(), any());
    }

    @Test
    void rechazaConfirmacionDistintaYConservaToken() {
        Usuario usuario = usuario("ana@example.com");
        PasswordResetToken token = new PasswordResetToken(
                "token-debil", usuario, LocalDateTime.now().plusMinutes(30));
        when(tokenRepository.findByToken("token-debil")).thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class,
                () -> service().restablecer("token-debil", "Nueva123!", "Otra123!"));

        verify(usuarioService, never()).cambiarPassword(any(), any());
        verify(tokenRepository, never()).delete(token);
    }

    private PasswordResetService service() {
        return new PasswordResetService(usuarioService, tokenRepository, mailSenderProvider,
                30, "http://localhost:8080");
    }

    private Usuario usuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setEmail(email);
        return usuario;
    }
}
