package com.nexur.nexur.service;

import com.nexur.nexur.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorreoNotificacionServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;

    @Test
    void enviaCorreoCuandoLasNotificacionesEstanActivas() {
        Usuario usuario = new Usuario();
        usuario.setEmail("residente@example.com");
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        CorreoNotificacionService service = new CorreoNotificacionService(
                mailSenderProvider, "http://localhost:8080", true);
        service.enviar(usuario, "Nueva incidencia", "Se actualizó tu solicitud", "/incidencias");

        verify(mailSender).send(argThat((SimpleMailMessage correo) ->
                "residente@example.com".equals(correo.getTo()[0])
                        && correo.getSubject().contains("Nueva incidencia")
                        && correo.getText().contains("http://localhost:8080/incidencias")));
    }
}
