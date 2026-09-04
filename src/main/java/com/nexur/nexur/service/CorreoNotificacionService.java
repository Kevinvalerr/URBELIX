package com.nexur.nexur.service;

import com.nexur.nexur.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class CorreoNotificacionService {

    private static final Logger log = LoggerFactory.getLogger(CorreoNotificacionService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String applicationBaseUrl;
    private final boolean habilitado;
    private final EmailTemplateService emailTemplateService;

    @Autowired
    public CorreoNotificacionService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                     @Value("${app.base-url:http://localhost:8080}") String applicationBaseUrl,
                                     @Value("${app.notifications.email-enabled:false}") boolean habilitado,
                                     EmailTemplateService emailTemplateService) {
        this.mailSenderProvider = mailSenderProvider;
        this.applicationBaseUrl = applicationBaseUrl;
        this.habilitado = habilitado;
        this.emailTemplateService = emailTemplateService;
    }

    public CorreoNotificacionService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                     String applicationBaseUrl, boolean habilitado) {
        this(mailSenderProvider, applicationBaseUrl, habilitado, new EmailTemplateService());
    }

    public void enviar(Usuario usuario, String titulo, String mensaje, String enlace) {
        enviarConPlantilla(usuario, titulo, "email/notificacion", mensaje, enlace,
                null, null);
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

    public void enviarBienvenida(Usuario usuario) {
        enviarConPlantilla(usuario, "Bienvenido a Urbelix", "email/bienvenida-usuario",
                "Tu cuenta ya está lista para ayudarte a vivir mejor tu comunidad.", "/login", null, null);
    }

    public void enviarLlegadaVisitante(Usuario usuario, String visitante, String apartamento) {
        enviarConPlantilla(usuario, "Llegada de visitante", "email/llegada-visitante",
                "Se registró el ingreso de un visitante autorizado.", "/visitantes",
                "Visitante", visitante + " | Apto. " + apartamento);
    }

    public void enviarConfirmacionPago(Usuario usuario, String referencia, String monto) {
        enviarConPlantilla(usuario, "Pago confirmado", "email/confirmacion-pago",
                "Tu pago fue registrado correctamente en Urbelix.", "/pagos",
                "Referencia", referencia + " | " + monto);
    }

    public void enviarCambioContrasena(Usuario usuario) {
        enviarConPlantilla(usuario, "Contraseña actualizada", "email/cambio-contrasena",
                "La contraseña de tu cuenta fue actualizada correctamente.", "/perfil", null, null);
    }

    private void enviarConPlantilla(Usuario usuario, String titulo, String plantilla, String mensaje,
                                     String enlace, String detalleEtiqueta, String detalle) {
        String email = usuario == null ? null : usuario.getEmail();
        if (!habilitado || !StringUtils.hasText(email)) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Las notificaciones por correo están activas, pero no existe un JavaMailSender configurado");
            return;
        }
        try {
            var correo = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(correo, true, StandardCharsets.UTF_8.name());
            helper.setTo(email);
            helper.setSubject(titulo + " | Urbelix");
            Map<String, Object> variables = new HashMap<>();
            variables.put("titulo", titulo);
            variables.put("mensaje", mensaje);
            variables.put("enlace", url(enlace));
            variables.put("textoEnlace", "Ver en Urbelix");
            variables.put("detalleEtiqueta", detalleEtiqueta);
            variables.put("detalle", detalle);
            helper.setText(mensaje + enlaceTexto(enlace), emailTemplateService.render(plantilla, variables));
            mailSender.send(correo);
        } catch (Exception exception) {
            log.error("No se pudo enviar una notificación por correo a {}", email, exception);
        }
    }

    private String enlaceTexto(String enlace) {
        if (!StringUtils.hasText(enlace)) {
            return "";
        }
        return "\n\nConsulta el detalle en Urbelix: " + url(enlace);
    }

    private String url(String enlace) {
        return StringUtils.hasText(enlace) && enlace.startsWith("http")
                ? enlace : (StringUtils.hasText(enlace) ? applicationBaseUrl + enlace : "");
    }
}
