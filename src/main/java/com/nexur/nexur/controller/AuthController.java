package com.nexur.nexur.controller;

import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.service.UsuarioService;
import com.nexur.nexur.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;
    private final PasswordResetService passwordResetService;

    public AuthController(UsuarioService usuarioService, PasswordResetService passwordResetService) {
        this.usuarioService = usuarioService;
        this.passwordResetService = passwordResetService;
    }

    @InitBinder("usuario")
    void normalizarCamposDeRegistro(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, "email", new StringTrimmerEditor(false));
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
        model.addAttribute("usuario", new Usuario());
        return "auth/register";
    }

    @PostMapping("/register")
    public String submitRegister(@Valid @ModelAttribute("usuario") Usuario usuario,
                                 BindingResult bindingResult,
                                 @RequestParam(defaultValue = "") String confirmPassword,
                                 @RequestParam(defaultValue = "") String documento,
                                 @RequestParam(defaultValue = "") String telefono,
                                 @RequestParam(defaultValue = "") String numeroApartamento,
                                 @RequestParam(defaultValue = "") String codigoRegistro,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!confirmPassword.equals(usuario.getPassword())) {
            bindingResult.rejectValue("password", "Match", "Las contraseñas no coinciden.");
        }
        if (usuarioService.existePorEmail(usuario.getEmail())) {
            bindingResult.rejectValue("email", "Duplicate", "El email ya está en uso");
        }
        if (bindingResult.hasErrors()) {
            prepararDatosRegistro(model, documento, telefono, numeroApartamento, codigoRegistro);
            return "auth/register";
        }

        try {
            usuarioService.crearUsuarioConResidente(
                    usuario.getNombre(), usuario.getEmail(), usuario.getPassword(),
                    documento, telefono, numeroApartamento, codigoRegistro);
        } catch (RuntimeException exception) {
            bindingResult.reject("register", exception.getMessage());
            prepararDatosRegistro(model, documento, telefono, numeroApartamento, codigoRegistro);
            return "auth/register";
        }

        return "redirect:/login?registered=true";
    }

    private void prepararDatosRegistro(Model model, String documento, String telefono,
                                      String numeroApartamento, String codigoRegistro) {
        model.addAttribute("documento", documento);
        model.addAttribute("telefono", telefono);
        model.addAttribute("numeroApartamento", numeroApartamento);
        model.addAttribute("codigoRegistro", codigoRegistro);
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
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.solicitar(email);
            redirectAttributes.addAttribute("sent", "true");
        } catch (IllegalStateException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return "auth/forgot-password";
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam(required = false, defaultValue = "") String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("tokenValido", passwordResetService.tokenValido(token));
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String submitResetPassword(@RequestParam(required = false, defaultValue = "") String token,
                                      @RequestParam(required = false, defaultValue = "") String password,
                                      @RequestParam(required = false, defaultValue = "") String confirmPassword,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.restablecer(token, password, confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Contraseña actualizada. Ya puedes iniciar sesión.");
            return "redirect:/login";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("token", token);
            model.addAttribute("tokenValido", passwordResetService.tokenValido(token));
            model.addAttribute("errorMessage", exception.getMessage());
            return "auth/reset-password";
        }
    }
}
