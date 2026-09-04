package com.urbelix.urbelix.config;

import com.urbelix.urbelix.model.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean cambioPermitido = path.equals("/cuenta/cambiar-password")
                || path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.equals("/logout")
                || path.equals("/login");

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Usuario usuario
                && usuario.isDebeCambiarPassword() && !cambioPermitido) {
            response.sendRedirect(request.getContextPath() + "/cuenta/cambiar-password");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
