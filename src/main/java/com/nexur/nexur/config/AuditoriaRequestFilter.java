package com.nexur.nexur.config;

import com.nexur.nexur.service.AuditoriaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Registra mutaciones autorizadas sin conservar datos sensibles de la peticion.
 */
public class AuditoriaRequestFilter extends OncePerRequestFilter {

    private final AuditoriaService auditoriaService;

    public AuditoriaRequestFilter(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!esMutacionAuditable(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            String actor = actorActual();
            String ruta = request.getRequestURI().substring(request.getContextPath().length());
            try {
                auditoriaService.registrar(actor, request.getMethod(), ruta, null,
                        "status=" + response.getStatus());
            } catch (RuntimeException ignored) {
                // La bitacora no debe convertir una operacion ya ejecutada en un error.
            }
        }
    }

    private boolean esMutacionAuditable(HttpServletRequest request) {
        String metodo = request.getMethod();
        if (!("POST".equals(metodo) || "PUT".equals(metodo)
                || "PATCH".equals(metodo) || "DELETE".equals(metodo))) {
            return false;
        }
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return !"/login".equals(ruta) && !"/logout".equals(ruta);
    }

    private String actorActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "SISTEMA";
        }
        return authentication.getName();
    }
}
