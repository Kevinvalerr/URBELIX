package com.urbelix.urbelix.config;

import com.urbelix.urbelix.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authProvider) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/", "/home", "/login", "/register", "/forgot-password").permitAll()
                .requestMatchers("/cuenta/cambiar-password").authenticated()
                .requestMatchers("/usuarios-vista", "/usuarios/**", "/guardar-usuario", "/eliminar-usuario", "/editar-usuario", "/actualizar-usuario").hasRole("ADMIN")
                .requestMatchers("/reportes/**").hasRole("ADMIN")
                .requestMatchers("/incidencias/nueva", "/incidencias/guardar").hasAnyRole("ADMIN", "RESIDENTE")
                .requestMatchers("/incidencias/**").hasAnyRole("ADMIN", "RESIDENTE", "PORTERIA")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/visitantes/**").hasAnyRole("ADMIN", "PORTERIA", "RESIDENTE")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler())
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
            .authenticationProvider(authProvider);

            http.addFilterAfter(new PasswordChangeRequiredFilter(),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            if (authentication.getPrincipal() instanceof com.urbelix.urbelix.model.Usuario usuario
                    && usuario.isDebeCambiarPassword()) {
                response.sendRedirect(request.getContextPath() + "/cuenta/cambiar-password");
            } else {
                response.sendRedirect(request.getContextPath() + "/dashboard");
            }
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }



}
