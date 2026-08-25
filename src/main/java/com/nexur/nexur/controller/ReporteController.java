package com.nexur.nexur.controller;

import com.nexur.nexur.model.ReporteRegistro;
import com.nexur.nexur.service.ReporteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/reportes")
    public String verReportes(@RequestParam(required = false) String tipo,
                              @RequestParam(required = false) String fechaInicio,
                              @RequestParam(required = false) String fechaFin,
                              Model model) {
        LocalDate inicio = fechaInicio != null && !fechaInicio.isBlank() ? LocalDate.parse(fechaInicio) : null;
        LocalDate fin = fechaFin != null && !fechaFin.isBlank() ? LocalDate.parse(fechaFin) : null;

        List<ReporteRegistro> resultados = reporteService.filtrarRegistros(tipo, inicio, fin);

        model.addAttribute("titulo", "Reportes");
        model.addAttribute("currentPath", "/reportes");
        model.addAttribute("registros", resultados);
        model.addAttribute("tipo", tipo == null ? "TODOS" : tipo);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);

        return "reportes/lista";
    }
}
