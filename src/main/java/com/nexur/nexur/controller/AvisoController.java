package com.nexur.nexur.controller;

import com.nexur.nexur.model.Aviso;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.UsuarioRepository;
import com.nexur.nexur.service.AvisoService;
import com.nexur.nexur.service.NotificacionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/avisos")
public class AvisoController {

    private final AvisoService avisoService;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public AvisoController(AvisoService avisoService, UsuarioRepository usuarioRepository,
                           NotificacionService notificacionService) {
        this.avisoService = avisoService;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public String listar(Model model, Authentication authentication) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("avisos", esAdmin ? avisoService.listarParaAdministracion() : avisoService.listarVisibles());
        model.addAttribute("esAdmin", esAdmin);
        model.addAttribute("aviso", new Aviso());
        model.addAttribute("currentPath", "/avisos");
        model.addAttribute("titulo", "Avisos | Urbelix");
        model.addAttribute("volverUrl", "/dashboard");
        return "avisos/lista";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String publicar(@Valid @ModelAttribute("aviso") Aviso aviso,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepararError(model);
            return "avisos/lista";
        }
        try {
            Aviso publicado = avisoService.publicar(aviso);
            usuarioRepository.findAll().stream()
                    .filter(Usuario::isActivo)
                    .forEach(usuario -> notificar(usuario, publicado));
            redirectAttributes.addFlashAttribute("success", "Aviso publicado correctamente");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/avisos";
    }

    @PostMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam boolean activo,
                                RedirectAttributes redirectAttributes) {
        try {
            avisoService.cambiarEstado(id, activo);
            redirectAttributes.addFlashAttribute("success", activo ? "Aviso reactivado" : "Aviso desactivado");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/avisos";
    }

    private void prepararError(Model model) {
        model.addAttribute("avisos", avisoService.listarParaAdministracion());
        model.addAttribute("esAdmin", true);
        model.addAttribute("currentPath", "/avisos");
        model.addAttribute("titulo", "Avisos | Urbelix");
        model.addAttribute("volverUrl", "/dashboard");
    }

    private void notificar(Usuario usuario, Aviso aviso) {
        try {
            notificacionService.crear(usuario, "Nuevo aviso de administración",
                    aviso.getTitulo(), "/avisos");
        } catch (RuntimeException exception) {
            // Publicar el aviso no debe fallar por un problema temporal de notificaciones.
        }
    }
}
