package com.nexur.nexur.controller;

import com.nexur.nexur.service.VisitanteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/porteria")
@PreAuthorize("hasRole('PORTERIA')")
public class PorteriaController {

    private final VisitanteService visitanteService;

    public PorteriaController(VisitanteService visitanteService) {
        this.visitanteService = visitanteService;
    }

    @GetMapping
    public String dashboard(Model model) {
        var visitantesActivos = visitanteService.listarVisitantesActivos();
        model.addAttribute("visitantesActivos", visitantesActivos);
        model.addAttribute("visitantesActivosCount", visitantesActivos.size());
        model.addAttribute("visitantesTotal", visitanteService.listarVisitantes().size());
        model.addAttribute("solicitudesPendientes", visitanteService.listarSolicitudesPendientes().size());
        model.addAttribute("titulo", "Centro de control de portería");
        model.addAttribute("currentPath", "/porteria");
        model.addAttribute("volverUrl", "/dashboard");
        return "porteria/dashboard";
    }
}
