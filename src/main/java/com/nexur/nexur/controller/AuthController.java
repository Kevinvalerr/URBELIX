package com.nexur.nexur.controller;

import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.UsuarioRepository;
import com.nexur.nexur.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    public AuthController(UsuarioService usuarioService, UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String registered,
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
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(@RequestParam(required = false) String error,
                           Model model) {
        if (error != null) {
            model.addAttribute("formError", error);
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String submitRegister(@RequestParam String nombre,
                                 @RequestParam String email,
                                 @RequestParam String password,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addAttribute("error", "Las contraseñas no coinciden.");
            return "redirect:/register";
        }

        if (usuarioRepository.existsByEmail(email)) {
            redirectAttributes.addAttribute("error", "Ya existe una cuenta registrada con ese correo.");
            return "redirect:/register";
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPassword(password);
        usuario.setRol(Rol.RESIDENTE);
        usuarioService.guardarUsuario(usuario);

        return "redirect:/login?registered=true";
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
