package com.nexur.nexur.service;

import com.nexur.nexur.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CorreoNotificacionService {

    private static final Logger log = LoggerFactory.getLogger(CorreoNotificacionService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String applicationBaseUrl;
    private final boolean habilitado;

    public CorreoNotificacionService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                     @Value("${app.base-url:http://localhost:8080}") String applicationBaseUrl,
                                     @Value("${app.notifications.email-enabled:false}") boolean habilitado) {
        this.mailSenderProvider = mailSenderProvider;
        this.applicationBaseUrl = applicationBaseUrl;
        this.habilitado = habilitado;
    }

    public void enviar(Usuario usuario, String titulo, String mensaje, String enlace) {
        if (!habilitado || usuario == null || !StringUtils.hasText(usuario.getEmail())) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Las notificaciones por correo están activas, pero no existe un JavaMailSender configurado");
            return;
        }

        SimpleMailMessage correo = new SimpleMailMessage();
        correo.setTo(usuario.getEmail());
        correo.setSubject(titulo + " | Urbelix");
        correo.setText(mensaje + enlaceTexto(enlace));
        try {
            mailSender.send(correo);
        } catch (RuntimeException exception) {
            // La notificacion interna ya fue persistida; el correo no debe revertirla.
            log.error("No se pudo enviar una notificacion por correo a {}", usuario.getEmail(), exception);
        }
    }

    public void enviarCredencialesIniciales(Usuario usuario, String passwordTemporal) {
        if (usuario == null || !StringUtils.hasText(passwordTemporal)) {
            return;
        }
        enviar(usuario,
                "Cuenta inicial creada en Urbelix",
                "Tu cuenta de residente fue creada. Contraseña temporal: " + passwordTemporal
                        + "\n\nPor seguridad, cambia esta contraseña en tu primer ingreso.",
                "/login");
    }

    private String enlaceTexto(String enlace) {
        if (!StringUtils.hasText(enlace)) {
            return "";
        }
        String url = enlace.startsWith("http") ? enlace : applicationBaseUrl + enlace;
        return "\n\nConsulta el detalle en Urbelix: " + url;
    }
}
