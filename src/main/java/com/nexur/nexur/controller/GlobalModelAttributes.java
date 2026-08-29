package com.nexur.nexur.controller;

import com.nexur.nexur.service.NotificacionService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;

@ControllerAdvice
public class GlobalModelAttributes {

    private final NotificacionService notificacionService;

    public GlobalModelAttributes(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @ModelAttribute
    public void agregarContadorNotificaciones(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("notificacionesNoLeidas",
                    notificacionService.contarNoLeidas(authentication.getName()));
        }
    }
}
