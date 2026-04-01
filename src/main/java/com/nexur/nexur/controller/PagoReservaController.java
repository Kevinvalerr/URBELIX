package com.nexur.nexur.controller;

import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Reserva;
import com.nexur.nexur.service.ApartamentoService;
import com.nexur.nexur.service.PagoService;
import com.nexur.nexur.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;

@Controller
public class PagoReservaController {

    private final PagoService pagoService;
    private final ReservaService reservaService;
    private final ApartamentoService apartamentoService;

    public PagoReservaController(PagoService pagoService, ReservaService reservaService, ApartamentoService apartamentoService) {
        this.pagoService = pagoService;
        this.reservaService = reservaService;
        this.apartamentoService = apartamentoService;
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
                                 Model model) {
        if (reserva.getApartamento() == null || reserva.getApartamento().getId() == null) {
            bindingResult.rejectValue("apartamento", "NotNull", "Seleccione un apartamento");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("titulo", "Crear Reserva");
            model.addAttribute("currentPath", "/reservas/nueva");
            model.addAttribute("volverUrl", "/dashboard");
            model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
            return "reservas/nueva";
        }

        reservaService.guardar(reserva, reserva.getApartamento().getId());
        return "redirect:/reservas";
    }
}
