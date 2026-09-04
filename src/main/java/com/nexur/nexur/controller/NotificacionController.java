package com.nexur.nexur.controller;

import com.nexur.nexur.service.NotificacionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public String listar(Authentication authentication, Model model) {
        model.addAttribute("notificaciones", notificacionService.listar(authentication.getName()));
        model.addAttribute("noLeidas", notificacionService.contarNoLeidas(authentication.getName()));
        model.addAttribute("currentPath", "/notificaciones");
        model.addAttribute("titulo", "Notificaciones | Urbelix");
        model.addAttribute("volverUrl", "/dashboard");
        return "notificaciones/lista";
    }

    @PostMapping("/{id}/leer")
    public String marcarLeida(@PathVariable Long id, Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            notificacionService.marcarLeida(id, authentication.getName());
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/notificaciones";
    }

    @PostMapping("/todas/leer")
    public String marcarTodasLeidas(Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        int actualizadas = notificacionService.marcarTodasLeidas(authentication.getName());
        redirectAttributes.addFlashAttribute("success",
                actualizadas == 0 ? "No había notificaciones pendientes." :
                        actualizadas + " notificación(es) marcada(s) como leída(s).");
        return "redirect:/notificaciones";
    }
}
