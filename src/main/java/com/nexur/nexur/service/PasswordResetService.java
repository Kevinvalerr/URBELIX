package com.nexur.nexur.service;

import com.nexur.nexur.model.PasswordResetToken;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UsuarioService usuarioService;
    private final PasswordResetTokenRepository tokenRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final long expirationMinutes;
    private final String applicationBaseUrl;
    private final EmailTemplateService emailTemplateService;
    private final CorreoNotificacionService correoNotificacionService;

    @Autowired
    public PasswordResetService(UsuarioService usuarioService,
                                PasswordResetTokenRepository tokenRepository,
                                ObjectProvider<JavaMailSender> mailSenderProvider,
                                @Value("${app.password-reset.expiration-minutes:30}") long expirationMinutes,
                                @Value("${app.base-url:http://localhost:8080}") String applicationBaseUrl,
                                EmailTemplateService emailTemplateService,
                                CorreoNotificacionService correoNotificacionService) {
        this.usuarioService = usuarioService;
        this.tokenRepository = tokenRepository;
        this.mailSenderProvider = mailSenderProvider;
        this.expirationMinutes = expirationMinutes;
        this.applicationBaseUrl = applicationBaseUrl;
        this.emailTemplateService = emailTemplateService;
        this.correoNotificacionService = correoNotificacionService;
    }

    public PasswordResetService(UsuarioService usuarioService,
                                PasswordResetTokenRepository tokenRepository,
                                ObjectProvider<JavaMailSender> mailSenderProvider,
                                long expirationMinutes, String applicationBaseUrl) {
        this(usuarioService, tokenRepository, mailSenderProvider, expirationMinutes,
                applicationBaseUrl, new EmailTemplateService(), null);
    }

    @Transactional
    public void solicitar(String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }

        Usuario usuario = usuarioService.buscarPorEmail(email.trim().toLowerCase(Locale.ROOT));
        if (usuario == null) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("El servicio de correo no está configurado. Define MAIL_HOST, MAIL_USERNAME y MAIL_PASSWORD.");
        }

        String token = UUID.randomUUID().toString();
        String link = applicationBaseUrl + "/reset-password?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);

        // La relación con el usuario es única; forzamos el borrado antes de insertar
        // el nuevo token para que una segunda solicitud no choque con esa restricción.
        tokenRepository.deleteByUsuarioId(usuario.getId());
        tokenRepository.flush();
        tokenRepository.save(new PasswordResetToken(
                token, usuario, LocalDateTime.now().plusMinutes(expirationMinutes)));
        tokenRepository.flush();

        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(usuario.getEmail());
            helper.setSubject("Restablecer contraseña de Urbelix");
            Map<String, Object> variables = new HashMap<>();
            variables.put("titulo", "Restablece tu contraseña");
            variables.put("preheader", "Recibimos una solicitud para actualizar el acceso a tu cuenta.");
            variables.put("mensaje", "Usa el siguiente botón para crear una nueva contraseña segura.");
            variables.put("enlace", link);
            variables.put("textoEnlace", "Restablecer contraseña");
            variables.put("expiracion", "Este enlace estará disponible durante " + expirationMinutes + " minutos.");
            helper.setText("Solicitaste restablecer tu contraseña. Enlace: " + link,
                emailTemplateService.render("email/recuperacion-contrasena", variables));
            mailSender.send(message);
        } catch (Exception exception) {
            log.error("No se pudo enviar el correo de recuperación a {}", usuario.getEmail(), exception);
            throw new IllegalStateException("No se pudo enviar el correo de recuperación. Revisa la configuración SMTP.");
        }

    }

    @Transactional(readOnly = true)
    public boolean tokenValido(String token) {
        return StringUtils.hasText(token)
                && tokenRepository.findByToken(token).filter(t -> !t.isExpired()).isPresent();
    }

    @Transactional
    public void restablecer(String token, String password, String confirmPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("El enlace de recuperación no es válido"));
        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("El enlace de recuperación ha expirado");
        }
        if (!StringUtils.hasText(password) || !password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        usuarioService.cambiarPassword(resetToken.getUsuario(), password);
        if (correoNotificacionService != null) {
            correoNotificacionService.enviarCambioContrasena(resetToken.getUsuario());
        }
        tokenRepository.delete(resetToken);
    }
}
