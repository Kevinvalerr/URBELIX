package com.nexur.nexur.config;

import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Permite recuperar el acceso administrativo de forma deliberada y temporal.
 * Permanece desactivado si ADMIN_BOOTSTRAP_ENABLED no se establece en true.
 */
@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String email;
    private final String password;

    public AdminBootstrapRunner(UsuarioRepository usuarioRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.admin.bootstrap-enabled:false}") boolean enabled,
                                @Value("${app.admin.bootstrap-email:}") String email,
                                @Value("${app.admin.bootstrap-password:}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        String emailNormalizado = normalizarEmail(email);
        validarPassword(password);

        Usuario admin = usuarioRepository.findByEmail(emailNormalizado).orElseGet(Usuario::new);
        if (admin.getId() != null && admin.getRol() != null && admin.getRol() != Rol.ADMIN) {
            throw new IllegalStateException(
                    "La cuenta indicada para recuperacion no tiene rol ADMIN: " + emailNormalizado);
        }

        boolean creado = admin.getId() == null;
        if (creado) {
            admin.setNombre("Administrador");
            admin.setEmail(emailNormalizado);
        }
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRol(Rol.ADMIN);
        admin.setActivo(true);
        admin.setDebeCambiarPassword(true);
        usuarioRepository.save(admin);

        log.warn("Recuperacion administrativa aplicada para {}. La cuenta debe cambiar la clave al ingresar.",
                emailNormalizado);
        if (creado) {
            log.warn("Se creo una cuenta ADMIN de recuperacion; desactive ADMIN_BOOTSTRAP_ENABLED despues del uso.");
        }
    }

    private String normalizarEmail(String valor) {
        if (!StringUtils.hasText(valor) || !valor.trim().contains("@")) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_EMAIL debe contener un correo valido cuando la recuperacion esta activa");
        }
        return valor.trim().toLowerCase(Locale.ROOT);
    }

    private void validarPassword(String valor) {
        if (!StringUtils.hasText(valor) || !valor.matches(PASSWORD_PATTERN)) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_PASSWORD debe tener minimo 8 caracteres, mayuscula, minuscula, numero y simbolo");
        }
    }
}
