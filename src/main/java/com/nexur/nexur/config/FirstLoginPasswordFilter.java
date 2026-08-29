package com.nexur.nexur.config;

import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.UsuarioRepository;
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
 * Impide que una cuenta de primer ingreso use otros modulos antes de cambiar
 * la contraseña temporal asignada durante el alta.
 */
public class FirstLoginPasswordFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;

    public FirstLoginPasswordFilter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!requiereCambio(authentication) || rutaPermitidaDuranteCambio(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/perfil?cambiarPassword=true");
    }

    private boolean requiereCambio(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName()).orElse(null);
        return usuario != null && usuario.isDebeCambiarPassword();
    }

    private boolean rutaPermitidaDuranteCambio(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/perfil")
                || path.startsWith("/perfil/")
                || path.equals("/login")
                || path.equals("/logout")
                || path.equals("/acceso-denegado")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/");
    }
}
