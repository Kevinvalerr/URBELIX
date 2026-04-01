package com.nexur.nexur.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.Rol;
import com.nexur.nexur.repository.UsuarioRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsuarios(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {

            if (usuarioRepository.findByEmail("admin@nexur.com").isEmpty()) {

                Usuario admin = new Usuario();
                admin.setNombre("Administrador");
                admin.setEmail("admin@nexur.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRol(Rol.ADMIN);

                usuarioRepository.save(admin);

                System.out.println("Usuario admin creado");
            }

        };
    }
}