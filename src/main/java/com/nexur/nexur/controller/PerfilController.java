package com.nexur.nexur.controller;

import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.service.ResidenteService;
import com.nexur.nexur.service.UsuarioService;
import com.nexur.nexur.service.CorreoNotificacionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;
    private final ResidenteService residenteService;
    private final CorreoNotificacionService correoNotificacionService;

    public PerfilController(UsuarioService usuarioService, ResidenteService residenteService,
                             CorreoNotificacionService correoNotificacionService) {
        this.usuarioService = usuarioService;
        this.residenteService = residenteService;
        this.correoNotificacionService = correoNotificacionService;
    }

    @GetMapping
    public String verPerfil(Authentication authentication,
                            @RequestParam(required = false) boolean cambiarPassword,
                            Model model) {
        Usuario usuario = usuarioService.buscarPorEmail(authentication.getName());
        if (usuario == null) {
            return "redirect:/login?error=true";
        }
        Residente residente = null;
        try {
            residente = residenteService.buscarPorUsuarioEmail(authentication.getName());
        } catch (RuntimeException ignored) {
            // Las cuentas administrativas no tienen perfil de residente.
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("residente", residente);
        model.addAttribute("mostrarCambioPassword", cambiarPassword || usuario.isDebeCambiarPassword());
        model.addAttribute("currentPath", "/perfil");
        model.addAttribute("titulo", "Mi perfil | Urbelix");
        model.addAttribute("volverUrl", "/dashboard");
        return "perfil/ver";
    }

    @PostMapping("/password")
    public String actualizarPassword(Authentication authentication,
                                     @RequestParam String password,
                                     @RequestParam String confirmPassword,
                                     RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/perfil?cambiarPassword=true";
        }
        try {
            Usuario usuario = usuarioService.buscarPorEmail(authentication.getName());
            usuarioService.cambiarPassword(usuario, password);
            correoNotificacionService.enviarCambioContrasena(usuario);
            redirectAttributes.addFlashAttribute("success", "Contraseña actualizada correctamente");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/perfil?cambiarPassword=true";
        }
        return "redirect:/perfil";
    }

    @PostMapping
    public String actualizar(Authentication authentication,
                             @RequestParam String nombre,
                             @RequestParam(required = false) String telefono,
                             RedirectAttributes redirectAttributes) {
        try {
            usuarioService.actualizarPerfilPropio(authentication.getName(), nombre, telefono);
            redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/perfil";
    }
}
