package com.nexur.nexur.controller;

import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Reserva;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.service.ApartamentoService;
import com.nexur.nexur.service.PagoService;
import com.nexur.nexur.service.ReservaService;
import com.nexur.nexur.service.ResidenteService;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;


@Controller
public class PagoReservaController {

    private final PagoService pagoService;
    private final ReservaService reservaService;
    private final ApartamentoService apartamentoService;
    private final ResidenteService residenteService;

    public PagoReservaController(PagoService pagoService, ReservaService reservaService, ApartamentoService apartamentoService, ResidenteService residenteService) {
        this.pagoService = pagoService;
        this.reservaService = reservaService;
        this.apartamentoService = apartamentoService;
        this.residenteService = residenteService;
    }




    @GetMapping("/reservas")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENTE')")
    public String mostrarListaReservas(Model model, Authentication authentication) {
        model.addAttribute("titulo", "Reservas");
        model.addAttribute("currentPath", "/reservas");
        model.addAttribute("volverUrl", "/dashboard");

        boolean esResidente = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_RESIDENTE"));
        if (esResidente) {
            try {
                Residente residente = residenteService.buscarPorUsuarioEmail(authentication.getName());
                model.addAttribute("reservas", reservaService.listarReservasPorResidente(residente.getId()));
            } catch (RuntimeException e) {
                model.addAttribute("reservas", List.of());
            }
        } else {
            model.addAttribute("reservas", reservaService.listarReservas());
        }
        return "reservas/lista";
    }

    @GetMapping("/reservas/nueva")
    @PreAuthorize("hasRole('RESIDENTE')")
    public String mostrarFormularioReserva(Model model, Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/reservas";
        }
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_RESIDENTE"))) {
            return "redirect:/dashboard";
        }

        model.addAttribute("titulo", "Crear Reserva");
        model.addAttribute("currentPath", "/reservas/nueva");
        model.addAttribute("volverUrl", "/dashboard");
        Residente residente;
        try {
            residente = residenteService.buscarPorUsuarioEmail(authentication.getName());
        } catch (RuntimeException e) {
            return "redirect:/dashboard";
        }
        if (residente.getApartamento() == null || residente.getApartamento().getId() == null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("apartamentos", List.of(residente.getApartamento()));
        model.addAttribute("reserva", new Reserva());
        return "reservas/nueva";
    }

   @PreAuthorize("hasRole('RESIDENTE')")
   @PostMapping("/reservas/guardar")
public String guardarReserva(@Valid @ModelAttribute("reserva") Reserva reserva,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication) {

    String email = authentication.getName();
    Residente residente;
    try {
        residente = residenteService.buscarPorUsuarioEmail(email);
    } catch (RuntimeException e) {
        redirectAttributes.addFlashAttribute("error", "No se encontró su perfil de residente. Contacte a un administrador para completar sus datos antes de reservar.");
        return "redirect:/dashboard";
    }

    Long apartamentoEnviado = reserva.getApartamento() == null ? null : reserva.getApartamento().getId();
    Long apartamentoAsignado = residente.getApartamento() == null ? null : residente.getApartamento().getId();
    if (apartamentoEnviado == null || apartamentoAsignado == null || !apartamentoAsignado.equals(apartamentoEnviado)) {
        bindingResult.rejectValue("apartamento", "Invalid", "Solo puede reservar con su apartamento asignado");
    }

    if (bindingResult.hasErrors()) {
        model.addAttribute("titulo", "Crear Reserva");
        model.addAttribute("currentPath", "/reservas/nueva");
        model.addAttribute("volverUrl", "/dashboard");
        model.addAttribute("apartamentos", residente.getApartamento() == null
                ? List.of() : List.of(residente.getApartamento()));
        return "reservas/nueva";
    }

    reserva.setResidente(residente);

    try {
        reservaService.guardar(reserva, apartamentoAsignado);
    } catch (RuntimeException e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/reservas/nueva";
    }

    redirectAttributes.addFlashAttribute("success", "Reserva creada exitosamente");

    return "redirect:/reservas";
}

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reservas/aprobar/{id}")
    public String aprobar(@PathVariable Long id,
                          @RequestParam(value = "comentario", required = false) String comentario,
                      RedirectAttributes redirectAttributes) {
    try {
        reservaService.aprobarReserva(id, comentario);
        redirectAttributes.addFlashAttribute("success", "Reserva aprobada");
    } catch (IllegalArgumentException exception) {
        redirectAttributes.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/reservas";
}

@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/reservas/rechazar/{id}")
    public String rechazar(@PathVariable Long id,
                       @RequestParam(value = "comentario", required = false) String comentario,
                       RedirectAttributes redirectAttributes) {
    try {
        reservaService.rechazarReserva(id, comentario);
        redirectAttributes.addFlashAttribute("success", "Reserva rechazada");
    } catch (IllegalArgumentException exception) {
        redirectAttributes.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/reservas";
}



}
