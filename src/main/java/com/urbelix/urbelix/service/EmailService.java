package com.urbelix.urbelix.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import com.urbelix.urbelix.model.Apartamento;
import com.urbelix.urbelix.model.enums.EstadoIncidencia;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailHost;
    private final String mailUsername;
    private final String portalUrl;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${spring.mail.host:}") String mailHost,
                        @Value("${spring.mail.username:}") String mailUsername,
                        @Value("${urbelix.portal.url:http://localhost:8080/login}") String portalUrl) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailHost = mailHost;
        this.mailUsername = mailUsername;
        this.portalUrl = portalUrl;
    }

    public void enviarCredencialesIniciales(String destinatario, String nombre, String passwordTemporal) {
        if (!StringUtils.hasText(destinatario)) {
            throw new IllegalArgumentException("El correo del residente es obligatorio");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!StringUtils.hasText(mailHost) || mailSender == null) {
            log.warn("Correo no configurado; credenciales iniciales pendientes para destinatario no registrado");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(StringUtils.hasText(mailUsername) ? mailUsername : destinatario);
            helper.setTo(destinatario);
            helper.setSubject("Credenciales iniciales de URBELIX");
            helper.setText(htmlCredenciales(nombre, destinatario, passwordTemporal), true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("No fue posible preparar el correo de credenciales", ex);
        }
    }

    public void enviarActualizacionIncidencia(String destinatario, String nombre, String titulo,
                                              Apartamento apartamento, EstadoIncidencia estado, String comentario) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!StringUtils.hasText(mailHost) || mailSender == null) {
            log.warn("Correo no configurado; notificación de incidencia no enviada");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(StringUtils.hasText(mailUsername) ? mailUsername : destinatario);
            helper.setTo(destinatario);
            helper.setSubject("Actualización de incidencia URBELIX");
            String ubicacion = apartamento == null ? "No especificado" : apartamento.getTorre() + " - " + apartamento.getNumero();
            helper.setText("<div style='font-family:Arial,sans-serif;color:#17324d;max-width:620px'>"
                    + "<h2 style='color:#163B65'>URBELIX</h2><p>Hola, " + escape(nombre) + ".</p>"
                    + "<p>Tu incidencia <strong>" + escape(titulo) + "</strong> fue actualizada.</p>"
                    + "<p><strong>Apartamento:</strong> " + escape(ubicacion) + "<br><strong>Estado:</strong> " + estado + "</p>"
                    + (StringUtils.hasText(comentario) ? "<p><strong>Observación:</strong><br>" + escape(comentario) + "</p>" : "")
                    + "<p>Administración Residencial - URBELIX</p></div>", true);
            mailSender.send(message);
        } catch (MessagingException | RuntimeException ex) {
            log.error("No fue posible enviar la notificación de incidencia", ex);
        }
    }

    private String htmlCredenciales(String nombre, String destinatario, String passwordTemporal) {
        return "<!doctype html><html lang=\"es\"><body style=\"margin:0;background:#eef6ff;font-family:Arial,sans-serif;color:#17324d;\">"
                + "<div style=\"max-width:620px;margin:0 auto;padding:32px 16px;\"><div style=\"background:#ffffff;border-radius:18px;overflow:hidden;box-shadow:0 12px 30px rgba(15,42,74,.12);\">"
                + "<div style=\"padding:28px 32px;background:linear-gradient(135deg,#1d4ed8,#0ea5e9);color:#ffffff;\"><div style=\"font-size:25px;font-weight:800;letter-spacing:1px;\">URBELIX</div><div style=\"margin-top:6px;font-size:14px;opacity:.9;\">Sistema de Gestión Residencial</div></div>"
                + "<div style=\"padding:32px;\"><p style=\"font-size:17px;margin-top:0;\">Hola, " + escape(nombre) + ".</p>"
                + "<p style=\"line-height:1.6;\">Tu cuenta de URBELIX ha sido creada correctamente. Con estas credenciales podrás acceder al portal residencial.</p>"
                + "<div style=\"margin:26px 0;padding:22px;border:1px solid #dbeafe;border-radius:12px;background:#f8fbff;\"><div style=\"font-size:12px;font-weight:800;letter-spacing:1px;color:#1d4ed8;\">CREDENCIALES DE ACCESO</div>"
                + "<p style=\"margin:18px 0 8px;\"><strong>Correo:</strong><br>" + escape(destinatario) + "</p>"
                + "<p style=\"margin:0;\"><strong>Contraseña temporal:</strong><br><span style=\"font-family:monospace;font-size:16px;color:#0f172a;\">" + escape(passwordTemporal) + "</span></p></div>"
                + "<div style=\"padding:16px;border-left:4px solid #f59e0b;background:#fff8e8;line-height:1.55;\"><strong>IMPORTANTE</strong><br>Por motivos de seguridad, debes cambiar tu contraseña temporal durante tu primer inicio de sesión.</div>"
                + "<p style=\"text-align:center;margin:28px 0;\"><a href=\"" + escape(portalUrl) + "\" style=\"display:inline-block;padding:13px 24px;border-radius:9px;background:#1d4ed8;color:#ffffff;text-decoration:none;font-weight:700;\">ACCEDER AL PORTAL</a></p>"
                + "<p style=\"margin-bottom:0;color:#64748b;font-size:13px;\">Administración Residencial - URBELIX</p></div></div></div></body></html>";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
