package com.nexur.nexur.controller;

import com.nexur.nexur.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute
    public void addCommonAttributes(Model model, Authentication authentication, Principal principal) {
        String usuarioNombre = "Usuario";
        String rolTexto = "Usuario";
        String rolCodigo = "USUARIO";

        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            usuarioNombre = usuario.getNombre() != null ? usuario.getNombre() : usuario.getUsername();
            if (usuario.getRol() != null) {
                rolCodigo = usuario.getRol().name();
                rolTexto = switch (usuario.getRol()) {
                    case ADMIN -> "Administrador";
                    case RESIDENTE -> "Residente";
                    case PORTERIA -> "Portería";
                    default -> usuario.getRol().name();
                };
            }
        } else if (principal != null) {
            usuarioNombre = principal.getName();
        }

        model.addAttribute("currentUser", usuarioNombre);
        model.addAttribute("currentRole", rolCodigo);
        model.addAttribute("currentRoleName", rolTexto);
    }
}
