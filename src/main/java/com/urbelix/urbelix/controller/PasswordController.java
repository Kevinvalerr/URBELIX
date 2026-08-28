package com.urbelix.urbelix.controller;

import com.urbelix.urbelix.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class PasswordController {

    private final UsuarioService usuarioService;

    public PasswordController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/cuenta/cambiar-password")
    public String formulario(Model model) {
        model.addAttribute("titulo", "Cambiar contraseña");
        return "auth/cambiar-password";
    }

    @PostMapping("/cuenta/cambiar-password")
    public String cambiar(@RequestParam String passwordActual,
                          @RequestParam String passwordNueva,
                          @RequestParam String confirmarPassword,
                          Principal principal,
                          RedirectAttributes redirectAttributes) {
        if (!passwordNueva.equals(confirmarPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas nuevas no coinciden");
            return "redirect:/cuenta/cambiar-password";
        }
        try {
            usuarioService.cambiarPassword(principal.getName(), passwordActual, passwordNueva);
            return "redirect:/login?passwordChanged=true";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cuenta/cambiar-password";
        }
    }
}