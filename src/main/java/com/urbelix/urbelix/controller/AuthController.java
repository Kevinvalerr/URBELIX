package com.urbelix.urbelix.controller;

import com.urbelix.urbelix.model.Rol;
import com.urbelix.urbelix.model.Usuario;
import com.urbelix.urbelix.repository.UsuarioRepository;
import com.urbelix.urbelix.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    public AuthController(UsuarioService usuarioService, UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping({"/", "/home"})
    public String home(Principal principal) {
        if (principal != null) {
            return "redirect:/dashboard";
        }
        return "home";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String registered,
                        @RequestParam(required = false) String passwordChanged,
                        Model model) {
        if (error != null) {
            model.addAttribute("loginError", "Correo electrónico o contraseña inválidos.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Has cerrado sesión correctamente.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Registro exitoso. Inicia sesión ahora.");
        }
        if (passwordChanged != null) {
            model.addAttribute("successMessage", "Contraseña actualizada correctamente. Ahora puedes iniciar sesión con tu nueva contraseña.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(@RequestParam(required = false) String error,
                           Model model) {
        if (error != null) {
            model.addAttribute("formError", error);
        }
        model.addAttribute("usuario", new Usuario());
        return "auth/register";
    }

    @PostMapping("/register")
    public String submitRegister(RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("error", "Las cuentas de residentes se crean desde el módulo Residentes.");
        return "redirect:/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(@RequestParam(required = false) String sent,
                                 Model model) {
        if (sent != null) {
            model.addAttribute("successMessage", "Si el correo existe, recibirás un enlace de recuperación.");
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String submitForgotPassword(@RequestParam String email,
                                       RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("sent", "true");
        return "redirect:/forgot-password";
    }
}
