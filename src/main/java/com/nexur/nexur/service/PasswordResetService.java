package com.nexur.nexur.service;

import com.nexur.nexur.model.PasswordResetToken;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UsuarioService usuarioService;
    private final PasswordResetTokenRepository tokenRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final long expirationMinutes;
    private final String applicationBaseUrl;

    public PasswordResetService(UsuarioService usuarioService,
                                PasswordResetTokenRepository tokenRepository,
                                ObjectProvider<JavaMailSender> mailSenderProvider,
                                @Value("${app.password-reset.expiration-minutes:30}") long expirationMinutes,
                                @Value("${app.base-url:http://localhost:8080}") String applicationBaseUrl) {
        this.usuarioService = usuarioService;
        this.tokenRepository = tokenRepository;
        this.mailSenderProvider = mailSenderProvider;
        this.expirationMinutes = expirationMinutes;
        this.applicationBaseUrl = applicationBaseUrl;
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(usuario.getEmail());
        message.setSubject("Restablecer contraseña de Urbelix");
        message.setText("Solicitaste restablecer tu contraseña. Usa este enlace antes de "
                + expirationMinutes + " minutos:\n\n" + link);
        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
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
        tokenRepository.delete(resetToken);
    }
}
