package com.nexur.nexur.config;

import com.nexur.nexur.repository.UsuarioRepository;
import com.nexur.nexur.service.AuditoriaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public SecurityConfig(UsuarioRepository usuarioRepository, AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthenticationSuccessHandler successHandler) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/webhooks/pagos", "/webhooks/wompi"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/", "/home", "/login", "/register", "/forgot-password", "/reset-password").permitAll()
                .requestMatchers("/webhooks/pagos").permitAll()
                .requestMatchers("/webhooks/wompi").permitAll()
                .requestMatchers("/usuarios-vista", "/usuarios/**", "/guardar-usuario", "/eliminar-usuario", "/editar-usuario", "/actualizar-usuario").hasRole("ADMIN")
                .requestMatchers("/reportes/**").hasRole("ADMIN")
                .requestMatchers("/porteria/**").hasRole("PORTERIA")
                .requestMatchers("/usuarios/excel/residentes", "/pagos/excel").hasRole("ADMIN")
                .requestMatchers("/pagos/**").hasAnyRole("ADMIN", "RESIDENTE")
                .requestMatchers("/reservas/**").hasAnyRole("ADMIN", "RESIDENTE")
                .requestMatchers("/avisos/**").hasAnyRole("ADMIN", "RESIDENTE", "PORTERIA")
                .requestMatchers("/notificaciones/**").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/parqueaderos/nuevo", "/parqueaderos/editar/**",
                        "/parqueaderos/guardar", "/parqueaderos/eliminar/**").hasRole("ADMIN")
                .requestMatchers("/parqueaderos/mis-vehiculos/**").hasRole("RESIDENTE")
                .requestMatchers("/parqueaderos/**").hasAnyRole("ADMIN", "RESIDENTE")
                .requestMatchers("/incidencias/nueva").hasRole("RESIDENTE")
                .requestMatchers("/incidencias/**").hasAnyRole("ADMIN", "RESIDENTE")
                .requestMatchers("/visitantes/**").hasAnyRole("PORTERIA", "RESIDENTE")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/acceso-denegado")
            );

        http.addFilterBefore(new FirstLoginPasswordFilter(usuarioRepository),
                org.springframework.security.web.access.intercept.AuthorizationFilter.class);
        http.addFilterAfter(new AuditoriaRequestFilter(auditoriaService),
                org.springframework.security.web.access.intercept.AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler(UsuarioRepository usuarioRepository) {
        return (request, response, authentication) -> {
            var usuario = usuarioRepository.findByEmail(authentication.getName()).orElse(null);
            if (usuario != null && usuario.isDebeCambiarPassword()) {
                response.sendRedirect("/perfil?cambiarPassword=true");
                return;
            }
            response.sendRedirect("/dashboard");
        };
    }



}
