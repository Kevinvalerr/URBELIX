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

import org.springframework.security.access.prepost.PreAuthorize;



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

    @GetMapping("/pagos")
    public String mostrarListaPagos(Model model) {
        model.addAttribute("titulo", "Pagos");
        model.addAttribute("currentPath", "/pagos");
        model.addAttribute("volverUrl", "/dashboard");
        model.addAttribute("pagos", pagoService.listarPagos());
        return "pagos/lista";
    }

    @GetMapping("/pagos/nuevo")
    public String mostrarFormularioPago(Model model) {
        model.addAttribute("titulo", "Registrar Pago");
        model.addAttribute("currentPath", "/pagos/nuevo");
        model.addAttribute("volverUrl", "/dashboard");
        model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
        model.addAttribute("pago", new Pago());
        return "pagos/nuevo";
    }

    @PostMapping("/pagos/guardar")
    public String guardarPago(@Valid @ModelAttribute("pago") Pago pago,
                              BindingResult bindingResult,
                              Model model) {
        if (pago.getApartamento() == null || pago.getApartamento().getId() == null) {
            bindingResult.rejectValue("apartamento", "NotNull", "Seleccione un apartamento");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("titulo", "Registrar Pago");
            model.addAttribute("currentPath", "/pagos/nuevo");
            model.addAttribute("volverUrl", "/dashboard");
            model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
            return "pagos/nuevo";
        }

        pagoService.guardar(pago, pago.getApartamento().getId());
        return "redirect:/pagos";
    }

    @GetMapping("/reservas")
    public String mostrarListaReservas(Model model) {
        model.addAttribute("titulo", "Reservas");
        model.addAttribute("currentPath", "/reservas");
        model.addAttribute("volverUrl", "/dashboard");
        model.addAttribute("reservas", reservaService.listarReservas());
        return "reservas/lista";
    }

    @GetMapping("/reservas/nueva")
    public String mostrarFormularioReserva(Model model) {
        model.addAttribute("titulo", "Crear Reserva");
        model.addAttribute("currentPath", "/reservas/nueva");
        model.addAttribute("volverUrl", "/dashboard");
        model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
        model.addAttribute("reserva", new Reserva());
        return "reservas/nueva";
    }

   @PostMapping("/reservas/guardar")
public String guardarReserva(@Valid @ModelAttribute("reserva") Reserva reserva,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication) {

    if (reserva.getApartamento() == null || reserva.getApartamento().getId() == null) {
        bindingResult.rejectValue("apartamento", "NotNull", "Seleccione un apartamento");
    }

    if (bindingResult.hasErrors()) {
        model.addAttribute("titulo", "Crear Reserva");
        model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
        return "reservas/nueva";
    }
String email = authentication.getName();
Residente residente = residenteService.obtenerTodos().stream()
    .filter(r -> r.getNombre().equalsIgnoreCase(email))
    .findFirst()
    .orElseGet(() -> {
        Residente nuevo = new Residente();
        nuevo.setNombre(email);
        return residenteService.guardar(nuevo, reserva.getApartamento().getId());
    });
reserva.setResidente(residente);

    reservaService.guardar(reserva, reserva.getApartamento().getId());

    
    redirectAttributes.addFlashAttribute("success", "Reserva creada exitosamente");

    return "redirect:/reservas";
}

@PreAuthorize("hasAuthority('ADMIN')")
@PostMapping("/reservas/aprobar/{id}")
public String aprobar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    reservaService.aprobarReserva(id);
    redirectAttributes.addFlashAttribute("success", "Reserva aprobada");
    return "redirect:/reservas";
}

@PreAuthorize("hasAuthority('ADMIN')")
@PostMapping("/reservas/rechazar/{id}")
public String rechazar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    reservaService.rechazarReserva(id);
    redirectAttributes.addFlashAttribute("error", "Reserva rechazada");
    return "redirect:/reservas";
}
}
