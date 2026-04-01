package com.nexur.nexur.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PagoReservaController {

    @GetMapping("/pagos")
    public String mostrarListaPagos(Model model) {
        model.addAttribute("titulo", "Pagos");
        model.addAttribute("currentPath", "/pagos");
        return "pagos/lista";
    }

    @GetMapping("/pagos/nuevo")
    public String mostrarFormularioPago(Model model) {
        model.addAttribute("currentPath", "/pagos/nuevo");
        return "pagos/nuevo";
    }

    @PostMapping("/pagos/guardar")
    public String guardarPago(
            @RequestParam String residente,
            @RequestParam String monto,
            @RequestParam String metodo,
            @RequestParam String fecha) {
        return "redirect:/pagos";
    }

    @GetMapping("/reservas")
    public String mostrarListaReservas(Model model) {
        model.addAttribute("titulo", "Reservas");
        model.addAttribute("currentPath", "/reservas");
        return "reservas/lista";
    }

    @GetMapping("/reservas/nueva")
    public String mostrarFormularioReserva(Model model) {
        model.addAttribute("currentPath", "/reservas/nueva");
        return "reservas/nueva";
    }

    @PostMapping("/reservas/guardar")
    public String guardarReserva(
            @RequestParam String solicitante,
            @RequestParam String area,
            @RequestParam String fecha,
            @RequestParam String observaciones) {
        return "redirect:/reservas";
    }
}
