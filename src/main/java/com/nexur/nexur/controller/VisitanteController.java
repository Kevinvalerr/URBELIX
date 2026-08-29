package com.nexur.nexur.controller;

import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Visitante;
import com.nexur.nexur.service.ResidenteService;
import com.nexur.nexur.service.VisitanteService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/visitantes")
public class VisitanteController {

    private final VisitanteService visitanteService;
    private final ResidenteService residenteService;

    public VisitanteController(VisitanteService visitanteService,
                               ResidenteService residenteService) {
        this.visitanteService = visitanteService;
        this.residenteService = residenteService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PORTERIA', 'RESIDENTE')")
    public String listarVisitantes(Model model, Authentication authentication) {
        boolean esResidente = tieneRol(authentication, "RESIDENTE");
        List<Visitante> visitantes;
        if (esResidente) {
            try {
                Residente residente = residenteService.buscarPorUsuarioEmail(authentication.getName());
                Long apartamentoId = residente.getApartamento() == null
                        ? null : residente.getApartamento().getId();
                visitantes = apartamentoId == null
                        ? List.of() : visitanteService.buscarPorApartamento(apartamentoId);
            } catch (RuntimeException exception) {
                visitantes = List.of();
            }
        } else {
            visitantes = visitanteService.listarVisitantes();
        }
        model.addAttribute("visitantes", visitantes);
        model.addAttribute("esResidente", esResidente);
        model.addAttribute("currentPath", "/visitantes");
        model.addAttribute("volverUrl", "/dashboard");
        model.addAttribute("titulo", esResidente
                ? "Mis solicitudes de visitantes | Urbelix"
                : "Control de visitantes | Urbelix");
        return "visitantes/listaVisitantes";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('RESIDENTE')")
    public String mostrarFormulario(Model model, Authentication authentication) {
        try {
            Residente residente = residenteService.buscarPorUsuarioEmail(authentication.getName());
            if (residente.getApartamento() == null || residente.getApartamento().getId() == null) {
                return "redirect:/dashboard";
            }
            model.addAttribute("apartamento", residente.getApartamento());
        } catch (RuntimeException exception) {
            return "redirect:/dashboard";
        }
        model.addAttribute("visitante", new Visitante());
        model.addAttribute("currentPath", "/visitantes");
        model.addAttribute("volverUrl", "/visitantes");
        model.addAttribute("titulo", "Solicitar acceso de visitante | Urbelix");
        return "visitantes/formularioVisitante";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasRole('RESIDENTE')")
    public String solicitarVisitante(@Valid @ModelAttribute("visitante") Visitante visitante,
                                     BindingResult bindingResult,
                                     Authentication authentication,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepararFormulario(model, authentication);
            return "visitantes/formularioVisitante";
        }
        try {
            visitanteService.solicitar(visitante, authentication.getName());
            redirectAttributes.addFlashAttribute("success",
                    "Solicitud enviada. Portería debe aprobarla antes del ingreso.");
            return "redirect:/visitantes";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            prepararFormulario(model, authentication);
            return "visitantes/formularioVisitante";
        }
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('PORTERIA')")
    public String aprobarSolicitud(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            visitanteService.aprobarSolicitud(id);
            redirectAttributes.addFlashAttribute("success", "Solicitud aprobada");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/visitantes";
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('PORTERIA')")
    public String rechazarSolicitud(@PathVariable Long id,
                                    @RequestParam(required = false) String motivo,
                                    RedirectAttributes redirectAttributes) {
        try {
            visitanteService.rechazarSolicitud(id, motivo);
            redirectAttributes.addFlashAttribute("success", "Solicitud rechazada");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/visitantes";
    }

    @PostMapping("/{id}/entrada")
    @PreAuthorize("hasRole('PORTERIA')")
    public String registrarEntrada(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            visitanteService.registrarEntrada(id);
            redirectAttributes.addFlashAttribute("success", "Entrada registrada correctamente");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/visitantes";
    }

    @PostMapping("/salida/{id}")
    @PreAuthorize("hasRole('PORTERIA')")
    public String registrarSalida(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            visitanteService.registrarSalida(id);
            redirectAttributes.addFlashAttribute("success", "Salida registrada correctamente");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/visitantes";
    }

    private void prepararFormulario(Model model, Authentication authentication) {
        try {
            Residente residente = residenteService.buscarPorUsuarioEmail(authentication.getName());
            model.addAttribute("apartamento", residente.getApartamento());
        } catch (RuntimeException exception) {
            model.addAttribute("apartamento", null);
        }
        model.addAttribute("currentPath", "/visitantes");
        model.addAttribute("volverUrl", "/visitantes");
        model.addAttribute("titulo", "Solicitar acceso de visitante | Urbelix");
    }

    private boolean tieneRol(Authentication authentication, String rol) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + rol));
    }
}
