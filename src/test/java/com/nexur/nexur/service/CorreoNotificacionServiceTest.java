package com.nexur.nexur.service;

import com.nexur.nexur.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CorreoNotificacionServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;

    @Test
    void enviaCorreoHtmlCuandoLasNotificacionesEstanActivas() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("residente@example.com");
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(
                new MimeMessage(Session.getInstance(new Properties())));

        CorreoNotificacionService service = new CorreoNotificacionService(
                mailSenderProvider, "http://localhost:8080", true);
        service.enviar(usuario, "Nueva incidencia", "Se actualizó tu solicitud", "/incidencias");

        var captor = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage correo = captor.getValue();
        assertEquals("residente@example.com", correo.getAllRecipients()[0].toString());
        assertTrue(correo.getSubject().contains("Nueva incidencia"));
        var contenido = (jakarta.mail.Multipart) correo.getContent();
        assertTrue(contenido.getCount() > 0);
    }
}
