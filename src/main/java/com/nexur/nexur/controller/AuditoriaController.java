package com.nexur.nexur.controller;

import com.nexur.nexur.service.AuditoriaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/auditoria")
    public String listar(Model model) {
        model.addAttribute("auditorias", auditoriaService.listarRecientes());
        model.addAttribute("titulo", "Auditoria | Urbelix");
        model.addAttribute("currentPath", "/auditoria");
        model.addAttribute("volverUrl", "/dashboard");
        return "auditoria/lista";
    }
}
